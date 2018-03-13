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
package org.eclipse.jgit.submodule;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Generator that visits all submodule entries found in the index tree
 */
public class SubmoduleGenerator {

	/**
	 * Get submodule directory
	 *
	 * @param parent
	 * @param path
	 * @return directory
	 */
	public static File getSubmoduleDirectory(final Repository parent,
			final String path) {
		String normalized;
		if (File.separatorChar != '\\')
			normalized = path;
		else
			normalized = path.replace('/', '\\');
		return new File(parent.getWorkTree(), normalized);
	}

	private final Repository repository;

	private final TreeWalk walk;

	private StoredConfig repoConfig;

	private FileBasedConfig modulesConfig;

	private String path;

	/**
	 * Create submodule generator
	 *
	 * @param repository
	 * @throws IOException
	 */
	public SubmoduleGenerator(Repository repository) throws IOException {
		this(repository, null);
	}

	/**
	 * Create submodule generator
	 *
	 * @param repository
	 * @param filter
	 * @throws IOException
	 */
	public SubmoduleGenerator(final Repository repository,
			final TreeFilter filter) throws IOException {
		this.repository = repository;

		walk = new TreeWalk(repository);
		if (filter != null)
			walk.setFilter(AndTreeFilter.create(new SubmoduleTreeFilter(),
					filter));
		else
			walk.setFilter(new SubmoduleTreeFilter());
		reset();
	}

	private void loadConfig() throws IOException, ConfigInvalidException {
		if (modulesConfig == null) {
			File modulesFile = new File(repository.getWorkTree(),
					Constants.DOT_GIT_MODULES);
			FileBasedConfig config = new FileBasedConfig(modulesFile,
					repository.getFS());
			config.load();
			modulesConfig = config;
		}
	}

	/**
	 * Reset generator and start new submodule walk
	 *
	 * @return this generator
	 * @throws IOException
	 */
	public SubmoduleGenerator reset() throws IOException {
		repoConfig = repository.getConfig();
		modulesConfig = null;
		walk.reset();
		walk.addTree(new DirCacheIterator(repository.readDirCache()));
		return this;
	}

	/**
	 * Get directory that will be the root of the submodule's local repository
	 *
	 * @return submodule repository directory
	 */
	public File getDirectory() {
		if (path == null)
			return null;
		return getSubmoduleDirectory(repository, path);
	}

	/**
	 * Get the .git directory for the current submodule entry
	 *
	 * @return .git for submodule repository
	 */
	public File getGitDirectory() {
		if (path == null)
			return null;
		return new File(getDirectory(), Constants.DOT_GIT);
	}

	/**
	 * Advance to next submodule in the index tree.
	 *
	 * The object id and path of the next entry can be obtained by calling
	 * {@link #getObjectId()} and {@link #getPath()}.
	 *
	 * @return true if entry found, false otherwise
	 * @throws IOException
	 */
	public boolean next() throws IOException {
		boolean next = walk.next();
		path = next ? walk.getPathString() : null;
		return next;
	}

	/**
	 * Get path of current submodule entry
	 *
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Get object id of current submodule entry
	 *
	 * @return object id
	 */
	public ObjectId getObjectId() {
		return walk.getObjectId(0);
	}

	/**
	 * Get the configured path for current entry. This will be the value from
	 * the .gitmodules file in the current repository's working tree.
	 *
	 * @return configured path
	 * @throws ConfigInvalidException
	 * @throws IOException
	 */
	public String getModulesPath() throws IOException, ConfigInvalidException {
		if (path == null)
			return null;

		loadConfig();
		return modulesConfig.getString(
				ConfigConstants.CONFIG_SUBMODULE_SECTION, path,
				ConfigConstants.CONFIG_KEY_PATH);
	}

	/**
	 * Get the configured remote URL for current entry. This will be the value
	 * from the repository's config.
	 *
	 * @return configured URL
	 * @throws ConfigInvalidException
	 * @throws IOException
	 */
	public String getConfigUrl() throws IOException, ConfigInvalidException {
		if (path == null)
			return null;

		return repoConfig.getString(ConfigConstants.CONFIG_SUBMODULE_SECTION,
				path, ConfigConstants.CONFIG_KEY_URL);
	}

	/**
	 * Get the configured remote URL for current entry. This will be the value
	 * from the .gitmodules file in the current repository's working tree.
	 *
	 * @return configured URL
	 * @throws ConfigInvalidException
	 * @throws IOException
	 */
	public String getModulesUrl() throws IOException, ConfigInvalidException {
		if (path == null)
			return null;

		loadConfig();
		return modulesConfig.getString(
				ConfigConstants.CONFIG_SUBMODULE_SECTION, path,
				ConfigConstants.CONFIG_KEY_URL);
	}

	/**
	 * Get the configured update field for current entry. This will be the value
	 * from the repository's config.
	 *
	 * @return update value
	 * @throws ConfigInvalidException
	 * @throws IOException
	 */
	public String getConfigUpdate() throws IOException, ConfigInvalidException {
		if (path == null)
			return null;

		return repoConfig.getString(ConfigConstants.CONFIG_SUBMODULE_SECTION,
				path, ConfigConstants.CONFIG_KEY_UPDATE);
	}

	/**
	 * Get the configured update field for current entry. This will be the value
	 * from the .gitmodules file in the current repository's working tree.
	 *
	 * @return update value
	 * @throws ConfigInvalidException
	 * @throws IOException
	 */
	public String getModulesUpdate() throws IOException, ConfigInvalidException {
		if (path == null)
			return null;

		loadConfig();
		return modulesConfig.getString(
				ConfigConstants.CONFIG_SUBMODULE_SECTION, path,
				ConfigConstants.CONFIG_KEY_UPDATE);
	}

	/**
	 * Does the current submodule entry have a .git directory in the parent
	 * repository's working tree?
	 *
	 * @return true if .git directory exists, false otherwise
	 */
	public boolean hasGitDirectory() {
		File directory = getGitDirectory();
		return directory != null && directory.isDirectory();
	}

	/**
	 * Get repository for current submodule entry
	 *
	 * @see #hasGitDirectory()
	 * @return repository or null if not found
	 * @throws IOException
	 */
	public Repository getRepository() throws IOException {
		File directory = getGitDirectory();
		if (directory == null || !directory.isDirectory())
			return null;
		try {
			return Git.open(directory).getRepository();
		} catch (RepositoryNotFoundException e) {
			return null;
		}
	}
}
