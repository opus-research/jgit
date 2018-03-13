/*
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
import java.text.MessageFormat;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * A class used to execute a {@code Tag} command. It has setters for all
 * supported options and arguments of this command and a {@link #call()} method
 * to finally execute the command.
 *
 * @see <a
 *      href="http://www.kernel.org/pub/software/scm/git/docs/git-tag.html"
 *      >Git documentation about Tag</a>
 */
public class TagCommand extends GitCommand<RevTag> {
	private ObjectId id;

	private String name;

	private String message = new String();

	private PersonIdent author;

	private boolean signed;

	private boolean forceUpdate;

	/**
	 * @param repo
	 */
	protected TagCommand(Repository repo) {
		super(repo);
	}

	/**
	 * Executes the {@code tag} command with all the options and parameters
	 * collected by the setter methods of this class. Each instance of this
	 * class should only be used for one invocation of the command (means: one
	 * call to {@link #call()})
	 *
	 * @return a {@link RevTag} object representing the successful tag
	 * @throws NoHeadException
	 *             when called on a git repo without a HEAD reference
	 * @throws JGitInternalException
	 *             a low-level exception of JGit has occurred. The original
	 *             exception can be retrieved by calling
	 *             {@link Exception#getCause()}. Expect only
	 *             {@code IOException's} to be wrapped.
	 */
	public RevTag call() throws JGitInternalException, NoHeadException {
		checkCallable();

		RepositoryState state = repo.getRepositoryState();
		processOptions(state);

		try {
			// create the tag object
			Tag newTag = new Tag(repo);
			newTag.setTag(name);
			newTag.setMessage(message);
			newTag.setAuthor(author);

			Ref head = repo.getRef(Constants.HEAD);
			if (head == null)
				throw new NoHeadException(
						JGitText.get().commitOnRepoWithoutHEADCurrentlyNotSupported);

			// if no id is set, use head
			if (id == null) {
				id = repo.resolve(Constants.HEAD + "^{commit}");
			}

			newTag.setObjId(id);

			// write the tag object
			ObjectInserter inserter = repo.newObjectInserter();
			try {
				ObjectLoader object = repo.open(newTag.getObjId());
				newTag.setType(Constants.typeString(object.getType()));
				ObjectId tagId = inserter.insert(Constants.OBJ_TAG, inserter.format(newTag));
				newTag.setTagId(tagId);
				inserter.flush();

				RevWalk revWalk = new RevWalk(repo);
				try {
					RevTag revTag = revWalk.parseTag(newTag.getTagId());
					String refName = Constants.R_TAGS + newTag.getTag();
					RefUpdate tagRef = repo.updateRef(refName);
					tagRef.setNewObjectId(tagId);
					tagRef.setForceUpdate(forceUpdate);
					Result updateResult = tagRef.update();
					switch (updateResult) {
					case NEW:
					case FORCED:
						return revTag;
					default:
						throw new JGitInternalException(MessageFormat
								.format(JGitText.get().updatingRefFailed,
										Constants.HEAD,
										newTag.toString(), updateResult));
					}

				} finally {
					revWalk.release();
				}

			} finally {
				inserter.release();
			}

		} catch (IOException e) {
			throw new JGitInternalException(
					JGitText.get().exceptionCaughtDuringExecutionOfTagCommand, e);
		}
	}

	/**
	 * Sets default values for not explicitly specified options. Then validates
	 * that all required data has been provided.
	 *
	 * @param state
	 *            the state of the repository we are working on
	 *
	 * @throws JGitInternalException
	 *            if the tag name is null
	 * @throws UnsupportedOperationException
	 *            if the tag is signed (not supported yet)
	 */
	private void processOptions(RepositoryState state) throws JGitInternalException {
		if (author == null)
			author = new PersonIdent(repo);
		if (name == null)
			throw new JGitInternalException(JGitText.get().tagNameNotSpecified);
		if (signed == true)
			throw new UnsupportedOperationException();
	}

	/**
	 * @param name
	 *            the tag name used for the {@code tag}
	 * @return {@code this}
	 */
	public TagCommand setName(String name) {
		checkCallable();
		this.name = name;
		return this;
	}

	/**
	 * @return the tag name used for the <code>tag</code>
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the tag message used for the <code>tag</code>
	 */
	public String getMessage() {
		return name;
	}

	/**
	 * @param message
	 *            the tag message used for the {@code tag}
	 * @return {@code this}
	 */
	public TagCommand setMessage(String message) {
		checkCallable();
		this.message = message;
		return this;
	}

	/**
	 * If set to true the Tag command creates an annotated tag object.
	 * This corresponds to the parameter -a on the command line.
	 *
	 * @param signed
	 * @return {@code this}
	 */
	public TagCommand setSigned(boolean signed) {
		this.signed = signed;
		return this;
	}



	/**
	 * Sets the author of the tag. If the author is null, a PersonIdent
	 * will be created from the info in the repository.
	 *
	 * @param author
	 * @return {@code this}
	 */
	public TagCommand setAuthor(PersonIdent author) {
		this.author = author;
		return this;
	}

	/**
	 * Sets the object id of the tag. If the object id is null, the
	 * commit pointed to from HEAD will be used.
	 *
	 * @param id
	 * @return {@code this}
	 */
	public TagCommand setId(ObjectId id) {
		this.id = id;
		return this;
	}

	/**
	 * If set to true the Tag command replaces an existing tag object.
	 * This corresponds to the parameter -f on the command line.
	 *
	 * @param forceUpdate
	 * @return {@code this}
	 */
	public TagCommand setForceUpdate(boolean forceUpdate) {
		this.forceUpdate = forceUpdate;
		return this;
	}

}
