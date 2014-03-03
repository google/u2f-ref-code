package com.google.u2f.key;

public interface UserPresenceVerifier {
	public static final byte USER_PRESENT_FLAG = (byte) 0x01;
	
	byte verifyUserPresence();
}
