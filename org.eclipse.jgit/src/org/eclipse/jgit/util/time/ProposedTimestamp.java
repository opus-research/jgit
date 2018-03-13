/*
 * Copyright (C) 2016, Google Inc.
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

package org.eclipse.jgit.util.time;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A timestamp generated by {@link MonotonicClock#propose()}.
 * <p>
 * ProposedTimestamp implements AutoCloseable so that implementations can
 * release resources associated with obtaining certainty about time elapsing.
 * For example the constructing MonotonicClock may start network IO with peers
 * when creating the ProposedTimestamp, and {@link #close()} can ensure those
 * network resources are released in a timely fashion.
 *
 * @since 4.6
 */
public abstract class ProposedTimestamp implements AutoCloseable {
	/**
	 * Wait for several timestamps.
	 *
	 * @param times
	 *            timestamps to wait on.
	 * @param maxWait
	 *            how long to wait in {@code units}.
	 * @param unit
	 *            unit describing how to use {@code timeout} while sleeping.
	 * @throws InterruptedException
	 *             current thread was interrupted before the waiting process
	 *             completed normally.
	 * @throws TimeoutException
	 *             the timeout was reached without the proposed timestamp become
	 *             certainly in the past.
	 */
	public static void blockUntil(Iterable<ProposedTimestamp> times,
			long maxWait, TimeUnit unit)
					throws TimeoutException, InterruptedException {
		Iterator<ProposedTimestamp> itr = times.iterator();
		if (!itr.hasNext()) {
			return;
		}

		long now = System.currentTimeMillis();
		long deadline = now + unit.toMillis(maxWait);
		for (;;) {
			long w = deadline - now;
			if (w < 0) {
				throw new TimeoutException();
			}
			itr.next().blockUntil(w, MILLISECONDS);
			if (itr.hasNext()) {
				now = System.currentTimeMillis();
			} else {
				break;
			}
		}
	}

	/**
	 * Read the timestamp as {@code unit} since the epoch.
	 * <p>
	 * The timestamp value for a specific {@code ProposedTimestamp} object never
	 * changes, and can be read before {@link #blockUntil(long, TimeUnit)}.
	 *
	 * @param unit
	 *            what unit to return the timestamp in. The timestamp will be
	 *            rounded if the unit is bigger than the clock's granularity.
	 * @return {@code unit} since the epoch.
	 */
	public abstract long read(TimeUnit unit);

	/**
	 * Wait for this proposed timestamp to be certainly in the recent past.
	 * <p>
	 * This method forces the caller to wait up to {@code timeout} for
	 * {@code this} to pass sufficiently into the past such that the creating
	 * {@link MonotonicClock} instance will not create an earlier timestamp.
	 *
	 * @param maxWait
	 *            how long to wait in {@code units}.
	 * @param unit
	 *            unit describing how to use {@code timeout} while sleeping.
	 * @throws InterruptedException
	 *             current thread was interrupted before the waiting process
	 *             completed normally.
	 * @throws TimeoutException
	 *             the timeout was reached without the proposed timestamp become
	 *             certainly in the past.
	 */
	public abstract void blockUntil(long maxWait, TimeUnit unit)
			throws InterruptedException, TimeoutException;

	/** @return milliseconds since epoch; {@code read(MILLISECONDS}). */
	public long getMillis() {
		return read(MILLISECONDS);
	}

	/** @return time since epoch, with up to millisecond resolution. */
	public Date getDate() {
		return new Date(getMillis());
	}

	/** @return time since epoch, with up to microsecond resolution. */
	public Timestamp getTimestamp() {
		long usec = read(MICROSECONDS);
		Timestamp r = new Timestamp(usec / 1000L);
		r.setNanos((int) ((usec % 1000000L) * 1000L));
		return r;
	}

	/** Release resources allocated by this timestamp. */
	@Override
	public void close() {
		// Do nothing by default.
	}

	@Override
	public String toString() {
		return getTimestamp().toString();
	}
}
