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
