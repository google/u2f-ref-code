// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo.impl;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.u2f.gaedemo.storage.SecretKeys;
import com.google.u2f.gaedemo.storage.TokenStorageData;
import com.google.u2f.gaedemo.storage.UserTokens;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.data.EnrollSessionData;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SignSessionData;

public class DataStoreImpl implements DataStore {

  private final SecureRandom random = new SecureRandom();

  @Override
  public void addTrustedCertificate(X509Certificate certificate) {
    // do nothing
  }

  @Override
  public Set<X509Certificate> getTrustedCertificates() {
    return ImmutableSet.of();
  }

  @Override
  public String storeSessionData(EnrollSessionData sessionData) {

    SecretKey key = new SecretKeySpec(SecretKeys.get().sessionEncryptionKey(), "AES");
    byte[] ivBytes = new byte[16];
    random.nextBytes(ivBytes);
    final IvParameterSpec IV = new IvParameterSpec(ivBytes);
    Cipher cipher;
    try {
      cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, key, IV);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException |
        InvalidKeyException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }

    SealedObject sealed;
    try {
      sealed = new SealedObject(sessionData, cipher);
    } catch (IllegalBlockSizeException | IOException e) {
      throw new RuntimeException(e);
    }

    ByteArrayOutputStream out;
    try {
      out = new ByteArrayOutputStream();
      ObjectOutputStream outer = new ObjectOutputStream(out);

      outer.writeObject(sealed);
      outer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return Base64.encodeBase64URLSafeString(out.toByteArray());
  }

  @Override
  public EnrollSessionData getEnrollSessionData(String sessionId) {
    SecretKey key = new SecretKeySpec(SecretKeys.get().sessionEncryptionKey(), "AES");

    byte[] serialized = Base64.decodeBase64(sessionId);
    ByteArrayInputStream inner = new ByteArrayInputStream(serialized);
    try {
      ObjectInputStream in = new ObjectInputStream(inner);

      SealedObject sealed = (SealedObject) in.readObject();
      return (EnrollSessionData) sealed.getObject(key);
    } catch (InvalidKeyException | ClassNotFoundException
        | NoSuchAlgorithmException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SignSessionData getSignSessionData(String sessionId) {
    return (SignSessionData) getEnrollSessionData(sessionId);
  }

  @Override
  public void addSecurityKeyData(String accountName,
      SecurityKeyData securityKeyData) {
    UserTokens tokens = getUserTokens(accountName);
    TokenStorageData newToken = new TokenStorageData(securityKeyData);
    tokens.addToken(newToken);

    ofy().save().entity(tokens).now();
  }

  @Override
  public List<SecurityKeyData> getSecurityKeyData(String accountName) {
    ImmutableList.Builder<SecurityKeyData> result = ImmutableList.builder();
    for (TokenStorageData tokenStorageData : getAllTokens(accountName)) {
      result.add(tokenStorageData.getSecurityKeyData());
    }
    return result.build();
  }

  @Override
  public void removeSecurityKey(String accountName, byte[] publicKey) {
    UserTokens tokens = getUserTokens(accountName);
    tokens.removeToken(publicKey);
    ofy().save().entity(tokens).now();
  }

  private UserTokens getUserTokens(String accountName) {
    return Objects.firstNonNull(
        ofy().load().type(UserTokens.class).id(accountName).now(),
        new UserTokens(accountName));
  }

  private Collection<TokenStorageData> getAllTokens(String accountName) {
    return getUserTokens(accountName).getTokens();
  }

  @Override
  public void updateSecurityKeyCounter(String accountName, byte[] publicKey,
      int newCounterValue) {
    UserTokens tokens = getUserTokens(accountName);
    tokens.updateCounter(publicKey, newCounterValue);
    ofy().save().entity(tokens).now();
  }
}
