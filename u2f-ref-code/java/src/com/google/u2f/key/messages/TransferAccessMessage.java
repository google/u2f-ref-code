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
 * Allows transfer of access from one device to another. TransferAccessMessages can be chained to 
 * transfer access through a number of devices, for example from phone A to phone B to phone C.
 */
public class TransferAccessMessage {
  private static int RAW_PUBLIC_KEY_SIZE = 65;
  private static int RAW_APPLICATION_SHA_256_SIZE = 32;
  
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
    
    this.sequenceNumber = sequenceNumber;
    this.newUserPublicKey = Preconditions.checkNotNull(newUserPublicKey);
    this.applicationSha256 = Preconditions.checkNotNull(applicationSha256);
    this.newAttestationCertificate = Preconditions.checkNotNull(newAttestationCertificate);
    this.signatureUsingAuthenticationKey =
        Preconditions.checkNotNull(signatureUsingAuthenticationKey);
    this.signatureUsingAttestationKey = Preconditions.checkNotNull(signatureUsingAttestationKey);
  }
  
  /**
   * Build TransferAccessMessage from raw byte array.
   * @throws U2FException if parsing error occurs
   */
  public static TransferAccessMessage fromBytes(byte[] data) throws U2FException {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));

      int sequenceNumber = inputStream.readUnsignedByte();
      
      byte[] newUserPublicKey = new byte[RAW_PUBLIC_KEY_SIZE];
      inputStream.readFully(newUserPublicKey);
      
      byte[] applicationSha256 = new byte[RAW_APPLICATION_SHA_256_SIZE];
      inputStream.readFully(applicationSha256);
      
      X509Certificate newAttestationCertificate = (X509Certificate) CertificateFactory.getInstance(
          "X.509").generateCertificate(inputStream);
      
      int signatureUsingAuthenticationKeyLength = inputStream.readUnsignedByte();
      byte[] signatureUsingAuthenticationKey = new byte[signatureUsingAuthenticationKeyLength];
      inputStream.readFully(signatureUsingAuthenticationKey);
      
      int signatureUsingAttestationKeyLength = inputStream.readUnsignedByte();
      byte[] signatureUsingAttestationKey = new byte[signatureUsingAttestationKeyLength];
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
