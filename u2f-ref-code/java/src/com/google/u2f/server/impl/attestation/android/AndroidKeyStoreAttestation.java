package com.google.u2f.server.impl.attestation.android;

import com.google.u2f.server.impl.attestation.X509ExtensionParsingUtil;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLSequence;

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
  private static final int DESCRIPTION_LENGTH = 4;
  private static final int DESCRIPTION_VERSION_INDEX = 0;
  private static final int DESCRIPTION_CHALLENGE_INDEX = 1;
  private static final int DESCRIPTION_SOFTWARE_ENFORCED_INDEX = 2;
  private static final int DESCRIPTION_TEE_ENFORCED_INDEX = 3;

  // Don't expect more than 32 bits for any INTEGER
  private static final int MAX_INT_BITS = 32;
  private static final int MAX_LONG_BITS = 64;

  // Tags for Authorization List
  private static final int AUTHZ_PURPOSE_TAG = 1;
  private static final int AUTHZ_ALGORITHM_TAG = 2;

  private final int keymasterVersion;
  private final byte[] attestationChallenge;
  private final AuthorizationList softwareAuthorizationList;

  private AndroidKeyStoreAttestation(Integer keymasterVersion, byte[] attestationChallenge,
      AuthorizationList softwareAuthorizationList) {
    this.keymasterVersion = keymasterVersion;
    this.attestationChallenge = attestationChallenge;
    this.softwareAuthorizationList = softwareAuthorizationList;
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
   *       purpose [1] EXPLICIT SET OF INTEGER OPTIONAL,
   *       -- See keymaster_algorithm_t for algorithm values.
   *       algorithm   [2] EXPLICIT INTEGER OPTIONAL,
   *       -- keySize is measured in bits, not bytes, and the value must be
   *       -- positive and less than than 2^32, though realistic values are
   *       -- much smaller.
   *       keySize [3] EXPLICIT INTEGER OPTIONAL,
   *       -- See keymaster_block_mode_t for blockMode values.
   *       blockMode   [4] EXPLICIT SET OF INTEGER OPTIONAL,
   *       -- See keymaster_digest_t for digest values.
   *       digest  [5] EXPLICIT SET OF INTEGER OPTIONAL,
   *       -- See keymaster_padding_t for padding values.
   *       padding [6] EXPLICIT SET OF INTEGER OPTIONAL,
   *       callerNonce [7] EXPLICIT NULL OPTIONAL,
   *       -- minMacLength values must be positive and less than 2^32.
   *       minMacLength    [8] EXPLICIT INTEGER OPTIONAL,
   *       -- See keymaster_kdf_t for kdf values.
   *       kdf [9] EXPLICIT SEQUENCE OF INTEGER OPTIONAL,
   *       -- See keymaster_ec_curve_t for ecCurve values
   *       ecCurve [10] EXPLICIT INTEGER OPTIONAL,
   *       -- rsaPublicExponent must be a valid RSA public exponent less
   *       -- than 2^64.
   *       rsaPublicExponent   [200] EXPLICIT INTEGER OPTIONAL,
   *       eciesSingleHashMode [201] EXPLICIT NULL OPTIONAL,
   *       includeUniqueId [202] EXPLICIT NULL OPTIONAL,
   *       -- See keymaster_key_blob_usage_requirements for
   *       -- blobUsageRequirement values.
   *       blobUsageRequirement    [301] EXPLICIT INTEGER OPTIONAL,
   *       bootloaderOnly  [302] EXPLICIT NULL OPTIONAL,
   *       -- activeDateTime must be a 64-bit Java date/time value.
   *       activeDateTime  [400] EXPLICIT INTEGER OPTIONAL
   *       -- originationExpireDateTime must be a 64-bit Java date/time
   *       -- value.
   *       originationExpireDateTime   [401] EXPLICIT INTEGER OPTIONAL
   *       -- usageExpireDateTime must be a 64-bit Java date/time value.
   *       usageExpireDateTime     [402] EXPLICIT INTEGER OPTIONAL
   *       -- minSecondsBetweenOps must be non-negative and less than 2^32.
   *       minSecondsBetweenOps    [403] EXPLICIT INTEGER OPTIONAL,
   *       -- maxUsesPerBoot must be positive and less than 2^32.
   *       maxUsesPerBoot  [404] EXPLICIT INTEGER OPTIONAL,
   *       noAuthRequired  [503] EXPLICIT NULL OPTIONAL,
   *       -- See hw_authenticator_type_t for userAuthType values.  Note
   *       -- this field is a bitmask; multiple authenticator types may be
   *       -- ORed together.
   *       userAuthType    [504] EXPLICIT INTEGER OPTIONAL,
   *       -- authTimeout, if present, must be positive and less than 2^32.
   *       authTimeout [505] EXPLICIT INTEGER OPTIONAL,
   *       allApplications [600] EXPLICIT NULL OPTIONAL,
   *       applicationId   [601] EXPLICIT OCTET_STRING OPTIONAL,
   *       applicationData [700] EXPLICIT OCTET_STRING OPTIONAL,
   *       -- creationDateTime must be a 64-bit Java date/time value.
   *       creationDateTime    [701] EXPLICIT INTEGER OPTIONAL,
   *       -- See keymaster_origin_t for origin values.
   *       origin  [702] EXPLICIT INTEGER OPTIONAL,
   *       rollbackResistant   [703] EXPLICIT NULL OPTIONAL,
   *       -- rootOfTrust is included only if bootloader is not locked.
   *       rootOfTrust [704] EXPLICIT RootOfTrust OPTIONAL
   *       osVersion   [705] EXPLICIT INTEGER OPTIONAL,
   *       patchLevel  [706] EXPLICIT INTEGER OPTIONAL,
   *       uniqueId    [707] EXPLICIT NULL OPTIONAL,
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
    DEROctetString extensionValue =
        X509ExtensionParsingUtil.extractExtensionValue(cert, KEY_DESCRIPTION_OID);
    
    if (extensionValue == null) {
      return null;
    }

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

    // TODO(aczeskis) Extract the TEE authorization list

    return new AndroidKeyStoreAttestation(keymasterVersion, challenge, softwareAuthorizationList);
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
   * @return the parsed attestation challenge
   */
  public byte[] getAttestationChallenge() {
    return attestationChallenge;
  }

  private static DLSequence getKeyDescriptionSequence(DEROctetString octet)
      throws CertificateParsingException {
    // Read out the Sequence
    ASN1Object asn1Object = X509ExtensionParsingUtil.getAsn1Object(octet.getOctets());
    if (asn1Object == null || !(asn1Object instanceof DLSequence)) {
      throw new CertificateParsingException("Expected KeyDescription Sequence.");
    }
    DLSequence sequence = (DLSequence) asn1Object;

    if (sequence.size() != DESCRIPTION_LENGTH) {
      throw new CertificateParsingException("KeyDescription Sequence has " + sequence.size()
          + " elements.  Expected " + DESCRIPTION_LENGTH + " elements ");
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
    if (bigInt.bitLength() > MAX_INT_BITS) {
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
