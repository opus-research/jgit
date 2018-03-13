/*
 * Copyright (C) 2017, Google Inc.
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

package org.eclipse.jgit.internal.storage.reftable;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jgit.internal.storage.reftable.BlockWriter.compare;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.FILE_BLOCK_TYPE;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.FILE_HEADER_LEN;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.INDEX_BLOCK_TYPE;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.LOG_BLOCK_TYPE;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.OBJ_BLOCK_TYPE;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.REF_BLOCK_TYPE;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.VALUE_1ID;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.VALUE_2ID;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.VALUE_NONE;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.VALUE_TEXT;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.VALUE_TYPE_MASK;
import static org.eclipse.jgit.internal.storage.reftable.ReftableConstants.reverseUpdateIndex;
import static org.eclipse.jgit.lib.Constants.OBJECT_ID_LENGTH;
import static org.eclipse.jgit.lib.Constants.OBJECT_ID_STRING_LENGTH;
import static org.eclipse.jgit.lib.Ref.Storage.NEW;
import static org.eclipse.jgit.lib.Ref.Storage.PACKED;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.io.BlockSource;
import org.eclipse.jgit.lib.CheckoutEntry;
import org.eclipse.jgit.lib.InflaterCache;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.SymbolicRef;
import org.eclipse.jgit.util.IntList;
import org.eclipse.jgit.util.NB;
import org.eclipse.jgit.util.RawParseUtils;

/** Reads a single block for {@link ReftableReader}. */
class BlockReader {
	private byte blockType;
	private long endPosition;

	private byte[] buf;
	private int bufLen;
	private int ptr;

	private int keysStart;
	private int keysEnd;
	private int restartIdx;
	private int restartCount;

	private byte[] nameBuf = new byte[256];
	private int nameLen;
	private int valueType;

	byte type() {
		return blockType;
	}

	long endPosition() {
		return endPosition;
	}

	boolean next() {
		return ptr < keysEnd;
	}

	void parseKey() {
		int pfx = readVarint32();
		valueType = readVarint32();
		int sfx = valueType >>> 3;
		if (pfx + sfx > nameBuf.length) {
			int n = Math.max(pfx + sfx, nameBuf.length * 2);
			nameBuf = Arrays.copyOf(nameBuf, n);
		}
		System.arraycopy(buf, ptr, nameBuf, pfx, sfx);
		ptr += sfx;
		nameLen = pfx + sfx;
	}

	String name() {
		int len = nameLen;
		if (blockType == LOG_BLOCK_TYPE) {
			len -= 9;
		}
		return RawParseUtils.decode(UTF_8, nameBuf, 0, len);
	}

	boolean match(byte[] match, boolean matchIsPrefix) {
		int len = nameLen;
		if (blockType == LOG_BLOCK_TYPE) {
			len -= 9;
		}
		if (matchIsPrefix) {
			return len >= match.length
					&& compare(
							match, 0, match.length,
							nameBuf, 0, match.length) == 0;
		}
		return compare(match, 0, match.length, nameBuf, 0, len) == 0;
	}

	boolean matchAbbrevId(byte[] match) {
		int n = Math.min(match.length, nameLen);
		return compare(match, 0, n, nameBuf, 0, n) == 0;
	}

	long readIndex() throws IOException {
		if (blockType != INDEX_BLOCK_TYPE) {
			throw invalidBlock();
		}

		readVarint32(); // skip prefix length
		int n = readVarint32() >>> 3;
		ptr += n; // skip name
		return readVarint64();
	}

	Ref readRef() throws IOException {
		String name = RawParseUtils.decode(UTF_8, nameBuf, 0, nameLen);
		switch (valueType & VALUE_TYPE_MASK) {
		case VALUE_NONE: // delete
			return newRef(name);

		case VALUE_1ID:
			return new ObjectIdRef.PeeledNonTag(PACKED, name, readValueId());

		case VALUE_2ID: { // annotated tag
			ObjectId id1 = readValueId();
			ObjectId id2 = readValueId();
			return new ObjectIdRef.PeeledTag(PACKED, name, id1, id2);
		}

		case VALUE_TEXT: {
			String val = readValueString();
			if (val.startsWith("ref: ")) { //$NON-NLS-1$
				return new SymbolicRef(name, newRef(val.substring(5)));
			} else if (val.length() >= OBJECT_ID_STRING_LENGTH) {
				String idStr = val.substring(0, OBJECT_ID_STRING_LENGTH);
				if (ObjectId.isId(idStr)) {
					ObjectId id = ObjectId.fromString(idStr);
					return new ObjectIdRef.Unpeeled(PACKED, name, id);
				}
			}
			return newRef(name);
		}

		default:
			throw invalidBlock();
		}
	}

