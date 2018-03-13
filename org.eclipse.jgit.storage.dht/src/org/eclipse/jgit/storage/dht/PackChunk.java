/*
 * Copyright (C) 2011, Google Inc.
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

package org.eclipse.jgit.storage.dht;

import static org.eclipse.jgit.storage.dht.ChunkFormatter.TRAILER_SIZE;

import java.io.IOException;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.StoredObjectRepresentationNotAvailableException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.storage.pack.BinaryDelta;
import org.eclipse.jgit.storage.pack.PackOutputStream;
import org.eclipse.jgit.transport.PackParser;

/**
 * Chunk of object data, stored under a {@link ChunkKey}.
 * <p>
 * A chunk typically contains thousands of objects, compressed in the Git native
 * pack file format. Its associated {@link ChunkIndex} provides offsets for each
 * object's header and compressed data.
 * <p>
 * Chunks (and their indexes) are opaque binary blobs meant only to be read by
 * the Git implementation.
 */
public final class PackChunk {
	/** Constructs a {@link PackChunk} while reading from the DHT. */
	public static class Members {
		private ChunkKey chunkKey;

		private byte[] chunkData;

		private byte[] chunkIndex;

		private ChunkMeta meta;

		/** @return the chunk key. Never null. */
		public ChunkKey getChunkKey() {
			return chunkKey;
		}

		/**
		 * @param key
		 * @return {@code this}
		 */
		public Members setChunkKey(ChunkKey key) {
			this.chunkKey = key;
			return this;
		}

		/** @return the chunk data, or null if not available. */
		public byte[] getChunkData() {
			return chunkData;
		}

		/**
		 * @param chunkData
		 * @return {@code this}
		 */
		public Members setChunkData(byte[] chunkData) {
			this.chunkData = chunkData;
			return this;
		}

		/** @return the chunk index, or null if not available. */
		public byte[] getChunkIndex() {
			return chunkIndex;
		}

		/**
		 * @param chunkIndex
		 * @return {@code this}
		 */
		public Members setChunkIndex(byte[] chunkIndex) {
			this.chunkIndex = chunkIndex;
			return this;
		}

		/** @return the inline meta data, or null if not available. */
		public ChunkMeta getMeta() {
			return meta;
		}

		/**
		 * @param meta
		 * @return {@code this}
		 */
		public Members setMeta(ChunkMeta meta) {
			this.meta = meta;
			return this;
		}

		/**
		 * @return the PackChunk instance.
		 * @throws DhtException
		 *             if early validation indicates the chunk data is corrupt
		 *             or not recognized by this version of the library.
		 */
		public PackChunk build() throws DhtException {
			ChunkIndex i;
			if (chunkIndex != null)
				i = ChunkIndex.fromBytes(chunkKey, chunkIndex);
			else
				i = null;

			return new PackChunk(chunkKey, chunkData, i, meta);
		}
	}

	private final ChunkKey key;

	private final byte[] chunk;

	private final ChunkIndex index;

	private final ChunkMeta meta;

	private volatile Boolean valid;

	private volatile ChunkKey nextFragment;

	PackChunk(ChunkKey key, byte[] chunk, ChunkIndex index, ChunkMeta meta) {
		this.key = key;
		this.chunk = chunk;
		this.index = index;
		this.meta = meta;
	}

	/** @return unique name of this chunk in the database. */
	public ChunkKey getChunkKey() {
		return key;
	}

	/** @return index describing the objects stored within this chunk. */
	public ChunkIndex getIndex() {
		return index;
	}

	/** @return inline meta information, or null if no data was necessary. */
	public ChunkMeta getMeta() {
		return meta;
	}

	@Override
	public String toString() {
		return "PackChunk[" + getChunkKey() + "]";
	}

	boolean hasIndex() {
		return index != null;
	}

	int findOffset(RepositoryKey repo, AnyObjectId objId) {
		if (key.isFor(repo))
			return index.findOffset(objId);
		return -1;
	}

	boolean contains(RepositoryKey repo, AnyObjectId objId) {
		return 0 <= findOffset(repo, objId);
	}

