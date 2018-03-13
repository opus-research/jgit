/*
 * Copyright (C) 2012, Google Inc.
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

package org.eclipse.jgit.transport;

/**
 * Subscribe spec with matching functions. Can either contain a constant ref
 * ("refs/heads/master") or a wildcard pattern ("refs/heads/*").
 */
public class SubscribeSpec {
	/**
	 * Strip the wildcard character from the end of a wildcard ref.
	 *
	 * @param ref
	 *            the wildcard ref string, e.g "refs/heads/*"
	 * @return the ref without the trailing "*", e.g "refs/heads/"
	 */
	public static String stripWildcard(String ref) {
		return ref.substring(0, ref.length() - 1);
	}

	private final String refName;

	private final boolean isWildcard;

	/**
	 * @param name
	 *            the ref name or wildcard spec, e.g "refs/heads/master" or
	 *            "refs/heads/*"
	 */
	public SubscribeSpec(String name) {
		refName = name;
		isWildcard = RefSpec.isWildcard(name);
	}

	/** @return ref name or wildcard ref pattern */
	public String getRefName() {
		return refName;
	}

	/** @return true if this spec contains a wildcard (ends with "*") */
	public boolean isWildcard() {
		return isWildcard;
	}

	/**
	 * @param ref
	 *            to match against
	 * @return true if ref is an exact match if this spec is not a wildcard
	 *         pattern, or a wildcard match.
	 */
	public boolean isMatch(String ref) {
		if (ref == null)
			return false;
		if (isWildcard)
			return ref.startsWith(stripWildcard(refName));
		return refName.equals(ref);
	}

	@Override
	public int hashCode() {
		return refName.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof SubscribeSpec))
			return false;
		return ((SubscribeSpec) other).getRefName().equals(this.getRefName());
	}

	@Override
	public String toString() {
		return "SubscribeSpec[" + refName + "]";
	}
}
