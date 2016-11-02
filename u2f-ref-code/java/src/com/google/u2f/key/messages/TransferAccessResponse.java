package com.google.u2f.key.messages;

import java.util.Arrays;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.u2f.server.ControlFlags;

/**
 * TransferAccessResponse is constructed by a device to which access has been transferred. It wraps 
 * the TransferAccessMessage chain with necessary information to register a new key.
 */
public class TransferAccessResponse extends AuthenticateResponse {
  private final TransferAccessMessage[] transferAccessMessages;
  private final byte[] keyHandle;

  /**
   * Constructor for the TransferAccessResponse
   * 
   * @param controlFlags: Sets the {@link ControlFlags} for this Response.
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
    
    this.transferAccessMessages = Preconditions.checkNotNull(transferAccessMessages);
    this.keyHandle = Preconditions.checkNotNull(keyHandle);
  }

  /**
   * ControlFlags object tracks information about the TransferAccessResponse. The second least
   * significant bit should be set to 1 to indicate this is a transferAccessResponse.
   */
  public ControlFlags getControlFlags() {
    return ControlFlags.fromByte(super.getControlFlagByte());
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
  
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), transferAccessMessages, keyHandle);
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
    return super.equals(other)
        && Arrays.equals(transferAccessMessages, other.transferAccessMessages)
        && Arrays.equals(keyHandle, other.keyHandle);
  }
}
