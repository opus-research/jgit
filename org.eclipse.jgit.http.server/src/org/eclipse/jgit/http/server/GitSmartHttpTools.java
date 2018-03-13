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

package org.eclipse.jgit.http.server;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.eclipse.jgit.http.server.ServletUtils.ATTRIBUTE_HANDLER;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_SIDE_BAND;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_SIDE_BAND_64K;
import static org.eclipse.jgit.transport.GitProtocolConstants.CAPABILITY_SIDE_BAND_64K;
import static org.eclipse.jgit.transport.SideBandOutputStream.CH_ERROR;
import static org.eclipse.jgit.transport.SideBandOutputStream.SMALL_BUF;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.PacketLineIn;
import org.eclipse.jgit.transport.PacketLineOut;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RequestNotYetReadException;
import org.eclipse.jgit.transport.SideBandOutputStream;
import org.eclipse.jgit.transport.UploadPack;

/**
 * Utility functions for handling the Git-over-HTTP protocol.
 */
public class GitSmartHttpTools {
	private static final String INFO_REFS = Constants.INFO_REFS;

	/** Name of the git-upload-pack service. */
	public static final String UPLOAD_PACK = "git-upload-pack";

	/** Name of the git-receive-pack service. */
	public static final String RECEIVE_PACK = "git-receive-pack";

	/** Content type supplied by the client to the /git-upload-pack handler. */
	public static final String UPLOAD_PACK_REQUEST_TYPE =
			"application/x-git-upload-pack-request";

	/** Content type returned from the /git-upload-pack handler. */
	public static final String UPLOAD_PACK_RESULT_TYPE =
			"application/x-git-upload-pack-result";

	/** Content type supplied by the client to the /git-receive-pack handler. */
	public static final String RECEIVE_PACK_REQUEST_TYPE =
			"application/x-git-receive-pack-request";

	/** Content type returned from the /git-receive-pack handler. */
	public static final String RECEIVE_PACK_RESULT_TYPE =
			"application/x-git-receive-pack-result";

	/** Git service names accepted by the /info/refs?service= handler. */
	public static final List<String> VALID_SERVICES =
			Collections.unmodifiableList(Arrays.asList(new String[] {
					UPLOAD_PACK, RECEIVE_PACK }));

	private static final String INFO_REFS_PATH = "/" + INFO_REFS;
	private static final String UPLOAD_PACK_PATH = "/" + UPLOAD_PACK;
	private static final String RECEIVE_PACK_PATH = "/" + RECEIVE_PACK;

	private static final List<String> SERVICE_SUFFIXES =
			Collections.unmodifiableList(Arrays.asList(new String[] {
					INFO_REFS_PATH, UPLOAD_PACK_PATH, RECEIVE_PACK_PATH }));

	/**
	 * Check a request for Git-over-HTTP indicators.
	 *
	 * @param req
	 *            the current HTTP request that may have been made by Git.
	 * @return true if the request is likely made by a Git client program.
	 */
	public static boolean isGitClient(HttpServletRequest req) {
		return isInfoRefs(req) || isUploadPack(req) || isReceivePack(req);
	}

	/**
	 * Send an error to the Git client or browser.
	 * <p>
	 * Server implementors may use this method to send customized error messages
	 * to a Git protocol client using an HTTP 200 OK response with the error
	 * embedded in the payload. If the request was not issued by a Git client,
	 * an HTTP response code is returned instead.
	 *
	 * @param req
	 *            current request.
	 * @param res
	 *            current response.
	 * @param httpStatus
	 *            HTTP status code to set if the client is not a Git client.
	 * @throws IOException
	 *             the response cannot be sent.
	 */
	public static void sendError(HttpServletRequest req,
			HttpServletResponse res, int httpStatus) throws IOException {
		sendError(req, res, httpStatus, null);
	}

