package io.github.pseudoresonance.pixy2api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Java Port of Pixy2 Arduino Library
 * 
 * Defines Color Connected Components tracker for Pixy2
 * 
 * https://github.com/PseudoResonance/Pixy2JavaAPI
 * 
 * @author PseudoResonance (Josh Otake)
 *
 *         ORIGINAL HEADER -
 *         https://github.com/charmedlabs/pixy2/blob/master/src/host/arduino/libraries/Pixy2/Pixy2CCC.h
 *         ============================================================================================
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

public class Pixy2CCC {
	public final static int CCC_MAX_SIGNATURE = 7;

	public final static byte CCC_RESPONSE_BLOCKS = 0x21;
	public final static byte CCC_REQUEST_BLOCKS = 0x20;

	// Defines for sigmap:
	// You can bitwise "or" these together to make a custom sigmap.
	// For example if you're only interested in receiving blocks
	// with signatures 1 and 5, you could use a sigmap of
	// PIXY_SIG1 | PIXY_SIG5
	public final static byte CCC_SIG1 = 0x01;
	public final static byte CCC_SIG2 = 0x02;
	public final static byte CCC_SIG3 = 0x04;
	public final static byte CCC_SIG4 = 0x08;
	public final static byte CCC_SIG5 = 0x10;
	public final static byte CCC_SIG6 = 0x20;
	public final static byte CCC_SIG7 = 0x40;
	public final static byte CCC_COLOR_CODES = (byte) 0x80;

	public final static byte CCC_SIG_ALL = (byte) 0xff; // All bits or'ed together

	private final Pixy2 pixy;

	private ArrayList<Block> blocks = new ArrayList<Block>();

	/**
	 * Constructs Pixy2 Color Connected Components tracker
	 * 
	 * @param pixy Pixy2 instance
	 */
	protected Pixy2CCC(Pixy2 pixy) {
		this.pixy = pixy;
	}

	/**
	 * <p>Gets signature {@link Block}s from Pixy2</p>
	 * 
	 * <p>Defaults to waiting for a response, getting blocks from all signatures and a maximum of all 256 blocks</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getBlockCache()}</p>
	 * 
	 * @return Pixy2 error code
	 */
	public int getBlocks() {
		return getBlocks(true, CCC_SIG_ALL, 0xff);
	}

	/**
	 * <p>Gets signature {@link Block}s from Pixy2</p>
	 * 
	 * <p>Defaults to getting blocks from all signatures and a maximum of all 256 blocks</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getBlockCache()}</p>
	 * 
	 * @param wait      Whether to wait for Pixy2 if data is not available
	 * 
	 * @return Pixy2 error code
	 */
	public int getBlocks(boolean wait) {
		return getBlocks(wait, CCC_SIG_ALL, 0xff);
	}

	/**
	 * <p>Gets signature {@link Block}s from Pixy2</p>
	 * 
	 * <p>Defaults to getting a maximum of all 256 blocks</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getBlockCache()}</p>
	 * 
	 * @param wait      Whether to wait for Pixy2 if data is not available
	 * @param sigmap    Sigmap to look for
	 * 
	 * @return Pixy2 error code
	 */
	public int getBlocks(boolean wait, int sigmap) {
		return getBlocks(wait, sigmap, 0xff);
	}

