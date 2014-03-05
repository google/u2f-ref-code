package com.google.u2f.server;

public interface SessionIdGenerator {

  public String generateSessionId(String accountName);
}
