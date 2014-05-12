package com.google.u2f.gaedemo.storage;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.security.SecureRandom;

import com.google.common.base.Objects;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;

@Entity
public class SecretKeys {

  private static final int AES_KEY_SIZE = 16;
  
  @Id private String id;
  private byte[] sessionEncryptionKey;
  
  @Ignore private SecureRandom random = new SecureRandom();
  
  public static void generate() { 
    SecretKeys keys = Objects.firstNonNull(
        ofy().load().type(SecretKeys.class).id("singleton").now(),
        new SecretKeys());
    keys.generateNewKeys();
    ofy().save().entity(keys).now();
  }
  
  public static SecretKeys get() {
    return ofy().load().type(SecretKeys.class).id("singleton").now();
  }
  
  public SecretKeys() {
    id = "singleton";
  }
  
  private void generateNewKeys() {
    if (sessionEncryptionKey == null) {
      sessionEncryptionKey = new byte[AES_KEY_SIZE];
      random.nextBytes(sessionEncryptionKey);
    }
  }
  
  public byte[] sessionEncryptionKey() {
    return sessionEncryptionKey;
  }
}
