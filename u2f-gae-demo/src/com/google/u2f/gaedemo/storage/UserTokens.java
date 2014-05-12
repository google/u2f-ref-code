package com.google.u2f.gaedemo.storage;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class UserTokens {

  @Id String userId;
  List<TokenStorageData> tokens = Lists.newArrayList();

  public UserTokens() {}

  public UserTokens(String userId) {
    this.userId = userId;
  }

  public Collection<TokenStorageData> getTokens() {
    return ImmutableList.copyOf(tokens);
  }

  public void removeToken(byte[] publicKey) {
    for (TokenStorageData token : tokens) {
      if (Arrays.equals(token.getSecurityKeyData().getPublicKey(), publicKey)) {
        tokens.remove(token);
        break;
      }
    }
  }

  public void addToken(TokenStorageData token) {
    tokens.add(token);
  }
}
