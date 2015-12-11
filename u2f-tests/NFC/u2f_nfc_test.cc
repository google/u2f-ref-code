// Based on code from Google & Yubico.

// U2F NFC register / sign compliance test.
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <iostream>

#include "u2f.h"
#include "u2f_nfc_crypto.h"
#include "u2f_nfc_util.h"

// u2f_nfc_crypto functions
extern void enrollCheckSignature(U2F_REGISTER_REQ regReq,
                                 U2F_REGISTER_RESP regRsp);
extern void signCheckSignature(U2F_REGISTER_REQ regReq,
                               U2F_REGISTER_RESP regRsp,
                               U2F_AUTHENTICATE_REQ authReq,
                               U2F_AUTHENTICATE_RESP authResp,
                               int respLength);

// u2f_nfc_util functions
extern "C" int U2FNFC_connect(void);
extern "C" uint xchgAPDUShort(uint cla, uint ins, uint p1, uint p2, uint lc,
                              const void *data, uint *rapduLen, void *rapdu);
extern "C" uint xchgAPDUExtended(uint cla, uint ins, uint p1, uint p2, uint lc,
                                 const void *data, uint *rapduLen, void *rapdu);
extern "C" void AbortOrNot(void);
extern "C" void checkPause(const char* prompt);
extern "C" void setChainingLc(uint16_t size);

// Global Variables from u2f_nfc_utils
extern "C" flag log_Apdu;
extern "C" flag log_Crypto;
extern "C" flag arg_Pause;
extern "C" flag arg_Abort;
extern "C" cmd_apdu_type cmd_apdu;


U2F_REGISTER_REQ  regReq;
U2F_REGISTER_RESP regRsp;
U2F_AUTHENTICATE_REQ authReq;
U2F_AUTHENTICATE_RESP authRsp;

void test_Enroll(cmd_apdu_type cmd_apdu_in, uint expectedSW12 = 0x9000) {
  uint rspLen = sizeof(U2F_REGISTER_RESP);
  uint8_t rsp[APDU_BUFFER_SIZE];

  // pick random origin and challenge.
  for (size_t i = 0; i < sizeof(regReq.nonce); ++i) {
    regReq.nonce[i] = rand();
  }
  for (size_t i = 0; i < sizeof(regReq.appId); ++i) {
    regReq.appId[i] = rand();
  }

  // APDU Exchange - Select Short or Extended
  if (cmd_apdu_in == SHORT_APDU) {
    CHECK_EQ(expectedSW12, xchgAPDUShort(0, U2F_INS_REGISTER, U2F_AUTH_ENFORCE,
                                         0, sizeof(regReq),
                                         reinterpret_cast<uint8_t*>(&regReq),
                                         &rspLen, rsp));
  }
  if (cmd_apdu_in == EXTENDED_APDU) {
    CHECK_EQ(expectedSW12, xchgAPDUExtended(0, U2F_INS_REGISTER,
                                            U2F_AUTH_ENFORCE, 0, sizeof(regReq),
                                            reinterpret_cast<uint8_t*>(&regReq),
                                            &rspLen, rsp));
  }

  if (expectedSW12 != 0x9000) {
    CHECK_EQ(0, rspLen);
    return;
  }

  CHECK_NE(0, rspLen);
  CHECK_LE(rspLen, sizeof(U2F_REGISTER_RESP));

  memcpy(&regRsp, rsp, rspLen);

  CHECK_EQ(regRsp.registerId, U2F_REGISTER_ID);
  CHECK_EQ(regRsp.pubKey.pointFormat, U2F_POINT_UNCOMPRESSED);
}

