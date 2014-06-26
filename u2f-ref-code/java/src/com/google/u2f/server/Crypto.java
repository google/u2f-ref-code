// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

import com.google.u2f.U2FException;

public interface Crypto {
  boolean verifySignature(X509Certificate attestationCertificate, byte[] signedBytes,
      byte[] signature) throws U2FException;

  boolean verifySignature(PublicKey publicKey, byte[] signedBytes,
      byte[] signature) throws U2FException;

  PublicKey decodePublicKey(byte[] encodedPublicKey) throws U2FException;

  byte[] computeSha256(byte[] bytes) throws U2FException;
}
