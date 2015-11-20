// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

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

  public void updateCounter(byte[] publicKey, int newCounterValue) {
    for (TokenStorageData token : tokens) {
      if (Arrays.equals(token.getSecurityKeyData().getPublicKey(), publicKey)) {
        token.updateCounter(newCounterValue);
        break;
      }
    }
  }

  public void addToken(TokenStorageData token) {
    tokens.add(token);
  }
}
