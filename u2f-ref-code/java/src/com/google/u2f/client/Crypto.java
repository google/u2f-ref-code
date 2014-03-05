package com.google.u2f.client;

import com.google.u2f.U2FException;

public interface Crypto {
	byte[] computeSha256(String message) throws U2FException;
}
