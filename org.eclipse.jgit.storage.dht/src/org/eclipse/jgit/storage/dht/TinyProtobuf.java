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

package org.eclipse.jgit.storage.dht;

import static org.eclipse.jgit.lib.Constants.OBJECT_ID_STRING_LENGTH;

import java.text.MessageFormat;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.util.RawParseUtils;

/**
 * A tiny implementation of a subset of the Google Protocol Buffers format.
 * <p>
 * For more information on the network format, see the canonical documentation
 * at <a href="http://code.google.com/p/protobuf/">Google Protocol Buffers</a>.
 */
public class TinyProtobuf {
	private static final int WIRE_VARINT = 0;

	private static final int WIRE_FIXED_64 = 1;

	private static final int WIRE_LEN_DELIM = 2;

	private static final int WIRE_FIXED_32 = 5;

	/**
	 * Create a new encoder.
	 *
	 * @param estimatedSize
	 *            estimated size of the message. If the size is accurate,
	 *            copying of the result can be avoided during
	 *            {@link Encoder#asByteArray()}. If the size is too small, the
	 *            buffer will be dynamically resized on the fly.
	 * @return a new encoder
	 */
	public static Encoder encode(int estimatedSize) {
		return new Encoder(estimatedSize);
	}

	/**
	 * Decode a buffer.
	 *
	 * @param buf
	 *            the buffer to read.
	 * @return a new decoder
	 */
	public static Decoder decode(byte[] buf) {
		return decode(buf, 0, buf.length);
	}

	/**
	 * Decode a buffer.
	 *
	 * @param buf
	 *            the buffer to read.
	 * @param off
	 *            offset to begin reading from {@code buf}.
	 * @param len
	 *            total number of bytes to read from {@code buf}.
	 * @return a new decoder
	 */
	public static Decoder decode(byte[] buf, int off, int len) {
		return new Decoder(buf, off, len);
	}

	/** Decode fields from a binary protocol buffer. */
	public static class Decoder {
		private final byte[] buf;

		private final int end;

		private int ptr;

		private int field;

		private int type;

		private Decoder(byte[] buf, int off, int len) {
			this.buf = buf;
			this.ptr = off;
			this.end = off + len;
		}

		/** @return get the field tag number, 0 on end of buffer. */
		public int next() {
			if (ptr == end)
				return 0;

			int fieldAndType = varint();
			field = fieldAndType >>> 3;
			type = fieldAndType & 7;
			return field;
		}

		/** Skip the current field's value. */
		public void skip() {
			switch (type) {
			case WIRE_VARINT:
				varint();
				break;
			case WIRE_FIXED_64:
				ptr += 8;
				break;
			case WIRE_LEN_DELIM:
				ptr += varint();
				break;
			case WIRE_FIXED_32:
				ptr += 4;
				break;
			default:
				throw new IllegalStateException(MessageFormat.format(
						DhtText.get().protobufUnsupportedFieldType, type));
			}
		}

		/** @return decode the current field as an int32. */
		public int int32() {
			checkFieldType(WIRE_VARINT);
			return varint();
		}

		/** @return decode the current field as a bool. */
		public boolean bool() {
			checkFieldType(WIRE_VARINT);
			int val = varint();
			switch (val) {
			case 0:
				return false;
			case 1:
				return true;
			default:
				throw new IllegalStateException(MessageFormat.format(
						DhtText.get().protobufNotBooleanValue, field, val));
			}
		}

		/** @return decode the current field as a string. */
		public String string() {
			checkFieldType(WIRE_LEN_DELIM);
			int len = varint();
			String s = RawParseUtils.decode(buf, ptr, ptr + len);
			ptr += len;
			return s;
		}

		/** @return decode the current hex string to an ObjectId. */
		public ObjectId stringObjectId() {
			checkFieldType(WIRE_LEN_DELIM);
			int len = varint();
			if (len != OBJECT_ID_STRING_LENGTH)
				throw new IllegalStateException(MessageFormat.format(
						DhtText.get().protobufWrongFieldLength, field,
						OBJECT_ID_STRING_LENGTH, len));

			ObjectId id = ObjectId.fromString(buf, ptr);
			ptr += OBJECT_ID_STRING_LENGTH;
			return id;
		}

		/** @return decode the current field as an array of bytes. */
		public byte[] bytes() {
			checkFieldType(WIRE_LEN_DELIM);
			byte[] r = new byte[varint()];
			System.arraycopy(buf, ptr, r, 0, r.length);
			ptr += r.length;
			return r;
		}

		/** @return decode the current field as a nested message. */
		public Decoder message() {
			checkFieldType(WIRE_LEN_DELIM);
			int len = varint();
			Decoder msg = decode(buf, ptr, len);
			ptr += len;
			return msg;
		}

