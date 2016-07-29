/**
 * 
 */
package com.google.u2f.key.messages;

import java.security.cert.X509Certificate;

/**
 * @author at
 *
 */
public class TransferAccessResponse extends AuthenticateResponse {
    private final byte controlByte; // What happens when we have multiple control bytes? If that is the case, does this need to be an array?
    private final byte[] userPublicKey; // the new public key to be registered
    private final byte[] keyHandle;	   // the new key handle associated with the to-be-registered public key
    private final X509Certificate attestationCertificate;
    private final byte[][] transferAccessResponses; // An array of transferAccessMessages, which are byte arrays themselves.
    private final int counter;
    private final byte[] signature;

    public TransferAccessResponse(byte controlByte, byte[] userPublicKey, byte[][] transferAccessResponses, 
				  byte[] keyHandle, X509Certificate attestationCertificate, int counter, byte[] signature) {
    	super(controlByte, counter, signature); // This may need a different signature, since it signs different things.
    	this.controlByte = controlByte;
    	this.userPublicKey = userPublicKey;
		this.transferAccessResponses = transferAccessResponses;
		this.keyHandle = keyHandle;
		this.attestationCertificate = attestationCertificate;
		this.counter = counter;
		this.signature = signature;
    }

	
    public byte getControlByte() {
    	return controlByte;
    }
	
    public byte[] getUserPublicKey() {
    	return userPublicKey;
    }

    public byte[][] getTransferAccessResponses() {
    	return transferAccessResponses;
    }


    public byte[] getKeyHandle() {
    	return keyHandle;
    }

    public X509Certificate getAttestationCertificate() {
    	return attestationCertificate;
    }

    public int getCounter() {
    	return counter;
    }

    public byte[] getSignature() {
    	return signature;
    }
}
