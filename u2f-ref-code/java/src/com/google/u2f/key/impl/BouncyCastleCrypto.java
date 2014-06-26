// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.key.impl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.u2f.U2FException;
import com.google.u2f.key.Crypto;

public class BouncyCastleCrypto implements Crypto {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Override
  public byte[] sign(byte[] signedData, PrivateKey privateKey) throws U2FException {
    try {
      Signature signature = Signature.getInstance("SHA256withECDSA");
      signature.initSign(privateKey);
      signature.update(signedData);
      return signature.sign();
    } catch (NoSuchAlgorithmException e) {
      throw new U2FException("Error when signing", e);
    } catch (SignatureException e) {
      throw new U2FException("Error when signing", e);
    } catch (InvalidKeyException e) {
      throw new U2FException("Error when signing", e);
    }
  }
}
