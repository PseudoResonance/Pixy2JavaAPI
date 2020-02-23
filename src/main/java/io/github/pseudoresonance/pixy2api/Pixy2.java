package io.github.pseudoresonance.pixy2api;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import io.github.pseudoresonance.pixy2api.links.I2CLink;
import io.github.pseudoresonance.pixy2api.links.Link;
import io.github.pseudoresonance.pixy2api.links.SPILink;
import io.github.pseudoresonance.pixy2api.links.UARTLink;

/**
 * Java Port of Pixy2 Arduino Library
 * 
 * Interfaces with the Pixy2 over any provided, compatible link
 * 
 * https://github.com/PseudoResonance/Pixy2JavaAPI
 * 
 * @author PseudoResonance (Josh Otake)
 *
 *         ORIGINAL HEADER -
 *         https://github.com/charmedlabs/pixy2/blob/master/src/host/arduino/libraries/Pixy2/TPixy2.h
 *         ==========================================================================================
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
 *         Main Pixy template class. This class takes a link class and uses it
 *         to communicate with Pixy over I2C, SPI, UART or USB using the Pixy
 *         packet protocol.
 */

public class Pixy2 {

	public final static int PIXY_BUFFERSIZE = 0x104;
	public final static int PIXY_SEND_HEADER_SIZE = 4;
	public final static int PIXY_MAX_PROGNAME = 33;
	public final static int PIXY_DEFAULT_ARGVAL = 0x80000000;
	public final static int PIXY_CHECKSUM_SYNC = 0xc1af;
	public final static int PIXY_NO_CHECKSUM_SYNC = 0xc1ae;

	// Packet types
	public final static byte PIXY_TYPE_REQUEST_CHANGE_PROG = 0x02;
	public final static byte PIXY_TYPE_REQUEST_RESOLUTION = 0x0c;
	public final static byte PIXY_TYPE_RESPONSE_RESOLUTION = 0x0d;
	public final static byte PIXY_TYPE_REQUEST_VERSION = 0x0e;
	public final static byte PIXY_TYPE_RESPONSE_VERSION = 0x0f;
	public final static byte PIXY_TYPE_RESPONSE_RESULT = 0x01;
	public final static byte PIXY_TYPE_RESPONSE_ERROR = 0x03;
	public final static byte PIXY_TYPE_REQUEST_BRIGHTNESS = 0x10;
	public final static byte PIXY_TYPE_REQUEST_SERVO = 0x12;
	public final static byte PIXY_TYPE_REQUEST_LED = 0x14;
	public final static byte PIXY_TYPE_REQUEST_LAMP = 0x16;
	public final static byte PIXY_TYPE_REQUEST_FPS = 0x18;

	// Return result values
	public final static byte PIXY_RESULT_OK = 0;
	public final static byte PIXY_RESULT_ERROR = -1;
	public final static byte PIXY_RESULT_BUSY = -2;
	public final static byte PIXY_RESULT_CHECKSUM_ERROR = -3;
	public final static byte PIXY_RESULT_TIMEOUT = -4;
	public final static byte PIXY_RESULT_BUTTON_OVERRIDE = -5;
	public final static byte PIXY_RESULT_PROG_CHANGING = -6;

	// RC-servo values
	public final static int PIXY_RCS_MIN_POS = 0;
	public final static int PIXY_RCS_MAX_POS = 1000;
	public final static int PIXY_RCS_CENTER_POS = ((PIXY_RCS_MAX_POS - PIXY_RCS_MIN_POS) / 2);

	public enum LinkType {
		SPI, I2C, UART;
	}

	private Link link = null;

	protected byte[] buffer = null;
	protected int length = 0;
	protected int type = 0;
	protected byte[] bufferPayload = null;

	protected int frameWidth = -1;
	protected int frameHeight = -1;

	protected Version version = null;

	protected Pixy2CCC ccc = null;
	protected Pixy2Line line = null;
	protected Pixy2Video video = null;

	protected boolean m_cs = false;

	/**
	 * Constructs Pixy2 object with supplied communication link
	 * 
	 * @param link {@link Link} to communicate with Pixy2
	 */
	private Pixy2(Link link) {
		this.link = link;
		// Initializes send/return buffer and payload buffer
		buffer = new byte[PIXY_BUFFERSIZE + PIXY_SEND_HEADER_SIZE];
		bufferPayload = new byte[PIXY_BUFFERSIZE];
		// Initializes tracker objects
		this.ccc = new Pixy2CCC(this);
		this.line = new Pixy2Line(this);
		this.video = new Pixy2Video(this);
	}

