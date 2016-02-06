package com.google.u2f.server.impl.attestation.u2f;

import com.google.u2f.server.data.SecurityKeyData.Transports;
import com.google.u2f.server.impl.attestation.X509ExtensionParsingUtil;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERBitString;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class U2fAttestation {
  // Object Identifier for the attestation certificate transport extension fidoU2FTransports
  private static final String TRANSPORT_EXTENSION_OID = "1.3.6.1.4.1.45724.2.1.1";

  // The number of bits in a byte. It is used to know at which index in a BitSet to look for
  // specific transport values
  private static final int BITS_IN_A_BYTE = 8;

  private final List<Transports> transports;

  /**
   * Parses a transport extension from an attestation certificate and returns
   * a List of HardwareFeatures supported by the security key. The specification of
   * the HardwareFeatures in the certificate should match their internal definition in
   * device_auth.proto
   *
   * <p>The expected transport extension value is a BIT STRING containing the enabled
   * transports:
   *
   *  <p>FIDOU2FTransports ::= BIT STRING {
   *       bluetoothRadio(0), -- Bluetooth Classic
   *       bluetoothLowEnergyRadio(1),
   *       uSB(2),
   *       nFC(3)
   *     }
   *
   *   <p>Note that the BIT STRING must be wrapped in an OCTET STRING.
   *   An extension that encodes BT, BLE, and NFC then looks as follows:
   *
   *   <p>SEQUENCE (2 elem)
   *      OBJECT IDENTIFIER 1.3.6.1.4.1.45724.2.1.1
   *      OCTET STRING (1 elem)
   *        BIT STRING (4 bits) 1101
   *
   * @param cert the certificate to parse for extension
   * @return the supported transports as a List of HardwareFeatures or null if no extension
   * was found
   * @throws CertificateParsingException
   */
  public static U2fAttestation Parse(X509Certificate cert) throws CertificateParsingException {
    ASN1OctetString extValue =
        X509ExtensionParsingUtil.extractExtensionValue(cert, TRANSPORT_EXTENSION_OID);

    if (extValue == null) {
      // No Transport extension was found
      return new U2fAttestation(null);
    }

    // Read out the BitString
    ASN1Object asn1Object = X509ExtensionParsingUtil.getAsn1Object(extValue.getOctets());
    if (asn1Object == null || !(asn1Object instanceof DERBitString)) {
      throw new CertificateParsingException("No BitString found in transports extension");
    }
    DERBitString bitString = (DERBitString) asn1Object;

    byte[] values = bitString.getBytes();
    BitSet bitSet = BitSet.valueOf(values);

    // We might have more defined transports than used by the extension
    List<Transports> transports = new ArrayList<Transports>();
    for (int i = 0; i < BITS_IN_A_BYTE; i++) {
      if (bitSet.get(BITS_IN_A_BYTE - i - 1)) {
        transports.add(Transports.values()[i]);
      }
    }

    return new U2fAttestation(transports);
  }

  private U2fAttestation(List<Transports> transports) {
    this.transports = transports;
  }

  /**
   * @return transports parsed from the attestation
   */
  public List<Transports> getTransports() {
    return transports;
  }
}