	/**
	 * <p>Gets signature {@link Block}s from Pixy2</p>
	 * 
	 * <p>Returned data should be retrieved from the cache with {@link #getBlockCache()}</p>
	 * 
	 * @param wait      Whether to wait for Pixy2 if data is not available
	 * @param sigmap    Sigmap to look for
	 * @param maxBlocks Maximum blocks to look for
	 * 
	 * @return Pixy2 error code
	 */
	public int getBlocks(boolean wait, int sigmap, int maxBlocks) {
		long start = System.currentTimeMillis();

		while (true) {
			// Fill in request data
			pixy.bufferPayload[0] = (byte) sigmap;
			pixy.bufferPayload[1] = (byte) maxBlocks;
			pixy.length = 2;
			pixy.type = CCC_REQUEST_BLOCKS;

			// Send request
			pixy.sendPacket();
			if (pixy.receivePacket() == 0) {
				if (pixy.type == CCC_RESPONSE_BLOCKS) {
					// Clears current cache of blocks
					blocks.clear();
					// Iterates through and creates block objects from buffer
					for (int i = 0; i + 13 < pixy.length; i += 14) {
						Block b = new Block(((pixy.buffer[i + 1] & 0xff) << 8) | (pixy.buffer[i] & 0xff),
								((pixy.buffer[i + 3] & 0xff) << 8) | (pixy.buffer[i + 2] & 0xff),
								((pixy.buffer[i + 5] & 0xff) << 8) | (pixy.buffer[i + 4] & 0xff),
								((pixy.buffer[i + 7] & 0xff) << 8) | (pixy.buffer[i + 6] & 0xff),
								((pixy.buffer[i + 9] & 0xff) << 8) | (pixy.buffer[i + 8] & 0xff),
								((pixy.buffer[i + 11] & 0xff) << 8) | (pixy.buffer[i + 10] & 0xff),
								(pixy.buffer[i + 12] & 0xff), (pixy.buffer[i + 13] & 0xff));
						blocks.add(b);
					}
					return blocks.size(); // Success
				} else if (pixy.type == Pixy2.PIXY_TYPE_RESPONSE_ERROR) {
					// Deal with busy and program changing states from Pixy2 (we'll wait)
					if (pixy.buffer[0] == Pixy2.PIXY_RESULT_BUSY) {
						if (!wait)
							return Pixy2.PIXY_RESULT_BUSY; // New data not available yet
					} else if (pixy.buffer[0] == Pixy2.PIXY_RESULT_PROG_CHANGING) {
						return pixy.buffer[0];
					}

				}
			} else {
				return Pixy2.PIXY_RESULT_ERROR; // Some kind of bitstream error
			}
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
	 * <p>Gets ArrayList of signature blocks from cache</p>
	 * 
	 * <p>{@link #getBlocks(boolean, int, int)} must be executed first to get the data actual from Pixy2</p>
	 * 
	 * @return Pixy2 signature Blocks
	 */
	public ArrayList<Block> getBlockCache() {
		return blocks;
	}

	public static class Block {

		private int signature, x, y, width, height, angle, index, age;

		/**
		 * Constructs signature block instance
		 * 
		 * @param signature Block signature
		 * @param x         X value
		 * @param y         Y value
		 * @param width     Block width
		 * @param height    Block height
		 * @param angle     Angle from camera
		 * @param index     Block index
		 * @param age       Block age
		 */
		public Block(int signature, int x, int y, int width, int height, int angle, int index, int age) {
			this.signature = signature;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.angle = angle;
			this.index = index;
			this.age = age;
		}

		/**
		 * Prints signature block data to console
		 */
		public void print() {
			System.out.println(toString());
		}

		/**
		 * Returns a string of signature block data
		 * 
		 * @return String of signature block data
		 */
		public String toString() {
			int i, j;
			int[] sig = new int[6];
			int d;
			boolean flag;
			String out = "";
			if (signature > CCC_MAX_SIGNATURE) {
				// Color code! (CC)
				// Convert signature number to an octal string
				for (i = 12, j = 0, flag = false; i >= 0; i -= 3) {
					d = (signature >> i) & 0x07;
					if (d > 0 && !flag)
						flag = true;
					if (flag)
						sig[j++] = d + '0';
				}
				sig[j] = '\0';
				out = "CC block sig: " + Arrays.toString(sig) + " (" + signature + " decimal) x: " + x + " y: " + y
						+ " width: " + width + " height: " + height + " angle: " + angle + " index: " + index + " age: "
						+ age;

			} else // Regular block. Note, angle is always zero, so no need to print
				out = "sig: " + signature + " x: " + x + " y: " + y + " width: " + width + " height: " + height
						+ " index: " + index + " age: " + age;
			return out;
		}

		/**
		 * @return Block signature
		 */
		public int getSignature() {
			return signature;
		}

		/**
		 * @return Block X value
		 */
		public int getX() {
			return x;
		}

		/**
		 * @return Block Y value
		 */
		public int getY() {
			return y;
		}

		/**
		 * @return Block width
		 */
		public int getWidth() {
			return width;
		}

		/**
		 * @return Block height
		 */
		public int getHeight() {
			return height;
		}

		/**
		 * @return Angle from camera
		 */
		public int getAngle() {
			return angle;
		}

		/**
		 * @return Block index
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return Block age
		 */
		public int getAge() {
			return age;
		}

	}

}