	/**
	 * Initializes Pixy2 and waits for startup to complete
	 * 
	 * @param argument Argument to setup {@link Link}
	 * 
	 * @return Pixy2 error code
	 */
	public int init(int argument) {
		// Opens link
		int ret = link.open(argument);
		if (ret >= 0) {
			// Tries to connect, times out if unable to communicate after 5 seconds
			for (long t = System.currentTimeMillis(); System.currentTimeMillis() - t < 5000;) {
				// Gets version and resolution to check if communication is successful and stores for future use
				if (getVersion() >= 0) {
					getResolution();
					return PIXY_RESULT_OK;
				}
				try {
					TimeUnit.MICROSECONDS.sleep(5000);
				} catch (InterruptedException e) {
				}
			}
			return PIXY_RESULT_TIMEOUT;
		}
		return PIXY_RESULT_ERROR;
	}

	/**
	 * Initializes Pixy2 and waits for startup to complete using default link argument
	 * value
	 * 
	 * @return Pixy2 error code
	 */
	public int init() {
		return init(PIXY_DEFAULT_ARGVAL);
	}

	/**
	 * Gets Pixy2 instance with supplied communication link
	 * 
	 * @param link Communication {@link Link} to Pixy2
	 * 
	 * @return Pixy2 instance
	 */
	public static Pixy2 createInstance(Link link) {
		return new Pixy2(link);
	}

	/**
	 * Gets Pixy2 instance with supplied communication link type
	 * 
	 * @param type Communication {@link LinkType} to Pixy2
	 * 
	 * @return Pixy2 instance
	 */
	public static Pixy2 createInstance(LinkType type) {
		Link link = null;
		switch (type) {
		case SPI:
			link = new SPILink();
			break;
		case I2C:
			link = new I2CLink();
			break;
		case UART:
			link = new UARTLink();
			break;
		default:
			return null;
		}
		return new Pixy2(link);
	}

	/**
	 * Closes Pixy2
	 */
	public void close() {
		link.close();
	}

	/**
	 * Get Pixy2 Color Connected Components tracker
	 * 
	 * @return Pixy2 Color Connected Components tracker
	 */
	public Pixy2CCC getCCC() {
		return this.ccc;
	}

	/**
	 * Get Pixy2 line tracker
	 * 
	 * @return Pixy2 line tracker
	 */
	public Pixy2Line getLine() {
		return this.line;
	}

	/**
	 * Get Pixy2 video tracker
	 * 
	 * @return Pixy2 video tracker
	 */
	public Pixy2Video getVideo() {
		return this.video;
	}

	public static class Version {

		protected int hardware = 0;
		protected int firmwareMajor = 0;
		protected int firmwareMinor = 0;
		protected int firmwareBuild = 0;
		protected char[] firmwareType = new char[10];

		/**
		 * Constructs version object with given buffer of version data
		 * 
		 * @param version Buffer output from Pixy2 containing version data
		 */
		private Version(byte[] version) {
			hardware = ((int) (version[1] & 0xff) << 8) | (int) (version[0] & 0xff);
			firmwareMajor = version[2];
			firmwareMinor = version[3];
			firmwareBuild = ((int) (version[5] & 0xff) << 8) | (int) (version[4] & 0xff);
			for (int i = 0; i < 10; i++) {
				firmwareType[i] = (char) (version[i + 6] & 0xFF);
			}
		}

		/**
		 * Prints version data to console
		 */
		public void print() {
			System.out.println(toString());
		}

		/**
		 * Returns a string of version data
		 * 
		 * @return String of version data;
		 */
		public String toString() {
			return "hardware ver: 0x" + hardware + " firmware ver: " + firmwareMajor + "." + firmwareMinor + "."
					+ firmwareBuild + " " + new String(firmwareType);
		}

		/**
		 * Gets Pixy2 Hardware Version
		 * 
		 * @return Pixy2 Hardware Version
		 */
		public int getHardware() {
			return hardware;
		}

