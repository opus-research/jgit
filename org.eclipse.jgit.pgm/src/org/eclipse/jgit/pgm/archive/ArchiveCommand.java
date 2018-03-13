/*
 * Copyright (C) 2012 Google Inc.
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
package org.eclipse.jgit.pgm.archive;

import java.lang.String;
import java.lang.System;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;
import java.text.MessageFormat;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.pgm.CLIText;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Create an archive of files from a named tree.
 * <p>
 * Examples (<code>git</code> is a {@link Git} instance):
 * <p>
 * Create a tarball from HEAD:
 *
 * <pre>
 * db = git.getRepository();
 * new ArchiveCommand(db).setTree(db.resolve(&quot;HEAD&quot;))
 *		.setOutputStream(out).call()
 * </pre>
 * <p>
 * Create a ZIP file from master:
 *
 * <pre>
 * new ArchiveCommand(db).setTree(db.resolve(&quot;master&quot;))
 *		.setFormat(ArchiveCommand.Format.ZIP)
 *		.setOutputStream(out).call()
 * </pre>
 *
 * @see <a href="http://git-htmldocs.googlecode.com/git/git-archive.html"
 *      >Git documentation about archive</a>
 */
public class ArchiveCommand extends GitCommand<OutputStream> {
	/**
	 * Available archival formats (corresponding to values for
	 * the --format= option)
	 */
	public static enum Format {
		ZIP,
		TAR
	}

	private static interface Archiver {
		ArchiveOutputStream createArchiveOutputStream(OutputStream s);
		void putEntry(String path, FileMode mode,
				ObjectLoader loader, ArchiveOutputStream out)
				throws IOException;
	}

	private static void warnArchiveEntryModeIgnored(String name) {
		System.err.println(MessageFormat.format( //
				CLIText.get().archiveEntryModeIgnored, //
				name));
	}

	private static final Map<Format, Archiver> formats;

	static {
		Map<Format, Archiver> fmts = new EnumMap<Format, Archiver>(Format.class);
		fmts.put(Format.ZIP, new Archiver() {
			public ArchiveOutputStream createArchiveOutputStream(OutputStream s) {
				return new ZipArchiveOutputStream(s);
			}

			public void putEntry(String path, FileMode mode, //
					ObjectLoader loader, ArchiveOutputStream out) //
					throws IOException {
				final ZipArchiveEntry entry = new ZipArchiveEntry(path);

				if (mode == FileMode.REGULAR_FILE) {
					// ok
				} else if (mode == FileMode.EXECUTABLE_FILE
						|| mode == FileMode.SYMLINK) {
					entry.setUnixMode(mode.getBits());
				} else {
					warnArchiveEntryModeIgnored(path);
				}
				entry.setSize(loader.getSize());
				out.putArchiveEntry(entry);
				loader.copyTo(out);
				out.closeArchiveEntry();
			}
		});
		fmts.put(Format.TAR, new Archiver() {
			public ArchiveOutputStream createArchiveOutputStream(OutputStream s) {
				return new TarArchiveOutputStream(s);
			}

			public void putEntry(String path, FileMode mode, //
					ObjectLoader loader, ArchiveOutputStream out) //
					throws IOException {
				if (mode == FileMode.SYMLINK) {
					final TarArchiveEntry entry = new TarArchiveEntry( //
							path, TarConstants.LF_SYMLINK);
					entry.setLinkName(new String( //
							loader.getCachedBytes(100), "UTF-8")); //$NON-NLS-1$
					out.putArchiveEntry(entry);
					out.closeArchiveEntry();
					return;
				}

				final TarArchiveEntry entry = new TarArchiveEntry(path);
				if (mode == FileMode.REGULAR_FILE ||
				    mode == FileMode.EXECUTABLE_FILE)
					entry.setMode(mode.getBits());
				else
					warnArchiveEntryModeIgnored(path);
				entry.setSize(loader.getSize());
				out.putArchiveEntry(entry);
				loader.copyTo(out);
				out.closeArchiveEntry();
			}
		});
		formats = fmts;
	}

	private OutputStream out;

	private TreeWalk walk = new TreeWalk(repo);

	private Format format = Format.TAR;

	/**
	 * @param repo
	 */
	public ArchiveCommand(Repository repo) {
		super(repo);
	}

	/**
	 * @return the stream to which the archive has been written
	 */
	public OutputStream call() {
		try {
			final ObjectReader reader = walk.getObjectReader();
			final MutableObjectId idBuf = new MutableObjectId();
			final Archiver fmt = formats.get(format);
			final ArchiveOutputStream outa = fmt.createArchiveOutputStream(out);

			try {
				walk.setRecursive(true);
				while (walk.next()) {
					final String name = walk.getPathString();
					final FileMode mode = walk.getFileMode(0);

					if (mode == FileMode.TREE)
						// ZIP entries for directories are optional.
						// Leave them out, mimicking "git archive".
						continue;

					walk.getObjectId(idBuf, 0);
					fmt.putEntry(name, mode, reader.open(idBuf), outa);
				}

				return out;
			} finally {
				outa.close();
			}
		} finally {
			walk.release();
		}
	}

	/**
	 * @param tree
	 *            the commit or tree object to produce an archive for
	 * @return this
	 */
	public ArchiveCommand setTree(ObjectId tree) {
		walk.reset(tree);
		return this;
	}

	/**
	 * @param out
	 *            the stream to which to write the archive
	 * @return this
	 */
	public ArchiveCommand setOutputStream(OutputStream out) {
		this.out = out;
		return this;
	}

	/**
	 * @param fmt
	 *            archive format (e.g., Format.TAR)
	 * @return this
	 */
	public ArchiveCommand setFormat(Format fmt) {
		this.format = fmt;
		return this;
	}


}
