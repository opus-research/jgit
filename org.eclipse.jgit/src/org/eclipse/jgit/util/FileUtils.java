/*
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jgit.JGitText;

/**
 * File Utilities
 */
public class FileUtils {
	/**
	 * @param f
	 *            the File to be deleted
	 * @throws IOException
	 *             if file deletion failed
	 */
	public static void delete(final File f) throws IOException {
		if (!f.exists())
			return;

		if (!f.delete())
			throw new IOException(MessageFormat.format(
					JGitText.get().deleteFileFailed, f.getAbsolutePath()));
	}

	/**
	 * Recursively delete a file or directory
	 *
	 * @param d
	 *            the File to delete recursively
	 * @throws IOException
	 *             if file deletion failed
	 */
	public static void recursiveDelete(final File d) throws IOException {
		if (!d.exists())
			return;

		if (d.isDirectory()) {
			final File[] items = d.listFiles();
			if (items != null) {
				for (final File c : items)
					recursiveDelete(c);
			}
		}
		FileUtils.delete(d);
	}

	/**
	 * Deletes a file. Deletion is retried 10 times to avoid failing deletion
	 * caused by concurrent read.
	 *
	 * @param file
	 *            File to be deleted
	 * @throws IOException
	 */
	public static void deleteRepeated(File file) throws IOException {
		boolean deleted = false;
		for (int i = 0; i < 10; i++) {
			deleted = file.delete();
			if (deleted)
				break;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		if (!deleted) {
			throw new IOException(MessageFormat.format(
					JGitText.get().deleteFileWithRepetitionsFailed,
					file.getPath()));
		}
	}

	/**
	 * Recursively delete a file or directory. Deletion is retried 10 times to
	 * avoid failing deletion caused by concurrent read.
	 *
	 * @param d
	 *            the File to delete recursively
	 * @throws IOException
	 *             if file can not be deleted
	 */
	public static void recursiveDeleteRepeated(File d) throws IOException {
		if (!d.exists())
			return;

		if (d.isDirectory()) {
			final File[] items = d.listFiles();
			if (items != null) {
				for (final File c : items)
					recursiveDeleteRepeated(c);
			}
		}
		deleteRepeated(d);
		assert !d.exists();
	}

}
