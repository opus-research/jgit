/*
 * Copyright (C) 2010, Google Inc.
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

/**
 * A collection of object annotations associated with {@link RevCommit}s.
 *
 * This annotation type associates a single object with each RevCommit,
 * permitting the application to associate additional data with each commit.
 * Each annotation is stored in a densely packed object array.
 *
 * @see DenseIntAnnotation
 * @param <V>
 *            type of value stored.
 */
public final class DenseAnnotation<V extends Object> extends RevAnnotation<V> {
	private V[] values = emptyArray();

	public V get(RevCommit obj) {
		final int annotationId = obj.annotationId;
		if (annotationId < values.length)
			return values[annotationId];
		return null;
	}

	public void set(RevCommit obj, V value) {
		final int annotationId = obj.annotationId;
		if (annotationId <= values.length) {
			V[] n = newArray(newSize(annotationId));
			System.arraycopy(values, 0, n, 0, values.length);
			values = n;
		}
		values[annotationId] = value;
	}

	private int newSize(int annotationId) {
		return Math.max(16, Math.max(annotationId + 1, 2 * values.length));
	}

	@SuppressWarnings("unchecked")
	private static <V> V[] newArray(int size) {
		return (V[]) new Object[size];
	}

	private static final Object[] EMPTY_ARRAY = {};

	@SuppressWarnings("unchecked")
	private static <V> V[] emptyArray() {
		return (V[]) EMPTY_ARRAY;
	}
}
