/*
 * Copyright (C) 2016, Christian Halstrick <christian.halstrick@sap.com>
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
package org.eclipse.jgit.lfs;

import java.io.OutputStream;
import java.io.PrintStream;

import org.eclipse.jgit.lfs.lib.Constants;
import org.eclipse.jgit.lfs.lib.LongObjectId;

/**
 * Represents an LFS pointer file
 *
 * @since 4.6
 */
public class LfsPointer {
	/**
	 * The version of the LfsPointer file format
	 */
	public static final String VERSION = "https://git-lfs.github.com/spec/v1"; //$NON-NLS-1$

	/**
	 * The name of the hash function as used in the pointer files. This will
	 * evaluate to "sha256"
	 */
	public static final String HASH_FUNCTION_NAME = Constants.LONG_HASH_FUNCTION
			.toLowerCase().replace("-", ""); //$NON-NLS-1$ //$NON-NLS-2$

	private LongObjectId oid;

	private long size;

	/**
	 * @param oid
	 *            the id of the content
	 * @param size
	 *            the size of the content
	 */
	public LfsPointer(LongObjectId oid, long size) {
		this.oid = oid;
		this.size = size;
	}

	/**
	 * @return the id of the content
	 */
	public LongObjectId getOid() {
		return oid;
	}

	/**
	 * @return the size of the content
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Encode this object into the LFS format defined by {@link #VERSION}
	 *
	 * @param out
	 *            the {@link OutputStream} into which the encoded data should be
	 *            written
	 */
	public void encode(OutputStream out) {
		try (PrintStream ps = new PrintStream(out)) {
			ps.print("version "); //$NON-NLS-1$
			ps.println(VERSION);
			ps.print("oid " + HASH_FUNCTION_NAME + ":"); //$NON-NLS-1$ //$NON-NLS-2$
			ps.println(LongObjectId.toString(oid));
			ps.print("size "); //$NON-NLS-1$
			ps.println(size);
		}
	}

	@Override
	public String toString() {
		return "LfsPointer: oid=" + LongObjectId.toString(oid) + ", size=" //$NON-NLS-1$ //$NON-NLS-2$
				+ size;
	}
}