// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.codec;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.google.u2f.U2FException;
import com.google.u2f.key.messages.AuthenticateRequest;
import com.google.u2f.key.messages.AuthenticateResponse;
import com.google.u2f.key.messages.RegisterRequest;
import com.google.u2f.key.messages.RegisterResponse;

/**
 * Raw message formats, as per FIDO U2F: Raw Message Formats - Draft 4
 */
public class RawMessageCodec {
  public static final byte REGISTRATION_RESERVED_BYTE_VALUE = (byte) 0x05;
  public static final byte REGISTRATION_SIGNED_RESERVED_BYTE_VALUE = (byte) 0x00;

  public static byte[] encodeRegisterRequest(RegisterRequest registerRequest) {
    byte[] appIdSha256 = registerRequest.getApplicationSha256();
    byte[] challengeSha256 = registerRequest.getChallengeSha256();
    byte[] result = new byte[appIdSha256.length + challengeSha256.length];

    ByteBuffer.wrap(result)
    .put(challengeSha256)
    .put(appIdSha256);
    return result;
  }

  public static RegisterRequest decodeRegisterRequest(byte[] data) throws U2FException {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
      byte[] appIdSha256 = new byte[32];
      byte[] challengeSha256 = new byte[32];
      inputStream.readFully(challengeSha256);
      inputStream.readFully(appIdSha256);

      if (inputStream.available() != 0) {
        throw new U2FException("Message ends with unexpected data");
      }

      return new RegisterRequest(appIdSha256, challengeSha256);
    } catch (IOException e) {
      throw new U2FException("Error when parsing raw RegistrationResponse", e);
    }
  }

  public static byte[] encodeRegisterResponse(RegisterResponse registerResponse)
      throws U2FException {
    byte[] userPublicKey = registerResponse.getUserPublicKey();
    byte[] keyHandle = registerResponse.getKeyHandle();
    X509Certificate attestationCertificate = registerResponse.getAttestationCertificate();
    byte[] signature = registerResponse.getSignature();

    byte[] attestationCertificateBytes;
    try {
      attestationCertificateBytes = attestationCertificate.getEncoded();
    } catch (CertificateEncodingException e) {
      throw new U2FException("Error when encoding attestation certificate.", e);
    }

    if (keyHandle.length > 255) {
      throw new U2FException("keyHandle length cannot be longer than 255 bytes!");
    }

    byte[] result = new byte[1 + userPublicKey.length + 1 + keyHandle.length
                             + attestationCertificateBytes.length + signature.length];
    ByteBuffer.wrap(result)
    .put(REGISTRATION_RESERVED_BYTE_VALUE)
    .put(userPublicKey)
    .put((byte) keyHandle.length)
    .put(keyHandle)
    .put(attestationCertificateBytes)
    .put(signature);
    return result;
  }

  public static RegisterResponse decodeRegisterResponse(byte[] data) throws U2FException {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
      byte reservedByte = inputStream.readByte();
      byte[] userPublicKey = new byte[65];
      inputStream.readFully(userPublicKey);
      byte[] keyHandle = new byte[inputStream.readUnsignedByte()];
      inputStream.readFully(keyHandle);
      X509Certificate attestationCertificate = (X509Certificate) CertificateFactory.getInstance(
          "X.509").generateCertificate(inputStream);
      byte[] signature = new byte[inputStream.available()];
      inputStream.readFully(signature);

      if (inputStream.available() != 0) {
        throw new U2FException("Message ends with unexpected data");
      }

      if (reservedByte != REGISTRATION_RESERVED_BYTE_VALUE) {
        throw new U2FException(String.format(
            "Incorrect value of reserved byte. Expected: %d. Was: %d",
            REGISTRATION_RESERVED_BYTE_VALUE, reservedByte));
      }

      return new RegisterResponse(userPublicKey, keyHandle, attestationCertificate, signature);
    } catch (IOException e) {
      throw new U2FException("Error when parsing raw RegistrationResponse", e);
    } catch (CertificateException e) {
      throw new U2FException("Error when parsing attestation certificate", e);
    }
  }

  public static byte[] encodeAuthenticateRequest(AuthenticateRequest authenticateRequest)
      throws U2FException {
    byte controlByte = authenticateRequest.getControl();
    byte[] appIdSha256 = authenticateRequest.getApplicationSha256();
    byte[] challengeSha256 = authenticateRequest.getChallengeSha256();
    byte[] keyHandle = authenticateRequest.getKeyHandle();

    if (keyHandle.length > 255) {
      throw new U2FException("keyHandle length cannot be longer than 255 bytes!");
    }

    byte[] result = new byte[1 + appIdSha256.length + challengeSha256.length + 1 + keyHandle.length];
    ByteBuffer.wrap(result)
    .put(controlByte)
    .put(challengeSha256)
    .put(appIdSha256)
    .put((byte) keyHandle.length)
    .put(keyHandle);
    return result;
  }

  public static AuthenticateRequest decodeAuthenticateRequest(byte[] data) throws U2FException {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
      byte controlByte = inputStream.readByte();
      byte[] challengeSha256 = new byte[32];
      inputStream.readFully(challengeSha256);
      byte[] appIdSha256 = new byte[32];
      inputStream.readFully(appIdSha256);
      byte[] keyHandle = new byte[inputStream.readUnsignedByte()];
      inputStream.readFully(keyHandle);

      if (inputStream.available() != 0) {
        throw new U2FException("Message ends with unexpected data");
      }

      return new AuthenticateRequest(controlByte, challengeSha256, appIdSha256, keyHandle);
    } catch (IOException e) {
      throw new U2FException("Error when parsing raw RegistrationResponse", e);
    }
  }

  public static byte[] encodeAuthenticateResponse(AuthenticateResponse authenticateResponse)
      throws U2FException {
    byte userPresence = authenticateResponse.getUserPresence();
    int counter = authenticateResponse.getCounter();
    byte[] signature = authenticateResponse.getSignature();

    byte[] result = new byte[1 + 4 + signature.length];
    ByteBuffer.wrap(result)
    .put(userPresence)
    .putInt(counter)
    .put(signature);
    return result;
  }

  public static AuthenticateResponse decodeAuthenticateResponse(byte[] data) throws U2FException {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
      byte userPresence = inputStream.readByte();
      int counter = inputStream.readInt();
      byte[] signature = new byte[inputStream.available()];
      inputStream.readFully(signature);

      if (inputStream.available() != 0) {
        throw new U2FException("Message ends with unexpected data");
      }

      return new AuthenticateResponse(userPresence, counter, signature);
    } catch (IOException e) {
      throw new U2FException("Error when parsing rawSignData", e);
    }
  }

  public static byte[] encodeRegistrationSignedBytes(byte[] applicationSha256,
      byte[] challengeSha256, byte[] keyHandle, byte[] userPublicKey) {
    byte[] signedData = new byte[1 + applicationSha256.length + challengeSha256.length
                                 + keyHandle.length + userPublicKey.length];
    ByteBuffer.wrap(signedData)
    .put(REGISTRATION_SIGNED_RESERVED_BYTE_VALUE) // RFU
    .put(applicationSha256)
    .put(challengeSha256)
    .put(keyHandle)
    .put(userPublicKey);
    return signedData;
  }

  public static byte[] encodeAuthenticateSignedBytes(byte[] applicationSha256, byte userPresence,
      int counter, byte[] challengeSha256) {
    byte[] signedData = new byte[applicationSha256.length + 1 + 4 + challengeSha256.length];
    ByteBuffer.wrap(signedData)
    .put(applicationSha256)
    .put(userPresence)
    .putInt(counter)
    .put(challengeSha256);
    return signedData;
  }
}
