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

package org.eclipse.jgit.storage.dfs;

import java.util.Set;

import org.eclipse.jgit.lib.ObjectId;

/**
 * Description of a DFS stored pack/index file.
 * <p>
 * Implementors may extend this class and add additional data members.
 * <p>
 * Instances of this class are cached with the DfsPackFile, and should not be
 * modified once initialized and presented to the JGit DFS library.
 */
public class DfsPackDescription implements Comparable<DfsPackDescription> {
	private final String packName;

	private long lastModified;

	private long packSize;

	private long indexSize;

	private long objectCount;

	private long deltaCount;

	private Set<ObjectId> tips;

	/**
	 * Initialize a description by pack name.
	 * <p>
	 * The corresponding index file is assumed to exist and end with ".idx"
	 * instead of ".pack". If this is not true implementors must extend the
	 * class and override {@link #getIndexName()}.
	 * <p>
	 * Callers should also try to fill in other fields if they are reasonably
	 * free to access at the time this instance is being initialized.
	 *
	 * @param name
	 *            name of the pack file. Must end with ".pack".
	 */
	public DfsPackDescription(String name) {
		this.packName = name;
	}

	/** @return name of the pack file. */
	public String getPackName() {
		return packName;
	}

	/** @return name of the index file. */
	public String getIndexName() {
		String name = getPackName();
		int dot = name.lastIndexOf('.');
		if (dot < 0)
			dot = name.length();
		return name.substring(0, dot) + ".idx";
	}

	/** @return time the pack was created, in milliseconds. */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * @param timeMillis
	 *            time the pack was created, in milliseconds. 0 if not known.
	 * @return {@code this}
	 */
	public DfsPackDescription setLastModified(long timeMillis) {
		lastModified = timeMillis;
		return this;
	}

	/** @return size of the pack, in bytes. If 0 the pack size is not yet known. */
	public long getPackSize() {
		return packSize;
	}

	/**
	 * @param bytes
	 *            size of the pack in bytes. If 0 the size is not known and will
	 *            be determined on first read.
	 * @return {@code this}
	 */
	public DfsPackDescription setPackSize(long bytes) {
		packSize = Math.max(0, bytes);
		return this;
	}

	/**
	 * @return size of the index, in bytes. If 0 the index size is not yet
	 *         known.
	 */
	public long getIndexSize() {
		return indexSize;
	}

	/**
	 * @param bytes
	 *            size of the index in bytes. If 0 the size is not known and
	 *            will be determined on first read.
	 * @return {@code this}
	 */
	public DfsPackDescription setIndexSize(long bytes) {
		indexSize = Math.max(0, bytes);
		return this;
	}

	/** @return number of objects in the pack. */
	public long getObjectCount() {
		return objectCount;
	}

	/**
	 * @param cnt
	 *            number of objects in the pack.
	 * @return {@code this}
	 */
	public DfsPackDescription setObjectCount(long cnt) {
		objectCount = Math.max(0, cnt);
		return this;
	}

	/** @return number of delta compressed objects in the pack. */
	public long getDeltaCount() {
		return deltaCount;
	}

	/**
	 * @param cnt
	 *            number of delta compressed objects in the pack.
	 * @return {@code this}
	 */
	public DfsPackDescription setDeltaCount(long cnt) {
		deltaCount = Math.max(0, cnt);
		return this;
	}

	/** @return the tips that created this pack, if known. */
	public Set<ObjectId> getTips() {
		return tips;
	}

	/**
	 * @param tips
	 *            the tips of the pack, null if it has no known tips.
	 * @return {@code this}
	 */
	public DfsPackDescription setTips(Set<ObjectId> tips) {
		this.tips = tips;
		return this;
	}

	@Override
	public int hashCode() {
		return getPackName().hashCode();
	}

	@Override
	public boolean equals(Object b) {
		if (b instanceof DfsPackDescription)
			return getPackName().equals(((DfsPackDescription) b).getPackName());
		return false;
	}

	/**
	 * Sort packs according to the optimal lookup ordering.
	 * <p>
	 * This method tries to position packs in the order readers should examine
	 * them when looking for objects by SHA-1. The default tries to sort packs
	 * with more recent modification dates before older packs, and packs with
	 * fewer objects before packs with more objects.
	 *
	 * @param b
	 *            the other pack.
	 */
	public int compareTo(DfsPackDescription b) {
		// Newer packs should sort first.
		int cmp = Long.signum(b.getLastModified() - getLastModified());
		if (cmp != 0)
			return cmp;

		// Break ties on smaller index first. Readers may get lucky and find
		// the object they care about in the smaller index. This also pushes
		// big historical packs to the end of the list, due to more objects.
		cmp = Long.signum(getObjectCount() - b.getObjectCount());
		if (cmp != 0)
			return cmp;

		cmp = Long.signum(getIndexSize() - b.getIndexSize());
		if (cmp != 0)
			return cmp;

		// Break remaining ties on pack name.
		return getPackName().compareTo(b.getPackName());
	}

	@Override
	public String toString() {
		return getPackName();
	}
}
