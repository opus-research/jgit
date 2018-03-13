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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;

/**
 * A subscription to a single repository using one or more SubscriptionSpecs.
 * Updates are multiplexed over a single Subscriber connection.
 */
public class SubscribedRepository {
	private final Repository repository;

	private final String remoteName;

	private RemoteConfig remoteConfig;

	/** The name unique for this repository on this host, usually the path. */
	private final String name;

	private List<RefSpec> specs;

	private Map<String, Ref> remoteRefs;

	/**
	 * Get the PubSub ref location from a remote tracking ref. The /pubsub/ ref
	 * tree is different from the /remotes/ tree in that it can store branches
	 * and tags. Branches are under /heads/ and tags are under /tags/.
	 *
	 * @param rc
	 * @param trackingRef
	 *            e.g "refs/remotes/origin/master"
	 * @return pubsub ref location, e.g "refs/pubsub/origin/heads/master"
	 */
	public static String getPubSubRefFromTracking(
			RemoteConfig rc, String trackingRef) {
		String ref = getRemoteRefFromTracking(rc, trackingRef);
		return getPubSubRefFromRemote(rc.getName(), ref);
	}

	/**
	 * Get the PubSub ref location from a remote ref.
	 *
	 * @param remote
	 *            e.g "origin"
	 * @param remoteRef
	 *            e.g "refs/heads/master"
	 * @return pubsub ref location, e.g "refs/pubsub/origin/heads/master"
	 */
	public static String getPubSubRefFromRemote(
			String remote, String remoteRef) {
		return translateRef(
				Constants.R_REFS, Constants.R_PUBSUB + remote + "/", remoteRef);
	}

	/**
	 * Get the tracking ref location from a pubsub ref. This strips off the
	 * pubsub prefix /heads/. Translation is only allowed from a pubsub branch
	 * ref to a remote ref, because the /remotes/ tree only stores branches.
	 *
	 * @param rc
	 * @param pubsubRef
	 *            e.g "refs/pubsub/origin/heads/master"
	 * @return tracking ref location, e.g "refs/remotes/origin/master"
	 */
	public static String getTrackingRefFromPubSub(
			RemoteConfig rc, String pubsubRef) {
		String remote = getRemoteRefFromPubSub(rc.getName(), pubsubRef);
		return getTrackingRefFromRemote(rc, remote);
	}

	/**
	 * Get the remote ref location from a tracking ref.
	 *
	 * @param rc
	 * @param trackingRef
	 *            e.g "refs/remotes/origin/master"
	 * @return remote ref location, e.g "refs/heads/master"
	 */
	public static String getRemoteRefFromTracking(
			RemoteConfig rc, String trackingRef) {
		// Match ref against the tracking side of a fetch spec
		String local = null;
		for (RefSpec r : rc.getFetchRefSpecs()) {
			if (r.matchDestination(trackingRef)) {
				if (r.isWildcard())
					local = r.getSource()
							.substring(0, r.getSource().length() - 1)
							+ trackingRef.substring(
									r.getDestination().length() - 1);
				else
					local = r.getSource();
			}
		}
		if (local == null)
			throw new IllegalArgumentException(MessageFormat.format(
					JGitText.get().noMatchingFetchSpec, trackingRef,
					rc.getName()));
		return local;
	}

	/**
	 * Get the tracking ref location from a remote ref.
	 *
	 * @param rc
	 * @param remoteRef
	 *            e.g "refs/heads/master"
	 * @return tracking ref location, e.g "refs/remotes/origin/master"
	 */
	public static String getTrackingRefFromRemote(
			RemoteConfig rc, String remoteRef) {
		// Match ref against the remote side of a fetch spec
		String local = null;
		for (RefSpec r : rc.getFetchRefSpecs()) {
			if (r.matchSource(remoteRef)) {
				if (r.isWildcard())
					local = r.getDestination()
							.substring(0, r.getDestination().length() - 1)
							+ remoteRef.substring(r.getSource().length() - 1);
				else
					local = r.getDestination();
			}
		}
		if (local == null)
			throw new IllegalArgumentException(MessageFormat.format(
					JGitText.get().noMatchingFetchSpec, remoteRef,
					rc.getName()));
		return local;
	}

	/**
	 * Get the remote ref location from a pubsub ref.
	 *
	 * @param remote
	 *            e.g "origin"
	 * @param ref
	 *            e.g "refs/pubsub/origin/heads/master"
	 * @return remote ref location, e.g "refs/heads/master"
	 */
	public static String getRemoteRefFromPubSub(String remote, String ref) {
		return translateRef(
				Constants.R_PUBSUB + remote + "/", Constants.R_REFS, ref);
	}

	private static String translateRef(String oldPrefix, String newPrefix,
			String ref) {
		if (!ref.startsWith(oldPrefix))
			throw new IllegalArgumentException(MessageFormat.format(
					JGitText.get().invalidRefName, ref));
		return newPrefix + ref.substring(oldPrefix.length());
	}

