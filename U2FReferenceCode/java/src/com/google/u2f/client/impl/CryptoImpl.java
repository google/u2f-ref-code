package com.google.u2f.client.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.u2f.U2FException;
import com.google.u2f.client.Crypto;

public class CryptoImpl implements Crypto {

	@Override
	public byte[] computeSha256(String message) throws U2FException {
		try {
	    return MessageDigest.getInstance("SHA-256").digest(message.getBytes());
    } catch (NoSuchAlgorithmException e) {
    	throw new U2FException("Cannot compute SHA-256", e);
    }
	}

}
