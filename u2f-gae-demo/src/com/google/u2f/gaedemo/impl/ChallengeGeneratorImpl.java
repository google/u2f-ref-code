// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo.impl;

import java.security.SecureRandom;

import com.google.u2f.server.ChallengeGenerator;

public class ChallengeGeneratorImpl implements ChallengeGenerator {

  private static final int CHALLENGE_LENGTH = 16;

  private final SecureRandom random = new SecureRandom();

  @Override
  public byte[] generateChallenge(String accountName) {
    byte[] result = new byte[CHALLENGE_LENGTH];
    random.nextBytes(result);
    return result;
  }
}