		/**
		 * Gets Pixy2 Firmware Version Major
		 * 
		 * @return Pixy2 Firmware Version Major
		 */
		public int getFirmwareMajor() {
			return firmwareMajor;
		}

		/**
		 * Gets Pixy2 Firmware Version Minor
		 * 
		 * @return Pixy2 Firmware Version Minor
		 */
		public int getFirmwareMinor() {
			return firmwareMinor;
		}

		/**
		 * Gets Pixy2 Firmware Version Build
		 * 
		 * @return Pixy2 Firmware Version Build
		 */
		public int getFirmwareBuild() {
			return firmwareBuild;
		}

		/**
		 * Gets Pixy2 Firmware Type
		 * 
		 * @return Pixy2 Firmware Type
		 */
		public char[] getfirmwareType() {
			return firmwareType;
		}

		/**
		 * Gets Pixy2 Firmware Type
		 * 
		 * @return Pixy2 Firmware Type
		 */
		public String getFirmwareTypeString() {
			return new String(firmwareType);
		}
	}

	/**
	 * Get width of the pixy's visual frame after initialization
	 * 
	 * @return Pixy2 Frame Width
	 */
	public int getFrameWidth() {
		return frameWidth;
	}

	/**
	 * Get height of the pixy's visual frame after initialization
	 * 
	 * @return Pixy2 Frame Height
	 */
 	public int getFrameHeight() {
		return frameHeight;
	}

	/**
	 * Looks for Pixy2 communication synchronization bytes to find start of message
	 * 
	 * @return Pixy2 error code
	 */
	private byte getSync() {
		int i, attempts, cprev, res, start, ret;
		byte[] c = new byte[1];

		// Parse incoming bytes until sync bytes are found
		for (i = attempts = cprev = 0; true; i++) {
			res = link.receive(c, 1) & 0xff;
			if (res >= PIXY_RESULT_OK) {
				ret = c[0] & 0xff;
				// Since we're using little endian, previous byte is least significant byte
				start = cprev;
				// Current byte is most significant byte
				start |= ret << 8;
				cprev = ret;
				if (start == PIXY_CHECKSUM_SYNC) {
					m_cs = true;
					return PIXY_RESULT_OK;
				}
				if (start == PIXY_NO_CHECKSUM_SYNC) {
					m_cs = false;
					return PIXY_RESULT_OK;
				}
			}
			// If we've read some bytes and no sync, then wait and try again.
			// And do that several more times before we give up.
			// Pixy2 guarantees to respond within 100us.
			if (i >= 4) {
				if (attempts >= 4)
					return PIXY_RESULT_ERROR;
				try {
					TimeUnit.MICROSECONDS.sleep(25);
				} catch (InterruptedException e) {
				}
				attempts++;
				i = 0;
			}
		}
	}

	/**
	 * Gets stored Pixy2 {@link Version} info or retrieves from Pixy2 if not present
	 * 
	 * @return Pixy2 Version Info
	 */
	public Version getVersionInfo() {
		if (version == null)
			getVersion();
		return version;
	}

	/**
	 * Receives packet from Pixy2 and outputs to buffer for further processing
	 * 
	 * @return Length of bytes received or Pixy2 error code
	 */
	protected int receivePacket() {
		int csSerial, res;
		Checksum csCalc = new Checksum();

		// Waits for sync bytes
		res = getSync();
		if (res < 0)
			// Sync not found
			return res;
		if (m_cs) {
			// Checksum sync
			res = link.receive(buffer, 4);
			if (res < 0)
				return res;

			type = buffer[0] & 0xff;
			length = buffer[1] & 0xff;

			csSerial = ((buffer[3] & 0xff) << 8) | (buffer[2] & 0xff);

			// Receives message from buffer
			res = link.receive(buffer, length, csCalc);

			if (res < 0)
				return res;
			// Checks for accuracy with checksum
			if (csSerial != csCalc.getChecksum())
				return PIXY_RESULT_CHECKSUM_ERROR;
		} else {
			// Non-checksum sync
			res = link.receive(buffer, 2);
			if (res < 0)
				return res;

			type = buffer[0] & 0xff;
			length = buffer[1] & 0xff;

			// Receives message from buffer
			res = link.receive(buffer, length);

			if (res < 0)
				return res;
		}
		return PIXY_RESULT_OK;
	}

