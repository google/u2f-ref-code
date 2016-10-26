package com.google.u2f.key.messages;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.u2f.TestVectors;
import com.google.u2f.U2FException;
import com.google.u2f.server.ControlFlags;

public class TransferAccessResponseTest extends TestVectors {
  private static final byte CONTROL_FLAGS = 0x03;
  private static final int COUNTER = 0;
  private static final TransferAccessMessage[] TRANSFER_ACCESS_MESSAGES =
      new TransferAccessMessage[1];
  private static final TransferAccessMessage[] TRANSFER_ACCESS_MESSAGES_OTHER =
      new TransferAccessMessage[3];
  private static final TransferAccessResponse TRANSFER_ACCESS_RESPONSE =
      new TransferAccessResponse(CONTROL_FLAGS, 
                                 TRANSFER_ACCESS_MESSAGES, 
                                 KEY_HANDLE_B, COUNTER,
                                 TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B);

  /**
   * This should be the only assignment for each of these values. They should be treated as 
   * constants.
   * @throws U2FException
   */
  @BeforeClass
  public static void setup() throws U2FException {
    TRANSFER_ACCESS_MESSAGES[0] = TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_A_TO_B);
    TRANSFER_ACCESS_MESSAGES_OTHER[0] =
        TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_A_TO_B);
    TRANSFER_ACCESS_MESSAGES_OTHER[1] =
        TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_B_TO_C);
    TRANSFER_ACCESS_MESSAGES_OTHER[2] =
        TransferAccessMessage.fromBytes(TRANSFER_ACCESS_MESSAGE_C_TO_D);
  }

  @Test
  public final void testEquals() {
    TransferAccessResponse transferAccessResponse1 = 
        new TransferAccessResponse(CONTROL_FLAGS,
                                   TRANSFER_ACCESS_MESSAGES, 
                                   KEY_HANDLE_B, COUNTER, 
                                   TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B);
    assertEquals(transferAccessResponse1, TRANSFER_ACCESS_RESPONSE);
  }

  @Test
  public final void testNotEquals_ControlFlagsDiffer() {
    byte controlFlags_Other = 0x02;
    TransferAccessResponse transferAccessResponse1 = 
        new TransferAccessResponse(controlFlags_Other,
                                   TRANSFER_ACCESS_MESSAGES, 
                                   KEY_HANDLE_B, COUNTER, 
                                   TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B);
    assertNotEquals(transferAccessResponse1, TRANSFER_ACCESS_RESPONSE);
  }

  @Test
  public final void testNotEquals_TransferAccessMessagesDiffer() {
    TransferAccessResponse transferAccessResponse1 =
        new TransferAccessResponse(CONTROL_FLAGS, 
                                   TRANSFER_ACCESS_MESSAGES_OTHER, 
                                   KEY_HANDLE_B,
                                   COUNTER, 
                                   TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B);
    assertNotEquals(transferAccessResponse1, TRANSFER_ACCESS_RESPONSE);
  }

  @Test
  public final void testNotEquals_KeyHandleDiffers() {
    byte[] newKeyHandle_Other = KEY_HANDLE_D;
    TransferAccessResponse transferAccessResponse1 =
        new TransferAccessResponse(CONTROL_FLAGS, 
                                   TRANSFER_ACCESS_MESSAGES, 
                                   newKeyHandle_Other,
                                   COUNTER, 
                                   TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B);
    assertNotEquals(transferAccessResponse1, TRANSFER_ACCESS_RESPONSE);
  }

  @Test
  public final void testNotEquals_CounterDiffers() {
    int counter_Other = 1;
    TransferAccessResponse transferAccessResponse1 =
        new TransferAccessResponse(CONTROL_FLAGS, 
                                   TRANSFER_ACCESS_MESSAGES, 
                                   KEY_HANDLE_B,
                                   counter_Other,
                                   TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B);
    assertNotEquals(transferAccessResponse1, TRANSFER_ACCESS_RESPONSE);
  }

  @Test
  public final void testNotEquals_SignatureDiffers() {
    byte[] signature_Other = TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B_TO_C_TO_D;
    TransferAccessResponse transferAccessResponse1 = 
        new TransferAccessResponse(CONTROL_FLAGS,
                                   TRANSFER_ACCESS_MESSAGES, 
                                   KEY_HANDLE_B, 
                                   COUNTER, 
                                   signature_Other);
    assertNotEquals(transferAccessResponse1, TRANSFER_ACCESS_RESPONSE);
  }

  @Test
  public final void testGetters_singleTransferAccessMessage() throws U2FException{     
    TransferAccessResponse transferAccessResponse = 
        new TransferAccessResponse(CONTROL_FLAGS,
                                   TRANSFER_ACCESS_MESSAGES, 
                                   KEY_HANDLE_B, 
                                   COUNTER, 
                                   TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B);
    
    assertEquals(CONTROL_FLAGS, ControlFlags.toByte(transferAccessResponse.getControlFlags()));
    assertArrayEquals(TRANSFER_ACCESS_MESSAGES, transferAccessResponse.getTransferAccessMessages());
    assertArrayEquals(KEY_HANDLE_B, transferAccessResponse.getKeyHandle());
    assertEquals(COUNTER, transferAccessResponse.getCounter());
    assertArrayEquals(TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B,
        transferAccessResponse.getSignature());
  }

  @Test
  public final void testGetters_transferAccessMessageChain() throws U2FException{        
    TransferAccessResponse transferAccessResponse =
        new TransferAccessResponse(CONTROL_FLAGS, 
                                   TRANSFER_ACCESS_MESSAGES_OTHER,
                                   KEY_HANDLE_B, 
                                   COUNTER, 
                                   TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B_TO_C_TO_D);
    
    assertEquals(CONTROL_FLAGS, ControlFlags.toByte(transferAccessResponse.getControlFlags()));
    assertArrayEquals(TRANSFER_ACCESS_MESSAGES_OTHER,
        transferAccessResponse.getTransferAccessMessages());
    assertArrayEquals(KEY_HANDLE_B, transferAccessResponse.getKeyHandle());
    assertEquals(COUNTER, transferAccessResponse.getCounter());
    assertArrayEquals(TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B_TO_C_TO_D,
        transferAccessResponse.getSignature());
  }
  
}
