/*
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>
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

import java.text.MessageFormat;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeStrategy;

/**
 * Encapsulates the result of a {@link MergeCommand}.
 */
public class MergeResult {

	/**
	 * The status the merge resulted in.
	 */
	public enum MergeStatus {
		/** */
		FAST_FORWARD {
			@Override
			public String toString() {
				return "fast forward";
			}
		},
		/** */
		ALREADY_UP_TO_DATE {
			public String toString() {
				return "already up to date";
			}
		},
		/** */
		FAILED {
			public String toString() {
				return "failed";
			}
		},
		/** */
		MERGED {
			public String toString() {
				return "merged";
			}
		},
		/** */
		NOT_SUPPORTED {
			public String toString() {
				return "not yet supported";
			}
		}
	}

	private ObjectId newHead;

	private MergeStatus mergeStatus;

	private String description;

	private MergeStrategy mergeStrategy;

	/**
	 * @param newHead the object the head points at after the merge
	 * @param mergeStatus the status the merge resulted in
	 * @param mergeStrategy the used {@link MergeStrategy}
	 */
	public MergeResult(ObjectId newHead, MergeStatus mergeStatus,
			MergeStrategy mergeStrategy) {
		this.newHead = newHead;
		this.mergeStatus = mergeStatus;
		this.mergeStrategy = mergeStrategy;
	}

	/**
	 * @param newHead the object the head points at after the merge
	 * @param mergeStatus the status the merge resulted in
	 * @param mergeStrategy the used {@link MergeStrategy}
	 * @param description a user friendly description of the merge result
	 */
	public MergeResult(ObjectId newHead, MergeStatus mergeStatus,
			MergeStrategy mergeStrategy, String description) {
		this.newHead = newHead;
		this.mergeStatus = mergeStatus;
		this.mergeStrategy = mergeStrategy;
		this.description = description;
	}

	/**
	 * @return the object the head points at after the merge
	 */
	public ObjectId getNewHead() {
		return newHead;
	}

	/**
	 * @return the status the merge resulted in
	 */
	public MergeStatus getMergeStatus() {
		return mergeStatus;
	}

	@Override
	public String toString() {
		return MessageFormat.format(
				JGitText.get().mergeUsingStrategyResultedInDescription,
				mergeStrategy.getName(), mergeStatus, (description == null ? ""
						: ", " + description));
	}

}
