package com.google.u2f.server.data;

public class SessionData {
  private final String accountName;
  private final byte[] challenge;

  public SessionData(String accountName, byte[] challenge) {
    this.accountName = accountName;
    this.challenge = challenge;
  }

  public String getAccountName() {
    return accountName;
  }

  public byte[] getChallenge() {
    return challenge;
  }
}
