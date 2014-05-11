package com.google.u2f.server;

import java.util.List;

import com.google.u2f.U2FException;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignRequest;
import com.google.u2f.server.messages.SignResponse;

public interface U2FServer {

  // registration //
  public RegistrationRequest getRegistrationRequest(String accountName, String appId) throws U2FException;

  public SecurityKeyData processRegistrationResponse(RegistrationResponse registrationResponse)
      throws U2FException;

  // authentication //
  public List<SignRequest> getSignRequest(String accountName, String appId) throws U2FException;

  public void processSignResponse(SignResponse signResponse) throws U2FException;
  
  // token management //
  public List<SecurityKeyData> getAllSecurityKeys(String accountName);

  public void removeSecurityKey(String accountName, byte[] publicKey) throws U2FException;  
}