	IntList readBlockList() {
		int n = valueType & VALUE_TYPE_MASK;
		if (n == 0) {
			n = readVarint32();
		}
		IntList b = new IntList(n);
		b.add(readVarint32());
		for (int j = 1; j < n; j++) {
			int prior = b.get(j - 1);
			b.add(prior + readVarint32());
		}
		return b;
	}

	long readLogUpdateIndex() {
		return reverseUpdateIndex(NB.decodeUInt64(nameBuf, nameLen - 8));
	}

	ReflogEntry readLog() {
		ObjectId oldId = readValueId();
		ObjectId newId = readValueId();
		long ms = readVarint64() * 1000L;
		short tz = readInt16();
		String name = readValueString();
		String email = readValueString();
		String comment = readValueString();

		return new ReflogEntry() {
			@Override
			public ObjectId getOldId() {
				return oldId;
			}

			@Override
			public ObjectId getNewId() {
				return newId;
			}

			@Override
			public PersonIdent getWho() {
				return new PersonIdent(name, email, ms, tz);
			}

			@Override
			public String getComment() {
				return comment;
			}

			@Override
			public CheckoutEntry parseCheckout() {
				return null;
			}
		};
	}

	private ObjectId readValueId() {
		ObjectId id = ObjectId.fromRaw(buf, ptr);
		ptr += OBJECT_ID_LENGTH;
		return id;
	}

	private String readValueString() {
		int len = readVarint32();
		int end = ptr + len;
		String s = RawParseUtils.decode(UTF_8, buf, ptr, end);
		ptr = end;
		return s;
	}

	void readBlock(BlockSource src, long pos, int estBlockSize)
			throws IOException {
		readBlockIntoBuf(src, pos, estBlockSize);
		parseBlockStart(pos);
	}

	private void readBlockIntoBuf(BlockSource src, long pos, int size)
			throws IOException {
		ByteBuffer b = src.read(pos, size);
		bufLen = b.position();
		if (bufLen <= 0) {
			throw invalidBlock();
		}
		if (b.hasArray() && b.arrayOffset() == 0) {
			buf = b.array();
		} else {
			buf = new byte[bufLen];
			b.flip();
			b.get(buf);
		}
		endPosition = pos + bufLen;
	}

	private void parseBlockStart(long pos) throws IOException {
		ptr = 0;
		if (pos == 0) {
			if (bufLen == FILE_HEADER_LEN) {
				setupEmptyFileBlock();
				return;
			}
			ptr += FILE_HEADER_LEN; // first block begins with file header
		}

		int typeAndSize = NB.decodeInt32(buf, ptr);
		ptr += 4;

		blockType = (byte) (typeAndSize >>> 24);
		int blockLen;
		if ((blockType & INDEX_BLOCK_TYPE) == INDEX_BLOCK_TYPE) {
			// Index blocks are allowed to grow up to 31-bit blockSize.
			blockType = INDEX_BLOCK_TYPE;
			blockLen = decodeIndexSize(typeAndSize);
		} else {
			blockLen = typeAndSize & 0xffffff;
		}
		if (blockType == LOG_BLOCK_TYPE) {
			// Log blocks must be inflated after the header.
			long deflatedSize = inflateBuf(blockLen);
			endPosition = pos + 4 + deflatedSize;
		}
		if (bufLen < blockLen) {
			throw invalidBlock();
		} else if (bufLen > blockLen) {
			bufLen = blockLen;
		}

		keysStart = ptr;
		if (blockType != FILE_BLOCK_TYPE) {
			restartCount = NB.decodeUInt16(buf, bufLen - 2);
			restartIdx = bufLen - (restartCount * 3 + 2);
			keysEnd = restartIdx;
		} else {
			keysEnd = keysStart;
		}
	}

	static int decodeIndexSize(int typeAndSize) {
		return typeAndSize & 0x7fffffff;
	}

	private long inflateBuf(int blockLen) throws IOException {
		byte[] dst = new byte[4 + blockLen];
		System.arraycopy(buf, 0, dst, 0, 4);

		long deflatedSize = 0;
		Inflater inf = InflaterCache.get();
		try {
			inf.setInput(buf, ptr, bufLen - ptr);
			for (int o = 4;;) {
				int n = inf.inflate(dst, o, dst.length - o);
				o += n;
				if (inf.finished()) {
					deflatedSize = inf.getBytesRead();
					break;
				} else if (n <= 0) {
					throw invalidBlock();
				}
			}
		} catch (DataFormatException e) {
			throw invalidBlock(e);
		} finally {
			InflaterCache.release(inf);
		}

		buf = dst;
		bufLen = dst.length;
		return deflatedSize;
	}

