package com.google.u2f.server.impl.attestation.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.u2f.TestVectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.security.cert.CertificateParsingException;

/**
 * Unit tests for {@link AndroidKeyStoreAttestation}
 */
@RunWith(JUnit4.class)
public class AndroidKeyStoreAttestationTest extends TestVectors {
  @Test
  public void testValidCert() throws Exception {
    AndroidKeyStoreAttestation attestation =
        AndroidKeyStoreAttestation.Parse(ANDROID_KEYSTORE_ATTESTATION_CERT_CHAIN[0]);

    assertNotNull("Not expecting null attestation", attestation);

    // Check version
    assertEquals("Incorrect keymaster version", 2, attestation.getKeyMasterVersion().intValue());

    // Check challenge
    assertArrayEquals(
        "Incorrect challenge", "challenge".getBytes(), attestation.getAttestationChallenge());

    // Get software authorization list
    AuthorizationList softwareAuthorizationList = attestation.getSoftwareAuthorizationList();
    assertNotNull("Not expecting null software authorization list", softwareAuthorizationList);

    // Check purpose
    assertEquals("Incorrect software authorization list purpose list size", 2,
        softwareAuthorizationList.getPurposeList().size());
    assertTrue(
        "Software authorization list purpose list doesn't have SIGN",
        softwareAuthorizationList.getPurposeList().contains(Purpose.KM_PURPOSE_SIGN));
    assertTrue(
        "Software authorization list purpose list doesn't have VERIFY",
        softwareAuthorizationList.getPurposeList().contains(Purpose.KM_PURPOSE_VERIFY));

    // Check algorithm
    assertEquals("Software authorization list incorrect algorithm", Algorithm.KM_ALGORITHM_EC,
        softwareAuthorizationList.getAlgorithm());

    // Check key size
    assertEquals("Software authorization list incorrect keysize", 256,
        softwareAuthorizationList.getKeySize().intValue());

    // Block mode
    assertEquals("Not expecting software authorization list block mode", null,
        softwareAuthorizationList.getBlockModeList());

    // Get the TEE authorization list
    AuthorizationList teeAuthorizationList = attestation.getTeeAuthorizationList();
    assertNotNull("Not expecting null TEE authorization list", teeAuthorizationList);
    assertEquals(
        "Expecting null TEE authorization list purpose", null, teeAuthorizationList.getPurposeList());
    assertEquals("Expecting null TEE authorization list algorithm", null,
        teeAuthorizationList.getAlgorithm());
  }

  @Test(expected = CertificateParsingException.class)
  public void testInvalidCertNotEnoughInDescriptionTest() throws Exception {
    AndroidKeyStoreAttestation.Parse(ANDROID_KEYSTORE_ATTESTATION_CERT_NO_VERSION);
  }
}
