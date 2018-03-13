/*
 * Copyright (C) 2017 Thomas Wolf <thomas.wolf@paranor.ch>
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
package org.eclipse.jgit.ignore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS.ExecutionResult;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.jgit.util.TemporaryBuffer;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that verify that the set of ignore files in a repository is the same in
 * JGit and in C-git.
 */
public class CGitIgnoreTest extends RepositoryTestCase {

	@Before
	public void initRepo() throws IOException {
		// These tests focus on .gitignore files inside the repository. Because
		// we run C-git, we must ensure that global or user exclude files cannot
		// influence the tests. So we set core.excludesFile to an empty file
		// inside the repository.
		File fakeUserGitignore = writeTrashFile(".fake_user_gitignore", "");
		StoredConfig config = db.getConfig();
		config.setString("core", null, "excludesFile",
				fakeUserGitignore.getAbsolutePath());
		// Disable case-insensitivity -- JGit doesn't handle that yet.
		config.setBoolean("core", null, "ignoreCase", false);
		config.save();
	}

	private void createFiles(String... paths) throws IOException {
		for (String path : paths) {
			writeTrashFile(path, "x");
		}
	}

	private String toString(TemporaryBuffer b) throws IOException {
		return RawParseUtils.decode(b.toByteArray());
	}

	private String[] cgitIgnored() throws Exception {
		FS fs = db.getFS();
		ProcessBuilder builder = fs.runInShell("git", new String[] { "ls-files",
				"--ignored", "--exclude-standard", "-o" });
		builder.directory(db.getWorkTree());
		ExecutionResult result = fs.execute(builder,
				new ByteArrayInputStream(new byte[0]));
		assertEquals("External git failed", 0, result.getRc());
		assertEquals("External git reported errors", "",
				toString(result.getStderr()));
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				new BufferedInputStream(result.getStdout().openInputStream()),
				Constants.CHARSET))) {
			return r.lines().toArray(String[]::new);
		}
	}

	private LinkedHashSet<String> jgitIgnored() throws IOException {
		// Do a tree walk that does descend into ignored directories and return
		// a list of all ignored files
		LinkedHashSet<String> result = new LinkedHashSet<>();
		try (TreeWalk walk = new TreeWalk(db)) {
			walk.addTree(new FileTreeIterator(db));
			walk.setRecursive(true);
			while (walk.next()) {
				if (walk.getTree(WorkingTreeIterator.class).isEntryIgnored()) {
					result.add(walk.getPathString());
				}
			}
		}
		return result;
	}

	private void assertNoIgnoredVisited(Set<String> ignored) throws Exception {
		// Do a recursive tree walk with a NotIgnoredFilter and verify that none
		// of the files visited is in the ignored set
		try (TreeWalk walk = new TreeWalk(db)) {
			walk.addTree(new FileTreeIterator(db));
			walk.setFilter(new NotIgnoredFilter(0));
			walk.setRecursive(true);
			while (walk.next()) {
				String path = walk.getPathString();
				assertFalse("File " + path + " is ignored, should not appear",
						ignored.contains(path));
			}
		}
	}

	private void assertSameAsCGit(String... notIgnored) throws Exception {
		LinkedHashSet<String> ignored = jgitIgnored();
		String[] cgit = cgitIgnored();
		assertArrayEquals(cgit, ignored.toArray());
		for (String notExcluded : notIgnored) {
			assertFalse("File " + notExcluded + " should not be ignored",
					ignored.contains(notExcluded));
		}
		assertNoIgnoredVisited(ignored);
	}

	@Test
	public void testSimpleIgnored() throws Exception {
		createFiles("a.txt", "a.tmp", "src/sub/a.txt", "src/a.tmp",
				"src/a.txt/b.tmp", "ignored/a.tmp", "ignored/not_ignored/a.tmp",
				"ignored/other/a.tmp");
		writeTrashFile(".gitignore",
				"*.txt\n" + "/ignored/*\n" + "!/ignored/not_ignored");
		assertSameAsCGit("ignored/not_ignored/a.tmp");
	}

	@Test
	public void testDirOnlyMatch() throws Exception {
		createFiles("a.txt", "src/foo/a.txt", "src/a.txt", "foo/a.txt");
		writeTrashFile(".gitignore", "foo/");
		assertSameAsCGit();
	}

	@Test
	public void testDirOnlyMatchDeep() throws Exception {
		createFiles("a.txt", "src/foo/a.txt", "src/a.txt", "foo/a.txt");
		writeTrashFile(".gitignore", "**/foo/");
		assertSameAsCGit();
	}

	@Test
	public void testStarMatchOnSlashNot() throws Exception {
		createFiles("sub/a.txt", "foo/sext", "foo/s.txt");
		writeTrashFile(".gitignore", "s*xt");
		assertSameAsCGit("sub/a.txt");
	}

	@Test
	public void testPrefixMatch() throws Exception {
		createFiles("src/new/foo.txt");
		writeTrashFile(".gitignore", "src/new");
		assertSameAsCGit();
	}

	@Test
	public void testDirectoryMatchSubRecursive() throws Exception {
		createFiles("src/new/foo.txt", "foo/src/new/foo.txt", "sub/src/new");
		writeTrashFile(".gitignore", "**/src/new/");
		assertSameAsCGit();
	}
}
