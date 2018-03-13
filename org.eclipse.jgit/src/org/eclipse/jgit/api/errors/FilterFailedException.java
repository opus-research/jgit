/*
 * Copyright (C) 2015, Christian Halstrick <christian.halstrick@sap.com> and
 * other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v1.0 which accompanies this
 * distribution, is reproduced below, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.api.errors;

import java.text.MessageFormat;

import org.eclipse.jgit.internal.JGitText;

/**
 * Exception thrown when the execution of a filter command failed
 *
 * @since 4.2
 */
public class FilterFailedException extends GitAPIException {
	private static final long serialVersionUID = 1L;

	private String filterCommand;

	private String path;

	private byte[] stdout;

	private String stderr;

	private int rc;

	/**
	 * Thrown if during execution of filter command an exception occurred
	 *
	 * @param cause
	 *            the exception
	 * @param filterCommand
	 *            the command which failed
	 * @param path
	 *            the path processed by the filter
	 */
	public FilterFailedException(Exception cause, String filterCommand,
			String path) {
		super(MessageFormat.format(JGitText.get().filterExecutionFailed,
				filterCommand, path), cause);
		this.filterCommand = filterCommand;
		this.path = path;
	}

	/**
	 * Thrown if a filter command returns a non-zero return code
	 *
	 * @param rc
	 *            the return code
	 * @param filterCommand
	 *            the command which failed
	 * @param path
	 *            the path processed by the filter
	 * @param stdout
	 *            the output the filter generated so far. This should be limited
	 *            to reasonable size.
	 * @param stderr
	 *            the stderr output of the filter
	 */
	@SuppressWarnings("boxing")
	public FilterFailedException(int rc, String filterCommand, String path,
			byte[] stdout, String stderr) {
		super(MessageFormat.format(JGitText.get().filterExecutionFailedRc,
				filterCommand, path, rc));
		this.rc = rc;
		this.filterCommand = filterCommand;
		this.path = path;
		this.stdout = stdout;
		this.stderr = stderr;
	}

	/**
	 * @return the filterCommand
	 */
	public String getFilterCommand() {
		return filterCommand;
	}

	/**
	 * @return the path of the file processed by the filter command
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the output generated by the filter command. Might be truncated to
	 *         limit memory consumption.
	 */
	public byte[] getOutput() {
		return stdout;
	}

	/**
	 * @return the error output returned by the filter command
	 */
	public String getError() {
		return stderr;
	}

	/**
	 * @return the return code returned by the filter command
	 */
	public int getReturnCode() {
		return rc;
	}

}
