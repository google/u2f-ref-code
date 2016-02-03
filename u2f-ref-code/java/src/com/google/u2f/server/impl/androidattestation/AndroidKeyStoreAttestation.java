package com.google.u2f.server.impl.androidattestation;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLSequence;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Parses and contains an Android KeyStore attestation.
 */
public class AndroidKeyStoreAttestation {
  private static final String KEY_DESCRIPTION_OID = "1.3.6.1.4.1.11129.2.1.17";

  // Indexes for data in KeyDescription sequence
  private static final int DESCRIPTION_LENGTH_MIN = 4;
  private static final int DESCRIPTION_LENGTH_MAX = 5;
  private static final int DESCRIPTION_VERSION_INDEX = 0;
  private static final int DESCRIPTION_CHALLENGE_INDEX = 1;
  private static final int DESCRIPTION_SOFTWARE_ENFORCED_INDEX = 2;
  private static final int DESCRIPTION_TEE_ENFORCED_INDEX = 3;
  private static final int DESCRIPTION_UNIQUE_INDEX = 4;

  // Don't expect more than 32 bits for any INTEGER
  private static final int MAX_INTEGER_BITS = 32;

  // Tags for Authorization List
  private static final int AUTHZ_PURPOSE_TAG = 1;
  private static final int AUTHZ_ALGORITHM_TAG = 2;

  private final Integer keymasterVersion;
  private final byte[] attestationChallenge;
  private final AuthorizationList softwareAuthorizationList;
  private final byte[] uniqueId;

  private AndroidKeyStoreAttestation(Integer keymasterVersion, byte[] attestationChallenge,
      AuthorizationList softwareAuthorizationList, byte[] uniqueId) {
    this.keymasterVersion = keymasterVersion;
    this.attestationChallenge = attestationChallenge;
    this.softwareAuthorizationList = softwareAuthorizationList;
    this.uniqueId = uniqueId;
  }

