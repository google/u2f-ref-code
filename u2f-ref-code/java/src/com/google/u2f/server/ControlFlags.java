package com.google.u2f.server;

/**
 * If later versions add more fields, this object can store the parsed control byte data. This can
 * be extended to an arbitrary number of fields, so that many control bytes can be chained together
 * to allow the server to process many different types of messages.
 */
public class ControlFlags {
  private final boolean userPresence;
  private final boolean isTransferAccessResponse;
  
  private static final byte MASK_USER_PRESENCE = 0x1;
  private static final byte MASK_TRANSFER_ACCESS_RESPONSE= 0x2;


  /**
   * Hidden constructor, use builder instead
   *
   * @param userPresence (optional) true if user verifies presence
   * @param isTransferAccessResponse (optional) true if these flags are attached to a TransferAccess
   *        Response
   */
  private ControlFlags(boolean userPresence, boolean isTransferAccessResponse) {
    this.userPresence = userPresence;
    this.isTransferAccessResponse = isTransferAccessResponse;
  }

  /**
   * Build ControlFlags object from a byte. Future versions may need to accept a byte array.
   */
  public static ControlFlags fromByte(byte controlFlags) {
    boolean userPresence = (controlFlags & MASK_USER_PRESENCE) != 0;
    boolean isTransferAccessResponse =
        (controlFlags & MASK_TRANSFER_ACCESS_RESPONSE) != 0;

    return new ControlFlags.ControlFlagsBuilder()
        .setUserPresenceBit(userPresence)
        .setIsTransferAccessResponseBit(isTransferAccessResponse)
        .build();
  }


  /**
   * Write ControlFlags out to a byte. In the future, this may return a byte array. As of this
   * version, only two flags get used, so this returns a single byte.
   */
  public byte toByte() {
    byte controlByte = 0x0;

    if (userPresence) {
      controlByte = (byte) (controlByte | MASK_USER_PRESENCE);
    }

    if (isTransferAccessResponse) {
      controlByte = (byte) (controlByte | MASK_TRANSFER_ACCESS_RESPONSE);
    }

    return controlByte;
  }

  public boolean getUserPresence() {
    return userPresence;
  }

  public boolean getIsTransferAccessResponse() {
    return isTransferAccessResponse;
  }

  /**
   * Builder class. When newer versions introduce new flags, add them here. They can be built by
   * passing an array of bytes if there are more than six flags.
   */
  public static class ControlFlagsBuilder {
    private boolean userPresence;
    private boolean isTransferAccessResponse;

    public ControlFlagsBuilder setUserPresenceBit(boolean userPresence) {
      this.userPresence = userPresence;
      return this;
    }

    public ControlFlagsBuilder setIsTransferAccessResponseBit(boolean isTransferAccessResponse) {
      this.isTransferAccessResponse = isTransferAccessResponse;
      return this;
    }

    public ControlFlags build() {
      return new ControlFlags(userPresence, isTransferAccessResponse);
    }

  }
}
