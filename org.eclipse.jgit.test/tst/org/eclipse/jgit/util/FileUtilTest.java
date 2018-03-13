/*
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
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

package org.eclipse.jgit.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileUtilTest {

	private final File trash = new File(new File("target"), "trash");

	@Before
	public void setUp() throws Exception {
		assertTrue(trash.mkdirs());
	}

	@After
	public void tearDown() throws Exception {
		FileUtils.delete(trash, FileUtils.RECURSIVE | FileUtils.RETRY);
	}

	@Test
	public void testDeleteFile() throws IOException {
		File f = new File(trash, "test");
		FileUtils.createNewFile(f);
		FileUtils.delete(f);
		assertFalse(f.exists());

		try {
			FileUtils.delete(f);
			fail("deletion of non-existing file must fail");
		} catch (IOException e) {
			// expected
		}

		try {
			FileUtils.delete(f, FileUtils.SKIP_MISSING);
		} catch (IOException e) {
			fail("deletion of non-existing file must not fail with option SKIP_MISSING");
		}
	}

	@Test
	public void testDeleteRecursive() throws IOException {
		File f1 = new File(trash, "test/test/a");
		FileUtils.mkdirs(f1.getParentFile());
		FileUtils.createNewFile(f1);
		File f2 = new File(trash, "test/test/b");
		FileUtils.createNewFile(f2);
		File d = new File(trash, "test");
		FileUtils.delete(d, FileUtils.RECURSIVE);
		assertFalse(d.exists());

		try {
			FileUtils.delete(d, FileUtils.RECURSIVE);
			fail("recursive deletion of non-existing directory must fail");
		} catch (IOException e) {
			// expected
		}

		try {
			FileUtils.delete(d, FileUtils.RECURSIVE | FileUtils.SKIP_MISSING);
		} catch (IOException e) {
			fail("recursive deletion of non-existing directory must not fail with option SKIP_MISSING");
		}
	}

	@Test
	public void testMkdir() throws IOException {
		File d = new File(trash, "test");
		FileUtils.mkdir(d);
		assertTrue(d.exists() && d.isDirectory());

		try {
			FileUtils.mkdir(d);
			fail("creation of existing directory must fail");
		} catch (IOException e) {
			// expected
		}

		FileUtils.mkdir(d, true);
		assertTrue(d.exists() && d.isDirectory());

		assertTrue(d.delete());
		File f = new File(trash, "test");
		FileUtils.createNewFile(f);
		try {
			FileUtils.mkdir(d);
			fail("creation of directory having same path as existing file must"
					+ " fail");
		} catch (IOException e) {
			// expected
		}
		assertTrue(f.delete());
	}

	@Test
	public void testMkdirs() throws IOException {
		File root = new File(trash, "test");
		assertTrue(root.mkdir());

		File d = new File(root, "test/test");
		FileUtils.mkdirs(d);
		assertTrue(d.exists() && d.isDirectory());

		try {
			FileUtils.mkdirs(d);
			fail("creation of existing directory hierarchy must fail");
		} catch (IOException e) {
			// expected
		}

		FileUtils.mkdirs(d, true);
		assertTrue(d.exists() && d.isDirectory());

		FileUtils.delete(root, FileUtils.RECURSIVE);
		File f = new File(trash, "test");
		FileUtils.createNewFile(f);
		try {
			FileUtils.mkdirs(d);
			fail("creation of directory having path conflicting with existing"
					+ " file must fail");
		} catch (IOException e) {
			// expected
		}
		assertTrue(f.delete());
	}

	@Test
	public void testCreateNewFile() throws IOException {
		File f = new File(trash, "x");
		FileUtils.createNewFile(f);
		assertTrue(f.exists());

		try {
			FileUtils.createNewFile(f);
			fail("creation of already existing file must fail");
		} catch (IOException e) {
			// expected
		}

		FileUtils.delete(f);
	}

	@Test
	public void testDeleteEmptyTreeOk() throws IOException {
		File t = new File(trash, "t");
		FileUtils.mkdir(t);
		FileUtils.mkdir(new File(t, "d"));
		FileUtils.mkdir(new File(new File(t, "d"), "e"));
		FileUtils.delete(t, FileUtils.EMPTY_DIRECTORIES_ONLY | FileUtils.RECURSIVE);
		assertFalse(t.exists());
	}

	@Test
	public void testDeleteNotEmptyTreeNotOk() throws IOException {
		File t = new File(trash, "t");
		FileUtils.mkdir(t);
		FileUtils.mkdir(new File(t, "d"));
		File f = new File(new File(t, "d"), "f");
		FileUtils.createNewFile(f);
		FileUtils.mkdir(new File(new File(t, "d"), "e"));
		try {
			FileUtils.delete(t, FileUtils.EMPTY_DIRECTORIES_ONLY | FileUtils.RECURSIVE);
			fail("expected failure to delete f");
		} catch (IOException e) {
			assertThat(e.getMessage(), endsWith(f.getAbsolutePath()));
		}
		assertTrue(t.exists());
	}

	@Test
	public void testDeleteNotEmptyTreeNotOkButIgnoreFail() throws IOException {
		File t = new File(trash, "t");
		FileUtils.mkdir(t);
		FileUtils.mkdir(new File(t, "d"));
		File f = new File(new File(t, "d"), "f");
		FileUtils.createNewFile(f);
		File e = new File(new File(t, "d"), "e");
		FileUtils.mkdir(e);
		FileUtils.delete(t, FileUtils.EMPTY_DIRECTORIES_ONLY | FileUtils.RECURSIVE
				| FileUtils.IGNORE_ERRORS);
		// Should have deleted as much as possible, but not all
		assertTrue(t.exists());
		assertTrue(f.exists());
		assertFalse(e.exists());
	}
}
