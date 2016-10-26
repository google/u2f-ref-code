package com.google.u2f.key.messages;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.u2f.TestVectors;
import com.google.u2f.U2FException;
import com.google.u2f.server.impl.BouncyCastleCrypto;
import com.google.u2f.server.Crypto;


import java.security.cert.X509Certificate;

public class TransferAccessMessageTest extends TestVectors {
  private static final int SEQUENCE_NUMBER = 1;
  private static final TransferAccessMessage TRANSFER_ACCESS_MESSAGE = new TransferAccessMessage(
      SEQUENCE_NUMBER, TRANSFER_ACCESS_PUBLIC_KEY_B_HEX, APP_ID_SIGN_SHA256, VENDOR_CERTIFICATE,
      TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B,
      TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B);
  
  private final Crypto crypto = new BouncyCastleCrypto();

  @Test
  public void testEquals() {
    System.out.println("Testing equals");
    TransferAccessMessage transferAccessMessage1 = 
        new TransferAccessMessage(SEQUENCE_NUMBER,
                                  TRANSFER_ACCESS_PUBLIC_KEY_B_HEX, 
                                  APP_ID_SIGN_SHA256, 
                                  VENDOR_CERTIFICATE,
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B, 
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B);
    assertEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_SequenceNumberDiffers() {
    int sequenceNumber_Other = 0;
    TransferAccessMessage transferAccessMessage1 = 
        new TransferAccessMessage(sequenceNumber_Other,
                                  TRANSFER_ACCESS_PUBLIC_KEY_B_HEX, 
                                  APP_ID_SIGN_SHA256, 
                                  VENDOR_CERTIFICATE,
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B, 
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_NewUserPublicKeysDiffer() {
    byte[] newUserPublicKey_Other = TRANSFER_ACCESS_PUBLIC_KEY_C_HEX;
    TransferAccessMessage transferAccessMessage1 = 
        new TransferAccessMessage(SEQUENCE_NUMBER,
                                  newUserPublicKey_Other, 
                                  APP_ID_SIGN_SHA256, 
                                  VENDOR_CERTIFICATE,
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B, 
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_Application_Sha256Differs() {
    byte[] appIdSignSha256_Other = APP_ID_ENROLL_SHA256;
    TransferAccessMessage transferAccessMessage1 = 
        new TransferAccessMessage(SEQUENCE_NUMBER,
                                  TRANSFER_ACCESS_PUBLIC_KEY_B_HEX, 
                                  appIdSignSha256_Other, 
                                  VENDOR_CERTIFICATE,
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B, 
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_AttestationCertificatesDiffer() {
    X509Certificate newAttestationCertificate_Other = TRUSTED_CERTIFICATE_2;
    TransferAccessMessage transferAccessMessage1 = 
        new TransferAccessMessage(SEQUENCE_NUMBER,
                                  TRANSFER_ACCESS_PUBLIC_KEY_B_HEX, 
                                  APP_ID_SIGN_SHA256,
                                  newAttestationCertificate_Other,
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B,
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_SignatureUsingAuthenticationKeyDiffers() {
    byte[] signatureUsingAuthenticationKey_Other = new byte[1];
    TransferAccessMessage transferAccessMessage1 = 
        new TransferAccessMessage(SEQUENCE_NUMBER,
                                  TRANSFER_ACCESS_PUBLIC_KEY_B_HEX, 
                                  APP_ID_SIGN_SHA256, 
                                  VENDOR_CERTIFICATE,
                                  signatureUsingAuthenticationKey_Other, 
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_SignatureUsingAttestationKeyDiffers() {
    byte[] signatureUsingAttestationKey_Other = new byte[1];
    TransferAccessMessage transferAccessMessage1 = 
        new TransferAccessMessage(SEQUENCE_NUMBER,
                                  TRANSFER_ACCESS_PUBLIC_KEY_B_HEX, 
                                  APP_ID_SIGN_SHA256, 
                                  VENDOR_CERTIFICATE,
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B, 
                                  signatureUsingAttestationKey_Other);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testGetters() {
    TransferAccessMessage transferAccessMessage = 
        new TransferAccessMessage(SEQUENCE_NUMBER,
                                  TRANSFER_ACCESS_PUBLIC_KEY_B_HEX, 
                                  APP_ID_SIGN_SHA256, 
                                  VENDOR_CERTIFICATE,
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B,
                                  TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B);

    assertEquals(SEQUENCE_NUMBER, transferAccessMessage.getMessageSequenceNumber());
    assertEquals(TRANSFER_ACCESS_PUBLIC_KEY_B_HEX, transferAccessMessage.getNewUserPublicKey());
    assertArrayEquals(APP_ID_SIGN_SHA256, transferAccessMessage.getApplicationSha256());
    assertEquals(VENDOR_CERTIFICATE, transferAccessMessage.getNewAttestationCertificate());
    assertArrayEquals(TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAuthenticationKey());
    assertArrayEquals(TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAttestationKey());
  }

  @Test
  public void testTransferAccessMessage_FromBytes() throws U2FException {
    TransferAccessMessage transferAccessMessage =
        TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_A_TO_B);

    assertEquals(1, transferAccessMessage.getMessageSequenceNumber());
    assertArrayEquals(TRANSFER_ACCESS_PUBLIC_KEY_B_HEX,
        transferAccessMessage.getNewUserPublicKey());
    assertArrayEquals(APP_ID_SIGN_SHA256, transferAccessMessage.getApplicationSha256());
    assertEquals(VENDOR_CERTIFICATE, transferAccessMessage.getNewAttestationCertificate());
    assertArrayEquals(TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAuthenticationKey());
    assertArrayEquals(TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAttestationKey());
    
    assertTrue(crypto.verifySignature(transferAccessMessage.getNewAttestationCertificate(),
        EXPECTED_TRANSFER_ACCESS_SIGNED_BYTES_FOR_ATTESTATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAttestationKey()));
    assertTrue(crypto.verifySignature(USER_PUBLIC_KEY_SIGN,
        EXPECTED_TRANSFER_ACCESS_SIGNED_BYTES_FOR_AUTHENTICATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAuthenticationKey()));
  }

  @Test
  public void testTransferAccessMessage_FromBytes_2() throws U2FException {
    TransferAccessMessage transferAccessMessage =
        TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_C_TO_D);

    assertEquals(3, transferAccessMessage.getMessageSequenceNumber());
    assertArrayEquals(TRANSFER_ACCESS_PUBLIC_KEY_D_HEX,
        transferAccessMessage.getNewUserPublicKey());
    assertArrayEquals(APP_ID_SIGN_SHA256, transferAccessMessage.getApplicationSha256());
    assertEquals(VENDOR_CERTIFICATE, transferAccessMessage.getNewAttestationCertificate());
    assertArrayEquals(TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_C_TO_D,
        transferAccessMessage.getSignatureUsingAuthenticationKey());
    assertArrayEquals(TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_C_TO_D,
        transferAccessMessage.getSignatureUsingAttestationKey());
  }

  @Test
  public void testTransferAccessMessage_FromBytes_ExtraBytes() {
    try {
      TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_EXTRA_BYTES);
      fail("expected exception, but didn't get it");
    } catch (U2FException e) {
      assertTrue(e.getMessage().contains("Message ends with unexpected data"));
    }
  }

  @Test
  public void testTransferAccessMessage_FromBytes_TooFewBytes() {
    try {
      TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_TOO_FEW_BYTES);
      fail("expected exception, but didn't get it");
    } catch (U2FException e) {
      assertTrue(e.getCause() instanceof java.io.EOFException);
    }
  }

  @Test
  public void testTransferAccessMessage_FromBytes_WayTooFewBytes() {
    try {
      TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_WAY_TOO_FEW_BYTES);
      fail("expected exception, but didn't get it");
    } catch (U2FException e) {
      assertTrue(e.getCause() instanceof java.io.EOFException);
    }
  }

  @Test
  public void testTransferAccessMessage_FromBytes_CutAttestationCertificate() {
    try {
      TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_CUT_ATTESTATION_CERT);
      fail("expected exception, but didn't get it");
    } catch (U2FException e) {
      assertTrue(e.getCause() instanceof java.io.EOFException);
    }
  }

  @Test
  public void testTransferAccessMessage_FromBytes_DoubleCutAttestationCertificate() {
    try {
      TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_DOUBLE_CUT_ATTESTATION_CERT);
      fail("expected exception, but didn't get it");
    } catch (U2FException e) {
      assertTrue(e.getMessage().contains("Error when parsing attestation certificate"));
      assertTrue(e.getCause() instanceof java.security.cert.CertificateException);
    }
  }


  @Test
  public void testTransferAccessMessage_FromBytes_InvalidAuthenticationSignature()
      throws U2FException {
    TransferAccessMessage transferAccessMessage =
        TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_BAD_AUTHENTICATION_SIGNATURE);

    assertTrue(crypto.verifySignature(transferAccessMessage.getNewAttestationCertificate(),
        EXPECTED_TRANSFER_ACCESS_SIGNED_BYTES_FOR_ATTESTATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAttestationKey()));
    assertFalse(crypto.verifySignature(USER_PUBLIC_KEY_SIGN,
        EXPECTED_TRANSFER_ACCESS_SIGNED_BYTES_FOR_AUTHENTICATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAuthenticationKey()));
  }

  @Test
  public void testTransferAccessMessage_FromBytes_InvalidAttestationSignature()
      throws U2FException {
    TransferAccessMessage transferAccessMessage =
        TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_BAD_ATTESTATION_SIGNATURE);

    assertFalse(crypto.verifySignature(transferAccessMessage.getNewAttestationCertificate(),
        EXPECTED_TRANSFER_ACCESS_SIGNED_BYTES_FOR_ATTESTATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAttestationKey()));
    assertTrue(crypto.verifySignature(USER_PUBLIC_KEY_SIGN,
        EXPECTED_TRANSFER_ACCESS_SIGNED_BYTES_FOR_AUTHENTICATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAuthenticationKey()));
  }

}