	/**
	 * Create a new SubscribedRepository using the Subscriber config. Create
	 * a new FileRepository instance using the directory field from the config.
	 *
	 * @param s
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public SubscribedRepository(PubSubConfig.Subscriber s)
			throws IOException, URISyntaxException {
		this(s, new FileRepository(s.getDirectory()));
	}

	/**
	 * Create a new SubscribedRepository using the Subscriber config and the
	 * given repository. Ignore the repository directory field in the config.
	 *
	 * @param s
	 * @param r
	 */
	public SubscribedRepository(PubSubConfig.Subscriber s, Repository r) {
		remoteName = s.getRemote();
		repository = r;
		specs = s.getSubscribeSpecs();
		name = s.getName();
	}

	/**
	 * Set up the refs/pubsub/* space and copy in all matching refs. Remove any
	 * refs that no longer match any subscriptions.
	 *
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void setUpRefs() throws IOException, URISyntaxException {
		remoteConfig = new RemoteConfig(repository.getConfig(), remoteName);
		Set<String> existingRefs = new LinkedHashSet<String>(
				repository.getRefDatabase().getRefs(
						getPubSubRefFromRemote(remoteName, Constants.R_REFS))
						.keySet());
		Map<String, Ref> refs = getRemoteRefs();
		// Delete all non-matching refs in refs/pubsub/* first
		for (Map.Entry<String, Ref> entry : refs.entrySet()) {
			String ref = entry.getKey();
			String existingRef = ref.substring(Constants.R_REFS.length());
			existingRefs.remove(existingRef);
		}
		for (String r : existingRefs) {
			String pubsubRef = getPubSubRefFromRemote(
					remoteName, Constants.R_REFS + r);
			if (repository.getRef(pubsubRef) == null)
				continue;
			RefUpdate ru = repository.updateRef(pubsubRef);
			ru.setForceUpdate(true);
			ru.delete();
		}
		// Set up space in refs/pubsub/* by copying all locally matching refs
		for (Map.Entry<String, Ref> entry : refs.entrySet()) {
			String ref = entry.getKey();
			String pubsubRef = getPubSubRefFromRemote(remoteName, ref);
			if (repository.getRef(pubsubRef) != null)
				continue;
			RefUpdate ru = repository.updateRef(pubsubRef);
			// Create refs/pubsub/<remote name>/<ref>
			ru.setExpectedOldObjectId(ObjectId.zeroId());
			ru.setNewObjectId(entry.getValue().getObjectId());
			ru.setRefLogMessage("pubsub setup", false);
			ru.forceUpdate();
		}
	}

	/** @return repository. */
	public Repository getRepository() {
		return repository;
	}

	/** @return the remote name. */
	public String getRemote() {
		return remoteName;
	}

	/** @return the set of subscribe specs for this repository. */
	public List<RefSpec> getSubscribeSpecs() {
		return Collections.unmodifiableList(specs);
	}

	/**
	 * @param s
	 */
	public void setSubscribeSpecs(List<RefSpec> s) {
		specs = s;
		remoteRefs = null;
	}

	private void cacheRemoteRefs() throws IOException {
		Map<String, Ref> matches = new HashMap<String, Ref>();
		RefDatabase refdb = repository.getRefDatabase();
		for (RefSpec spec : getSubscribeSpecs()) {
			String remoteRef;
			boolean isTag = spec.getSource().startsWith(Constants.R_TAGS);
			if (isTag)
				remoteRef = spec.getSource();
			else
				remoteRef = getTrackingRefFromRemote(remoteConfig, spec.getSource());
			Collection<Ref> c;
			if (spec.isWildcard()) {
				remoteRef = remoteRef.substring(0, remoteRef.length() - 1);
				c = refdb.getRefs(remoteRef).values();
			} else {
				Ref r = refdb.getRef(remoteRef);
				if (r == null)
					continue;
				c = Collections.singleton(r);
			}
			for (Ref r : c) {
				if (isTag)
					matches.put(r.getName(), r);
				else
					matches.put(getRemoteRefFromTracking(remoteConfig, r.getName()), r);
			}
		}
		remoteRefs = matches;
	}

	/**
	 * @return all matching tracking ref heads in refs/remotes/remote/* and tag
	 *         values in refs/tags/*, with keys corresponding to refs/*.
	 * @throws IOException
	 */
	public Map<String, Ref> getRemoteRefs() throws IOException {
		if (remoteRefs == null)
			cacheRemoteRefs();
		return remoteRefs;
	}

	/**
	 * @return all pubsub refs in refs/pubsub/remote/*, with keys corresponding
	 *         to refs/*.
	 * @throws IOException
	 */
	public Map<String, Ref> getPubSubRefs() throws IOException {
		Map<String, Ref> matches = new HashMap<String, Ref>();
		RefDatabase rdb = repository.getRefDatabase();
		for (RefSpec spec : getSubscribeSpecs()) {
			String pubsubRef = getPubSubRefFromRemote(remoteName, spec.getSource());
			if (spec.isWildcard()) {
				pubsubRef = pubsubRef.substring(0, pubsubRef.length() - 1);
				for (Ref r : rdb.getRefs(pubsubRef).values())
					matches.put(getRemoteRefFromPubSub(remoteName, r.getName()), r);
			} else {
				Ref r = rdb.getRef(pubsubRef);
				if (r != null)
					matches.put(getRemoteRefFromPubSub(remoteName, r.getName()), r);
			}
		}
		return matches;
	}

	/** @return the name (usually the uri path) of this repository. */
	public String getName() {
		return name;
	}

	/** Close the open repository. */
	public void close() {
		repository.close();
	}
}