	@SuppressWarnings("null")
	static ObjectLoader read(PackChunk pc, int pos, final DhtReader ctx,
			final int typeHint) throws IOException {
		try {
			Delta delta = null;
			byte[] data = null;
			int type = Constants.OBJ_BAD;
			boolean cached = false;

			SEARCH: for (;;) {
				if (pc.meta != null && pc.meta.getFragmentCount() != 0)
					throw new DhtException.TODO("read fragmented objects");

				final byte[] chunkData = pc.chunk;
				int c = chunkData[pos] & 0xff;
				int typeCode = (c >> 4) & 7;
				long sz = c & 15;
				int shift = 4;
				int p = 1;
				while ((c & 0x80) != 0) {
					c = chunkData[pos + p++] & 0xff;
					sz += (c & 0x7f) << shift;
					shift += 7;
				}

				switch (typeCode) {
				case Constants.OBJ_COMMIT:
				case Constants.OBJ_TREE:
				case Constants.OBJ_BLOB:
				case Constants.OBJ_TAG: {
					if (sz < Integer.MAX_VALUE)
						data = pc.decompress(pos + p, (int) sz, ctx);

					if (delta != null) {
						type = typeCode;
						break SEARCH;
					}

					if (data != null)
						return new ObjectLoader.SmallObject(typeCode, data);
					else
						return new LargeObject(chunkData, typeCode, sz, pos + p);
				}

				case Constants.OBJ_OFS_DELTA: {
					c = chunkData[pos + p++] & 0xff;
					long base = c & 127;
					while ((c & 128) != 0) {
						base += 1;
						c = chunkData[pos + p++] & 0xff;
						base <<= 7;
						base += (c & 127);
					}

					ChunkKey baseChunkKey;
					int basePosInChunk;

					if (base <= pos) {
						// Base occurs in the same chunk, just earlier.
						baseChunkKey = pc.getChunkKey();
						basePosInChunk = pos - (int) base;
					} else {
						// Long offset delta, base occurs in another chunk.
						// Adjust distance to be from our chunk start.
						base = base - pos;

						ChunkMeta.BaseChunk baseChunk;
						baseChunk = pc.meta.getBaseChunk(base);
						baseChunkKey = baseChunk.getChunkKey();
						basePosInChunk = (int) (baseChunk.relativeStart - base);
					}

					delta = new Delta(delta, //
							pc.key, pos, (int) sz, p, //
							baseChunkKey, basePosInChunk);
					if (sz != delta.deltaSize)
						break SEARCH;

					DeltaBaseCache.Entry e = delta.getBase(ctx);
					if (e != null) {
						type = e.type;
						data = e.data;
						cached = true;
						break SEARCH;
					}
					if (baseChunkKey != pc.getChunkKey())
						pc = ctx.getChunk(baseChunkKey);
					pos = basePosInChunk;
					continue SEARCH;
				}

				case Constants.OBJ_REF_DELTA: {
					ObjectId id = ObjectId.fromRaw(chunkData, pos + p);
					PackChunk nc = pc;
					int base = pc.index.findOffset(id);
					if (base < 0) {
						DhtReader.ChunkAndOffset n = ctx.getChunk(id, typeHint);
						nc = n.chunk;
						base = n.offset;
					}
					delta = new Delta(delta, //
							pc.key, pos, (int) sz, p + 20, //
							nc.getChunkKey(), base);
					if (sz != delta.deltaSize)
						break SEARCH;

					DeltaBaseCache.Entry e = delta.getBase(ctx);
					if (e != null) {
						type = e.type;
						data = e.data;
						cached = true;
						break SEARCH;
					}
					pc = nc;
					pos = base;
					continue SEARCH;
				}

				default:
					throw new DhtException(MessageFormat.format(
							DhtText.get().unsupportedObjectTypeInChunk, //
							Integer.valueOf(typeCode), //
							pc.getChunkKey(), //
							Integer.valueOf(pos)));
				}
			}

			// At this point there is at least one delta to apply to data.
			// (Whole objects with no deltas to apply return early above.)

			if (data == null)
				return delta.large();

			do {
				if (!delta.deltaChunk.equals(pc.getChunkKey()))
					pc = ctx.getChunk(delta.deltaChunk);
				pos = delta.deltaPos;

				// Cache only the base immediately before desired object.
				if (cached)
					cached = false;
				else if (delta.next == null)
					delta.putBase(ctx, type, data);

				final byte[] cmds = delta.decompress(pc, ctx);
				if (cmds == null) {
					data = null; // Discard base in case of OutOfMemoryError
					return delta.large();
				}

				final long sz = BinaryDelta.getResultSize(cmds);
				if (Integer.MAX_VALUE <= sz)
					return delta.large();

				final byte[] result;
				try {
					result = new byte[(int) sz];
				} catch (OutOfMemoryError tooBig) {
					data = null; // Discard base in case of OutOfMemoryError
					return delta.large();
				}

				BinaryDelta.apply(data, cmds, result);
				data = result;
				delta = delta.next;
			} while (delta != null);

			return new ObjectLoader.SmallObject(type, data);

		} catch (DataFormatException dfe) {
			CorruptObjectException coe = new CorruptObjectException(
					MessageFormat.format(DhtText.get().corruptCompressedObject,
							pc.getChunkKey(), Integer.valueOf(pos)));
			coe.initCause(dfe);
			throw coe;
		}
	}

