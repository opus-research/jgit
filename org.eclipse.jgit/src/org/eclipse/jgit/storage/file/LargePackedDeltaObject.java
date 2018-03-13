/*
 * Copyright (C) 2010, Google Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.storage.file;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.InflaterInputStream;

import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.storage.pack.BinaryDelta;
import org.eclipse.jgit.storage.pack.DeltaStream;
import org.eclipse.jgit.util.io.CachingSeekableInputStream;
import org.eclipse.jgit.util.io.SeekableInputStream;

class LargePackedDeltaObject extends ObjectLoader {
	private static final long SIZE_UNKNOWN = -1;

	private int type;

	private long size;

	private final long objectOffset;

	private final long baseOffset;

	private final int headerLength;

	private final PackFile pack;

	private final FileObjectDatabase db;

	LargePackedDeltaObject(long objectOffset, long baseOffset,
			int headerLength, PackFile pack, FileObjectDatabase db) {
		this.type = Constants.OBJ_BAD;
		this.size = SIZE_UNKNOWN;
		this.objectOffset = objectOffset;
		this.baseOffset = baseOffset;
		this.headerLength = headerLength;
		this.pack = pack;
		this.db = db;
	}

	@Override
	public int getType() {
		if (type == Constants.OBJ_BAD) {
			WindowCursor wc = new WindowCursor(db);
			try {
				type = pack.getObjectType(wc, objectOffset);
			} catch (IOException packGone) {
				// If the pack file cannot be pinned into the cursor, it
				// probably was repacked recently. Go find the object
				// again and get the type from that location instead.
				//
				try {
					type = wc.open(getObjectId()).getType();
				} catch (IOException packGone2) {
					// "He's dead, Jim." We just can't discover the type
					// and the interface isn't supposed to be lazy here.
					// Report an invalid type code instead, callers will
					// wind up bailing out with an error at some point.
				}
			} finally {
				wc.release();
			}
		}
		return type;
	}

	@Override
	public long getSize() {
		if (size == SIZE_UNKNOWN) {
			WindowCursor wc = new WindowCursor(db);
			try {
				byte[] b = pack.getDeltaHeader(wc, objectOffset + headerLength);
				size = BinaryDelta.getResultSize(b);
			} catch (DataFormatException objectCorrupt) {
				// The zlib stream for the delta is corrupt. We probably
				// cannot access the object. Keep the size negative and
				// report that bogus result to the caller.
			} catch (IOException packGone) {
				// If the pack file cannot be pinned into the cursor, it
				// probably was repacked recently. Go find the object
				// again and get the size from that location instead.
				//
				try {
					size = wc.open(getObjectId()).getSize();
				} catch (IOException packGone2) {
					// "He's dead, Jim." We just can't discover the size
					// and the interface isn't supposed to be lazy here.
					// Report an invalid type code instead, callers will
					// wind up bailing out with an error at some point.
				}
			} finally {
				wc.release();
			}
		}
		return size;
	}

	@Override
	public boolean isLarge() {
		return true;
	}

	@Override
	public byte[] getCachedBytes() throws LargeObjectException {
		try {
			throw new LargeObjectException(getObjectId());
		} catch (IOException cannotObtainId) {
			LargeObjectException err = new LargeObjectException();
			err.initCause(cannotObtainId);
			throw err;
		}
	}

	@Override
	public ObjectStream openStream() throws MissingObjectException, IOException {
		final WindowCursor wc = new WindowCursor(db);
		final InputStream delta;
		try {
			delta = new PackInputStream(pack, objectOffset + headerLength, wc);
		} catch (IOException packGone) {
			// If the pack file cannot be pinned into the cursor, it
			// probably was repacked recently. Go find the object
			// again and open the stream from that location instead.
			//
			return wc.open(getObjectId()).openStream();
		}

		ObjectStream os = pack.load(wc, baseOffset).openStream();
		if (type == Constants.OBJ_BAD)
			type = os.getType();

		final int bs = 2048;
		SeekableInputStream base = new CachingSeekableInputStream(
				new LargeDeltaBaseInputStream(os, pack, baseOffset, wc, bs), //
				bs, //
				wc.getStreamFileThreshold());

		DeltaStream ds = new DeltaStream(new InflaterInputStream(delta), base);
		if (size == SIZE_UNKNOWN)
			size = ds.getSize();

		final InputStream in = new BufferedInputStream(ds, 8192);
		return new ObjectStream.Filter(getType(), getSize(), in) {
			@Override
			public void close() throws IOException {
				super.close();
				wc.release();
			}
		};
	}

	private ObjectId getObjectId() throws IOException {
		return pack.findObjectForOffset(objectOffset);
	}
}
