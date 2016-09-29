package com.google.u2f.key.messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import com.google.u2f.U2FException;

/**
 * Allows transfer of access from one device to another. TransferAccessMessages can be chained in
 * case devices do not sign in before transferring access again.
 */
public class TransferAccessMessage {
  private final int sequenceNumber;
  private final byte[] newUserPublicKey;
  private final byte[] applicationSha256;
  private final X509Certificate newAttestationCertificate;
  private final byte[] signatureUsingAuthenticationKey;
  private final byte[] signatureUsingAttestationKey;

  /**
   * Constructor for TransferAccessMessage.
   * 
   * @param sequenceNumber: Int indicating position in the chain. 1 is first. 0 is reserved.
   * @param newUserPublicKey: Public Key to which access is being transferred.
   * @param applicationSha256
   * @param newAttestationCertificate: Attestation cert of the key to which access is being
   *        transferred
   * @param signatureUsingAuthenticationKey: Signature using key from which access is being
   *        transferred. Signature over sequenceNuber, access-transferred-from public key (old),
   *        acces-transferred-to public key (newUserPublicKey), applicationSha256, and the
   *        newAttestationCertificate.
   * @param signatureUsingAttestationKey: Signature using the old (transferred access from)
   *        attestation private key over all above parameters, including the
   *        signatureUsingAuthenticationKey.
   */
  public TransferAccessMessage(int sequenceNumber, byte[] newUserPublicKey,
      byte[] applicationSha256, X509Certificate newAttestationCertificate,
      byte[] signatureUsingAuthenticationKey, byte[] signatureUsingAttestationKey) {
    
    Preconditions.checkNotNull(sequenceNumber, "Sequence Number should not be null");
    Preconditions.checkNotNull(newUserPublicKey, "New user public key should not be null");
    Preconditions.checkNotNull(applicationSha256, "Application Sha256 should not be null");
    Preconditions.checkNotNull(newAttestationCertificate,
        "New attestation certificate should not be null");
    Preconditions.checkNotNull(signatureUsingAuthenticationKey,
        "Signature using authentication key should not be null");
    Preconditions.checkNotNull(signatureUsingAttestationKey,
        "Signature using attestation Key should not be null");
    
    this.sequenceNumber = sequenceNumber;
    this.newUserPublicKey = newUserPublicKey;
    this.applicationSha256 = applicationSha256;
    this.newAttestationCertificate = newAttestationCertificate;
    this.signatureUsingAuthenticationKey = signatureUsingAuthenticationKey;
    this.signatureUsingAttestationKey = signatureUsingAttestationKey;
  }
  
  /**
   * Build TransferAccessMessage from raw byte  array.
   */
  public static TransferAccessMessage fromBytes(byte[] data) throws U2FException {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));

      int sequenceNumber = inputStream.readUnsignedByte();
      
      byte[] newUserPublicKey = new byte[65];
      inputStream.readFully(newUserPublicKey);
      
      byte[] applicationSha256 = new byte[32];
      inputStream.readFully(applicationSha256);
      
      // TODO(alextaka): This doesn't seem to read the correct number of bytes
      X509Certificate newAttestationCertificate = (X509Certificate) CertificateFactory.getInstance(
          "X.509").generateCertificate(inputStream);
      
      byte[] signatureUsingAuthenticationKey = new byte[inputStream.readUnsignedByte()];
      inputStream.readFully(signatureUsingAuthenticationKey);
      
      byte[] signatureUsingAttestationKey = new byte[inputStream.readUnsignedByte()];
      inputStream.readFully(signatureUsingAttestationKey);
      
      if (inputStream.available() != 0) {
        byte[] remainingBytes = new byte[inputStream.available()];
        inputStream.readFully(remainingBytes);
        throw new U2FException(
            "Message ends with unexpected data: " + BaseEncoding.base16().encode(remainingBytes));
      }

      return new TransferAccessMessage(sequenceNumber, newUserPublicKey, applicationSha256,
          newAttestationCertificate, signatureUsingAuthenticationKey, signatureUsingAttestationKey);
    } catch (IOException e) {
      throw new U2FException("Error when parsing raw Transfer Access Message", e);
    } catch (CertificateException e) {
      throw new U2FException("Error when parsing attestation certificate", e);
    }
  }
  
  /**
   * Integer indicated the position in the chain of TRANSFER_ACCESS_MESSAGES. Higher numbers for 
   * more recent messages. Zero is reserved, so '1' is the first TransferAccessMessage in the chain. 
   */
  public int getMessageSequenceNumber() {
    return sequenceNumber;
  }

  /**
   * The new public key to be registered. This is the (uncompressed) x,y-representation of a curve
   * point on the P-256 NIST elliptic curve.
   */
  public byte[] getNewUserPublicKey() {
    return newUserPublicKey;
  }

  /**
   * The application parameter is the SHA-256 hash of the application identity
   * of the application requesting the registration
   */
  public byte[] getApplicationSha256() {
    return applicationSha256;
  }

  /**
   * This is a X.509 certificate.
   */
  public X509Certificate getNewAttestationCertificate() {
    return newAttestationCertificate;
  }

  /** This is a ECDSA signature (on P-256) signed with the Private Key used for authentication*/
  public byte[] getSignatureUsingAuthenticationKey() {
    return signatureUsingAuthenticationKey;
  }

  /** This is a ECDSA signature (on P-256) signed with the Attestation Private Key*/
  public byte[] getSignatureUsingAttestationKey() {
    return signatureUsingAttestationKey;
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(sequenceNumber, newUserPublicKey, applicationSha256,
        newAttestationCertificate, signatureUsingAuthenticationKey, signatureUsingAttestationKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TransferAccessMessage other = (TransferAccessMessage) obj;
    return Objects.equals(sequenceNumber, other.sequenceNumber)
        && Arrays.equals(newUserPublicKey, other.newUserPublicKey)
        && Arrays.equals(applicationSha256, other.applicationSha256)
        && Objects.equals(newAttestationCertificate, other.newAttestationCertificate)
        && Arrays.equals(signatureUsingAuthenticationKey, other.signatureUsingAuthenticationKey)
        && Arrays.equals(signatureUsingAttestationKey, other.signatureUsingAttestationKey);
  }

}
