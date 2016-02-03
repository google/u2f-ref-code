package com.google.u2f.server.impl.androidattestation;

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
        AndroidKeyStoreAttestation.Parse(ANDROID_KEYSTORE_ATTESTATION_CERT);

    assertNotNull("Not expecting null attestation", attestation);

    // Check version
    assertEquals("Incorrect keymaster version", 2, attestation.getKeyMasterVersion().intValue());

    // Check challenge
    assertArrayEquals(
        "Incorrect challenge", "challenge".getBytes(), attestation.getAttestationChallenge());

    // Get software authz list
    AuthorizationList softwareAuthorizationList = attestation.getSoftwareAuthorizationList();
    assertNotNull("Not expecting null software authorization list", softwareAuthorizationList);

    // Check purpose
    assertEquals("Incorrect purpose list size", 2, softwareAuthorizationList.getPurpose().size());
    assertTrue(
        "Purpose list doesn't have SIGN",
        softwareAuthorizationList.getPurpose().contains(Purpose.KM_PURPOSE_SIGN));
    assertTrue(
        "Purpose list doesn't have VERIFY",
        softwareAuthorizationList.getPurpose().contains(Purpose.KM_PURPOSE_VERIFY));

    // Check algorithm
    assertEquals("Incorrect algorithm", Algorithm.KM_ALGORITHM_RSA,
        softwareAuthorizationList.getAlgorithm());
  }

  @Test(expected = CertificateParsingException.class)
  public void testInvalidCertNotEnoughInDescriptionTest() throws Exception {
    AndroidKeyStoreAttestation.Parse(ANDROID_KEYSTORE_ATTESTATION_CERT_NO_VERSION);
  }
}