	private void setupEmptyFileBlock() {
		// An empty reftable has only the file header in first block.
		blockType = FILE_BLOCK_TYPE;
		ptr = FILE_HEADER_LEN;
		restartCount = 0;
		restartIdx = bufLen;
		keysStart = bufLen;
		keysEnd = bufLen;
	}

	void verifyIndex() throws IOException {
		if (blockType != INDEX_BLOCK_TYPE) {
			throw invalidBlock();
		}
	}

	int seekKey(byte[] key) {
		return seek(key, true);
	}

	int seekAbbrevId(byte[] key) {
		return seek(key, false);
	}

	private static int compareKey(boolean useKeyLen, byte[] a,
			byte[] b, int bi, int bLen) {
		if (useKeyLen) {
			return compare(a, 0, a.length, b, bi, bLen);
		}
		int n = Math.min(bLen, a.length);
		return compare(a, 0, n, b, bi, n);
	}

	private int seek(byte[] key, boolean useKeyLen) {
		int low = 0;
		int end = restartCount;
		for (;;) {
			int mid = (low + end) >>> 1;
			int p = NB.decodeUInt24(buf, restartIdx + mid * 3);
			ptr = p + 1; // skip 0 prefix length
			int n = readVarint32() >>> 3;
			int cmp = compareKey(useKeyLen, key, buf, ptr, n);
			if (cmp < 0) {
				end = mid;
			} else if (cmp == 0) {
				ptr = p;
				return 0;
			} else /* if (cmp > 0) */ {
				low = mid + 1;
			}
			if (low >= end) {
				return seekToKey(key, useKeyLen, p, low, cmp);
			}
		}
	}

	private int seekToKey(byte[] key, boolean useKeyLen,
			int rPtr, int rIdx, int rCmp) {
		if (rCmp < 0) {
			if (rIdx == 0) {
				ptr = keysStart;
				return -1;
			}
			ptr = NB.decodeUInt24(buf, restartIdx + (rIdx - 1) * 3);
		} else {
			ptr = rPtr;
		}

		int cmp;
		do {
			int savePtr = ptr;
			parseKey();
			cmp = compareKey(useKeyLen, key, nameBuf, 0, nameLen);
			if (cmp <= 0) {
				// cmp < 0, name should be in this block, but is not.
				// cmp = 0, block is positioned at name.
				ptr = savePtr;
				return cmp < 0 && savePtr == keysStart ? -1 : 0;
			}
			skipValue();
		} while (ptr < keysEnd);
		return cmp;
	}

	void skipValue() {
		switch (blockType) {
		case REF_BLOCK_TYPE:
			switch (valueType & VALUE_TYPE_MASK) {
			case VALUE_NONE:
				return;
			case VALUE_1ID:
				ptr += OBJECT_ID_LENGTH;
				return;
			case VALUE_2ID:
				ptr += 2 * OBJECT_ID_LENGTH;
				return;
			case VALUE_TEXT:
				skipString();
				return;
			}
			break;

		case OBJ_BLOCK_TYPE: {
			int n = valueType & VALUE_TYPE_MASK;
			if (n == 0) {
				n = readVarint32();
			}
			while (n-- > 0) {
				readVarint32();
			}
			return;
		}

		case INDEX_BLOCK_TYPE:
			readVarint32();
			return;

		case LOG_BLOCK_TYPE:
			ptr += 2 * OBJECT_ID_LENGTH; // 2x id;
			readVarint64(); // time
			ptr += 2; // 2-byte tz
			skipString(); // name
			skipString(); // email
			skipString(); // comment
			return;
		}

		throw new IllegalStateException();
	}

	private void skipString() {
		int n = readVarint32(); // string length
		ptr += n;
	}

	private short readInt16() {
		return (short) NB.decodeUInt16(buf, ptr += 2);
	}

	private int readVarint32() {
		byte c = buf[ptr++];
		int val = c & 0x7f;
		while ((c & 0x80) != 0) {
			c = buf[ptr++];
			val++;
			val <<= 7;
			val |= (c & 0x7f);
		}
		return val;
	}

	private long readVarint64() {
		byte c = buf[ptr++];
		long val = c & 0x7f;
		while ((c & 0x80) != 0) {
			c = buf[ptr++];
			val++;
			val <<= 7;
			val |= (c & 0x7f);
		}
		return val;
	}

	private static Ref newRef(String name) {
		return new ObjectIdRef.Unpeeled(NEW, name, null);
	}

	private static IOException invalidBlock() {
		return invalidBlock(null);
	}

	private static IOException invalidBlock(Throwable cause) {
		return new IOException(JGitText.get().invalidReftableBlock, cause);
	}
}
