package com.google.u2f.server;

import com.google.u2f.U2FException;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignRequest;
import com.google.u2f.server.messages.SignResponse;

public interface U2FServer {

  public RegistrationRequest getRegistrationRequest(String accountName, String appId) throws U2FException;

  public SecurityKeyData processRegistrationResponse(RegistrationResponse registrationResponse)
      throws U2FException;

  public SignRequest getSignRequest(String accountName, String appId) throws U2FException;

  public void processSignResponse(SignResponse signResponse) throws U2FException;
}