	/**
	 * Send an error to the Git client or browser.
	 * <p>
	 * Server implementors may use this method to send customized error messages
	 * to a Git protocol client using an HTTP 200 OK response with the error
	 * embedded in the payload. If the request was not issued by a Git client,
	 * an HTTP response code is returned instead.
	 * <p>
	 * This method may only be called before handing off the request to
	 * {@link UploadPack#upload(java.io.InputStream, OutputStream, OutputStream)}
	 * or
	 * {@link ReceivePack#receive(java.io.InputStream, OutputStream, OutputStream)}.
	 *
	 * @param req
	 *            current request.
	 * @param res
	 *            current response.
	 * @param httpStatus
	 *            HTTP status code to set if the client is not a Git client.
	 * @param textForGit
	 *            plain text message to display on the user's console. This is
	 *            shown only if the client is likely to be a Git client. If null
	 *            or the empty string a default text is chosen based on the HTTP
	 *            response code.
	 * @throws IOException
	 *             the response cannot be sent.
	 */
	public static void sendError(HttpServletRequest req,
			HttpServletResponse res, int httpStatus, String textForGit)
			throws IOException {
		if (textForGit == null || textForGit.length() == 0) {
			switch (httpStatus) {
			case SC_FORBIDDEN:
				textForGit = HttpServerText.get().repositoryAccessForbidden;
				break;
			case SC_NOT_FOUND:
				textForGit = HttpServerText.get().repositoryNotFound;
				break;
			case SC_INTERNAL_SERVER_ERROR:
				textForGit = HttpServerText.get().internalServerError;
				break;
			default:
				textForGit = "HTTP " + httpStatus;
				break;
			}
		}

		if (isInfoRefs(req)) {
			sendInfoRefsError(req, res, textForGit);
		} else if (isUploadPack(req)) {
			sendUploadPackError(req, res, textForGit);
		} else if (isReceivePack(req)) {
			sendReceivePackError(req, res, textForGit);
		} else {
			if (httpStatus < 400)
				ServletUtils.consumeRequestBody(req);
			res.sendError(httpStatus);
		}
	}

