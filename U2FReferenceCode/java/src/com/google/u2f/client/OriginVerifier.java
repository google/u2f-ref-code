package com.google.u2f.client;

import com.google.u2f.U2FException;

public interface OriginVerifier {
	void validateOrigin(String appId, String origin) throws U2FException;
}
