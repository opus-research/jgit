/*
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
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
package org.eclipse.jgit.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Used to obtain a list of branches.
 *
 * @see <a
 *      href="http://www.kernel.org/pub/software/scm/git/docs/git-branch.html"
 *      >Git documentation about Branch</a>
 */
public class ListBranchCommand extends GitCommand<List<Ref>> {
	private ListMode listMode;

	/**
	 * The modes available for listing branches (corresponding to the -r and -a
	 * options)
	 */
	public enum ListMode {
		/**
		 * Corresponds to the -a option (all branches)
		 */
		ALL,
		/**
		 * Corresponds to the -r option (remote branches only)
		 */
		REMOTE;
	}

	/**
	 * @param repo
	 */
	protected ListBranchCommand(Repository repo) {
		super(repo);
	}

	/**
	 * @throws JGitInternalException
	 *             upon internal failure
	 */
	public List<Ref> call() throws JGitInternalException {
		checkCallable();
		Map<String, Ref> refList;
		try {
			if (listMode == null) {
				refList = repo.getRefDatabase().getRefs(Constants.R_HEADS);
			} else if (listMode == ListMode.REMOTE) {
				refList = repo.getRefDatabase().getRefs(Constants.R_REMOTES);
			} else {
				refList = repo.getRefDatabase().getRefs(Constants.R_HEADS);
				refList.putAll(repo.getRefDatabase().getRefs(
						Constants.R_REMOTES));
			}
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}
		List<Ref> resultRefs = new ArrayList<Ref>();
		resultRefs.addAll(refList.values());
		Collections.sort(resultRefs, new Comparator<Ref>() {
			public int compare(Ref o1, Ref o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		setCallable(false);
		return resultRefs;
	}

	/**
	 * @param listMode
	 *            optional: corresponds to the -r/-a options; by default, only
	 *            local branches will be listed
	 * @return this instance
	 */
	public ListBranchCommand setListMode(ListMode listMode) {
		checkCallable();
		this.listMode = listMode;
		return this;
	}
}