	private static void sendInfoRefsError(HttpServletRequest req,
			HttpServletResponse res, String textForGit) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
		PacketLineOut pck = new PacketLineOut(buf);
		String svc = req.getParameter("service");
		pck.writeString("# service=" + svc + "\n");
		pck.end();
		pck.writeString("ERR " + textForGit);
		send(req, res, infoRefsResultType(svc), buf.toByteArray());
	}

	private static void sendUploadPackError(HttpServletRequest req,
			HttpServletResponse res, String textForGit) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
		PacketLineOut pckOut = new PacketLineOut(buf);

		boolean sideband;
		UploadPack up = (UploadPack) req.getAttribute(ATTRIBUTE_HANDLER);
		if (up != null) {
			try {
				sideband = up.isSideBand();
			} catch (RequestNotYetReadException e) {
				sideband = isUploadPackSideBand(req);
			}
		} else
			sideband = isUploadPackSideBand(req);

		if (sideband)
			writeSideBand(buf, textForGit);
		else
			writePacket(pckOut, textForGit);
		send(req, res, UPLOAD_PACK_RESULT_TYPE, buf.toByteArray());
	}

	private static boolean isUploadPackSideBand(HttpServletRequest req) {
		try {
			// The client may be in a state where they have sent the sideband
			// capability and are expecting a response in the sideband, but we might
			// not have an UploadPack, or it might not have read any of the request.
			// So, cheat and read the first line.
			String line = new PacketLineIn(req.getInputStream()).readString();
			UploadPack.FirstLine parsed = new UploadPack.FirstLine(line);
			return (parsed.getOptions().contains(OPTION_SIDE_BAND)
					|| parsed.getOptions().contains(OPTION_SIDE_BAND_64K));
		} catch (IOException e) {
			// Probably the connection is closed and a subsequent write will fail, but
			// try it just in case.
			return false;
		}
	}

	private static void sendReceivePackError(HttpServletRequest req,
			HttpServletResponse res, String textForGit) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
		PacketLineOut pckOut = new PacketLineOut(buf);

		boolean sideband;
		ReceivePack rp = (ReceivePack) req.getAttribute(ATTRIBUTE_HANDLER);
		if (rp != null) {
			try {
				sideband = rp.isSideBand();
			} catch (RequestNotYetReadException e) {
				sideband = isReceivePackSideBand(req);
			}
		} else
			sideband = isReceivePackSideBand(req);

		if (sideband)
			writeSideBand(buf, textForGit);
		else
			writePacket(pckOut, textForGit);
		send(req, res, RECEIVE_PACK_RESULT_TYPE, buf.toByteArray());
	}

	private static boolean isReceivePackSideBand(HttpServletRequest req) {
		try {
			// The client may be in a state where they have sent the sideband
			// capability and are expecting a response in the sideband, but we might
			// not have a ReceivePack, or it might not have read any of the request.
			// So, cheat and read the first line.
			String line = new PacketLineIn(req.getInputStream()).readString();
			ReceivePack.FirstLine parsed = new ReceivePack.FirstLine(line);
			return parsed.getCapabilities().contains(CAPABILITY_SIDE_BAND_64K);
		} catch (IOException e) {
			// Probably the connection is closed and a subsequent write will fail, but
			// try it just in case.
			return false;
		}
	}

	private static void writeSideBand(OutputStream out, String textForGit)
			throws IOException {
		@SuppressWarnings("resource" /* java 7 */)
		OutputStream msg = new SideBandOutputStream(CH_ERROR, SMALL_BUF, out);
		msg.write(Constants.encode("error: " + textForGit));
		msg.flush();
	}

	private static void writePacket(PacketLineOut pckOut, String textForGit)
			throws IOException {
		pckOut.writeString("error: " + textForGit);
	}

	private static void send(HttpServletRequest req, HttpServletResponse res,
			String type, byte[] buf) throws IOException {
		ServletUtils.consumeRequestBody(req);
		res.setStatus(SC_OK);
		res.setContentType(type);
		res.setContentLength(buf.length);
		OutputStream os = res.getOutputStream();
		try {
			os.write(buf);
		} finally {
			os.close();
		}
	}

	/**
	 * Get the response Content-Type a client expects for the request.
	 * <p>
	 * This method should only be invoked if
	 * {@link #isGitClient(HttpServletRequest)} is true.
	 *
	 * @param req
	 *            current request.
	 * @return the Content-Type the client expects.
	 * @throws IllegalArgumentException
	 *             the request is not a Git client request. See
	 *             {@link #isGitClient(HttpServletRequest)}.
	 */
	public static String getResponseContentType(HttpServletRequest req) {
		if (isInfoRefs(req))
			return infoRefsResultType(req.getParameter("service"));
		else if (isUploadPack(req))
			return UPLOAD_PACK_RESULT_TYPE;
		else if (isReceivePack(req))
			return RECEIVE_PACK_RESULT_TYPE;
		else
			throw new IllegalArgumentException();
	}

	static String infoRefsResultType(String svc) {
		return "application/x-" + svc + "-advertisement";
	}

	/**
	 * Strip the Git service suffix from a request path.
	 *
	 * Generally the suffix is stripped by the {@code SuffixPipeline} handling
	 * the request, so this method is rarely needed.
	 *
	 * @param path
	 *            the path of the request.
	 * @return the path up to the last path component before the service suffix;
	 *         the path as-is if it contains no service suffix.
	 */
	public static String stripServiceSuffix(String path) {
		for (String suffix : SERVICE_SUFFIXES) {
			if (path.endsWith(suffix))
				return path.substring(0, path.length() - suffix.length());
		}
		return path;
	}

	/**
	 * Check if the HTTP request was for the /info/refs?service= Git handler.
	 *
	 * @param req
	 *            current request.
	 * @return true if the request is for the /info/refs service.
	 */
	public static boolean isInfoRefs(HttpServletRequest req) {
		return req.getRequestURI().endsWith(INFO_REFS_PATH)
				&& VALID_SERVICES.contains(req.getParameter("service"));
	}

	/**
	 * Check if the HTTP request path ends with the /git-upload-pack handler.
	 *
	 * @param pathOrUri
	 *            path or URI of the request.
	 * @return true if the request is for the /git-upload-pack handler.
	 */
	public static boolean isUploadPack(String pathOrUri) {
		return pathOrUri != null && pathOrUri.endsWith(UPLOAD_PACK_PATH);
	}

	/**
	 * Check if the HTTP request was for the /git-upload-pack Git handler.
	 *
	 * @param req
	 *            current request.
	 * @return true if the request is for the /git-upload-pack handler.
	 */
	public static boolean isUploadPack(HttpServletRequest req) {
		return isUploadPack(req.getRequestURI())
				&& UPLOAD_PACK_REQUEST_TYPE.equals(req.getContentType());
	}

	/**
	 * Check if the HTTP request was for the /git-receive-pack Git handler.
	 *
	 * @param req
	 *            current request.
	 * @return true if the request is for the /git-receive-pack handler.
	 */
	public static boolean isReceivePack(HttpServletRequest req) {
		String uri = req.getRequestURI();
		return uri != null && uri.endsWith(RECEIVE_PACK_PATH)
				&& RECEIVE_PACK_REQUEST_TYPE.equals(req.getContentType());
	}

	private GitSmartHttpTools() {
	}
}
