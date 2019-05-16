package io.github.pseudoresonance.pixy2api.links;

import edu.wpi.first.wpilibj.SPI;
import io.github.pseudoresonance.pixy2api.Pixy2.Checksum;

/**
 * Java Port of Pixy2 Arduino Library
 * 
 * FIRST Robotics WPI API SPI Link to Pixy2
 * 
 * https://github.com/PseudoResonance/Pixy2JavaAPI
 * 
 * @author PseudoResonance (Josh Otake)
 * 
 *         ORIGINAL HEADER -
 *         https://github.com/charmedlabs/pixy2/blob/master/src/host/arduino/libraries/Pixy2/Pixy2.h
 *         =========================================================================================
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
 *         Arduino ICSP SPI link class
 */

public class SPILink implements Link {
	private final static int PIXY_SPI_CLOCKRATE = 2000;

	private SPI spi = null;

	/**
	 * Opens SPI port
	 *
	 * @param arg SPI port
	 * 
	 * @return Returns 0
	 */
	public int open(int arg) {
		SPI.Port port;
		switch (arg) {
		case 1:
			port = SPI.Port.kOnboardCS1;
			break;
		case 2:
			port = SPI.Port.kOnboardCS2;
			break;
		case 3:
			port = SPI.Port.kOnboardCS3;
			break;
		case 4:
			port = SPI.Port.kMXP;
			break;
		case 0:
		default:
			port = SPI.Port.kOnboardCS0;
		}
		spi = new SPI(port);
		spi.setClockRate(PIXY_SPI_CLOCKRATE);
		spi.setMSBFirst();
		spi.setSampleDataOnTrailingEdge();
		spi.setClockActiveLow();
		spi.setChipSelectActiveLow();
		return 0;
	}

	/**
	 * Closes SPI port
	 */
	public void close() {
		spi.close();
	}

	/**
	 * Receives and reads specified length of bytes from SPI
	 *
	 * @param buffer Byte buffer to return value
	 * @param length Length of value to read
	 * @param cs     Checksum
	 * 
	 * @return Length of value read
	 */
	public int receive(byte[] buffer, int length, Checksum cs) {
		if (cs != null)
			cs.reset();
		spi.read(false, buffer, length);
		if (cs != null)
			for (int i = 0; i < length; i++) {
				int csb = buffer[i] & 0xff;
				cs.updateChecksum(csb);
			}
		return length;
	}

	/**
	 * Receives and reads specified length of bytes from SPI
	 *
	 * @param buffer Byte buffer to return value
	 * @param length Length of value to read
	 * 
	 * @return Length of value read
	 */
	public int receive(byte[] buffer, int length) {
		return receive(buffer, length, null);
	}

	/**
	 * Writes and sends buffer over SPI
	 *
	 * @param buffer Byte buffer to send
	 * @param length Length of value to send
	 * 
	 * @return Length of value sent
	 */
	public int send(byte[] buffer, int length) {
		spi.write(buffer, length);
		return length;
	}
}