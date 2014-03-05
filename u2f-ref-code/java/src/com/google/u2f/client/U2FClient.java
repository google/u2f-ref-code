package com.google.u2f.client;

import com.google.u2f.U2FException;

public interface U2FClient {
  void register(String origin, String accountName) throws U2FException;

  void authenticate(String origin, String accountName) throws U2FException;
}