	byte[] decompress(int pos, int sz, DhtReader reader)
			throws DataFormatException {
		byte[] dstbuf;
		try {
			dstbuf = new byte[sz];
		} catch (OutOfMemoryError noMemory) {
			// The size may be larger than our heap allows, return null to
			// let the caller know allocation isn't possible and it should
			// use the large object streaming approach instead.
			//
			// For example, this can occur when sz is 640 MB, and JRE
			// maximum heap size is only 256 MB. Even if the JRE has
			// 200 MB free, it cannot allocate a 640 MB byte array.
			return null;
		}

		// Because the chunk ends in a 4 byte CRC, there is always
		// more data available for input than the inflater needs.
		// This also helps with an optimization in libz where it
		// wants at least 1 extra byte of input beyond the end.

		final Inflater inf = reader.inflater();
		final int stride = 512;
		int dstoff = 0;

		int bs = Math.min(chunk.length - pos, stride);
		inf.setInput(chunk, pos, bs);
		pos += bs;

		while (dstoff < dstbuf.length) {
			int n = inf.inflate(dstbuf, dstoff, dstbuf.length - dstoff);
			if (n == 0) {
				if (inf.needsInput()) {
					bs = Math.min(chunk.length - pos, stride);
					inf.setInput(chunk, pos, bs);
					pos += bs;
					continue;
				}
				break;
			}
			dstoff += n;
		}

		if (dstoff != sz)
			throw new DataFormatException(MessageFormat.format(
					DhtText.get().shortCompressedObject, key, Integer
							.valueOf(pos)));
		return dstbuf;
	}

	int readObjectTypeAndSize(int ptr, PackParser.ObjectTypeAndSize info) {
		int c = chunk[ptr++] & 0xff;
		int typeCode = (c >> 4) & 7;
		long sz = c & 15;
		int shift = 4;
		while ((c & 0x80) != 0) {
			c = chunk[ptr++] & 0xff;
			sz += (c & 0x7f) << shift;
			shift += 7;
		}

		switch (typeCode) {
		case Constants.OBJ_OFS_DELTA:
			c = chunk[ptr++] & 0xff;
			while ((c & 128) != 0)
				c = chunk[ptr++] & 0xff;
			break;

		case Constants.OBJ_REF_DELTA:
			ptr += 20;
			break;
		}

		info.type = typeCode;
		info.size = sz;
		return ptr;
	}

	int read(int ptr, byte[] dst, int dstPos, int cnt) {
		// Do not allow readers to read the CRC-32 from the tail.
		int n = Math.min(cnt, (chunk.length - 4) - ptr);
		System.arraycopy(chunk, ptr, dst, dstPos, n);
		return n;
	}

