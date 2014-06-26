// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.codec;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.u2f.U2FException;
import com.google.u2f.key.messages.AuthenticateRequest;
import com.google.u2f.key.messages.AuthenticateResponse;
import com.google.u2f.key.messages.RegisterRequest;
import com.google.u2f.key.messages.RegisterResponse;
import com.google.u2f.key.messages.U2FRequest;

public class SerialCodec {
  public static final byte VERSION = (byte) 0x02;
  public static final byte COMMAND_REGISTER = (byte) 0x01;
  public static final byte COMMAND_AUTHENTICATE = (byte) 0x02;

  public static void sendRegisterRequest(OutputStream outputStream, RegisterRequest registerRequest)
      throws IOException, U2FException {
    sendRequest(outputStream, COMMAND_REGISTER, RawMessageCodec.encodeRegisterRequest(registerRequest));
  }

  public static void sendRegisterResponse(OutputStream outputStream,
      RegisterResponse registerResponse) throws IOException, U2FException {
    sendResponse(outputStream, RawMessageCodec.encodeRegisterResponse(registerResponse));
  }

  public static void sendAuthenticateRequest(OutputStream outputStream,
      AuthenticateRequest authenticateRequest) throws IOException, U2FException {
    sendRequest(outputStream, COMMAND_AUTHENTICATE,
        RawMessageCodec.encodeAuthenticateRequest(authenticateRequest));
  }

  public static void sendAuthenticateResponse(OutputStream outputStream,
      AuthenticateResponse authenticateResponse) throws IOException, U2FException {
    sendResponse(outputStream, RawMessageCodec.encodeAuthenticateResponse(authenticateResponse));
  }

  private static void sendRequest(OutputStream outputStream, byte command,
      byte[] encodedBytes) throws U2FException, IOException {
    if (encodedBytes.length > 65535) {
      throw new U2FException("Message is too long to be transmitted over this protocol");
    }

    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
    dataOutputStream.write(VERSION);
    dataOutputStream.write(command);
    dataOutputStream.writeShort(encodedBytes.length);
    dataOutputStream.write(encodedBytes);
    dataOutputStream.flush();
  }

  private static void sendResponse(OutputStream outputStream, byte[] encodedBytes)
      throws U2FException, IOException {
    if (encodedBytes.length > 65535) {
      throw new U2FException("Message is too long to be transmitted over this protocol");
    }

    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
    dataOutputStream.writeShort(encodedBytes.length);
    dataOutputStream.write(encodedBytes);
    dataOutputStream.flush();
  }

  public static U2FRequest parseRequest(InputStream inputStream)
      throws U2FException, IOException {

    DataInputStream dataInputStream = new DataInputStream(inputStream);

    byte version = dataInputStream.readByte();
    if (version != VERSION) {
      throw new U2FException(String.format("Unsupported message version: %d", version));
    }

    byte command = dataInputStream.readByte();
    switch (command) {
    case COMMAND_REGISTER:
      return RawMessageCodec.decodeRegisterRequest(parseMessage(dataInputStream));
    case COMMAND_AUTHENTICATE:
      return RawMessageCodec.decodeAuthenticateRequest(parseMessage(dataInputStream));
    default:
      throw new U2FException(String.format("Unsupported command: %d", command));
    }
  }

  public static RegisterResponse parseRegisterResponse(InputStream inputStream)
      throws U2FException, IOException {
    DataInputStream dataInputStream = new DataInputStream(inputStream);
    return RawMessageCodec.decodeRegisterResponse(parseMessage(dataInputStream));
  }

  public static AuthenticateResponse parseAuthenticateResponse(InputStream inputStream)
      throws U2FException, IOException {
    DataInputStream dataInputStream = new DataInputStream(inputStream);
    return RawMessageCodec.decodeAuthenticateResponse(parseMessage(dataInputStream));
  }

  private static byte[] parseMessage(DataInputStream dataInputStream) throws IOException {
    byte[] result = new byte[dataInputStream.readUnsignedShort()];
    dataInputStream.readFully(result);
    return result;
  }
}
