// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f;

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

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;

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

  public static X509Certificate parseCertificate(byte[] encodedDerCertificate) {
    try {
      return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
          new ByteArrayInputStream(encodedDerCertificate));
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }
  }

  public static X509Certificate parseCertificate(String encodedDerCertificateHex) {
    return parseCertificate(parseHex(encodedDerCertificateHex));
  }

  public static X509Certificate parseCertificateBase64(String encodedDerCertificate) {
    return parseCertificate(parseBase64(encodedDerCertificate));
  }

  public static X509Certificate[] parseCertificateChainBase64(String encodedDerCertificates) {
    try {
      Collection<? extends Certificate> certCollection =
          CertificateFactory.getInstance("X.509").generateCertificates(
              new ByteArrayInputStream(parseBase64(encodedDerCertificates)));
      return certCollection.toArray(new X509Certificate[0]);
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }
  }

  public static PrivateKey parsePrivateKey(String keyBytesHex) {
    try {
      KeyFactory fac = KeyFactory.getInstance("ECDSA");
      X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
      ECParameterSpec curveSpec = new ECParameterSpec(
          curve.getCurve(), curve.getG(), curve.getN(), curve.getH());
      ECPrivateKeySpec keySpec = new ECPrivateKeySpec(
          new BigInteger(keyBytesHex, 16),
          curveSpec);
      return fac.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  public static PublicKey parsePublicKey(byte[] keyBytes) {
    try {
      X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
      ECParameterSpec curveSpec = new ECParameterSpec(curve.getCurve(), curve.getG(), curve.getN(),
          curve.getH());
      ECPoint point = curve.getCurve().decodePoint(keyBytes);
      return KeyFactory.getInstance("ECDSA").generatePublic(
          new ECPublicKeySpec(point, curveSpec));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (InvalidKeySpecException e) {
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
