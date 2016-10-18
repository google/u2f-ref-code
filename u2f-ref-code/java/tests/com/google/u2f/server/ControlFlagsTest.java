package com.google.u2f.server;

import static org.junit.Assert.*;

import org.junit.Test;

public class ControlFlagsTest {
  private static final byte USER_PRESENCE_BIT_SET = 0x1;
  private static final byte TRANSFER_ACCESS_MESSAGE_BIT_SET = 0x2;
  private static final byte USER_PRESENCE_AND_TRANSFER_ACCESS_BITS_SET = 0x3;
  private static final byte NO_BITS_SET = 0x0;

  @Test
  public void testToByteFromByte() {
    assertEquals(USER_PRESENCE_BIT_SET,
        ControlFlags.toByte(ControlFlags.fromByte(USER_PRESENCE_BIT_SET)));
    assertEquals(TRANSFER_ACCESS_MESSAGE_BIT_SET,
        ControlFlags.toByte(ControlFlags.fromByte(TRANSFER_ACCESS_MESSAGE_BIT_SET)));
    assertEquals(USER_PRESENCE_AND_TRANSFER_ACCESS_BITS_SET,
        ControlFlags.toByte(ControlFlags.fromByte(USER_PRESENCE_AND_TRANSFER_ACCESS_BITS_SET)));
    assertEquals(NO_BITS_SET, ControlFlags.toByte(ControlFlags.fromByte(NO_BITS_SET)));
  }

  @Test
  public void testUserPresenceFromByte() {
    assertTrue(ControlFlags.fromByte(USER_PRESENCE_BIT_SET).getUserPresence());
    assertTrue(ControlFlags.fromByte(USER_PRESENCE_AND_TRANSFER_ACCESS_BITS_SET).getUserPresence());
    assertFalse(ControlFlags.fromByte(NO_BITS_SET).getUserPresence());
    assertFalse(ControlFlags.fromByte(TRANSFER_ACCESS_MESSAGE_BIT_SET).getUserPresence());
  }

  @Test
  public void testIsTransferAccessMessageFromByte() {
    assertTrue(ControlFlags.fromByte(TRANSFER_ACCESS_MESSAGE_BIT_SET).getIsTransferAccessResponse());
    assertTrue(ControlFlags.fromByte(USER_PRESENCE_AND_TRANSFER_ACCESS_BITS_SET)
        .getIsTransferAccessResponse());
    assertFalse(ControlFlags.fromByte(USER_PRESENCE_BIT_SET).getIsTransferAccessResponse());
    assertFalse(ControlFlags.fromByte(NO_BITS_SET).getIsTransferAccessResponse());
  }

  @Test
  public void testBuilder() {
    assertTrue(new ControlFlags.ControlFlagsBuilder().setUserPresenceBit(true).build().getUserPresence());
    assertFalse(
        new ControlFlags.ControlFlagsBuilder().setUserPresenceBit(false).build().getUserPresence());
    assertFalse(new ControlFlags.ControlFlagsBuilder().build().getUserPresence());
    assertFalse(new ControlFlags.ControlFlagsBuilder().setIsTransferAccessResponseBit(true).build()
        .getUserPresence());
    assertTrue(new ControlFlags.ControlFlagsBuilder().setIsTransferAccessResponseBit(true)
        .setUserPresenceBit(true).build().getUserPresence());
    assertFalse(new ControlFlags.ControlFlagsBuilder().setUserPresenceBit(true).build()
        .getIsTransferAccessResponse());
    assertTrue(new ControlFlags.ControlFlagsBuilder().setIsTransferAccessResponseBit(true).build()
        .getIsTransferAccessResponse());
    assertFalse(new ControlFlags.ControlFlagsBuilder().setIsTransferAccessResponseBit(false).build()
        .getIsTransferAccessResponse());
  }

}