		private int varint() {
			int c = buf[ptr++];
			int r = c & 0x7f;

			if ((c & 0x80) == 0)
				return r;

			c = buf[ptr++];
			r |= (c & 0x7f) << 7;

			if ((c & 0x80) == 0)
				return r;

			c = buf[ptr++];
			r |= (c & 0x7f) << 14;

			if ((c & 0x80) == 0)
				return r;

			c = buf[ptr++];
			r |= (c & 0x7f) << 21;

			if ((c & 0x80) == 0)
				return r;

			c = buf[ptr++];
			return r | ((c & 0x7f) << 28);
		}

		private void checkFieldType(int expected) {
			if (type != expected)
				throw new IllegalStateException(MessageFormat.format(
						DhtText.get().protobufWrongFieldType, field, type,
						expected));
		}
	}

	/** Encode values into a binary protocol buffer. */
	public static class Encoder {
		private byte[] buf;

		private int ptr;

		private Encoder(int estimatedSize) {
			buf = new byte[estimatedSize];
		}

		/**
		 * Encode a variable length positive integer.
		 *
		 * @param field
		 *            field tag number.
		 * @param value
		 *            the value to store. Must be >= 0.
		 */
		public void int32(int field, int value) {
			if (value < 0)
				throw new IllegalArgumentException(
						DhtText.get().protobufNegativeValuesNotSupported);

			field(field, WIRE_VARINT);
			varint(value);
		}

		/**
		 * Encode a boolean value.
		 *
		 * @param field
		 *            field tag number.
		 * @param value
		 *            the value to store.
		 */
		public void bool(int field, boolean value) {
			field(field, WIRE_VARINT);
			varint(value ? 1 : 0);
		}

		/**
		 * Encode a length delimited bytes field.
		 *
		 * @param field
		 *            field tag number.
		 * @param value
		 *            the value to store.
		 */
		public void bytes(int field, byte[] value) {
			bytes(field, value, 0, value.length);
		}

		/**
		 * Encode a length delimited bytes field.
		 *
		 * @param field
		 *            field tag number.
		 * @param value
		 *            the value to store.
		 * @param off
		 *            position to copy from.
		 * @param len
		 *            number of bytes to copy.
		 */
		public void bytes(int field, byte[] value, int off, int len) {
			field(field, WIRE_LEN_DELIM);
			varint(len);
			copy(value, off, len);
		}

		/**
		 * Encode an ObjectId as a string (in hex format).
		 *
		 * @param field
		 *            field tag number.
		 * @param value
		 *            the value to store, as a hex string.
		 */
		public void string(int field, AnyObjectId value) {
			field(field, WIRE_LEN_DELIM);
			varint(OBJECT_ID_STRING_LENGTH);
			ensureSpace(OBJECT_ID_STRING_LENGTH);
			value.copyTo(buf, ptr);
			ptr += OBJECT_ID_STRING_LENGTH;
		}

		/**
		 * Encode a plain Java string.
		 *
		 * @param field
		 *            field tag number.
		 * @param value
		 *            the value to store, in raw binary.
		 */
		public void string(int field, String value) {
			bytes(field, Constants.encode(value));
		}

		/**
		 * Encode a row key as a string.
		 *
		 * @param field
		 *            field tag number.
		 * @param key
		 *            the row key to store as a string.
		 */
		public void string(int field, RowKey key) {
			bytes(field, key.asByteArray());
		}

		/**
		 * Encode a nested message.
		 *
		 * @param field
		 *            field tag number.
		 * @param msg
		 *            message to store.
		 */
		public void message(int field, Encoder msg) {
			bytes(field, msg.buf, 0, msg.ptr);
		}

		private void field(int field, int type) {
			varint((field << 3) | type);
		}

		private void varint(int value) {
			if (buf.length - ptr < 5) {
				int v = value >>> 7;
				int need = 1;
				for (; v != 0; v >>>= 7)
					need++;
				ensureSpace(need);
			}

			do {
				byte b = (byte) (value & 0x7f);
				value >>>= 7;
				if (value != 0)
					b |= 0x80;
				buf[ptr++] = b;
			} while (value != 0);
		}

		private void copy(byte[] src, int off, int cnt) {
			ensureSpace(cnt);
			System.arraycopy(src, off, buf, ptr, cnt);
			ptr += cnt;
		}

		private void ensureSpace(int need) {
			if (buf.length - ptr < need) {
				byte[] n = new byte[Math.max(ptr + need, buf.length * 2)];
				System.arraycopy(buf, 0, n, 0, ptr);
				buf = n;
			}
		}

		/** @return the current buffer, as a byte array. */
		public byte[] asByteArray() {
			if (ptr == buf.length)
				return buf;
			byte[] r = new byte[ptr];
			System.arraycopy(buf, 0, r, 0, ptr);
			return r;
		}
	}

	private TinyProtobuf() {
		// Don't make instances.
	}
}