uint test_Sign(cmd_apdu_type cmd_apdu_in,
               uint expectedSW12 = 0x9000,
               bool checkOnly = false) {
  uint rspLen = sizeof(U2F_AUTHENTICATE_RESP);
  uint8_t rsp[APDU_BUFFER_SIZE];

  // pick random challenge and use registered appId.
  for (size_t i = 0; i < sizeof(authReq.nonce); ++i) {
    authReq.nonce[i] = rand();
  }
  memcpy(authReq.appId, regReq.appId, sizeof(authReq.appId));
  authReq.keyHandleLen = regRsp.keyHandleLen;
  memcpy(authReq.keyHandle, regRsp.keyHandleCertSig, authReq.keyHandleLen);

  uint reqSize = sizeof(authReq.nonce) + sizeof(authReq.appId) + 1 +
      regRsp.keyHandleLen;

  // APDU Exchange - Select Short or Extended
  if (cmd_apdu_in == SHORT_APDU) {
    CHECK_EQ(expectedSW12,
             xchgAPDUShort(0, U2F_INS_AUTHENTICATE,
                           checkOnly ? U2F_AUTH_CHECK_ONLY : U2F_AUTH_ENFORCE,
                           0, reqSize, reinterpret_cast<uint8_t*>(&authReq),
                           &rspLen, rsp)); }

  if (cmd_apdu_in == EXTENDED_APDU) {
    CHECK_EQ(expectedSW12,
             xchgAPDUExtended(0, U2F_INS_AUTHENTICATE,
                              checkOnly ? U2F_AUTH_CHECK_ONLY : U2F_AUTH_ENFORCE,
                              0, reqSize, reinterpret_cast<uint8_t*>(&authReq),
                              &rspLen, rsp));
  }

  if (expectedSW12 != 0x9000) {
    CHECK_EQ(0, rspLen);
    return rspLen;
  }

  CHECK_NE(0, rspLen);
  CHECK_LE(rspLen, sizeof(U2F_AUTHENTICATE_RESP));

  memcpy(&authRsp, rsp, rspLen);
  return rspLen;
}

void check_Compilation() {
  // Couple of sanity checks.
  CHECK_EQ(sizeof(U2F_EC_POINT), 65);
  CHECK_EQ(sizeof(U2F_REGISTER_REQ), 64);
}

