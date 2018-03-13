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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeChunk;
import org.eclipse.jgit.merge.MergeChunk.ConflictState;
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
				return "Fast-forward";
			}
		},
		/** */
		ALREADY_UP_TO_DATE {
			public String toString() {
				return "Already-up-to-date";
			}
		},
		/** */
		FAILED {
			public String toString() {
				return "Failed";
			}
		},
		/** */
		MERGED {
			public String toString() {
				return "Merged";
			}
		},
		/** */
		CONFLICTING {
			public String toString() {
				return "Conflicting";
			}
		},
		/** */
		NOT_SUPPORTED {
			public String toString() {
				return "Not-yet-supported";
			}
		}
	}

	private ObjectId[] mergedCommits;

	private ObjectId base;

	private ObjectId newHead;

	private Map<String, int[][]> conflicts;

	private MergeStatus mergeStatus;

	private String description;

	private MergeStrategy mergeStrategy;

	/**
	 * @param newHead
	 *            the object the head points at after the merge
	 * @param base
	 *            the common base which was used to produce a content-merge. May
	 *            be <code>null</code> if the merge-result was produced without
	 *            computing a common base
	 * @param mergedCommits
	 *            all the commits which have been merged together
	 * @param mergeStatus
	 *            the status the merge resulted in
	 * @param mergeStrategy
	 *            the used {@link MergeStrategy}
	 * @param lowLevelResults
	 * @param description
	 *            a user friendly description of the merge result
	 */
	public MergeResult(ObjectId newHead, ObjectId base,
			ObjectId[] mergedCommits, MergeStatus mergeStatus,
			MergeStrategy mergeStrategy,
			Map<String, org.eclipse.jgit.merge.MergeResult> lowLevelResults,
			String description) {
		this.newHead = newHead;
		this.mergedCommits = mergedCommits;
		this.base = base;
		this.mergeStatus = mergeStatus;
		this.mergeStrategy = mergeStrategy;
		this.description = description;
		if (lowLevelResults != null)
			for (String path : lowLevelResults.keySet())
				addConflict(path, lowLevelResults.get(path));
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

	/**
	 * @return all the commits which have been merged together
	 */
	public ObjectId[] getMergedCommits() {
		return mergedCommits;
	}

	/**
	 * @return base the common base which was used to produce a content-merge.
	 *         May be <code>null</code> if the merge-result was produced without
	 *         computing a common base
	 */
	public ObjectId getBase() {
		return base;
	}

	@Override
	public String toString() {
		return MessageFormat.format(
				JGitText.get().mergeUsingStrategyResultedInDescription,
				mergeStrategy.getName(), mergeStatus, (description == null ? ""
						: ", " + description));
	}

	/**
	 * @param conflicts
	 *            the conflicts to set
	 */
	public void setConflicts(Map<String, int[][]> conflicts) {
		this.conflicts = conflicts;
	}

	/**
	 * @param path
	 * @param conflictingRanges
	 *            the conflicts to set
	 */
	public void addConflict(String path, int[][] conflictingRanges) {
		if (conflicts == null)
			conflicts = new HashMap<String, int[][]>();
		conflicts.put(path, conflictingRanges);
	}

	/**
	 * @param path
	 * @param lowLevelResult
	 */
	public void addConflict(String path, org.eclipse.jgit.merge.MergeResult lowLevelResult) {
		if (conflicts == null)
			conflicts = new HashMap<String, int[][]>();
		int nrOfConflicts = 0;
		// just counting
		for (MergeChunk mergeChunk : lowLevelResult) {
			if (mergeChunk.getConflictState().equals(ConflictState.FIRST_CONFLICTING_RANGE)) {
				nrOfConflicts++;
			}
		}
		int currentConflict = -1;
		int[][] ret=new int[nrOfConflicts][mergedCommits.length+1];
		for (MergeChunk mergeChunk : lowLevelResult) {
			// to store the end of this chunk (end of the last conflicting range)
			int endOfChunk = 0;
			if (mergeChunk.getConflictState().equals(ConflictState.FIRST_CONFLICTING_RANGE)) {
				if (currentConflict > -1) {
					// there was a previous conflicting range for which the end
					// is not set yet - set it!
					ret[currentConflict][mergedCommits.length] = endOfChunk;
				}
				currentConflict++;
				endOfChunk = mergeChunk.getEnd();
				ret[currentConflict][mergeChunk.getSequenceIndex()] = mergeChunk.getBegin();
			}
			if (mergeChunk.getConflictState().equals(ConflictState.NEXT_CONFLICTING_RANGE)) {
				if (mergeChunk.getEnd() > endOfChunk)
					endOfChunk = mergeChunk.getEnd();
				ret[currentConflict][mergeChunk.getSequenceIndex()] = mergeChunk.getBegin();
			}
		}
		conflicts.put(path, ret);
	}

	/**
	 * Returns information about the conflicts which occurred during a
	 * {@link MergeCommand}. The returned value maps the path of a conflicting
	 * file to a two-dimensional int-array of line-numbers telling where in the
	 * file conflict markers for which merged commit can be found.
	 * <p>
	 * If the returned value contains a mapping "path"->[x][y]=z then this means
	 * <ul>
	 * <li>the file with path "path" contains conflicts</li>
	 * <li>if y<"number of merged commits": for conflict number x in this file
	 * the chunk which was copied from commit number y starts on line number z.
	 * All numberings and line numbers start with 0.</li>
	 * <li>if y=="number of merged commits": the first non-conflicting line
	 * after conflict number x starts at line number z</li>
	 * </ul>
	 * <p>
	 * Example code how to parse this data:
	 * <pre> MergeResult m=...;
	 * Map<String, int[][]> allConflicts = m.getConflicts();
	 * for (String path : allConflicts.keySet()) {
	 * 	int[][] c = allConflicts.get(path);
	 * 	System.out.println("Conflicts in file " + path);
	 * 	for (int i = 0; i < c.length; ++i) {
	 * 		System.out.println("  Conflict #" + i);
	 * 		for (int j = 0; j < (c[i].length) - 1; ++j) {
	 * 			if (c[i][j] >= 0)
	 * 				System.out.println("    Chunk for "
	 * 						+ m.getMergedCommits()[j] + " starts on line #"
	 * 						+ c[i][j]);
	 * 		}
	 * 	}
	 * }</pre>
	 *
	 * @return the conflicts or <code>null</code> if no conflict occured
	 */
	public Map<String, int[][]> getConflicts() {
		return conflicts;
	}
}
