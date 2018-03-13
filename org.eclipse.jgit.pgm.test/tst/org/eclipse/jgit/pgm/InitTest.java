/*
 * Copyright (C) 2016, Rüdiger Herrmann <ruediger.herrmann@gmx.de>
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

package org.eclipse.jgit.pgm;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;

import org.eclipse.jgit.lib.CLIRepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InitTest extends CLIRepositoryTestCase {

	@Rule
	public final TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testInitBare() throws Exception {
		File directory = tempFolder.getRoot();

		String[] result = execute(
				"git init '" + directory.getCanonicalPath() + "' --bare");

		String[] expecteds = new String[] {
				"Initialized empty Git repository in "
						+ directory.getCanonicalPath(),
				"" };
		assertArrayEquals(expecteds, result);
	}

	@Test
	public void testInitDirectory() throws Exception {
		File workDirectory = tempFolder.getRoot();
		File gitDirectory = new File(workDirectory, Constants.DOT_GIT);

		String[] result = execute(
				"git init '" + workDirectory.getCanonicalPath() + "'");

		String[] expecteds = new String[] {
				"Initialized empty Git repository in "
						+ gitDirectory.getCanonicalPath(),
				"" };
		assertArrayEquals(expecteds, result);
	}

}
