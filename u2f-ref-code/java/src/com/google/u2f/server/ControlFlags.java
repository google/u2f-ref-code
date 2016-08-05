package com.google.u2f.server;

/**
 * If later versions add more fields, this object can store the parsed control byte data. This can
 * be extended to an arbitrary number of fields, so that many control bytes can be chained together
 * to allow the server to process many different types of messages.
 */
public class ControlFlags {
  private final boolean userPresence;
  private final boolean isTransferAccessMessage;
  
  private static final byte MASK_USER_PRESENCE = 0x1;
  private static final byte MASK_TRANSFER_ACCESS_MESSAGE = 0x2;


  /**
   * Hidden constructor, use builder instead
   *
   * @param userPresence (optional) true if user verifies presence
   * @param isTransferAccessMessage (optional) true if these flags are attached to a TransferAccess
   *        Message
   */
  private ControlFlags(boolean userPresence, boolean isTransferAccessMessage) {
    this.userPresence = userPresence;
    this.isTransferAccessMessage = isTransferAccessMessage;
  }

  /**
   * Build ControlFlags object from a byte. Future versions may need to accept a byte array.
   */
  public static ControlFlags fromByte(byte controlFlags) {
    boolean userPresence = (controlFlags & MASK_USER_PRESENCE) != 0;
    boolean isTransferAccessMessage =
        (controlFlags & MASK_TRANSFER_ACCESS_MESSAGE) != 0;

    return new ControlFlags.ControlFlagsBuilder()
        .setUserPresenceBit(userPresence)
        .setIsTransferAccessMessageBit(isTransferAccessMessage)
        .build();
  }


  /**
   * Write ControlFlags out to a byte. In the future, this may return a byte array. As of this
   * version, only two flags get used, so this returns a single byte.
   */
  public static byte toByte(ControlFlags controlFlags) {
    byte controlByte = 0x0;

    if (controlFlags.getUserPresence()) {
      controlByte = (byte) (controlByte | MASK_USER_PRESENCE);
    }

    if (controlFlags.getIsTransferAccessMessage()) {
      controlByte = (byte) (controlByte | MASK_TRANSFER_ACCESS_MESSAGE);
    }

    return controlByte;
  }

  public boolean getUserPresence() {
    return userPresence;
  }

  public boolean getIsTransferAccessMessage() {
    return isTransferAccessMessage;
  }

  /**
   * Builder class. When newer versions introduce new flags, add them here. They can be built by
   * passing an array of bytes if there are more than six flags.
   */
  public static class ControlFlagsBuilder {
    private boolean userPresence;
    private boolean isTransferAccessMessage;

    public ControlFlagsBuilder setUserPresenceBit(boolean userPresence) {
      this.userPresence = userPresence;
      return this;
    }

    public ControlFlagsBuilder setIsTransferAccessMessageBit(boolean isTransferAccessMessage) {
      this.isTransferAccessMessage = isTransferAccessMessage;
      return this;
    }

    public ControlFlags build() {
      return new ControlFlags(userPresence, isTransferAccessMessage);
    }

  }
}
