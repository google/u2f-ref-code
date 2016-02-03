package com.google.u2f.server.impl.attestation;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.DEROctetString;

import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

/**
 * A set of utilities for parsing X509 Extensions
 */
public class X509ExtentionParsingUtil {
  public static DEROctetString extractExtensionValue(X509Certificate cert, String Oid)
      throws CertificateParsingException {
    byte[] extensionValue = cert.getExtensionValue(Oid);

    if (extensionValue == null || extensionValue.length == 0) {
      throw new CertificateParsingException("Did not find extension with OID " + Oid);
    }

    ASN1Object asn1Object  = getAsn1Object(extensionValue);
    if (asn1Object == null || !(asn1Object instanceof DEROctetString)) {
      throw new CertificateParsingException("Expected DEROctetString.");
    }
    
    return (DEROctetString) asn1Object;
  }
  
  public static ASN1Object getAsn1Object(byte[] octets) throws CertificateParsingException {
    ASN1InputStream ais = new ASN1InputStream(octets);
    // Read the key description octet string
    try {
      return ais.readObject();
    } catch (IOException e) {
      throw new CertificateParsingException("Not able to read ASN.1 object", e);
    } finally {
      try {
        ais.close();
      } catch (IOException e) {
        throw new CertificateParsingException("Not able to close ASN.1 stream", e);  
      }
    }
  }
}
