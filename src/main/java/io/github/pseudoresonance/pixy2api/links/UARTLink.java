package io.github.pseudoresonance.pixy2api.links;

import java.util.concurrent.TimeUnit;

import edu.wpi.first.wpilibj.SerialPort;
import io.github.pseudoresonance.pixy2api.Pixy2;
import io.github.pseudoresonance.pixy2api.Pixy2.Checksum;

/**
 * Java Port of Pixy2 Arduino Library
 * 
 * FIRST Robotics WPI API UART/Serial Link to Pixy2
 * 
 * CURRENTLY UNTESTED - Use SPI if possible!
 * 
 * https://github.com/PseudoResonance/Pixy2JavaAPI
 * 
 * @author PseudoResonance (Josh Otake)
 * 
 *         ORIGINAL HEADER -
 *         https://github.com/charmedlabs/pixy2/blob/master/src/host/arduino/libraries/Pixy2/Pixy2UART.h
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
 *         Arduino UART link class, intended to be used with an Arduino with
 *         more than 1 UART, like the Arduino MEGA 2560.
 */

public class UARTLink implements Link {
	private final static int PIXY_UART_BAUDRATE = 19200;

	private SerialPort serial = null;

	/**
	 * Opens UART/Serial port
	 *
	 * @param arg UART/Serial port
	 * 
	 * @return Returns 0
	 */
	public int open(int arg) {
		SerialPort.Port port;
		switch (arg) {
		case 1:
			port = SerialPort.Port.kUSB;
			break;
		case 2:
			port = SerialPort.Port.kUSB1;
			break;
		case 3:
			port = SerialPort.Port.kUSB2;
			break;
		case 4:
			port = SerialPort.Port.kMXP;
			break;
		case Pixy2.PIXY_DEFAULT_ARGVAL:
		default:
			port = SerialPort.Port.kOnboard;
		}
		serial = new SerialPort(PIXY_UART_BAUDRATE, port);
		return 0;
	}

	/**
	 * Closes UART/Serial port
	 */
	public void close() {
		serial.close();
	}

	/**
	 * Receives and reads specified length of bytes from UART/Serial
	 *
	 * @param buffer Byte buffer to return value
	 * @param length Length of value to read
	 * @param cs     Checksum
	 * 
	 * @return Length of value read
	 */
	public int receive(byte[] buffer, int length, Checksum cs) {
		int i, j, c;
		if (cs != null)
			cs.reset();
		for (i = 0; i < length; i++) {
			// wait for byte, timeout after 2ms
			// note for a baudrate of 19.2K, each byte takes about 500us
			for (j = 0; true; j++) {
				if (j == 200)
					return -1;
				c = serial.read(1)[0];
				if (c >= 0)
					break;
				try {
					TimeUnit.MICROSECONDS.sleep(10);
				} catch (InterruptedException e) {
				}
			}
			buffer[i] = (byte) c;
			if (cs != null) {
				byte b = buffer[i];
				int csb = b & 0xff;
				cs.updateChecksum(csb);
			}
		}
		return length;
	}

	/**
	 * Receives and reads specified length of bytes from UART/Serial
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
	 * Writes and sends buffer over UART/Serial
	 *
	 * @param buffer Byte buffer to send
	 * @param length Length of value to send
	 * 
	 * @return Length of value sent
	 */
	public int send(byte[] buffer, int length) {
		return serial.write(buffer, length);
	}
}
