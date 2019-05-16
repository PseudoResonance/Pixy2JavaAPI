package io.github.pseudoresonance.pixy2api.links;

import io.github.pseudoresonance.pixy2api.Pixy2.Checksum;

/**
 * Java Port of Pixy2 Arduino Library
 * 
 * Link interface for connecting to Pixy2
 * 
 * https://github.com/PseudoResonance/Pixy2JavaAPI
 * 
 * @author PseudoResonance (Josh Otake)
 */

public interface Link {
	
	/**
	 * Opens link
	 *
	 * @param arg Link argument
	 * 
	 * @return Returns state
	 */
	public int open(int arg);

	/**
	 * Closes link
	 */
	public void close();

	/**
	 * Receives and reads specified length of bytes over link
	 *
	 * @param buffer Byte buffer to return value
	 * @param length Length of value to read
	 * @param cs     Checksum
	 * 
	 * @return Length of value read
	 */
	public int receive(byte[] buffer, int length, Checksum cs);

	/**
	 * Receives and reads specified length of bytes over link
	 *
	 * @param buffer Byte buffer to return value
	 * @param length Length of value to read
	 * 
	 * @return Length of value read
	 */
	public int receive(byte[] buffer, int length);

	/**
	 * Writes and sends buffer over link
	 *
	 * @param buffer Byte buffer to send
	 * @param length Length of value to send
	 * 
	 * @return Length of value sent
	 */
	public int send(byte[] buffer, int length);
}