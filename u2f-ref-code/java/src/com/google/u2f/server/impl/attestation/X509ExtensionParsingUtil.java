package com.google.u2f.server.impl.attestation;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DLSequence;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

/**
 * A set of utilities for parsing X509 Extensions
 */
public class X509ExtensionParsingUtil {
  // Don't expect more than 32 bits for any INTEGER
  private static final int MAX_INT_BITS = 32;
  private static final int MAX_LONG_BITS = 64;

  /**
   * Extract a {@link ASN1OctetString} that represents the value of a given extension
   *
   * @param cert is X509 certificate out of which an extension should be extracted
   * @param Oid is the Object IDentifier for the extension
   * @return a {@link ASN1OctetString} that represents an extension or {@code null} if no such
   * extension is found.
   * @throws CertificateParsingException if a parsing error occurs
   */
  public static ASN1OctetString extractExtensionValue(X509Certificate cert, String Oid)
      throws CertificateParsingException {
    byte[] extensionValue = cert.getExtensionValue(Oid);

    if (extensionValue == null || extensionValue.length == 0) {
      // Did not find extension
      return null;
    }

    ASN1Object asn1Object = getAsn1Object(extensionValue);
    if (asn1Object == null || !(asn1Object instanceof ASN1OctetString)) {
      throw new CertificateParsingException("Expected ASN1OctetString.");
    }

    return (ASN1OctetString) asn1Object;
  }

  /**
   * Extracts an {@link ASN1Object} from an array of octets
   * @throws CertificateParsingException
   */
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

  /**
   * Extracts an {@code int} from an {@link ASN1Encodable}.
   * @throws CertificateParsingException
   */
  public static int getInt(ASN1Encodable asn1Encodable) throws CertificateParsingException {
    if (asn1Encodable == null || !(asn1Encodable instanceof ASN1Integer)) {
      throw new CertificateParsingException("Expected INTEGER type.");
    }
    ASN1Integer asn1Integer = (ASN1Integer) asn1Encodable;
    BigInteger bigInt = asn1Integer.getPositiveValue();
    if (bigInt.bitLength() > MAX_INT_BITS) {
      throw new CertificateParsingException("INTEGER too big");
    }
    return bigInt.intValue();
  }

  /**
   * Extracts an {@code long} from an {@link ASN1Encodable}.
   * @throws CertificateParsingException
   */
  public static long getLong(ASN1Encodable asn1Encodable) throws CertificateParsingException {
    if (asn1Encodable == null || !(asn1Encodable instanceof ASN1Integer)) {
      throw new CertificateParsingException("Expected INTEGER type.");
    }
    ASN1Integer asn1Integer = (ASN1Integer) asn1Encodable;
    BigInteger bigInt = asn1Integer.getPositiveValue();
    if (bigInt.bitLength() > MAX_LONG_BITS) {
      throw new CertificateParsingException("INTEGER too big");
    }
    return bigInt.longValue();
  }

  /**
   * Extracts a {@code byte[]} from an {@link ASN1Encodable}
   */
  public static byte[] getByteArray(ASN1Encodable asn1Encodable)
      throws CertificateParsingException {
    if (asn1Encodable == null || !(asn1Encodable instanceof ASN1OctetString)) {
      throw new CertificateParsingException("Expected ASN1OctetString");
    }
    ASN1OctetString derOctectString = (ASN1OctetString) asn1Encodable;
    return derOctectString.getOctets();
  }

  /**
   * Returns a {@link HashMap} whose keys represent the tags and whose values represent the values
   * of a {@link DLSequence}.
   */
  public static HashMap<Integer, ASN1Primitive> extractTaggedObjects(ASN1Sequence asn1Sequence)
      throws CertificateParsingException {
    HashMap<Integer, ASN1Primitive> taggedObjects = new HashMap<Integer, ASN1Primitive>();

    for (ASN1Encodable asn1EncodablePurpose : asn1Sequence.toArray()) {
      if (asn1EncodablePurpose == null || !(asn1EncodablePurpose instanceof ASN1TaggedObject)) {
        throw new CertificateParsingException("Expected DERTagged object");
      }
      ASN1TaggedObject asn1TaggedObject = (ASN1TaggedObject) asn1EncodablePurpose;
      taggedObjects.put(Integer.valueOf(asn1TaggedObject.getTagNo()), asn1TaggedObject.getObject());
    }

    return taggedObjects;
  }
}
