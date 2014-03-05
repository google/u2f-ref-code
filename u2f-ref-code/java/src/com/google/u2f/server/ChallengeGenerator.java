package com.google.u2f.server;

public interface ChallengeGenerator {

  byte[] generateChallenge(String accountName);
}