  /**
   * Parses the key description extension.  Expected format is:
   *   KeyDescription ::= SEQUENCE {
   *       keymasterVersion     INTEGER,
   *       attestationChallenge OCTET_STRING,
   *       softwareEnforced     AuthorizationList,
   *       teeEnforced          AuthorizationList,
   *       uniqueId             OCTET_STRING OPTIONAL,
   *   }
   *
   *   AuthorizationList ::= SEQUENCE {
   *       -- See keymaster_purpose_t for purpose values.
   *       purpose [1] IMPLICIT SET OF INTEGER OPTIONAL,
   *       -- See keymaster_algorithm_t for algorithm values.
   *       algorithm   [2] IMPLICIT INTEGER OPTIONAL,
   *       -- keySize is measured in bits, not bytes, and the value must be
   *       -- positive and less than than 2^32, though realistic values are
   *       -- much smaller.
   *       keySize [3] IMPLICIT INTEGER OPTIONAL,
   *       -- See keymaster_block_mode_t for blockMode values.
   *       blockMode   [4] IMPLICIT SET OF INTEGER OPTIONAL,
   *       -- See keymaster_digest_t for digest values.
   *       digest  [5] IMPLICIT SET OF INTEGER OPTIONAL,
   *       -- See keymaster_padding_t for padding values.
   *       padding [6] IMPLICIT SET OF INTEGER OPTIONAL,
   *       callerNonce [7] IMPLICIT NULL OPTIONAL,
   *       -- minMacLength values must be positive and less than 2^32.
   *       minMacLength    [8] IMPLICIT INTEGER OPTIONAL,
   *       -- See keymaster_kdf_t for kdf values.
   *       kdf [9] IMPLICIT SEQUENCE OF INTEGER OPTIONAL,
   *       -- See keymaster_ec_curve_t for ecCurve values
   *       ecCurve [10] IMPLICIT INTEGER OPTIONAL,
   *       -- rsaPublicExponent must be a valid RSA public exponent less
   *       -- than 2^64.
   *       rsaPublicExponent   [200] IMPLICIT INTEGER OPTIONAL,
   *       eciesSingleHashMode [201] IMPLICIT NULL OPTIONAL,
   *       includeUniqueId [202] IMPLICIT NULL OPTIONAL,
   *       -- See keymaster_key_blob_usage_requirements for
   *       -- blobUsageRequirement values.
   *       blobUsageRequirement    [301] IMPLICIT INTEGER OPTIONAL,
   *       bootloaderOnly  [302] IMPLICIT NULL OPTIONAL,
   *       -- activeDateTime must be a 64-bit Java date/time value.
   *       activeDateTime  [400] IMPLICIT INTEGER OPTIONAL
   *       -- originationExpireDateTime must be a 64-bit Java date/time
   *       -- value.
   *       originationExpireDateTime   [401] IMPLICIT INTEGER OPTIONAL
   *       -- usageExpireDateTime must be a 64-bit Java date/time value.
   *       usageExpireDateTime     [402] IMPLICIT INTEGER OPTIONAL
   *       -- minSecondsBetweenOps must be non-negative and less than 2^32.
   *       minSecondsBetweenOps    [403] IMPLICIT INTEGER OPTIONAL,
   *       -- maxUsesPerBoot must be positive and less than 2^32.
   *       maxUsesPerBoot  [404] IMPLICIT INTEGER OPTIONAL,
   *       noAuthRequired  [503] IMPLICIT NULL OPTIONAL,
   *       -- See hw_authenticator_type_t for userAuthType values.  Note
   *       -- this field is a bitmask; multiple authenticator types may be
   *       -- ORed together.
   *       userAuthType    [504] IMPLICIT INTEGER OPTIONAL,
   *       -- authTimeout, if present, must be positive and less than 2^32.
   *       authTimeout [505] IMPLICIT INTEGER OPTIONAL,
   *       allApplications [600] IMPLICIT NULL OPTIONAL,
   *       applicationId   [601] IMPLICIT OCTET_STRING OPTIONAL,
   *       applicationData [700] IMPLICIT OCTET_STRING OPTIONAL,
   *       -- creationDateTime must be a 64-bit Java date/time value.
   *       creationDateTime    [701] IMPLICIT INTEGER OPTIONAL,
   *       -- See keymaster_origin_t for origin values.
   *       origin  [702] IMPLICIT INTEGER OPTIONAL,
   *       rollbackResistant   [703] IMPLICIT NULL OPTIONAL,
   *       -- rootOfTrust is included only if bootloader is not locked.
   *       rootOfTrust [704] IMPLICIT RootOfTrust OPTIONAL
   *       osVersion   [705] IMPLICIT INTEGER OPTIONAL,
   *       patchLevel  [706] IMPLICIT INTEGER OPTIONAL,
   *       uniqueId    [707] IMPLICIT NULL OPTIONAL,
   *   }
   *
   *   RootOfTrust ::= SEQUENCE {
   *       verifiedBootKey OCTET_STRING,
   *       osVersion   INTEGER,
   *       patchMonthYear  INTEGER,
   *   }
   */
  public static AndroidKeyStoreAttestation Parse(X509Certificate cert)
      throws CertificateParsingException {
    // Extract the extension from the certificate
    byte[] extensionValue = extractExtensionValue(cert);

    // Get the KeyDescription sequence
    DLSequence keyDescriptionSequence = getKeyDescriptionSequence(extensionValue);

    // Extract version
    Integer keymasterVersion = getKeymasterVersion(keyDescriptionSequence);

    // Extract challenge
    byte[] challenge = getAttestationChallenge(keyDescriptionSequence);

    // Extract the software authorization list
    DLSequence softwareEnforcedSequence = getSoftwareEncodedSequence(keyDescriptionSequence);
    AuthorizationList softwareAuthorizationList =
        extractAuthorizationList(softwareEnforcedSequence);

    // Get the unique id
    byte[] uniqueId = null;
    if (keyDescriptionSequence.size() == DESCRIPTION_LENGTH_MAX) {
      uniqueId = getUniqueId(keyDescriptionSequence);
    }

    return new AndroidKeyStoreAttestation(
        keymasterVersion, challenge, softwareAuthorizationList, uniqueId);
  }

