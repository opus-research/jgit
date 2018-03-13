/*
 * Copyright (C) 2011, GitHub Inc.
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
package org.eclipse.jgit.api.blame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.blame.BlameGenerator;
import org.eclipse.jgit.lib.RepositoryTestCase;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

/** Unit tests of {@link BlameGenerator}. */
public class BlameGeneratorTest extends RepositoryTestCase {
	@Test
	public void testBoundLineDelete() throws Exception {
		Git git = new Git(db);

		String[] content1 = new String[] { "first", "second" };
		writeTrashFile("file.txt", join(content1));
		git.add().addFilepattern("file.txt").call();
		RevCommit c1 = git.commit().setMessage("create file").call();

		String[] content2 = new String[] { "third", "first", "second" };
		writeTrashFile("file.txt", join(content2));
		git.add().addFilepattern("file.txt").call();
		RevCommit c2 = git.commit().setMessage("create file").call();

		BlameGenerator generator = new BlameGenerator(db, "file.txt");
		try {
			assertEquals(3, generator.getResultContents().size());

			assertTrue(generator.next());
			assertEquals(c2, generator.getSourceCommit());
			assertEquals(1, generator.getRegionLength());
			assertEquals(0, generator.getResultStart());
			assertEquals(1, generator.getResultEnd());
			assertEquals(0, generator.getSourceStart());
			assertEquals(1, generator.getSourceEnd());
			assertEquals("file.txt", generator.getSourcePath());

			assertTrue(generator.next());
			assertEquals(c1, generator.getSourceCommit());
			assertEquals(2, generator.getRegionLength());
			assertEquals(1, generator.getResultStart());
			assertEquals(3, generator.getResultEnd());
			assertEquals(0, generator.getSourceStart());
			assertEquals(2, generator.getSourceEnd());
			assertEquals("file.txt", generator.getSourcePath());

			assertFalse(generator.next());
		} finally {
			generator.release();
		}
	}

	@Test
	public void testLinesAllDeletedShortenedWalk() throws Exception {
		Git git = new Git(db);

		String[] content1 = new String[] { "first", "second", "third" };

		writeTrashFile("file.txt", join(content1));
		git.add().addFilepattern("file.txt").call();
		git.commit().setMessage("create file").call();

		String[] content2 = new String[] { "" };

		writeTrashFile("file.txt", join(content2));
		git.add().addFilepattern("file.txt").call();
		git.commit().setMessage("create file").call();

		writeTrashFile("file.txt", join(content1));
		git.add().addFilepattern("file.txt").call();
		RevCommit c3 = git.commit().setMessage("create file").call();

		BlameGenerator generator = new BlameGenerator(db, "file.txt");
		try {
			assertEquals(3, generator.getResultContents().size());

			assertTrue(generator.next());
			assertEquals(c3, generator.getSourceCommit());
			assertEquals(0, generator.getResultStart());
			assertEquals(3, generator.getResultEnd());

			assertFalse(generator.next());
		} finally {
			generator.release();
		}
	}

	private static String join(String... lines) {
		StringBuilder joined = new StringBuilder();
		for (String line : lines)
			joined.append(line).append('\n');
		return joined.toString();
	}
}
