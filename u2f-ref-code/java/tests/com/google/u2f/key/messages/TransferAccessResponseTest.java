package com.google.u2f.key.messages;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.u2f.TestVectors;
import com.google.u2f.U2FException;
import com.google.u2f.server.ControlFlags;

public class TransferAccessResponseTest extends TestVectors {
  private static final byte CONTROL_FLAGS = 0x03;
  private static final byte CONTROL_FLAGS_OTHER = 0x02;
  private static final int COUNTER = 0;
  private static final int COUNTER_OTHER = 1;
  private static final byte[] NEW_KEY_HANDLE = KEY_HANDLE_B;
  private static final byte[] NEW_KEY_HANDLE_OTHER = KEY_HANDLE_D;
  private static final byte[] SIGNATURE = TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B;
  private static final byte[] SIGNATURE_OTHER = TRANSFER_ACCESS_RESPONSE_SIGNATURE_A_TO_B_TO_C_TO_D;
  private static final TransferAccessMessage[] TRANSFER_ACCESS_MESSAGES =
      new TransferAccessMessage[1];
  private static final TransferAccessMessage[] TRANSFER_ACCESS_MESSAGES_OTHER =
      new TransferAccessMessage[3];
  
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
    TransferAccessResponse transferAccessResponse1 = new TransferAccessResponse(CONTROL_FLAGS, TRANSFER_ACCESS_MESSAGES, NEW_KEY_HANDLE, COUNTER, SIGNATURE);
    TransferAccessResponse transferAccessResponse2 = new TransferAccessResponse(CONTROL_FLAGS, TRANSFER_ACCESS_MESSAGES, NEW_KEY_HANDLE, COUNTER, SIGNATURE);
    assertEquals(transferAccessResponse1, transferAccessResponse2);
    
    transferAccessResponse1 = new TransferAccessResponse(CONTROL_FLAGS_OTHER, TRANSFER_ACCESS_MESSAGES, NEW_KEY_HANDLE, COUNTER, SIGNATURE);
    assertNotEquals(transferAccessResponse1, transferAccessResponse2);
    
    transferAccessResponse1 = new TransferAccessResponse(CONTROL_FLAGS, TRANSFER_ACCESS_MESSAGES_OTHER, NEW_KEY_HANDLE, COUNTER, SIGNATURE);
    assertNotEquals(transferAccessResponse1, transferAccessResponse2);

    transferAccessResponse1 = new TransferAccessResponse(CONTROL_FLAGS, TRANSFER_ACCESS_MESSAGES, NEW_KEY_HANDLE_OTHER, COUNTER, SIGNATURE);
    assertNotEquals(transferAccessResponse1, transferAccessResponse2);

    transferAccessResponse1 = new TransferAccessResponse(CONTROL_FLAGS, TRANSFER_ACCESS_MESSAGES, NEW_KEY_HANDLE, COUNTER_OTHER, SIGNATURE);
    assertNotEquals(transferAccessResponse1, transferAccessResponse2);

    transferAccessResponse1 = new TransferAccessResponse(CONTROL_FLAGS, TRANSFER_ACCESS_MESSAGES, NEW_KEY_HANDLE, COUNTER, SIGNATURE_OTHER);
    assertNotEquals(transferAccessResponse1, transferAccessResponse2);

  }

  @Test
  public final void testGetters_singleTransferAccessMessage() throws U2FException{     
    TransferAccessResponse transferAccessResponse = new TransferAccessResponse(CONTROL_FLAGS,
        TRANSFER_ACCESS_MESSAGES, NEW_KEY_HANDLE, COUNTER, SIGNATURE);
    
    assertEquals(CONTROL_FLAGS, ControlFlags.toByte(transferAccessResponse.getControlFlags()));
    assertArrayEquals(TRANSFER_ACCESS_MESSAGES, transferAccessResponse.getTransferAccessMessages());
    assertArrayEquals(NEW_KEY_HANDLE, transferAccessResponse.getKeyHandle());
    assertEquals(COUNTER, transferAccessResponse.getCounter());
    assertArrayEquals(SIGNATURE, transferAccessResponse.getSignature());
  }

  @Test
  public final void testGetters_transferAccessMessageChain() throws U2FException{        
    TransferAccessResponse transferAccessResponse =
        new TransferAccessResponse(CONTROL_FLAGS_OTHER, TRANSFER_ACCESS_MESSAGES_OTHER,
            NEW_KEY_HANDLE_OTHER, COUNTER_OTHER, SIGNATURE_OTHER);
    
    assertEquals(CONTROL_FLAGS_OTHER,
        ControlFlags.toByte(transferAccessResponse.getControlFlags()));
    assertArrayEquals(TRANSFER_ACCESS_MESSAGES_OTHER,
        transferAccessResponse.getTransferAccessMessages());
    assertArrayEquals(NEW_KEY_HANDLE_OTHER, transferAccessResponse.getKeyHandle());
    assertEquals(COUNTER_OTHER, transferAccessResponse.getCounter());
    assertArrayEquals(SIGNATURE_OTHER, transferAccessResponse.getSignature());
  }
  
  
  
}
