// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.key;

import java.security.KeyPair;

public interface DataStore {
  void storeKeyPair(byte[] keyHandle, KeyPair keyPair);

  KeyPair getKeyPair(byte[] keyHandle);

  int incrementCounter();
}
