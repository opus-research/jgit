/*
 * Copyright (C) 2014, Google Inc.
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
package org.eclipse.jgit.gitrepo;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.gitrepo.internal.RepoText;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A class used to execute a repo command.
 *
 * This will parse a repo XML manifest, convert it into .gitmodules file and the
 * repository config file.
 *
 * @see <a href="https://code.google.com/p/git-repo/">git-repo project page</a>
 * @since 3.4
 */
public class RepoCommand extends GitCommand<Void> {

	private String path;

	private String uri;

	private ProgressMonitor monitor;

	private static class Project {
		final String name;
		final String path;

		Project(String name, String path) {
			this.name = name;
			this.path = path;
		}
	}

	private static class XmlManifest extends DefaultHandler {
		private final RepoCommand command;
		private final String filename;
		private final String baseUrl;
		private final Map<String, String> remotes;
		private final List<Project> projects;
		private String defaultRemote;

		XmlManifest(RepoCommand command, String filename, String baseUrl) {
			this.command = command;
			this.filename = filename;
			this.baseUrl = baseUrl;
			remotes = new HashMap<String, String>();
			projects = new ArrayList<Project>();
		}

		void read() throws IOException {
			final XMLReader xr;
			try {
				xr = XMLReaderFactory.createXMLReader();
			} catch (SAXException e) {
				throw new IOException(JGitText.get().noXMLParserAvailable);
			}
			xr.setContentHandler(this);
			final FileInputStream in = new FileInputStream(filename);
			try {
				xr.parse(new InputSource(in));
			} catch (SAXException e) {
				IOException error = new IOException(MessageFormat.format(
							RepoText.get().errorParsingManifestFile, filename));
				error.initCause(e);
				throw error;
			} finally {
				in.close();
			}
		}

		@Override
		public void startElement(
				String uri,
				String localName,
				String qName,
				Attributes attributes) throws SAXException {
			if ("project".equals(qName)) //$NON-NLS-1$
				projects.add(new Project(attributes.getValue("name"), attributes.getValue("path"))); //$NON-NLS-1$ //$NON-NLS-2$
			else if ("remote".equals(qName)) //$NON-NLS-1$
				remotes.put(attributes.getValue("name"), attributes.getValue("fetch")); //$NON-NLS-1$ //$NON-NLS-2$
			else if ("default".equals(qName)) //$NON-NLS-1$
				defaultRemote = attributes.getValue("remote"); //$NON-NLS-1$
			else if ("copyfile".equals(qName)) { //$NON-NLS-1$
				// TODO(fishywang): Handle copyfile. Do nothing for now.
			}
		}

		@Override
		public void endDocument() throws SAXException {
			if (defaultRemote == null) {
				throw new SAXException(MessageFormat.format(
						RepoText.get().errorNoDefault, filename));
			}
			final String remoteUrl;
			try {
				URI uri = new URI(String.format("%s/%s/", baseUrl, remotes.get(defaultRemote))); //$NON-NLS-1$
				remoteUrl = uri.normalize().toString();
			} catch (URISyntaxException e) {
				throw new SAXException(e);
			}
			for (Project proj : projects) {
				String url = remoteUrl + proj.name;
				command.addSubmodule(url, proj.path);
			}
		}
	}

	private static class ManifestErrorException extends GitAPIException {
		ManifestErrorException(Throwable cause) {
			super(RepoText.get().invalidManifest, cause);
		}
	}

	/**
	 * @param repo
	 */
	public RepoCommand(final Repository repo) {
		super(repo);
	}

	/**
	 * Set path to the manifest XML file
	 *
	 * @param path
	 *            (with <code>/</code> as separator)
	 * @return this command
	 */
	public RepoCommand setPath(final String path) {
		this.path = path;
		return this;
	}

	/**
	 * Set base URI of the pathes inside the XML
	 *
	 * @param uri
	 * @return this command
	 */
	public RepoCommand setURI(final String uri) {
		this.uri = uri;
		return this;
	}

	/**
	 * The progress monitor associated with the clone operation. By default,
	 * this is set to <code>NullProgressMonitor</code>
	 *
	 * @see org.eclipse.jgit.lib.NullProgressMonitor
	 * @param monitor
	 * @return this command
	 */
	public RepoCommand setProgressMonitor(final ProgressMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	@Override
	public Void call() throws GitAPIException {
		checkCallable();
		if (path == null || path.length() == 0)
			throw new IllegalArgumentException(JGitText.get().pathNotConfigured);
		if (uri == null || uri.length() == 0)
			throw new IllegalArgumentException(JGitText.get().uriNotConfigured);

		XmlManifest manifest = new XmlManifest(this, path, uri);
		try {
			manifest.read();
		} catch (IOException e) {
			throw new ManifestErrorException(e);
		}

		return null;
	}

	private void addSubmodule(String url, String name) throws SAXException {
		SubmoduleAddCommand add = new SubmoduleAddCommand(repo);
		if (monitor != null)
			add.setProgressMonitor(monitor);
		try {
			add.setPath(name).setURI(url).call();
		} catch (GitAPIException e) {
			throw new SAXException(e);
		}
	}
}
