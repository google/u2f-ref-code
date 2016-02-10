package com.google.u2f.tools;

import com.google.u2f.U2FException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A set of high-level X509 parsing and validation utils
 */
public class X509Util {
  private static final Logger Log = Logger.getLogger(X509Util.class.getName());
  private static final String X509 = "X.509";

  /**
   * Parse a single certificate out of a DER-encoded byte array
   *
   * @param encodedDerCertificate DER encoded X509 certificate
   * @return a parsed certificate
   * @throws CertificateException if parsing could not be done
   */
  public static X509Certificate parseCertificate(byte[] encodedDerCertificate)
      throws CertificateException {
    return (X509Certificate) CertificateFactory.getInstance(X509).generateCertificate(
        new ByteArrayInputStream(encodedDerCertificate));
  }

  /**
   * Parse a certificate chain out of a DER-encoded byte array.  Will attempt to parse certs
   * out of the byte array until it can no longer do so.  Will return all the certs (in order) that
   * were parsed successfully.
   *
   * @param encodedDerCertificates DER encoded X509 certificates
   * @return a parsed certificate chain
   */
  public static X509Certificate[] parseCertificateChain(byte[] encodedDerCertificates) {
    return parseCertificateChain(
        new DataInputStream(new ByteArrayInputStream(encodedDerCertificates)));
  }

  /**
   * Parse a certificate chain out of a DER-encoded DataInputStream.  Will attempt to parse certs
   * out of the stream until it can no longer do so.  Will return all the certs (in order) that were
   * parsed successfully.
   *
   * @param encodedDerCertificates DER encoded X509 certificates
   * @return a parsed certificate chain
   */
  public static X509Certificate[] parseCertificateChain(DataInputStream encodedDerCertificates) {
    List<X509Certificate> certChain = new LinkedList<X509Certificate>();
    try {
      while (encodedDerCertificates.available() > 0) {
        certChain.add((X509Certificate) CertificateFactory.getInstance(X509).generateCertificate(
            encodedDerCertificates));
        System.out.println("Cert added");
      }
    } catch (IOException | CertificateException e) {
      // no more certs to read
      System.out.println("No more certs");
    }
    return certChain.toArray(new X509Certificate[0]);
  }

  /**
   * Attempts to verify a certificate chain and makes sure that it chains up to at least one of the
   * provided CA root certificates.
   *
   * @param certChain The certificate chain to validate.  The leaf certificate is assumed to be in
   *   {@code certChain[0]} and it is assumed that {@code certChain[i]} certificate is signed by
   *   {@code certChain[i+1]}.  Finally, {@code certChain[certChain.length-1]} should be singed
   *   by one of the root CA certificates.
   * @param caCerts The trusted root certificates.
   * @return {@code true} if validation succeeded
   * @throws U2FException if there was a parsing error
   */
  public static boolean verifyCertChain(X509Certificate[] certChain, X509Certificate[] caCerts)
      throws U2FException {
    if (caCerts == null || certChain == null || certChain.length == 0) {
      return false;
    }

    // Walk through intermediates up the chain
    try {
      for (int i = 0; i < certChain.length - 1; i++) {
        certChain[i].verify(certChain[i + 1].getPublicKey());
      }
    } catch (SignatureException | NoSuchProviderException | InvalidKeyException
        | NoSuchAlgorithmException | CertificateException e) {
      Log.log(Level.SEVERE, "Cannot validate cert chain is correctly signed by intermediaries", e);
      return false;
    }

    // Now attempt to verify up to one of the roots
    boolean validated = true;
    for (int i = 0; i < caCerts.length; i++) {
      try {
        certChain[certChain.length - 1].verify(caCerts[i].getPublicKey());
      } catch (SignatureException | NoSuchProviderException | InvalidKeyException
          | NoSuchAlgorithmException | CertificateException e) {
        Log.log(Level.WARNING, "Cert chain validation failed to match a root.", e);

        // If it's the last cert, that means we didn't find a matching root ca
        if (i == caCerts.length - 1) {
          validated = false;
          Log.log(Level.SEVERE, "Cannot validate cert chain is correctly signed by any known root");
        }
      }
    }

    return validated;
  }

  /**
   * Encodes an array of certificates into a DER-encoded byte array
   * @param certsArray to encode
   * @return the encoded certs
   */
  public static byte[] encodeCertArray(X509Certificate[] certsArray) {
    if (certsArray == null) {
      return null;
    }

    if (certsArray.length == 0) {
      return new byte[0];
    }

    try {
      ByteArrayOutputStream encodedCerts = new ByteArrayOutputStream();
      for (X509Certificate cert : certsArray) {
        encodedCerts.write(cert.getEncoded());
      }
      return encodedCerts.toByteArray();
    } catch (CertificateEncodingException | IOException e) {
      throw new RuntimeException();
    }
  }
}
