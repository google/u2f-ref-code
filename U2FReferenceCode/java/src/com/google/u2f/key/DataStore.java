package com.google.u2f.key;

import java.security.KeyPair;

public interface DataStore {

	void storeKeyPair(byte[] keyHandle, KeyPair keyPair);

	KeyPair getKeyPair(byte[] keyHandle);

	int incrementCounter();

}
