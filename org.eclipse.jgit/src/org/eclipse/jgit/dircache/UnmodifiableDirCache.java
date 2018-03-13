/*
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
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
package org.eclipse.jgit.dircache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.MutableInteger;

/**
 *
 *
 */
public class UnmodifiableDirCache extends DirCache {

	/**
	 * @param indexLocation
	 * @param fs
	 * @throws IOException
	 * @throws CorruptObjectException
	 */
	public UnmodifiableDirCache(final File indexLocation, final FS fs)
			throws CorruptObjectException, IOException {
		super(indexLocation, fs);
		super.read();
		super.getCacheTree(true);
	}

	@Override
	protected DirCacheEntry createDirCacheEntry(BufferedInputStream in,
			MessageDigest md, byte[] infos, MutableInteger infoAt)
			throws IOException {
		return new UnmodifiableDirCacheEntry(infos, infoAt, in, md);
	}

	@Override
	public DirCacheBuilder builder() {
		throw new UnsupportedOperationException();
	}

	@Override
	public DirCacheEditor editor() {
		throw new UnsupportedOperationException();
	}

	@Override
	void replace(DirCacheEntry[] e, int cnt) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void read() throws IOException, CorruptObjectException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean lock() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	void writeTo(OutputStream os) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean commit() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unlock() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ObjectId writeTree(ObjectInserter ow) throws UnmergedPathException,
			IOException {
		throw new UnsupportedOperationException();
	}

}