	/**
	 * Sends packet to Pixy2 from buffer
	 * 
	 * @return Length of bytes sent or Pixy2 error code
	 */
	protected int sendPacket() {
		// Write header info at beginning of buffer
		buffer[0] = (byte) (PIXY_NO_CHECKSUM_SYNC & 0xff);
		buffer[1] = (byte) ((PIXY_NO_CHECKSUM_SYNC >> 8) & 0xff);
		buffer[2] = (byte) type;
		buffer[3] = (byte) length;
		// Add payload data to buffer
		for (int i = 0; i < length; i++) {
			buffer[4 + i] = bufferPayload[i];
		}
		// Send buffer
		return link.send(buffer, (byte) (length + PIXY_SEND_HEADER_SIZE));
	}

	/**
	 * Sends change program packet to Pixy2
	 * 
	 * @param prog Program name
	 * 
	 * @return Pixy2 error code
	 */
	public byte changeProg(char[] prog) {
		int res = 0;

		// Poll for program to change
		while (true) {
			// Truncates supplied program name, or adds empty characters after to indicate end of string
			for (int i = 0; i < PIXY_MAX_PROGNAME; i++) {
				if (i < prog.length)
					bufferPayload[i] = (byte) prog[i];
				else
					bufferPayload[i] = Character.MIN_VALUE;
			}
			length = PIXY_MAX_PROGNAME;
			type = PIXY_TYPE_REQUEST_CHANGE_PROG;
			sendPacket();
			if (receivePacket() == 0) {
				res = ((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8)
						| (buffer[0] & 0xff);
				if (res > 0) {
					getResolution(); // Get resolution for future use
					return PIXY_RESULT_OK; // Success
				}
			} else
				return PIXY_RESULT_ERROR; // Some kind of bitstream error
			try {
				// Timeout to try again
				TimeUnit.MICROSECONDS.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Gets version info from Pixy2
	 * 
	 * @return Buffer length or Pixy2 error code
	 */
	public int getVersion() {
		length = 0;
		type = PIXY_TYPE_REQUEST_VERSION;
		sendPacket();
		if (receivePacket() == 0) {
			if (type == PIXY_TYPE_RESPONSE_VERSION) {
				version = new Version(buffer);
				return length; // Success
			} else if (type == PIXY_TYPE_RESPONSE_ERROR)
				return PIXY_RESULT_BUSY;
		}
		return PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	/**
	 * Gets camera resolution from Pixy2
	 * 
	 * @return Pixy2 error code
	 */
	public byte getResolution() {
		length = 1;
		bufferPayload[0] = 0; // Adds empty byte to payload as placeholder for future queries
		type = PIXY_TYPE_REQUEST_RESOLUTION;
		sendPacket();
		if (receivePacket() == 0) {
			if (type == PIXY_TYPE_RESPONSE_RESOLUTION) {
				frameWidth = ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff);
				frameHeight = ((buffer[3] & 0xff) << 8) | (buffer[2] & 0xff);
				return PIXY_RESULT_OK; // Success
			} else
				return PIXY_RESULT_ERROR;
		} else
			return PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	/**
	 * Sets Pixy2 camera brightness between 0-255
	 * 
	 * @param brightness Byte representing camera brightness
	 * 
	 * @return Pixy2 error code
	 */
	public byte setCameraBrightness(int brightness) {
		int res;

		// Limits brightness between the 0 and 255
		brightness = (brightness >= 255 ? 255 : (brightness <= 0 ? 0 : brightness));

		bufferPayload[0] = (byte) brightness;
		length = 1;
		type = PIXY_TYPE_REQUEST_BRIGHTNESS;
		sendPacket();
		if (receivePacket() == 0 && type == PIXY_TYPE_RESPONSE_RESULT && length == 4) {
			res = ((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8)
					| (buffer[0] & 0xff);
			return (byte) res; // Success
		} else
			return PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	/**
	 * Sets Pixy2 servo positions between 0-1000
	 * 
	 * @param pan  Pan servo position
	 * @param tilt Tilt servo position
	 * 
	 * @return Pixy2 error code
	 */
	public byte setServos(int pan, int tilt) {
		int res;

		// Limits servo values between 0 and 1000
		pan = (pan >= PIXY_RCS_MAX_POS ? PIXY_RCS_MAX_POS : (pan <= PIXY_RCS_MIN_POS ? PIXY_RCS_MIN_POS : pan));
		tilt = (tilt >= PIXY_RCS_MAX_POS ? PIXY_RCS_MAX_POS : (tilt <= PIXY_RCS_MIN_POS ? PIXY_RCS_MIN_POS : tilt));

		bufferPayload[0] = (byte) (pan & 0xff);
		bufferPayload[1] = (byte) ((pan >> 8) & 0xff);
		bufferPayload[2] = (byte) (tilt & 0xff);
		bufferPayload[3] = (byte) ((tilt >> 8) & 0xff);
		length = 4;
		type = PIXY_TYPE_REQUEST_SERVO;
		sendPacket();
		if (receivePacket() == 0 && type == PIXY_TYPE_RESPONSE_RESULT && length == 4) {
			res = ((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8)
					| (buffer[0] & 0xff);
			return (byte) res; // Success
		} else
			return PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	/**
	 * Sets Pixy2 LED color to specified Color
	 * 
	 * @param color Color
	 * 
	 * @return Pixy2 error code
	 */
	public byte setLED(Color color) {
		return setLED(color.getRed(), color.getGreen(), color.getBlue());
	}

	/**
	 * Sets Pixy2 LED color to specified RGB value
	 * 
	 * @param rgb RGB value
	 * 
	 * @return Pixy2 error code
	 */
	public byte setLED(int rgb) {
		return setLED((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
	}

	/**
	 * Sets Pixy2 LED color to specified RGB values between 0-255
	 * 
	 * @param r R value
	 * @param g G value
	 * @param b B value
	 * 
	 * @return Pixy2 error code
	 */
	public byte setLED(int r, int g, int b) {
		int res;

		// Limits rgb values between 0 and 255
		r = (r >= 255 ? 255 : (r <= 0 ? 0 : r));
		g = (g >= 255 ? 255 : (g <= 0 ? 0 : g));
		b = (b >= 255 ? 255 : (b <= 0 ? 0 : b));

		bufferPayload[0] = (byte) r;
		bufferPayload[1] = (byte) g;
		bufferPayload[2] = (byte) b;
		length = 3;
		type = PIXY_TYPE_REQUEST_LED;
		sendPacket();
		if (receivePacket() == 0 && type == PIXY_TYPE_RESPONSE_RESULT && length == 4) {
			res = ((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8)
					| (buffer[0] & 0xff);
			return (byte) res; // Success
		} else
			return PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	/**
	 * Turns Pixy2 light source on/off
	 * 
	 * Use 1 to indicate on, 0 to indicate off
	 * 
	 * @param upper Byte indicating status of white LEDs
	 * @param lower Byte indicating status of RGB LED
	 * 
	 * @return Pixy2 error code
	 */
	public byte setLamp(byte upper, byte lower) {
		int res;

		bufferPayload[0] = upper;
		bufferPayload[1] = lower;
		length = 2;
		type = PIXY_TYPE_REQUEST_LAMP;
		sendPacket();
		if (receivePacket() == 0 && type == PIXY_TYPE_RESPONSE_RESULT && length == 4) {
			res = ((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8)
					| (buffer[0] & 0xff);
			return (byte) res; // Success
		} else
			return PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	/**
	 * Gets Pixy2 camera framerate between 2-62fps
	 * 
	 * @return Framerate or Pixy2 error code
	 */
	public byte getFPS() {
		int res;

		length = 0; // no args
		type = PIXY_TYPE_REQUEST_FPS;
		sendPacket();
		if (receivePacket() == 0 && type == PIXY_TYPE_RESPONSE_RESULT && length == 4) {
			res = ((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8)
					| (buffer[0] & 0xff);
			return (byte) res; // Success
		} else
			return PIXY_RESULT_ERROR; // Some kind of bitstream error
	}

	// Checksum holder class
	public static class Checksum {

		int cs = 0;

		/**
		 * Adds byte to checksum
		 * 
		 * @param b Byte to be added
		 */
		public void updateChecksum(int b) {
			cs += b;
		}

		/**
		 * Returns calculated checksum
		 * 
		 * @return Calculated checksum
		 */
		public int getChecksum() {
			return cs;
		}

		/**
		 * Resets checksum
		 */
		public void reset() {
			cs = 0;
		}

	}

}