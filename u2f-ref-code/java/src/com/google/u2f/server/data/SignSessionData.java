package com.google.u2f.server.data;

public class SignSessionData extends EnrollSessionData {
  private static final long serialVersionUID = -1374014642398686120L;
  
  private final byte[] publicKey;
  
  public SignSessionData(String accountName, String appId, byte[] challenge, byte[] publicKey) {
    super(accountName, appId, challenge);
    this.publicKey = publicKey;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }
}