  /**
   * @return parsed keymaster version
   */
  public Integer getKeyMasterVersion() {
    return keymasterVersion;
  }

  /**
   * @return parsed software authorization list
   */
  public AuthorizationList getSoftwareAuthorizationList() {
    return softwareAuthorizationList;
  }

  /**
   * @return the parsed unique id or {@code null} if none was included
   */
  public byte[] getUniqueId() {
    return uniqueId;
  }

  /**
   * @return the parsed attestation challenge
   */
  public byte[] getAttestationChallenge() {
    return attestationChallenge;
  }

  private static byte[] extractExtensionValue(X509Certificate cert)
      throws CertificateParsingException {
    byte[] extensionValue = cert.getExtensionValue(KEY_DESCRIPTION_OID);

    if (extensionValue == null || extensionValue.length == 0) {
      throw new CertificateParsingException(
          "Did not find KeyDescription extension with OID " + KEY_DESCRIPTION_OID);
    }

    return extensionValue;
  }

  private static DLSequence getKeyDescriptionSequence(byte[] extensionValue)
      throws CertificateParsingException {
    ASN1InputStream ais = new ASN1InputStream(extensionValue);
    ASN1Object asn1Object;

    // Read the key description octet string
    try {
      asn1Object = ais.readObject();
      ais.close();
    } catch (IOException e) {
      throw new CertificateParsingException("Not able to read KeyDescription ASN.1 object", e);
    }
    if (asn1Object == null || !(asn1Object instanceof DEROctetString)) {
      throw new CertificateParsingException("Expected KeyDescription Octet String.");
    }
    DEROctetString octet = (DEROctetString) asn1Object;

    // Read out the Sequence
    ais = new ASN1InputStream(octet.getOctets());
    try {
      asn1Object = ais.readObject();
      ais.close();
    } catch (IOException e) {
      throw new CertificateParsingException("Not able to read KeyDescription Octet String.", e);
    }
    if (asn1Object == null || !(asn1Object instanceof DLSequence)) {
      throw new CertificateParsingException("Expected KeyDescription Sequence.");
    }
    DLSequence sequence = (DLSequence) asn1Object;

    if (sequence.size() < DESCRIPTION_LENGTH_MIN || sequence.size() > DESCRIPTION_LENGTH_MAX) {
      throw new CertificateParsingException("KeyDescription Sequence has " + sequence.size()
          + " elements.  Expected length between " + DESCRIPTION_LENGTH_MIN + " and "
          + DESCRIPTION_LENGTH_MAX);
    }

    return sequence;
  }

  private static DLSequence getSoftwareEncodedSequence(DLSequence keyDescriptionSequence)
      throws CertificateParsingException {
    ASN1Encodable asn1Encodable =
        keyDescriptionSequence.getObjectAt(DESCRIPTION_SOFTWARE_ENFORCED_INDEX);
    if (asn1Encodable == null || !(asn1Encodable instanceof DLSequence)) {
      throw new CertificateParsingException("Expected softwareEnforced DLSequence.");
    }
    return (DLSequence) asn1Encodable;
  }

  private static int getIntFromAsn1Encodable(ASN1Encodable asn1Encodable)
      throws CertificateParsingException {
    if (asn1Encodable == null || !(asn1Encodable instanceof ASN1Integer)) {
      throw new CertificateParsingException("Expected INTEGER type.");
    }
    ASN1Integer asn1Integer = (ASN1Integer) asn1Encodable;
    BigInteger bigInt = asn1Integer.getPositiveValue();
    if (bigInt.bitLength() > MAX_INTEGER_BITS) {
      throw new CertificateParsingException("INTEGER too big");
    }
    return bigInt.intValue();
  }

