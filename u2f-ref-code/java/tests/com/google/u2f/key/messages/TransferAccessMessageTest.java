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
  private static final int SEQUENCE_NUMBER_OTHER = 0;
  private static final byte[] NEW_USER_PUBLIC_KEY = TRANSFER_ACCESS_PUBLIC_KEY_B_HEX;
  private static final byte[] NEW_USER_PUBLIC_KEY_OTHER = TRANSFER_ACCESS_PUBLIC_KEY_C_HEX;
  private static final byte[] APPLICATION_SHA256 = APP_ID_SIGN_SHA256;
  private static final byte[] APPLICATION_SHA256_OTHER = APP_ID_ENROLL_SHA256;
  private static final X509Certificate NEW_ATTESTATION_CERTIFICATE = VENDOR_CERTIFICATE;
  private static final X509Certificate NEW_ATTESTATION_CERTIFICATE_OTHER = TRUSTED_CERTIFICATE_2;
  private static final byte[] SIGNATURE_USING_AUTHENTICATION_KEY =
      TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_AUTHENTICATION_KEY_A_TO_B;
  private static final byte[] SIGNATURE_USING_AUTHENTICATION_KEY_OTHER = new byte[1];
  private static final byte[] SIGNATURE_USING_ATTESTATION_KEY =
      TRANSFER_ACCESS_MESSAGE_SIGNATURE_USING_ATTESTATION_KEY_A_TO_B;
  private static final byte[] SIGNATURE_USING_ATTESTATION_KEY_OTHER = new byte[1];
  private static final TransferAccessMessage TRANSFER_ACCESS_MESSAGE = new TransferAccessMessage(
      SEQUENCE_NUMBER, NEW_USER_PUBLIC_KEY, APPLICATION_SHA256, NEW_ATTESTATION_CERTIFICATE,
      SIGNATURE_USING_AUTHENTICATION_KEY, SIGNATURE_USING_ATTESTATION_KEY);
  
  private final Crypto crypto = new BouncyCastleCrypto();

  @Test
  public void testEquals() {
    TransferAccessMessage transferAccessMessage1 = new TransferAccessMessage(SEQUENCE_NUMBER,
        NEW_USER_PUBLIC_KEY, APPLICATION_SHA256, NEW_ATTESTATION_CERTIFICATE,
        SIGNATURE_USING_AUTHENTICATION_KEY, SIGNATURE_USING_ATTESTATION_KEY);
    assertEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_SequenceNumberDiffers() {
    TransferAccessMessage transferAccessMessage1 = new TransferAccessMessage(SEQUENCE_NUMBER_OTHER,
        NEW_USER_PUBLIC_KEY, APPLICATION_SHA256, NEW_ATTESTATION_CERTIFICATE,
        SIGNATURE_USING_AUTHENTICATION_KEY, SIGNATURE_USING_ATTESTATION_KEY);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_NewUserPublicKeysDiffer() {
    TransferAccessMessage transferAccessMessage1 = new TransferAccessMessage(SEQUENCE_NUMBER,
        NEW_USER_PUBLIC_KEY_OTHER, APPLICATION_SHA256, NEW_ATTESTATION_CERTIFICATE,
        SIGNATURE_USING_AUTHENTICATION_KEY, SIGNATURE_USING_ATTESTATION_KEY);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_Application_Sha256Differs() {
    TransferAccessMessage transferAccessMessage1 = new TransferAccessMessage(SEQUENCE_NUMBER,
        NEW_USER_PUBLIC_KEY, APPLICATION_SHA256_OTHER, NEW_ATTESTATION_CERTIFICATE,
        SIGNATURE_USING_AUTHENTICATION_KEY, SIGNATURE_USING_ATTESTATION_KEY);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_AttestationCertificatesDiffer() {
    TransferAccessMessage transferAccessMessage1 = new TransferAccessMessage(SEQUENCE_NUMBER,
        NEW_USER_PUBLIC_KEY, APPLICATION_SHA256, NEW_ATTESTATION_CERTIFICATE_OTHER,
        SIGNATURE_USING_AUTHENTICATION_KEY, SIGNATURE_USING_ATTESTATION_KEY);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_SignatureUsingAuthenticationKeyDiffers() {
    TransferAccessMessage transferAccessMessage1 = new TransferAccessMessage(SEQUENCE_NUMBER,
        NEW_USER_PUBLIC_KEY, APPLICATION_SHA256, NEW_ATTESTATION_CERTIFICATE,
        SIGNATURE_USING_AUTHENTICATION_KEY_OTHER, SIGNATURE_USING_ATTESTATION_KEY);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testNotEquals_SignatureUsingAttestationKeyDiffers() {
    TransferAccessMessage transferAccessMessage1 = new TransferAccessMessage(SEQUENCE_NUMBER,
        NEW_USER_PUBLIC_KEY, APPLICATION_SHA256, NEW_ATTESTATION_CERTIFICATE,
        SIGNATURE_USING_AUTHENTICATION_KEY, SIGNATURE_USING_ATTESTATION_KEY_OTHER);
    assertNotEquals(transferAccessMessage1, TRANSFER_ACCESS_MESSAGE);
  }

  @Test
  public void testGetters() {
    TransferAccessMessage transferAccessMessage = new TransferAccessMessage(SEQUENCE_NUMBER,
        NEW_USER_PUBLIC_KEY, APPLICATION_SHA256, NEW_ATTESTATION_CERTIFICATE,
        SIGNATURE_USING_AUTHENTICATION_KEY, SIGNATURE_USING_ATTESTATION_KEY);

    assertEquals(SEQUENCE_NUMBER, transferAccessMessage.getMessageSequenceNumber());
    assertEquals(NEW_USER_PUBLIC_KEY, transferAccessMessage.getNewUserPublicKey());
    assertArrayEquals(APPLICATION_SHA256, transferAccessMessage.getApplicationSha256());
    assertEquals(NEW_ATTESTATION_CERTIFICATE, transferAccessMessage.getNewAttestationCertificate());
    assertArrayEquals(SIGNATURE_USING_AUTHENTICATION_KEY,
        transferAccessMessage.getSignatureUsingAuthenticationKey());
    assertArrayEquals(SIGNATURE_USING_ATTESTATION_KEY,
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
      @SuppressWarnings("unused")
      TransferAccessMessage transferAccessMessage =
          TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_EXTRA_BYTES);
      fail("expected exception, but didn't get it");
    } catch (U2FException e) {
      assertTrue(e.getMessage().contains("Message ends with unexpected data"));
    }
  }

  @Test
  public void testTransferAccessMessage_FromBytes_TooFewBytes() {
    try {
      @SuppressWarnings("unused")
      TransferAccessMessage transferAccessMessage =
          TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_TOO_FEW_BYTES);
      fail("expected exception, but didn't get it");
    } catch (U2FException e) {
      assertTrue(e.getCause() instanceof java.io.EOFException);
    }
  }

  @Test
  public void testTransferAccessMessage_FromBytes_WayTooFewBytes() {
    try {
      @SuppressWarnings("unused")
      TransferAccessMessage transferAccessMessage =
          TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_WAY_TOO_FEW_BYTES);
      fail("expected exception, but didn't get it");
    } catch (U2FException e) {
      assertTrue(e.getCause() instanceof java.io.EOFException);
    }
  }

  @Test
  /* TODO(alextaka): This will break the parsing sometimes, and sometimes everything will check out.
   * I think it just has to do with where the change gets made in the raw attestation cert. */
  public void testTransferAccessMessage_FromBytes_BadAttestationCertificate() throws U2FException {
    TransferAccessMessage transferAccessMessage =
        TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_BAD_ATTESTATION_CERT);
    
    assertFalse(crypto.verifySignature(transferAccessMessage.getNewAttestationCertificate(),
        EXPECTED_TRANSFER_ACCESS_SIGNED_BYTES_FOR_ATTESTATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAttestationKey()));
    assertTrue(crypto.verifySignature(USER_PUBLIC_KEY_SIGN,
        EXPECTED_TRANSFER_ACCESS_SIGNED_BYTES_FOR_AUTHENTICATION_KEY_A_TO_B,
        transferAccessMessage.getSignatureUsingAuthenticationKey()));
  }

  @Test
  public void testTransferAccessMessage_FromBytes_CutAttestationCertificate() {
    try {
      @SuppressWarnings("unused")
      TransferAccessMessage transferAccessMessage =
          TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_CUT_ATTESTATION_CERT);
      // TODO(alextaka): Somehow, the length seem to match up perfectly and this returns ok.
      fail("expected exception, but didn't get it");
    } catch (U2FException e) {
      assertTrue(e.getCause() instanceof java.io.EOFException);
    }
  }

  @Test
  public void testTransferAccessMessage_FromBytes_DoubleCutAttestationCertificate() {
    try {
      @SuppressWarnings("unused")
      TransferAccessMessage transferAccessMessage =
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
