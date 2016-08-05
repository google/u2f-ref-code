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
    assertTrue(ControlFlags.fromByte(TRANSFER_ACCESS_MESSAGE_BIT_SET).getIsTransferAccessMessage());
    assertTrue(ControlFlags.fromByte(USER_PRESENCE_AND_TRANSFER_ACCESS_BITS_SET)
        .getIsTransferAccessMessage());
    assertFalse(ControlFlags.fromByte(USER_PRESENCE_BIT_SET).getIsTransferAccessMessage());
    assertFalse(ControlFlags.fromByte(NO_BITS_SET).getIsTransferAccessMessage());
  }

  @Test
  public void testBuilder() {
    assertTrue(new ControlFlags.ControlFlagsBuilder().setUserPresenceBit(true).build().getUserPresence());
    assertFalse(
        new ControlFlags.ControlFlagsBuilder().setUserPresenceBit(false).build().getUserPresence());
    assertFalse(new ControlFlags.ControlFlagsBuilder().build().getUserPresence());
    assertFalse(new ControlFlags.ControlFlagsBuilder().setIsTransferAccessMessageBit(true).build()
        .getUserPresence());
    assertTrue(new ControlFlags.ControlFlagsBuilder().setIsTransferAccessMessageBit(true)
        .setUserPresenceBit(true).build().getUserPresence());
    assertFalse(new ControlFlags.ControlFlagsBuilder().setUserPresenceBit(true).build()
        .getIsTransferAccessMessage());
    assertTrue(new ControlFlags.ControlFlagsBuilder().setIsTransferAccessMessageBit(true).build()
        .getIsTransferAccessMessage());
    assertFalse(new ControlFlags.ControlFlagsBuilder().setIsTransferAccessMessageBit(false).build()
        .getIsTransferAccessMessage());
  }

}
