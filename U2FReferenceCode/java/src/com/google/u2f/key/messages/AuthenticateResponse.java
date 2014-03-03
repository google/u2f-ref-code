package com.google.u2f.key.messages;

import java.util.Arrays;

public class AuthenticateResponse extends U2FResponse {
	private final byte userPresence;
	private final int counter;
	private final byte[] signature;

	public AuthenticateResponse(byte userPresence, int counter, byte[] signature) {
		super();
		this.userPresence = userPresence;
		this.counter = counter;
		this.signature = signature;
	}

	/**
	 * Bit 0 is set to 1, which means that user presence was verified. (This
	 * version of the protocol doesn’t specify a way to request authentication
	 * responses without requiring user presence.) A different value of Bit 0, as
	 * well as Bits 1 through 7, are reserved for future use. The values of Bit 1
	 * through 7 SHOULD be 0
	 */
	public byte getUserPresence() {
		return userPresence;
	}

	/**
	 * This is the big-endian representation of a counter value that the U2F token
	 * increments every time it performs an authentication operation.
	 */
	public int getCounter() {
		return counter;
	}

	/** This is a ECDSA signature (on P-256) */
	public byte[] getSignature() {
		return signature;
	}

	@Override
  public int hashCode() {
	  final int prime = 31;
	  int result = 1;
	  result = prime * result + counter;
	  result = prime * result + Arrays.hashCode(signature);
	  result = prime * result + userPresence;
	  return result;
  }

	@Override
  public boolean equals(Object obj) {
	  if (this == obj)
		  return true;
	  if (obj == null)
		  return false;
	  if (getClass() != obj.getClass())
		  return false;
	  AuthenticateResponse other = (AuthenticateResponse) obj;
	  if (counter != other.counter)
		  return false;
	  if (!Arrays.equals(signature, other.signature))
		  return false;
	  if (userPresence != other.userPresence)
		  return false;
	  return true;
  }
}
