// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.SoyModule;
import com.google.template.soy.tofu.SoyTofu;
import com.google.u2f.gaedemo.impl.ChallengeGeneratorImpl;
import com.google.u2f.gaedemo.impl.DataStoreImpl;
import com.google.u2f.gaedemo.storage.SecretKeys;
import com.google.u2f.gaedemo.storage.UserTokens;
import com.google.u2f.server.ChallengeGenerator;
import com.google.u2f.server.Crypto;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.impl.BouncyCastleCrypto;
import com.google.u2f.server.impl.U2FServerReferenceImpl;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.ObjectifyService;

public class AppModule extends AbstractModule {

  static {
    ObjectifyService.register(UserTokens.class);
    ObjectifyService.register(SecretKeys.class);
  }

  @Override
  protected void configure() {
    install(new SoyModule());

    bind(ObjectifyFilter.class).in(Singleton.class);
  }

  @Provides
  public SoyTofu provideSoyTofu(SoyFileSet.Builder sfsBuilder) {
    SoyFileSet sfs = new SoyFileSet.Builder()
    .add(getClass().getResource("/soy/header.soy"))
    .add(getClass().getResource("/soy/card.soy"))
    .add(getClass().getResource("/soy/main.soy"))
    .add(getClass().getResource("/soy/admin.soy"))
    .build();
    return sfs.compileToTofu().forNamespace("u2fdemo");
  }

  @Provides
  public ChallengeGenerator provideChallengeGenerator() {
    return new ChallengeGeneratorImpl();
  }

  @Provides
  public Crypto provideCrypto() {
    return new BouncyCastleCrypto();
  }

  @Provides
  public DataStore provideDataStore() {
    return new DataStoreImpl();
  }

  @Provides @Singleton
  public U2FServer provideU2FServer(ChallengeGenerator challengeGenerator, Crypto crypto, DataStore dataStore) {
    return new U2FServerReferenceImpl(challengeGenerator, dataStore, crypto,
        ImmutableSet.of(
            // this implementation will only accept signatures from the following origins:
            "http://localhost:8888",
            "https://u2fdemo.appspot.com",
            "https://crxjs-dot-u2fdemo.appspot.com",
            "https://noext-dot-u2fdemo.appspot.com"));
  }
}
