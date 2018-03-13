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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.lfs.lib.LongObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.BuiltinCommand;
import org.eclipse.jgit.util.BuiltinCommandFactory;

/**
 * @since 4.4
 */
public class SmudgeFilter extends BuiltinCommand {
	/**
	 *
	 */
	public final static BuiltinCommandFactory FACTORY = new BuiltinCommandFactory() {
		@Override
		public BuiltinCommand create(Repository db, InputStream in, OutputStream out) {
			try {
				return new SmudgeFilter(db, in, out);
			} catch (IOException e) {
				return null; // TODO: improve error handling
			}
		}
	};

	LongObjectId id;

	private InputStream mIn;

	private LfsUtil lfsUtil;

	/**
	 * @param db
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public SmudgeFilter(Repository db, InputStream in, OutputStream out)
			throws IOException {
		super(in, out);
		lfsUtil = new LfsUtil(db.getDirectory().toPath().resolve("lfs")); //$NON-NLS-1$
		byte buffer[] = new byte[1024];
		LfsParserResult res = LfsPointer.parseLfsPointer(in, buffer);
		if (res.pointer != null) {
			Path mediaFile = lfsUtil.getMediaFile(res.pointer.getOid());
			if (Files.exists(mediaFile)) {
				mIn = Files.newInputStream(mediaFile);
			}
		}
	}

	@Override
	public int run() throws IOException {
		int b;
		if (mIn != null) {
			while ((b = mIn.read()) != -1)
				out.write(b);
			mIn.close();
		}
		out.close();
		return -1;
	}
}