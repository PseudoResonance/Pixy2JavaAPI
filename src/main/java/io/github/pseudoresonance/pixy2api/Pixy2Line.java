package io.github.pseudoresonance.pixy2api;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Java Port of Pixy2 Arduino Library
 * 
 * Defines line tracker for Pixy2
 * 
 * https://github.com/PseudoResonance/Pixy2JavaAPI
 * 
 * @author PseudoResonance (Josh Otake)
 *
 *         ORIGINAL HEADER -
 *         https://github.com/charmedlabs/pixy2/blob/master/src/host/arduino/libraries/Pixy2/Pixy2Line.h
 *         =============================================================================================
 *         begin license header
 *
 *         This file is part of Pixy CMUcam5 or "Pixy" for short
 *
 *         All Pixy source code is provided under the terms of the GNU General
 *         Public License v2 (http://www.gnu.org/licenses/gpl-2.0.html). Those
 *         wishing to use Pixy source code, software and/or technologies under
 *         different licensing terms should contact us at cmucam@cs.cmu.edu.
 *         Such licensing terms are available for all portions of the Pixy
 *         codebase presented here.
 *
 *         end license header
 *
 *         This file is for defining the Block struct and the Pixy template
 *         class version 2. (TPixy2). TPixy takes a communication link as a
 *         template parameter so that all communication modes (SPI, I2C and
 *         UART) can share the same code.
 */

public class Pixy2Line {
	public final static byte LINE_REQUEST_GET_FEATURES = 0x30;
	public final static byte LINE_RESPONSE_GET_FEATURES = 0x31;
	public final static byte LINE_REQUEST_SET_MODE = 0x36;
	public final static byte LINE_REQUEST_SET_VECTOR = 0x38;
	public final static byte LINE_REQUEST_SET_NEXT_TURN_ANGLE = 0x3a;
	public final static byte LINE_REQUEST_SET_DEFAULT_TURN_ANGLE = 0x3c;
	public final static byte LINE_REQUEST_REVERSE_VECTOR = 0x3e;

	public final static byte LINE_GET_MAIN_FEATURES = 0x00;
	public final static byte LINE_GET_ALL_FEATURES = 0x01;

	public final static byte LINE_MODE_TURN_DELAYED = 0x01;
	public final static byte LINE_MODE_MANUAL_SELECT_VECTOR = 0x02;
	public final static byte LINE_MODE_WHITE_LINE = (byte) 0x80;

	// Features
	public final static byte LINE_VECTOR = 0x01;
	public final static byte LINE_INTERSECTION = 0x02;
	public final static byte LINE_BARCODE = 0x04;
	public final static byte LINE_ALL_FEATURES = (LINE_VECTOR | LINE_INTERSECTION | LINE_BARCODE);

	public final static byte LINE_FLAG_INVALID = 0x02;
	public final static byte LINE_FLAG_INTERSECTION_PRESENT = 0x04;

	public final static byte LINE_MAX_INTERSECTION_LINES = 6;

	private final Pixy2 pixy;

	private Vector[] vectors = null;

	private Intersection[] intersections = null;

	private Barcode[] barcodes = null;

	/**
	 * Constructs Pixy2 Line Tracker
	 * 
	 * @param pixy Pixy2 instance
	 */
	protected Pixy2Line(Pixy2 pixy) {
		this.pixy = pixy;
	}

	/**
	 * <p>Gets all features from Pixy2</p>
	 * 
	 * <p>Defaults to getting all available features and waiting for a response</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getVectorCache()}, {@link #getIntersectionCache()} or {@link #getBarcodeCache()}</p>
	 * 
	 * @return Pixy2 error code
	 */
	public byte getAllFeatures() {
		return getFeatures(LINE_GET_ALL_FEATURES, LINE_ALL_FEATURES, true);
	}

	/**
	 * <p>Gets the main features from the Pixy2. This is a more constrained line
	 * tracking algorithm.</p>
	 * 
	 * <p>Defaults to getting all available features and waiting for a response</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getVectorCache()}, {@link #getIntersectionCache()} or {@link #getBarcodeCache()}</p>
	 * 
	 * @return Pixy2 error code
	 */
	public byte getMainFeatures() {
		return getFeatures(LINE_GET_MAIN_FEATURES, LINE_ALL_FEATURES, true);
	}

	/**
	 * <p>Gets all features from Pixy2</p>
	 * 
	 * <p>Defaults to waiting for a response</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getVectorCache()}, {@link #getIntersectionCache()} or {@link #getBarcodeCache()}</p>
	 * 
	 * @param features Features to get
	 * 
	 * @return Pixy2 error code
	 */
	public byte getAllFeatures(byte features) {
		return getFeatures(LINE_GET_ALL_FEATURES, features, true);
	}

	/**
	 * <p>Gets the main features from the Pixy2. This is a more constrained line
	 * tracking algorithm.</p>
	 * 
	 * <p>Defaults to waiting for a response</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getVectorCache()}, {@link #getIntersectionCache()} or {@link #getBarcodeCache()}</p>
	 * 
	 * @param features Features to get
	 * 
	 * @return Pixy2 error code
	 */
	public byte getMainFeatures(byte features) {
		return getFeatures(LINE_GET_MAIN_FEATURES, features, true);
	}

	/**
	 * <p>Gets all features from Pixy2</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getVectorCache()}, {@link #getIntersectionCache()} or {@link #getBarcodeCache()}</p>
	 * 
	 * @param features Features to get
	 * @param wait     Wait for response
	 * 
	 * @return Pixy2 error code
	 */
	public byte getAllFeatures(byte features, boolean wait) {
		return getFeatures(LINE_GET_ALL_FEATURES, features, wait);
	}

	/**
	 * <p>Gets the main features from the Pixy2. This is a more constrained line
	 * tracking algorithm.</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getVectorCache()}, {@link #getIntersectionCache()} or {@link #getBarcodeCache()}</p>
	 * 
	 * @param features Features to get
	 * @param wait     Wait for response
	 * 
	 * @return Pixy2 error code
	 */
	public byte getMainFeatures(byte features, boolean wait) {
		return getFeatures(LINE_GET_MAIN_FEATURES, features, wait);
	}

	/**
	 * <p>Gets specified features from Pixy2</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getVectorCache()}, {@link #getIntersectionCache()} or {@link #getBarcodeCache()}</p>
	 * 
	 * @param type     Type of features to get
	 * @param features Features to get
	 * @param wait     Wait for response
	 * 
	 * @return Pixy2 error code
	 */
	private byte getFeatures(byte type, byte features, boolean wait) {
		byte res;
		int offset, fsize, ftype;
		byte[] fdata;

		vectors = null;
		intersections = null;
		barcodes = null;

		long start = System.currentTimeMillis();

		while (true) {
			// Fill in request data
			pixy.length = 2;
			pixy.type = LINE_REQUEST_GET_FEATURES;
			pixy.bufferPayload[0] = type;
			pixy.bufferPayload[1] = features;

			// Send request
			pixy.sendPacket();
			if (pixy.receivePacket() == 0) {
				if (pixy.type == LINE_RESPONSE_GET_FEATURES) {
					// Parse line response
					for (offset = 0, res = 0; pixy.length > offset; offset += fsize + 2) {
						ftype = pixy.buffer[offset];
						fsize = pixy.buffer[offset + 1];
						fdata = Arrays.copyOfRange(pixy.buffer, offset + 2, pixy.length);
						if (ftype == LINE_VECTOR) {
							// Parse line data
							vectors = new Vector[(int) Math.floor(fdata.length / 6)];
							for (int i = 0; (i + 1) * 6 <= fdata.length; i++) {
								vectors[i] = new Vector(fdata[(6 * i)] & 0xFF, fdata[(6 * i) + 1] & 0xFF,
										fdata[(6 * i) + 2] & 0xFF, fdata[(6 * i) + 3] & 0xFF, fdata[(6 * i) + 4] & 0xFF,
										fdata[(6 * i) + 5] & 0xFF);
							}
							res |= LINE_VECTOR;
						} else if (ftype == LINE_INTERSECTION) {
							// Parse intersection data
							int size = 4 + (4 * LINE_MAX_INTERSECTION_LINES);
							intersections = new Intersection[(int) Math
									.floor(fdata.length / (4 + (4 * LINE_MAX_INTERSECTION_LINES)))];
							for (int i = 0; (i + 1) * size < fdata.length; i++) {
								IntersectionLine[] lines = new IntersectionLine[LINE_MAX_INTERSECTION_LINES];
								for (int l = 0; l < LINE_MAX_INTERSECTION_LINES; l++) {
									int arr = ((size * i) + 4);
									int index = fdata[arr + (4 * l)];
									int reserved = fdata[arr + (4 * l) + 1];
									short angle = (short) (((fdata[arr + (4 * l) + 3] & 0xff) << 8)
											| (fdata[arr + (4 * l) + 2] & 0xff));
									IntersectionLine intLine = new IntersectionLine(index, reserved, angle);
									lines[l] = intLine;
								}
								intersections[i] = new Intersection(fdata[size * i] & 0xFF,
										fdata[(size * i) + 1] & 0xFF, fdata[(size * i) + 2] & 0xFF,
										fdata[(size * i) + 3] & 0xFF, lines);
							}
							res |= LINE_INTERSECTION;
						} else if (ftype == LINE_BARCODE) {
							// Parse barcode data
							barcodes = new Barcode[(int) Math.floor(fdata.length / 4)];
							for (int i = 0; (i + 1) * 4 <= fdata.length; i++) {
								barcodes[i] = new Barcode(fdata[(4 * i)] & 0xFF, fdata[(4 * i) + 1] & 0xFF,
										fdata[(4 * i) + 2] & 0xFF, fdata[(4 * i) + 3] & 0xFF);
							}
							res |= LINE_BARCODE;
						} else
							break; // Parse error
					}
					return res; // Success
				} else if (pixy.type == Pixy2.PIXY_TYPE_RESPONSE_ERROR) {
					// If it's not a busy response, return the error
					if (pixy.buffer[0] != Pixy2.PIXY_RESULT_BUSY)
						return pixy.buffer[0];
					else if (!wait) // We're busy
						return Pixy2.PIXY_RESULT_BUSY; // New data not available yet
				}
			} else
				return Pixy2.PIXY_RESULT_ERROR; // Some kind of bitstream error

			if (System.currentTimeMillis() - start > 500) {
				return Pixy2.PIXY_RESULT_ERROR; // Timeout to prevent lockup
			}
			// If we're waiting for frame data, don't thrash Pixy with requests.
			// We can give up half a millisecond of latency (worst case)
			try {
				TimeUnit.MICROSECONDS.sleep(500);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * <p>Gets detected lines from cache</p>
	 * 
	 * <p>{@link #getFeatures(byte, byte, boolean)} must be executed first to get the data actual from Pixy2</p>
	 * 
	 * @return Pixy2 Lines
	 */
	public Vector[] getVectorCache() {
		return vectors;
	}

	/**
	 * <p>Gets detected intersections from cache</p>
	 * 
	 * <p>{@link #getFeatures(byte, byte, boolean)} must be executed first to get the data actual from Pixy2</p>
	 * 
	 * @return Pixy2 Intersectionss
	 */
	public Intersection[] getIntersectionCache() {
		return intersections;
	}

	/**
	 * <p>Gets detected barcodes from cache</p>
	 * 
	 * <p>{@link #getFeatures(byte, byte, boolean)} must be executed first to get the data actual from Pixy2</p>
	 * 
	 * @return Pixy2 Barcodes
	 */
	public Barcode[] getBarcodeCache() {
		return barcodes;
	}

	/**
	 * Sets Pixy2 line tracking mode
	 * 
	 * @param mode Pixy2 line tracking mode
	 * 
	 * @return Pixy2 error code
	 */
	public byte setMode(int mode) {
		int res;

		pixy.bufferPayload[0] = (byte) (mode & 0xff);
		pixy.bufferPayload[1] = (byte) ((mode >> 8) & 0xff);
		pixy.bufferPayload[2] = (byte) ((mode >> 16) & 0xff);
		pixy.bufferPayload[3] = (byte) ((mode >> 24) & 0xff);
		pixy.length = 1;
		pixy.type = LINE_REQUEST_SET_MODE;
		pixy.sendPacket();
		if (pixy.receivePacket() == 0 && pixy.type == Pixy2.PIXY_TYPE_RESPONSE_RESULT && pixy.length == 4) {
			res = ((pixy.buffer[3] & 0xff) << 24) | ((pixy.buffer[2] & 0xff) << 16) | ((pixy.buffer[1] & 0xff) << 8)
					| (pixy.buffer[0] & 0xff);
			return (byte) res; // Success
		} else
			return Pixy2.PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	/**
	 * Sets turn angle to use for next intersection
	 * 
	 * @param angle Turn angle
	 * 
	 * @return Pixy2 error code
	 */
	public byte setNextTurn(short angle) {
		int res;

		pixy.bufferPayload[0] = (byte) (angle & 0xff);
		pixy.bufferPayload[1] = (byte) ((angle >> 8) & 0xff);
		pixy.length = 2;
		pixy.type = LINE_REQUEST_SET_NEXT_TURN_ANGLE;
		pixy.sendPacket();
		if (pixy.receivePacket() == 0 && pixy.type == Pixy2.PIXY_TYPE_RESPONSE_RESULT && pixy.length == 4) {
			res = ((pixy.buffer[3] & 0xff) << 24) | ((pixy.buffer[2] & 0xff) << 16) | ((pixy.buffer[1] & 0xff) << 8)
					| (pixy.buffer[0] & 0xff);
			return (byte) res; // Success
		} else
			return Pixy2.PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	/**
	 * Sets default angle to turn to at an intersection
	 * 
	 * @param angle Turn angle
	 * 
	 * @return Pixy2 error code
	 */
	public byte setDefaultTurn(short angle) {
		int res;

		pixy.bufferPayload[0] = (byte) (angle & 0xff);
		pixy.bufferPayload[1] = (byte) ((angle >> 8) & 0xff);
		pixy.length = 2;
		pixy.type = LINE_REQUEST_SET_DEFAULT_TURN_ANGLE;
		pixy.sendPacket();
		if (pixy.receivePacket() == 0 && pixy.type == Pixy2.PIXY_TYPE_RESPONSE_RESULT && pixy.length == 4) {
			res = ((pixy.buffer[3] & 0xff) << 24) | ((pixy.buffer[2] & 0xff) << 16) | ((pixy.buffer[1] & 0xff) << 8)
					| (pixy.buffer[0] & 0xff);
			return (byte) res; // Success
		} else
			return Pixy2.PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	/**
	 * Choose vector to track manually
	 * 
	 * @param index Index of vector
	 * 
	 * @return Pixy2 error code
	 */
	public byte setVector(int index) {
		int res;

		pixy.bufferPayload[0] = (byte) index;
		pixy.length = 1;
		pixy.type = LINE_REQUEST_SET_VECTOR;
		pixy.sendPacket();
		if (pixy.receivePacket() == 0 && pixy.type == Pixy2.PIXY_TYPE_RESPONSE_RESULT && pixy.length == 4) {
			res = ((pixy.buffer[3] & 0xff) << 24) | ((pixy.buffer[2] & 0xff) << 16) | ((pixy.buffer[1] & 0xff) << 8)
					| (pixy.buffer[0] & 0xff);
			return (byte) res; // Success
		} else
			return Pixy2.PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	/**
	 * Requests to invert vector
	 * 
	 * @return Pixy2 error code
	 */
	public byte reverseVector() {
		int res;

		pixy.length = 0;
		pixy.type = LINE_REQUEST_REVERSE_VECTOR;
		pixy.sendPacket();
		if (pixy.receivePacket() == 0 && pixy.type == Pixy2.PIXY_TYPE_RESPONSE_RESULT && pixy.length == 4) {
			res = ((pixy.buffer[3] & 0xff) << 24) | ((pixy.buffer[2] & 0xff) << 16) | ((pixy.buffer[1] & 0xff) << 8)
					| (pixy.buffer[0] & 0xff);
			return (byte) res; // Success
		} else
			return Pixy2.PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	public static class Vector {

		private int x0, y0, x1, y1, index, flags;

		/**
		 * Constructs Vector instance
		 * 
		 * @param x0    X0 value
		 * @param y0    Y0 value
		 * @param x1    X1 value
		 * @param y1    Y1 value
		 * @param index Vector index
		 * @param flags Vector flags
		 */
		public Vector(int x0, int y0, int x1, int y1, int index, int flags) {
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
			this.index = index;
			this.flags = flags;
		}

		/**
		 * Prints vector data to console
		 */
		public void print() {
			System.out.println(toString());
		}

		/**
		 * Returns a string of vector data
		 * 
		 * @return String of vector data
		 */
		public String toString() {
			return "vector: (" + x0 + " " + y0 + ") (" + x1 + " " + y1 + ") index: " + index + " flags: " + flags;
		}

		/**
		 * @return X0 value
		 */
		public int getX0() {
			return x0;
		}

		/**
		 * @return Y0 value
		 */
		public int getY0() {
			return y0;
		}

		/**
		 * @return X1 value
		 */
		public int getX1() {
			return x1;
		}

		/**
		 * @return Y1 value
		 */
		public int getY1() {
			return y1;
		}

		/**
		 * @return Vector index
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return Vector flags
		 */
		public int getFlags() {
			return flags;
		}

	}

	public static class IntersectionLine {

		private int index, reserved;
		private short angle;

		/**
		 * Constructs IntersectionLine object
		 * 
		 * @param index    IntersectionLine index
		 * @param reserved Reserved
		 * @param angle    Line angle
		 */
		public IntersectionLine(int index, int reserved, short angle) {
			this.index = index;
			this.reserved = reserved;
			this.angle = angle;
		}

		/**
		 * Prints intersection line data to console
		 */
		public void print() {
			System.out.println(toString());
		}

		/**
		 * Returns a string of intersection line data
		 * 
		 * @return String of intersection line data
		 */
		public String toString() {
			return "intersection line: index: " + index + " reserved: " + reserved + " angle: " + angle;
		}

		/**
		 * @return IntersectionLine index
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return Reserved
		 */
		public int getReserved() {
			return reserved;
		}

		/**
		 * @return Line angle
		 */
		public short getAngle() {
			return angle;
		}

	}

	public static class Intersection {

		private int x, y, number, reserved;
		private IntersectionLine[] lines = new IntersectionLine[LINE_MAX_INTERSECTION_LINES];

		/**
		 * Constructs Intersection object
		 * 
		 * @param x        X value
		 * @param y        Y value
		 * @param number   Number of lines
		 * @param reserved Reserved
		 * @param lines    Array of lines
		 */
		public Intersection(int x, int y, int number, int reserved, IntersectionLine[] lines) {
			this.x = x;
			this.y = y;
			this.number = number;
			this.reserved = reserved;
			this.lines = lines;
		}

		/**
		 * Prints intersection data to console
		 */
		public void print() {
			System.out.println("intersection: (" + x + " " + y + ")");
			for (int i = 0; i < lines.length; i++) {
				IntersectionLine line = lines[i];
				System.out.println(" " + i + " index: " + line.getIndex() + " angle: " + line.getAngle());
			}
		}

		/**
		 * Returns a string of intersection data
		 * 
		 * @return String of intersection data
		 */
		public String toString() {
			String ret = "intersection: (" + x + " " + y + ")";
			for (int i = 0; i < lines.length; i++) {
				IntersectionLine line = lines[i];
				ret += " line: " + i + " index: " + line.getIndex() + " angle: " + line.getAngle();
			}
			return ret;
		}

		/**
		 * @return X value
		 */
		public int getX() {
			return x;
		}

		/**
		 * @return Y value
		 */
		public int getY() {
			return y;
		}

		/**
		 * @return Number of lines
		 */
		public int getNumber() {
			return number;
		}

		/**
		 * @return Reserved
		 */
		public int getReserved() {
			return reserved;
		}

		/**
		 * @return Array of lines
		 */
		public IntersectionLine[] getLines() {
			return lines;
		}

	}

	public static class Barcode {

		private int x, y, flags, code;

		/**
		 * Constructs barcode object
		 * 
		 * @param x     X value
		 * @param y     Y value
		 * @param flags Barcode flags
		 * @param code  Code
		 */
		public Barcode(int x, int y, int flags, int code) {
			this.x = x;
			this.y = y;
			this.flags = flags;
			this.code = code;
		}

		/**
		 * Prints barcode data to console
		 */
		public void print() {
			System.out.println(toString());
		}

		/**
		 * Returns a string of barcode data
		 * 
		 * @return String of barcode data
		 */
		public String toString() {
			return "barcode: (" + x + " " + y + ") value: " + code + " flags: " + flags;
		}

		/**
		 * @return X value
		 */
		public int getX() {
			return x;
		}

		/**
		 * @return Y value
		 */
		public int getY() {
			return y;
		}

		/**
		 * @return Barcode flags
		 */
		public int getFlags() {
			return flags;
		}

		/**
		 * @return Code
		 */
		public int getCode() {
			return code;
		}

	}

}