// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.client;

import com.google.u2f.U2FException;

public interface U2FClient {
  void register(String origin, String accountName) throws U2FException;

  void authenticate(String origin, String accountName) throws U2FException;
}
