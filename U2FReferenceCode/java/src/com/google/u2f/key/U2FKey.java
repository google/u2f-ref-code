package com.google.u2f.key;

import com.google.u2f.U2FException;
import com.google.u2f.key.messages.AuthenticateRequest;
import com.google.u2f.key.messages.AuthenticateResponse;
import com.google.u2f.key.messages.RegisterRequest;
import com.google.u2f.key.messages.RegisterResponse;

public interface U2FKey {
		RegisterResponse register(RegisterRequest registerRequest) throws U2FException;
		
		AuthenticateResponse authenticate(AuthenticateRequest authenticateRequest) throws U2FException;
}