int main(int argc, char* argv[]) {
  // Allocate buffers for response APDU
  uint rapduLen = 0;
  uint8_t rapdu[APDU_BUFFER_SIZE];
  uint32_t ctr;

  while (--argc > 0) {
    if (!strncmp(argv[argc], "-v", 2)) {
      // Log APDUs
      log_Apdu = flagON;
    }
    if (!strncmp(argv[argc], "-V", 2)) {
      // Log APDUs and Crypro
      log_Apdu = flagON;
      log_Crypto = flagON;
    }
    if (!strncmp(argv[argc], "-a", 2)) {
      // Don't abort, try to continue;
      arg_Abort = flagOFF;
    }
    if (!strncmp(argv[argc], "-p", 2)) {
      // Pause at abort
      arg_Pause = flagON;
    }
  }

  srand((unsigned int) time(NULL));
  PASS(check_Compilation());

  // Connect to the card reader
  CHECK_EQ(0, U2FNFC_connect());

  //---------------------------------------------------------------------------
  //                                 Tests
  //---------------------------------------------------------------------------
  std::cout << "\nApplet Select - Check Version Response";
  uint8_t u2fAID[U2F_APPLET_AID_LEN] = U2F_APPLET_AID;
  uint8_t u2fVer[U2F_VERSION_LEN] = U2F_VERSION;
  rapduLen = U2F_VERSION_LEN;
  CHECK_EQ(SW_NO_ERROR, (xchgAPDUShort(0, 0xa4, 0x04, 0x00, sizeof(u2fAID), u2fAID,  &rapduLen, rapdu)));
  CHECK_EQ(0, memcmp(u2fVer, rapdu, U2F_VERSION_LEN));

  std::cout << "\nCheck Unknown INS Response";
  CHECK_EQ(0x6D00, xchgAPDUShort(0, 0 /* not U2F INS */, 0, 0, 0, "", &rapduLen, rapdu));
  CHECK_EQ(0, rapduLen);
  CHECK_EQ(0x6D00, xchgAPDUExtended(0, 0 /* not U2F INS */, 0, 0, 0, "", &rapduLen, rapdu));
  CHECK_EQ(0, rapduLen);

  std::cout << "\nCheck Bad CLA Response";
  CHECK_NE(0x9000, xchgAPDUShort(1 /* not U2F CLA, 0x00 */, U2F_INS_AUTHENTICATE, 0, 0, 0, "abc", &rapduLen, rapdu));
  CHECK_EQ(0, rapduLen);

  std::cout << "\nCheck Wrong Length U2F_REGISTER Response";
  CHECK_EQ(0x6700u, xchgAPDUShort(0, U2F_INS_REGISTER, 0, 0, 0, "", &rapduLen, rapdu));
  CHECK_EQ(0, rapduLen);

  setChainingLc(256);
  std::cout << "\nValid U2F_REGISTER, Short APDU";
  PASS(test_Enroll(SHORT_APDU, 0x9000u));
  std::cout << "Check the Signature\n";
  PASS(enrollCheckSignature(regReq, regRsp));

  setChainingLc(100);
  std::cout << "\nValid U2F_REGISTER, Short APDU, Change BlockSize";
  PASS(test_Enroll(SHORT_APDU, 0x9000u));
  std::cout << "Check the Signature\n";
  PASS(enrollCheckSignature(regReq , regRsp));
  setChainingLc(256);

  std::cout << "\nValid U2F_REGISTER, Extended APDU";
  PASS(test_Enroll(EXTENDED_APDU, 0x9000u));
  std::cout << "Check the Signature\n";
  PASS(enrollCheckSignature(regReq, regRsp));

  std::cout << "\nValid U2F_AUTH, Short APDU";
  PASS(rapduLen = test_Sign(SHORT_APDU, 0x9000u));
  std::cout << "Check the Signature & Counter";
  PASS(signCheckSignature(regReq, regRsp, authReq, authRsp, rapduLen));
  ctr = MAKE_UINT32(authRsp.ctr);

  std::cout << "\nValid U2F_AUTH, Extended APDU";
  PASS(rapduLen = test_Sign(EXTENDED_APDU, 0x9000u));
  std::cout << "Check the Signature & Counter";
  PASS(signCheckSignature(regReq, regRsp, authReq, authRsp, rapduLen));
  CHECK_EQ(MAKE_UINT32(authRsp.ctr), ctr+1); ctr = MAKE_UINT32(authRsp.ctr);

  std::cout << "\nTest Auth with wrong keyHandle";
  regRsp.keyHandleCertSig[0] ^= 0x55;
  PASS(test_Sign(SHORT_APDU, 0x6a80));
  regRsp.keyHandleCertSig[0] ^= 0x55;

  std::cout << "\nTest Auth with wrong AppId";
  regReq.appId[0] ^= 0xaa;
  PASS(test_Sign(EXTENDED_APDU, 0x6a80));
  regReq.appId[0] ^= 0xaa;

  std::cout << "\nReTest Valid U2F_AUTH, Short APDU";
  PASS(rapduLen = test_Sign(SHORT_APDU, 0x9000u));
  std::cout << "Check the Signature & Counter";
  PASS(signCheckSignature(regReq, regRsp, authReq, authRsp, rapduLen));
  CHECK_EQ(MAKE_UINT32(authRsp.ctr), ctr+1); ctr = MAKE_UINT32(authRsp.ctr);

  std::cout << "\nReTest U2F_AUTH, Extended APDU";
  PASS(rapduLen = test_Sign(EXTENDED_APDU, 0x9000u));
  std::cout << "Check the Signature & Counter";
  PASS(signCheckSignature(regReq, regRsp, authReq, authRsp, rapduLen));
  CHECK_EQ(MAKE_UINT32(authRsp.ctr), ctr+1); ctr = MAKE_UINT32(authRsp.ctr);

  std::cout << "\nValid U2F_REGISTER, Extended APDU";
  PASS(test_Enroll(EXTENDED_APDU, 0x9000u));
  std::cout << "Check the Signature\n";
  PASS(enrollCheckSignature(regReq, regRsp));

  std::cout << "\nValid U2F_AUTH, Extended APDU";
  PASS(rapduLen = test_Sign(EXTENDED_APDU, 0x9000u));
  std::cout << "Check the Signature & Counter ";
  PASS(signCheckSignature(regReq, regRsp, authReq, authRsp, rapduLen));
  CHECK_EQ(MAKE_UINT32(authRsp.ctr), ctr+1); ctr = MAKE_UINT32(authRsp.ctr);

  std::cout << "\nValid U2F_REGISTER, Short APDU";
  PASS(test_Enroll(SHORT_APDU, 0x9000u));
  std::cout << "Check the Signature\n";
  PASS(enrollCheckSignature(regReq, regRsp));

  std::cout << "\nValid U2F_AUTH, Short APDU";
  PASS(rapduLen = test_Sign(SHORT_APDU, 0x9000u));
  std::cout << "Check the Signature & Counter";
  PASS(signCheckSignature(regReq, regRsp, authReq, authRsp, rapduLen));
  CHECK_EQ(MAKE_UINT32(authRsp.ctr), ctr+1); ctr = MAKE_UINT32(authRsp.ctr);
  checkPause("----------------------------------\nEnd of Test, Succesfully Completed\n----------------------------------\nHit Key To Exit...");
}
