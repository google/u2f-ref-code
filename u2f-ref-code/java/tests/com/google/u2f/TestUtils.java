// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f;

import com.google.u2f.tools.X509Util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

public class TestUtils {
  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static byte[] parseHex(String hexEncoded) {
    try {
      return Hex.decodeHex(hexEncoded.toCharArray());
    } catch (DecoderException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] parseBase64(String base64Encoded) {
    return Base64.decodeBase64(base64Encoded);
  }

  public static X509Certificate parseCertificateHex(String encodedDerCertificateHex) {
    try {
      return X509Util.parseCertificate(parseHex(encodedDerCertificateHex));
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }
  }

  public static X509Certificate parseCertificateBase64(String encodedDerCertificate) {
    try {
      return X509Util.parseCertificate(parseBase64(encodedDerCertificate));
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }
  }

  public static X509Certificate[] parseCertificateChainBase64(String encodedDerCertificates) {
      return X509Util.parseCertificateChain(parseBase64(encodedDerCertificates));
  }

  public static X509Certificate[] parseCertificateChainHex(String encodedDerCertificates) {
      return X509Util.parseCertificateChain(parseHex(encodedDerCertificates));
  }

  public static PrivateKey parsePrivateKey(String keyBytesHex) {
    try {
      KeyFactory fac = KeyFactory.getInstance("ECDSA");
      X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
      ECParameterSpec curveSpec =
          new ECParameterSpec(curve.getCurve(), curve.getG(), curve.getN(), curve.getH());
      ECPrivateKeySpec keySpec = new ECPrivateKeySpec(new BigInteger(keyBytesHex, 16), curveSpec);
      return fac.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  public static PublicKey parsePublicKey(byte[] keyBytes) {
    try {
      X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
      ECParameterSpec curveSpec =
          new ECParameterSpec(curve.getCurve(), curve.getG(), curve.getN(), curve.getH());
      ECPoint point = curve.getCurve().decodePoint(keyBytes);
      return KeyFactory.getInstance("ECDSA").generatePublic(new ECPublicKeySpec(point, curveSpec));
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] computeSha256(byte[] bytes) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] computeSha256(String data) {
    return computeSha256(data.getBytes());
  }
}
