package com.google.u2f.server.data;

import java.util.Arrays;

public class SecurityKeyData {
	private final byte[] keyHandle;
	private final byte[] publicKey;

	public SecurityKeyData(byte[] keyHandle, byte[] publicKey) {
		super();
		this.keyHandle = keyHandle;
		this.publicKey = publicKey;
	}

	public byte[] getKeyHandle() {
		return keyHandle;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(keyHandle);
		result = prime * result + Arrays.hashCode(publicKey);
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
		SecurityKeyData other = (SecurityKeyData) obj;
		if (!Arrays.equals(keyHandle, other.keyHandle))
			return false;
		if (!Arrays.equals(publicKey, other.publicKey))
			return false;
		return true;
	}
}
