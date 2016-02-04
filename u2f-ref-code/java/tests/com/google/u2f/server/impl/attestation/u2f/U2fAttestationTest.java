package com.google.u2f.server.impl.attestation.u2f;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.u2f.TestVectors;
import com.google.u2f.server.data.SecurityKeyData.Transports;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.security.cert.CertificateParsingException;
import java.util.List;

/**
 * Unit tests for {@link U2fAttestation}
 */
@RunWith(JUnit4.class)
public class U2fAttestationTest extends TestVectors {
  @Test
  public void testValidCertOneTransport() throws Exception {
    U2fAttestation attestation = U2fAttestation.Parse(TRUSTED_CERTIFICATE_ONE_TRANSPORT);

    assertNotNull(attestation);
    List<Transports> transports = attestation.getTransports();
    assertEquals(1, transports.size());
    assertTrue(transports.contains(Transports.BLUETOOTH_BREDR));
  }

  @Test(expected = CertificateParsingException.class)
  public void testMalformedCert() throws Exception {
    U2fAttestation.Parse(TRUSTED_CERTIFICATE_MALFORMED_TRANSPORTS_EXTENSION);
  }

  // There is no Transports Extension in the attestation cert 
  // and the current behavior is to throw (the ServerImplementation code catches).
  // TODO(aczeskis): change behavior of ServerImplementation and update test
  @Test(expected = CertificateParsingException.class)
  public void testValidCertNoTransports() throws Exception {
    U2fAttestation.Parse(TRUSTED_CERTIFICATE_2);
  }

  @Test
  public void testValidCertMultipleTransports() throws Exception {
    U2fAttestation attestation = U2fAttestation.Parse(TRUSTED_CERTIFICATE_MULTIPLE_TRANSPORTS);

    assertNotNull(attestation);
    List<Transports> transports = attestation.getTransports();
    assertEquals(3, transports.size());
    assertTrue(transports.contains(Transports.BLUETOOTH_BREDR));
    assertTrue(transports.contains(Transports.BLUETOOTH_LOW_ENERGY));
    assertTrue(transports.contains(Transports.NFC));
  }
}
