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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.SubscribeCommand.Command;

/**
 * Subscribes to a single remote Publisher process with multiple repositories.
 */
public class Subscriber {
	/** The default timeout for a subscribe connection. */
	public final static int SUBSCRIBE_TIMEOUT = 3 * 60 * 60;

	private final Transport transport;

	private PubSubConfig.Publisher config;

	private SubscribeConnection connection;

	private String restartToken;

	private String lastPackNumber;

	private final Map<String, SubscribedRepository> repoSubscriptions;

	private int timeout;

	/**
	 * @param uri
	 * @throws IOException
	 */
	public Subscriber(URIish uri) throws IOException {
		transport = Transport.open(uri);
		repoSubscriptions = new HashMap<String, SubscribedRepository>();
		timeout = SUBSCRIBE_TIMEOUT;
	}

	/**
	 * Set the timeout for the subscribe connection.
	 *
	 * @param timeout
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Sync the existing subscribe specs against the subscribe specs found in
	 * the publisher config.
	 *
	 * @param publisher
	 * @return Map of repository name to list of SubscribeCommands required to
	 *         sync the existing state to the state of the publisher config.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public Map<String, List<SubscribeCommand>> sync(
			PubSubConfig.Publisher publisher)
			throws IOException, URISyntaxException {
		config = publisher;
		Map<String, List<SubscribeCommand>> subscribeCommands = new HashMap<
				String, List<SubscribeCommand>>();

		for (PubSubConfig.Subscriber s : publisher.getSubscribers()) {
			SubscribedRepository sr = repoSubscriptions.get(s.getName());
			List<SubscribeCommand> repoCommands = new ArrayList<
					SubscribeCommand>();

			if (sr == null) {
				sr = new SubscribedRepository(s);
				for (RefSpec spec : s.getSubscribeSpecs())
					repoCommands.add(new SubscribeCommand(
							Command.SUBSCRIBE, spec.getSource()));
			} else {
				// Calculate a diff in subscriptions using the SubscribedRepos
				// and the new configuration. Use the exact string values.
				List<String> newSpecs = new ArrayList<String>();
				List<String> oldSpecs = new ArrayList<String>();
				for (RefSpec rs : s.getSubscribeSpecs())
					newSpecs.add(rs.getSource());
				for (RefSpec rs : sr.getSubscribeSpecs())
					oldSpecs.add(rs.getSource());

				Set<String> toAdd = new LinkedHashSet<String>(newSpecs);
				toAdd.removeAll(oldSpecs);
				for (String subscribe : toAdd)
					repoCommands.add(
							new SubscribeCommand(Command.SUBSCRIBE, subscribe));

				Set<String> toRemove = new LinkedHashSet<String>(oldSpecs);
				toRemove.removeAll(newSpecs);
				for (String unsubscribe : toRemove)
					repoCommands.add(new SubscribeCommand(
							Command.UNSUBSCRIBE, unsubscribe));

				// Update state with new specs
				sr.setSubscribeSpecs(s.getSubscribeSpecs());
			}

			subscribeCommands.put(sr.getName(), repoCommands);
			repoSubscriptions.put(sr.getName(), sr);
			sr.setUpRefs();
		}
		return subscribeCommands;
	}

	/**
	 * Advertise all refs to subscribe to, create SubscribedRepo instances for
	 * each repository. This method blocks until the connection is closed.
	 *
	 * @param commands
	 * @param monitor
	 * @throws NotSupportedException
	 * @throws InterruptedException
	 * @throws TransportException
	 * @throws IOException
	 */
	public void subscribe(final Map<String, List<SubscribeCommand>> commands,
			ProgressMonitor monitor) throws NotSupportedException,
			InterruptedException, TransportException, IOException {
		connection = transport.openSubscribe(this);
		transport.setTimeout(timeout);
		try {
			connection.doSubscribe(this, commands, monitor);
		} finally {
			close();
		}
	}

	/** @return a unique key to identify this subscriber (scheme://host/) */
	public String getKey() {
		return config.getKey();
	}

	/**
	 * @param r
	 * @return the repository with this key, or null.
	 */
	public SubscribedRepository getRepository(String r) {
		return repoSubscriptions.get(r);
	}

	/**
	 * @return the set of all repository names this subscriber will connect to.
	 */
	public Set<String> getAllRepositories() {
		return Collections.unmodifiableSet(repoSubscriptions.keySet());
	}

	/** @return fast restart token, or null if none. */
	public String getRestartToken() {
		return restartToken;
	}

	/** @param restart */
	public void setRestartToken(String restart) {
		restartToken = restart;
	}

	/** @return the last pack number. */
	public String getLastPackNumber() {
		return lastPackNumber;
	}

	/**
	 * Set the last pack number.
	 *
	 * @param number
	 */
	public void setLastPackNumber(String number) {
		lastPackNumber = number;
	}

	/** Close this subscription. */
	public void close() {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}
}
