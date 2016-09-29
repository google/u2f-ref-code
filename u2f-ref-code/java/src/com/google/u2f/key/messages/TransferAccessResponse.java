package com.google.u2f.key.messages;

import java.util.Arrays;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.u2f.server.ControlFlags;

/**
 * TransferAccessResponse is constructed by a device to whom access has been transferred. It wraps 
 * the TransferAccessMessage chain with necessary information to register a new key.
 */
public class TransferAccessResponse extends AuthenticateResponse {
  private final byte controlFlags;
  private final TransferAccessMessage[] transferAccessMessages;
  private final byte[] keyHandle;
  private final int counter;
  private final byte[] signature;

  /**
   * Constructor for the TransferAccessResponse
   * 
   * @param controlFlags: See ControlFlags. Should indicate this is a TransferAccessResponse
   * @param transferAccessMessages: Array of TransferAccessMessages. The first message is the first
   *        in the chain.
   * @param keyHandle: KeyHandle for the final key to be registered.
   * @param counter: Integer
   * @param signature: Signature using the attestation private key over ControlByte, Counter,
   *        Challenge from the AuthenticateRequest, KeyHandle, and a hash of the last
   *        TransferAccessMessage in the chain.
   */
  public TransferAccessResponse(byte controlFlags, TransferAccessMessage[] transferAccessMessages,
      byte[] keyHandle, int counter, byte[] signature) {
    super(controlFlags, counter, signature); 

    Preconditions.checkNotNull(controlFlags, "Control flags should not be null");
    Preconditions.checkNotNull(transferAccessMessages,
        "Array of transfer access messages should not be null");
    Preconditions.checkNotNull(keyHandle, "Key handle should not be null");
    Preconditions.checkNotNull(counter, "Counter should not be null");
    Preconditions.checkNotNull(signature, "Signature should not be null");
    
    this.controlFlags = controlFlags;
    this.transferAccessMessages = transferAccessMessages;
    this.keyHandle = keyHandle;
    this.counter = counter;
    this.signature = signature;
  }

  /**
   * ControlFlags object tracks information about the TransferAccessResponse. Bit 1 should be set to
   * 1 indicating this is a transferAccessResponse.
   */
  public ControlFlags getControlFlags() {
    return ControlFlags.fromByte(controlFlags);
  }

  /** An array of transferAccessMessages in the order in which they need to be processed */
  public TransferAccessMessage[] getTransferAccessMessages() {
    return transferAccessMessages;
  }

  /**
   * The new key handle associated with the to-be registered key. This handle allows the U2F token
   * to identify the generated key pair. U2F tokens MAY wrap the generated private key and the
   * application id it was generated for, and output that as the key handle.
   */
  public byte[] getKeyHandle() {
    return keyHandle;
  }
  
  /**
   * This is the big-endian representation of a counter value that the U2F token increments every
   * time it performs an authentication operation.
   */
  public int getCounter() {
    return counter;
  }

  /** This is a ECDSA signature (on P-256) */
  public byte[] getSignature() {
    return signature;
  }

  @Override
  public int hashCode() {
    return Objects.hash(controlFlags, transferAccessMessages, keyHandle, counter, signature);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TransferAccessResponse other = (TransferAccessResponse) obj;
    return Objects.equals(controlFlags, other.controlFlags)
        && Arrays.equals(transferAccessMessages, other.transferAccessMessages)
        && Arrays.equals(keyHandle, other.keyHandle) && Objects.equals(counter, other.counter)
        && Arrays.equals(signature, other.signature);
  }
}
