package com.google.u2f.server.impl.attestation.android;

import com.google.gson.JsonObject;
import com.google.u2f.server.impl.attestation.X509ExtensionParsingUtil;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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

  // Tags for Authorization List
  private static final int AUTHZ_PURPOSE_TAG = 1;
  private static final int AUTHZ_ALGORITHM_TAG = 2;
  private static final int AUTHZ_KEY_SIZE_TAG = 3;
  private static final int AUTHZ_BLOCK_MODE_TAG = 4;

  private final int keymasterVersion;
  private final byte[] attestationChallenge;
  private final AuthorizationList softwareAuthorizationList;
  private final AuthorizationList teeAuthorizationList;

  private AndroidKeyStoreAttestation(Integer keymasterVersion, byte[] attestationChallenge,
      AuthorizationList softwareAuthorizationList, AuthorizationList teeAuthorizationList) {
    this.keymasterVersion = keymasterVersion;
    this.attestationChallenge = attestationChallenge;
    this.softwareAuthorizationList = softwareAuthorizationList;
    this.teeAuthorizationList = teeAuthorizationList;
  }

  /**
   * Parses the key description extension.  Note that this method only parses the description
   * extension in the leaf cert.  It *does not* validate the certificate (or any chain).
   *
   * TODO(aczeskis): Add chain validation and remove/clarify the above comment.
   *
   * Expected format of the description extension is:
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
    ASN1OctetString extensionValue =
        X509ExtensionParsingUtil.extractExtensionValue(cert, KEY_DESCRIPTION_OID);

    if (extensionValue == null) {
      return null;
    }

    // Get the KeyDescription sequence
    ASN1Sequence keyDescriptionSequence = getKeyDescriptionSequence(extensionValue);

    // Extract version
    Integer keymasterVersion = getKeymasterVersion(keyDescriptionSequence);

    // Extract challenge
    byte[] challenge = getAttestationChallenge(keyDescriptionSequence);

    // Extract the software authorization list
    ASN1Sequence softwareEnforcedSequence = getSoftwareEncodedSequence(keyDescriptionSequence);
    AuthorizationList softwareAuthorizationList =
        extractAuthorizationList(softwareEnforcedSequence);

    // Extract the tee authorization list
    ASN1Sequence teeEnforcedSequence = getTeeEncodedSequence(keyDescriptionSequence);
    AuthorizationList teeAuthorizationList = extractAuthorizationList(teeEnforcedSequence);

    return new AndroidKeyStoreAttestation(
        keymasterVersion, challenge, softwareAuthorizationList, teeAuthorizationList);
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

  /**
   * @return the parsed TEE authorization list
   */
  public AuthorizationList getTeeAuthorizationList() {
    return teeAuthorizationList;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        attestationChallenge, keymasterVersion, softwareAuthorizationList, teeAuthorizationList);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    AndroidKeyStoreAttestation other = (AndroidKeyStoreAttestation) obj;
    return Objects.equals(attestationChallenge, other.attestationChallenge)
        && Objects.equals(keymasterVersion, other.keymasterVersion)
        && Objects.equals(softwareAuthorizationList, other.softwareAuthorizationList)
        && Objects.equals(teeAuthorizationList, other.teeAuthorizationList);
  }

  @Override
  public String toString() {
    StringBuilder attestation = new StringBuilder();
    attestation.append("[\n  keymasterVersion: " + keymasterVersion);

    if (attestationChallenge != null && attestationChallenge.length > 0) {
      attestation.append("\n  attestationChallenge: 0x");
      attestation.append(Hex.encodeHexString(attestationChallenge));
    }

    if (softwareAuthorizationList != null) {
      attestation.append("\n  softwareEnforced: ");
      attestation.append(softwareAuthorizationList.toString().replaceAll("\n", "\n  "));
    }

    if (teeAuthorizationList != null) {
      attestation.append("\n  teeEnforced: ");
      attestation.append(teeAuthorizationList.toString().replaceAll("\n", "\n  "));
    }

    attestation.append("\n]");

    return attestation.toString();
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("keymaster_version", keymasterVersion);
    json.addProperty("attestation_challenge", Hex.encodeHexString(attestationChallenge));
    json.add("software_encoded", softwareAuthorizationList.toJson());
    json.add("tee_encoded", teeAuthorizationList.toJson());
    return json;
  }

  private static ASN1Sequence getKeyDescriptionSequence(ASN1OctetString octet)
      throws CertificateParsingException {
    // Read out the Sequence
    ASN1Object asn1Object = X509ExtensionParsingUtil.getAsn1Object(octet.getOctets());
    if (asn1Object == null || !(asn1Object instanceof ASN1Sequence)) {
      throw new CertificateParsingException("Expected KeyDescription Sequence.");
    }
    ASN1Sequence sequence = (ASN1Sequence) asn1Object;

    if (sequence.size() != DESCRIPTION_LENGTH) {
      throw new CertificateParsingException("KeyDescription Sequence has " + sequence.size()
          + " elements.  Expected " + DESCRIPTION_LENGTH + " elements ");
    }

    return sequence;
  }

  private static ASN1Sequence getSoftwareEncodedSequence(ASN1Sequence keyDescriptionSequence)
      throws CertificateParsingException {
    ASN1Encodable asn1Encodable =
        keyDescriptionSequence.getObjectAt(DESCRIPTION_SOFTWARE_ENFORCED_INDEX);
    if (asn1Encodable == null || !(asn1Encodable instanceof ASN1Sequence)) {
      throw new CertificateParsingException("Expected softwareEnforced ASN1Sequence.");
    }
    return (ASN1Sequence) asn1Encodable;
  }

  private static ASN1Sequence getTeeEncodedSequence(ASN1Sequence keyDescriptionSequence)
      throws CertificateParsingException {
    ASN1Encodable asn1Encodable =
        keyDescriptionSequence.getObjectAt(DESCRIPTION_TEE_ENFORCED_INDEX);
    if (asn1Encodable == null || !(asn1Encodable instanceof ASN1Sequence)) {
      throw new CertificateParsingException("Expected teeEnforced ASN1Sequence.");
    }
    return (ASN1Sequence) asn1Encodable;
  }

  private static int getKeymasterVersion(ASN1Sequence keyDescriptionSequence)
      throws CertificateParsingException {
    ASN1Encodable asn1Encodable = keyDescriptionSequence.getObjectAt(DESCRIPTION_VERSION_INDEX);
    return X509ExtensionParsingUtil.getInt(asn1Encodable);
  }

  private static byte[] getAttestationChallenge(ASN1Sequence keyDescriptionSequence)
      throws CertificateParsingException {
    ASN1Encodable asn1Encodable = keyDescriptionSequence.getObjectAt(DESCRIPTION_CHALLENGE_INDEX);
    return X509ExtensionParsingUtil.getByteArray(asn1Encodable);
  }

  private static List<Purpose> getPurpose(HashMap<Integer, ASN1Primitive> taggedObjects)
      throws CertificateParsingException {
    return getListFromTaggedObjectSet(taggedObjects, AUTHZ_PURPOSE_TAG, Purpose.class);
  }

  private static Algorithm getAlgorithm(HashMap<Integer, ASN1Primitive> taggedObjects)
      throws CertificateParsingException {
    ASN1Primitive asn1Primitive = taggedObjects.get(AUTHZ_ALGORITHM_TAG);
    if (asn1Primitive == null) {
      // No algorithm found
      return null;
    }
    return Algorithm.fromValue(X509ExtensionParsingUtil.getInt(asn1Primitive));
  }

  private static Integer getKeySize(HashMap<Integer, ASN1Primitive> taggedObjects)
      throws CertificateParsingException {
    ASN1Primitive asn1Primitive = taggedObjects.get(AUTHZ_KEY_SIZE_TAG);
    if (asn1Primitive == null) {
      // No key size found
      return null;
    }
    return X509ExtensionParsingUtil.getInt(asn1Primitive);
  }

  private static List<BlockMode> getBlockMode(
      HashMap<Integer, ASN1Primitive> softwareEnforcedTaggedObjects)
      throws CertificateParsingException {
    return getListFromTaggedObjectSet(
        softwareEnforcedTaggedObjects, AUTHZ_BLOCK_MODE_TAG, BlockMode.class);
  }

  // TODO(aczeskis): There is a cleaner way of doing this in Java 8.  In Java 8, we can make Purpose
  // & BlockMode implement an interface (so the function could call .fromInt() on the
  // parameterized type).  Unfortunately, Java 7 does not allow interfaces to have static methods!
  // This decision was fixed in Java 8.
  private static <T> List<T> getListFromTaggedObjectSet(
      HashMap<Integer, ASN1Primitive> taggedObjects, int tag, Class<T> type)
      throws CertificateParsingException {
    ASN1Primitive asn1Primitive = taggedObjects.get(tag);
    if (asn1Primitive == null) {
      // No tagged object mode found
      return null;
    }

    if (!(asn1Primitive instanceof ASN1Set)) {
      throw new CertificateParsingException("Expected ASN1Set");
    }

    ASN1Set set = (ASN1Set) asn1Primitive;
    List<T> list = new ArrayList<T>();
    for (ASN1Encodable asn1Encodable : set.toArray()) {
      list.add(buildTypeFromInt(X509ExtensionParsingUtil.getInt(asn1Encodable), type));
    }

    return list;
  }

  @SuppressWarnings("unchecked")
  private static <T> T buildTypeFromInt(int value, Class<T> type)
      throws CertificateParsingException {
    if (type == Purpose.class) {
      return (T) Purpose.fromValue(value);
    } else if (type == BlockMode.class) {
      return (T) BlockMode.fromValue(value);
    } else {
      throw new CertificateParsingException("Cannot build type " + type.getSimpleName());
    }
  }

  private static AuthorizationList extractAuthorizationList(ASN1Sequence authorizationSequence)
      throws CertificateParsingException {
    HashMap<Integer, ASN1Primitive> taggedObjects =
        X509ExtensionParsingUtil.extractTaggedObjects(authorizationSequence);

    return new AuthorizationList.Builder()
        .setPurpose(getPurpose(taggedObjects))
        .setAlgorithm(getAlgorithm(taggedObjects))
        .setKeySize(getKeySize(taggedObjects))
        .setBlockMode(getBlockMode(taggedObjects))
        .build();
  }
}
