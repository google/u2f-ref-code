// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server;

import java.util.List;

import com.google.u2f.U2FException;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignResponse;
import com.google.u2f.server.messages.U2fSignRequest;

public interface U2FServer {

  // registration //
  public RegistrationRequest getRegistrationRequest(String accountName, String appId) throws U2FException;

  public SecurityKeyData processRegistrationResponse(RegistrationResponse registrationResponse,
      long currentTimeInMillis) throws U2FException;

  // authentication //
  public U2fSignRequest getSignRequest(String accountName, String appId) throws U2FException;

  public SecurityKeyData processSignResponse(SignResponse signResponse) throws U2FException;

  // token management //
  public List<SecurityKeyData> getAllSecurityKeys(String accountName);

  public void removeSecurityKey(String accountName, byte[] publicKey) throws U2FException;
}
