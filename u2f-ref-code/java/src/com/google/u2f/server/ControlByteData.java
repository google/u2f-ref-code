package com.google.u2f.server;

/**
 * If later versions add more fields, this object can store the parsed control byte data. This can
 * be extended to an arbitrary number of fields, so that many control bytes can be chained together
 * to allow the server to process many different types of messages.
 */
public class ControlByteData {
  private final boolean userPresence;
  private final boolean isTransferAccessMessage;
  
  public ControlByteData(boolean userPresence, boolean isTransferAccessMessage) {
    this.userPresence = userPresence;
    this.isTransferAccessMessage = isTransferAccessMessage;
  }

  public boolean getUserPresence() {
    return userPresence;
  }

  public boolean getIsTransferAccessMessage() {
    return isTransferAccessMessage;
  }
}
