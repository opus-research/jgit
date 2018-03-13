/*
 * Copyright (C) 2011, Google Inc.
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

package org.eclipse.jgit.revwalk;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileTreeEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Tree;
import org.junit.Test;

public class DepthObjectWalkBasicDepthTest extends RevWalkTestCase {
	protected DepthObjectWalk dow;

	@Override
	protected RevWalk createRevWalk() {
		return dow = new DepthObjectWalk(db, 1, null, null);
	}

	@Test
	public void testNoCommits() throws Exception {
		assertNull(dow.next());
	}

	@Test
	public void testTwoCommits() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		markStart(b);

		assertCommit(b, dow.next());
		assertCommit(a, dow.next());
		assertNull(dow.next());
	}

	@Test
	public void testOverdeepCommits() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);
		markStart(c);

		assertCommit(c, dow.next());
		assertCommit(b, dow.next());
		assertTrue(b.has(dow.SHALLOW));
		assertNull(dow.next());
		assertTrue(a.has(RevFlag.UNINTERESTING));
	}

	@Test
	public void testOverdeepCommits2() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);
		final RevCommit d = commit(c);
		final RevCommit e = commit(d);
		markStart(e);
		dow.setDepth(3);

		assertCommit(e, dow.next());
		assertCommit(d, dow.next());
		assertCommit(c, dow.next());
		assertCommit(b, dow.next());
		assertNull(dow.next());
		assertTrue(a.has(RevFlag.UNINTERESTING));
		assertTrue(b.has(dow.SHALLOW));
	}

	@Test
	public void testBranchyTree() throws Exception {
		final RevCommit a = commit();
		final RevCommit b1 = commit(a);
		final RevCommit b2 = commit(a);
		final RevCommit c1 = commit(b1);
		final RevCommit c2 = commit(b2);
		final RevCommit d = commit(c1, c2);
		final RevCommit e = commit(d);
		markStart(e);
		dow.setDepth(4);

		assertCommit(e, dow.next());
		assertCommit(d, dow.next());
		assertCommit(c1, dow.next());
		assertCommit(c2, dow.next());
		assertCommit(b1, dow.next());
		assertCommit(b2, dow.next());
		assertCommit(a, dow.next());
		assertNull(dow.next());
		assertTrue(a.has(dow.SHALLOW));
	}

	@Test
	public void testOverdeepBranchyTree() throws Exception {
		final RevCommit a = commit();
		final RevCommit b1 = commit(a);
		final RevCommit b2 = commit(a);
		final RevCommit c1 = commit(b1);
		final RevCommit c2 = commit(b2);
		final RevCommit d = commit(c1, c2);
		final RevCommit e = commit(d);
		markStart(e);
		dow.setDepth(3);

		assertCommit(e, dow.next());
		assertCommit(d, dow.next());
		assertCommit(c1, dow.next());
		assertCommit(c2, dow.next());
		assertCommit(b1, dow.next());
		assertCommit(b2, dow.next());
		assertNull(dow.next());
		assertTrue(b1.has(dow.SHALLOW));
		assertTrue(b2.has(dow.SHALLOW));
		assertTrue(a.has(RevFlag.UNINTERESTING));
	}

	@Test
	public void testShortestPath() throws Exception {
		final RevCommit a = commit();
		final RevCommit b1 = commit(a);
		final RevCommit b2 = commit(a);
		final RevCommit c1 = commit(b1);
		final RevCommit c2 = commit(b2);
		final RevCommit c3 = commit(b2);
		final RevCommit d = commit(c1, c2);
		final RevCommit e = commit(d, c3);
		markStart(e);
		dow.setDepth(3);

		assertCommit(e, dow.next());
		assertCommit(d, dow.next());
		assertCommit(c3, dow.next());
		assertCommit(c1, dow.next());
		assertCommit(c2, dow.next());
		assertCommit(b2, dow.next());
		assertCommit(b1, dow.next());
		assertCommit(a, dow.next());
		assertNull(dow.next());
		assertTrue(a.has(dow.SHALLOW));
		assertTrue(b1.has(dow.SHALLOW));
		assertTrue(!a.has(RevFlag.UNINTERESTING));
	}

	@Test
	public void testUninteresting() throws Exception {
		final RevCommit a = commit();
		final RevCommit b1 = commit(a);
		final RevCommit b2 = commit(a);
		final RevCommit c1 = commit(b1);
		final RevCommit c2 = commit(b2);
		final RevCommit c3 = commit(b2);
		final RevCommit d = commit(c1, c2);
		final RevCommit e = commit(d, c3);
		markStart(e);
		dow.setDepth(2);

		assertCommit(e, dow.next());
		assertCommit(d, dow.next());
		assertCommit(c3, dow.next());
		assertCommit(c1, dow.next());
		assertCommit(c2, dow.next());
		assertCommit(b2, dow.next());
		assertNull(dow.next());
		assertTrue(c1.has(dow.SHALLOW));
		assertTrue(c2.has(dow.SHALLOW));
		assertTrue(b2.has(dow.SHALLOW));
		assertTrue(b1.has(RevFlag.UNINTERESTING));
		assertTrue(a.has(RevFlag.UNINTERESTING));
		assertTrue(!b2.has(RevFlag.UNINTERESTING));
	}

	@Test
	public void testMultipleWants() throws Exception {
		final RevCommit a = commit();
		final RevCommit b1 = commit(a);
		final RevCommit b2 = commit(a);
		final RevCommit c1 = commit(b1);
		final RevCommit c2 = commit(b2);
		final RevCommit c3 = commit(b2);
		final RevCommit d = commit(c1, c2);
		final RevCommit e1 = commit(c1);
		final RevCommit e2 = commit(d, c3);
		final RevCommit e3 = commit(c3);
		markStart(e1);
		markStart(e3);
		dow.setDepth(2);

		assertCommit(e3, dow.next());
		assertCommit(e1, dow.next());
		assertCommit(c3, dow.next());
		assertCommit(c1, dow.next());
		assertCommit(b2, dow.next());
		assertCommit(b1, dow.next());
		assertNull(dow.next());
		assertTrue(b1.has(dow.SHALLOW));
		assertTrue(b2.has(dow.SHALLOW));
		assertTrue(a.has(RevFlag.UNINTERESTING));
	}
}
