// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.impl;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import com.google.u2f.U2FException;
import com.google.u2f.server.Crypto;

public class BouncyCastleCrypto implements Crypto {
  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Override
  public boolean verifySignature(X509Certificate attestationCertificate, byte[] signedBytes,
      byte[] signature) throws U2FException {
    return verifySignature(attestationCertificate.getPublicKey(), signedBytes, signature);
  }

  @Override
  public boolean verifySignature(PublicKey publicKey, byte[] signedBytes,
      byte[] signature) throws U2FException {
    try {
      Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA");
      ecdsaSignature.initVerify(publicKey);
      ecdsaSignature.update(signedBytes);
      return ecdsaSignature.verify(signature);
    } catch (InvalidKeyException e) {
      throw new U2FException("Error when verifying signature", e);
    } catch (SignatureException e) {
      throw new U2FException("Error when verifying signature", e);
    } catch (NoSuchAlgorithmException e) {
      throw new U2FException("Error when verifying signature", e);
    }
  }

  @Override
  public PublicKey decodePublicKey(byte[] encodedPublicKey) throws U2FException {
    try {
      X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
      ECPoint point;
      try {
        point = curve.getCurve().decodePoint(encodedPublicKey);
      } catch (RuntimeException e) {
        throw new U2FException("Couldn't parse user public key", e);
      }

      return KeyFactory.getInstance("ECDSA").generatePublic(
          new ECPublicKeySpec(point,
              new ECParameterSpec(
                  curve.getCurve(),
                  curve.getG(),
                  curve.getN(),
                  curve.getH())));
    } catch (InvalidKeySpecException e) {
      throw new U2FException("Error when decoding public key", e);
    } catch (NoSuchAlgorithmException e) {
      throw new U2FException("Error when decoding public key", e);
    }
  }

  @Override
  public byte[] computeSha256(byte[] bytes) throws U2FException {
    try {
      return MessageDigest.getInstance("SHA-256").digest(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new U2FException("Error when computing SHA-256", e);
    }
  }
}
