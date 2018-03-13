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

package org.eclipse.jgit.internal.storage.dfs;

/** IO statistics for a {@link DfsReader}. */
public class DfsReaderIoStats {
	/** POJO to accumulate IO statistics. */
	public static class Accumulator {
		/** Number of times the reader explicitly called scanPacks. */
		long scanPacks;

		/** Total number of complete pack indexes read into memory. */
		long readIdx;

		/** Total number of complete bitmap indexes read into memory. */
		long readBitmap;

		/** Total number of bytes read from indexes. */
		long readIdxBytes;

		/** Total microseconds spent reading pack or bitmap indexes. */
		long readIdxMicros;

		/** Total number of block cache hits. */
		long blockCacheHit;

		/** Total number of discrete blocks read from pack file(s). */
		long readBlock;

		/** Total number of compressed bytes read as block sized units. */
		long readBlockBytes;

		/** Total microseconds spent reading {@link #readBlock} blocks. */
		long readBlockMicros;

		/** Total number of bytes decompressed. */
		long inflatedBytes;

		Accumulator() {
		}
	}

	private final Accumulator stats;

	DfsReaderIoStats(Accumulator stats) {
		this.stats = stats;
	}

	/** @return number of times the reader explicitly called scanPacks. */
	public long getScanPacks() {
		return stats.scanPacks;
	}

	/** @return total number of complete pack indexes read into memory. */
	public long getReadPackIndexCount() {
		return stats.readIdx;
	}

	/** @return total number of complete bitmap indexes read into memory. */
	public long getReadBitmapIndexCount() {
		return stats.readBitmap;
	}

	/** @return total number of bytes read from indexes. */
	public long getReadIndexBytes() {
		return stats.readIdxBytes;
	}

	/** @return total microseconds spent reading pack or bitmap indexes. */
	public long getReadIndexMicros() {
		return stats.readIdxMicros;
	}

	/** @return total number of block cache hits. */
	public long getBlockCacheHits() {
		return stats.blockCacheHit;
	}

	/** @return total number of discrete blocks read from pack file(s). */
	public long getReadBlocksCount() {
		return stats.readBlock;
	}

	/** @return total number of compressed bytes read as block sized units. */
	public long getReadBlocksBytes() {
		return stats.readBlockBytes;
	}

	/** @return total microseconds spent reading blocks. */
	public long getReadBlocksMicros() {
		return stats.readBlockMicros;
	}

	/** @return total number of bytes decompressed. */
	public long getInflatedBytes() {
		return stats.inflatedBytes;
	}
}