	static void copyAsIs(PackChunk pc, PackOutputStream out,
			DhtObjectToPack obj, DhtReader ctx) throws IOException,
			StoredObjectRepresentationNotAvailableException {
		if (ctx.getOptions().isValidateOnCopyAsIs() && !pc.isValid()) {
			StoredObjectRepresentationNotAvailableException gone;

			gone = new StoredObjectRepresentationNotAvailableException(obj);
			gone.initCause(new DhtException(MessageFormat.format(
					DhtText.get().corruptChunk, pc.getChunkKey())));
			throw gone;
		}

		byte[] chunk = pc.chunk;
		int ptr = obj.offset;
		int c = chunk[ptr++] & 0xff;
		int typeCode = (c >> 4) & 7;
		long inflatedSize = c & 15;
		int shift = 4;
		while ((c & 0x80) != 0) {
			c = chunk[ptr++] & 0xff;
			inflatedSize += (c & 0x7f) << shift;
			shift += 7;
		}

		switch (typeCode) {
		case Constants.OBJ_OFS_DELTA:
			do {
				c = chunk[ptr++] & 0xff;
			} while ((c & 128) != 0);
			break;

		case Constants.OBJ_REF_DELTA:
			ptr += 20;
			break;
		}

		// If the size is positive, its accurate. If its -1, this is a
		// fragmented object that will need more handling below,
		// so copy all of the chunk, minus the trailer.

		final int copyLen;
		if (0 < obj.size)
			copyLen = obj.size;
		else if (-1 == obj.size)
			copyLen = (chunk.length - TRAILER_SIZE) - ptr;
		else
			throw new DhtException(MessageFormat.format(
					DhtText.get().expectedObjectSizeDuringCopyAsIs, obj));
		out.writeHeader(obj, inflatedSize);
		out.write(chunk, ptr, copyLen);

		// If the object was fragmented, send all of the other fragments.
		if (pc.meta != null && pc.meta.getFragmentCount() != 0) {
			int cnt = pc.meta.getFragmentCount();
			for (int fragId = 1; fragId < cnt; fragId++) {
				pc = ctx.getChunk(pc.meta.getFragmentKey(fragId));
				pc.copyEntireChunkAsIs(out, obj, ctx);
			}
		}
	}

	void copyEntireChunkAsIs(PackOutputStream out, DhtObjectToPack obj,
			DhtReader ctx) throws IOException {
		if (ctx.getOptions().isValidateOnCopyAsIs() && !isValid()) {
			if (obj != null)
				throw new CorruptObjectException(obj, MessageFormat.format(
						DhtText.get().corruptChunk, getChunkKey()));
			else
				throw new DhtException(MessageFormat.format(
						DhtText.get().corruptChunk, getChunkKey()));
		}

		// Do not copy the trailer onto the output stream.
		out.write(chunk, 0, chunk.length - TRAILER_SIZE);
	}

	@SuppressWarnings("boxing")
	private boolean isValid() {
		Boolean v = valid;
		if (v == null) {
			MessageDigest m = Constants.newMessageDigest();
			m.update(chunk, 0, chunk.length);
			v = key.getChunkHash().compareTo(m.digest(), 0) == 0;
			valid = v;
		}
		return v.booleanValue();
	}

	/** @return the complete size of this chunk, in memory. */
	int getTotalSize() {
		int sz = chunk.length;
		if (index != null)
			sz += index.getIndexSize();
		return sz;
	}

	ChunkKey getNextFragment() {
		if (meta == null)
			return null;

		ChunkKey next = nextFragment;
		if (next == null) {
			next = meta.getNextFragment(getChunkKey());
			nextFragment = next;
		}
		return next;
	}

	private static class Delta {
		/** Child that applies onto this object. */
		final Delta next;

		/** The chunk the delta is stored in. */
		final ChunkKey deltaChunk;

		/** Offset of the delta object. */
		final int deltaPos;

		/** Size of the inflated delta stream. */
		final int deltaSize;

		/** Total size of the delta's pack entry header (including base). */
		final int hdrLen;

		/** The chunk the base is stored in. */
		final ChunkKey baseChunk;

		/** Offset of the base object. */
		final int basePos;

		Delta(Delta next, ChunkKey dc, int ofs, int sz, int hdrLen,
				ChunkKey bc, int bp) {
			this.next = next;
			this.deltaChunk = dc;
			this.deltaPos = ofs;
			this.deltaSize = sz;
			this.hdrLen = hdrLen;
			this.baseChunk = bc;
			this.basePos = bp;
		}

		byte[] decompress(PackChunk chunk, DhtReader reader)
				throws DataFormatException {
			return chunk.decompress(deltaPos + hdrLen, deltaSize, reader);
		}

		DeltaBaseCache.Entry getBase(DhtReader ctx) {
			return ctx.getDeltaBaseCache().get(baseChunk, basePos);
		}

		void putBase(DhtReader ctx, int type, byte[] data) {
			ctx.getDeltaBaseCache().put(baseChunk, basePos, type, data);
		}

		ObjectLoader large() {
			Delta d = this;
			while (d.next != null)
				d = d.next;
			return d.newLargeLoader();
		}

		private ObjectLoader newLargeLoader() {
			throw new DhtException.TODO("large delta support");
		}
	}
}