  private static byte[] getByteArrayFromAsn1Encodable(ASN1Encodable asn1Encodable)
      throws CertificateParsingException {
    if (asn1Encodable == null || !(asn1Encodable instanceof DEROctetString)) {
      throw new CertificateParsingException("Expected DEROctetString");
    }
    DEROctetString derOctectString = (DEROctetString) asn1Encodable;
    return derOctectString.getOctets();
  }

  private static int checkValidTag(int tag) {
    // TODO(aczeskis): implement
    return tag;
  }

  private static HashMap<Integer, ASN1Primitive> extractTaggedObjects(DLSequence dlSequence)
      throws CertificateParsingException {
    HashMap<Integer, ASN1Primitive> taggedObjects = new HashMap<Integer, ASN1Primitive>();

    for (ASN1Encodable asn1EncodablePurpose : dlSequence.toArray()) {
      if (asn1EncodablePurpose == null || !(asn1EncodablePurpose instanceof DERTaggedObject)) {
        throw new CertificateParsingException("Expected DERTagged object");
      }
      DERTaggedObject derTaggedObject = (DERTaggedObject) asn1EncodablePurpose;
      taggedObjects.put(
          Integer.valueOf(checkValidTag(derTaggedObject.getTagNo())), derTaggedObject.getObject());
    }

    return taggedObjects;
  }

  private static int getKeymasterVersion(DLSequence keyDescriptionSequence)
      throws CertificateParsingException {
    ASN1Encodable asn1Encodable = keyDescriptionSequence.getObjectAt(DESCRIPTION_VERSION_INDEX);
    return getIntFromAsn1Encodable(asn1Encodable);
  }

  private static byte[] getAttestationChallenge(DLSequence keyDescriptionSequence)
      throws CertificateParsingException {
    ASN1Encodable asn1Encodable = keyDescriptionSequence.getObjectAt(DESCRIPTION_CHALLENGE_INDEX);
    return getByteArrayFromAsn1Encodable(asn1Encodable);
  }

  private static byte[] getUniqueId(DLSequence keyDescriptionSequence)
      throws CertificateParsingException {
    ASN1Encodable asn1Encodable = keyDescriptionSequence.getObjectAt(DESCRIPTION_UNIQUE_INDEX);
    return getByteArrayFromAsn1Encodable(asn1Encodable);
  }

  private static List<Purpose> getPurpose(
      HashMap<Integer, ASN1Primitive> softwareEnforcedTaggedObjects)
      throws CertificateParsingException {
    ASN1Primitive asn1Primitive = softwareEnforcedTaggedObjects.get(AUTHZ_PURPOSE_TAG);
    if (!(asn1Primitive instanceof DERSet)) {
      throw new CertificateParsingException("Expected DERSet");
    }

    DERSet set = (DERSet) asn1Primitive;
    List<Purpose> purpose = new ArrayList<Purpose>();
    for (ASN1Encodable asn1Encodable : set.toArray()) {
      purpose.add(Purpose.fromValue(getIntFromAsn1Encodable(asn1Encodable)));
    }

    return purpose;
  }

  private static Algorithm getAlgorithm(
      HashMap<Integer, ASN1Primitive> softwareEnforcedTaggedObjects)
      throws CertificateParsingException {
    ASN1Primitive asn1Primitive = softwareEnforcedTaggedObjects.get(AUTHZ_ALGORITHM_TAG);
    return Algorithm.fromValue(getIntFromAsn1Encodable(asn1Primitive));
  }

  private static AuthorizationList extractAuthorizationList(DLSequence authorizationSequence)
      throws CertificateParsingException {
    HashMap<Integer, ASN1Primitive> softwareEnforcedTaggedObjects =
        extractTaggedObjects(authorizationSequence);

    return new AuthorizationList.Builder()
        .setPurpose(getPurpose(softwareEnforcedTaggedObjects))
        .setAlgorithm(getAlgorithm(softwareEnforcedTaggedObjects))
        .build();
  }
}
