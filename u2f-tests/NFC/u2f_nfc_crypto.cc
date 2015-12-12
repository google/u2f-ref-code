// Based on code from Google & Yubico.

#include <stdio.h>
#include <stdarg.h>

#include <string>
#include <iostream>

#include "u2f.h"
#include "u2f_nfc_crypto.h"
#include "u2f_nfc_util.h"

#include "dsa_sig.h"
#include "p256.h"
#include "p256_ecdsa.h"
#include "sha256.h"

extern "C" void AbortOrNot();
extern "C" flag log_Crypto;

std::string b2a(const void* ptr, size_t size) {
  const uint8_t* p = reinterpret_cast<const uint8_t*>(ptr);
  std::string result;

  for (size_t i = 0; i < 2 * size; ++i) {
    int nib = p[i / 2];
    if ((i & 1) == 0) nib >>= 4;
    nib &= 15;
    result.push_back("0123456789ABCDEF"[nib]);
  }

  return result;
}
std::string b2a(const std::string& s) {
  return b2a(s.data(), s.size());
}

std::string a2b(const std::string& s) {
  std::string result;
  int v;
  for (size_t i = 0; i < s.size(); ++i) {
    if ((i & 1) == 1) {
      v <<= 4;
    } else {
      v = 0;
    }

    char d = s[i];
    if (d >= '0' && d <= '9') {
      v += (d - '0');
    } else if (d >= 'A' && d <= 'F') {
      v += (d - 'A' + 10);
    } else if (d >= 'a' && d <= 'f') {
      v += (d - 'a' + 10);
    }

    if ((i & 1) == 1) {
      result.push_back(v & 255);
    }
  }
  return result;
}

bool getCertificate(const U2F_REGISTER_RESP& rsp,
                    std::string* cert) {
  size_t hkLen = rsp.keyHandleLen;

  CHECK_GE(hkLen, MIN_KH_SIZE);
  CHECK_LE(hkLen, MAX_KH_SIZE);  // Superflous at the moment, but just in case MAX_KH_SIZE changes
  CHECK_LT(hkLen, sizeof(rsp.keyHandleCertSig));

  size_t certOff = hkLen;
  size_t certLen = sizeof(rsp.keyHandleCertSig) - certOff;
  const uint8_t* p = &rsp.keyHandleCertSig[certOff];

  CHECK_GE(certLen, 4);
  CHECK_EQ(p[0], 0x30);

  CHECK_GE(p[1], 0x81);
  CHECK_LE(p[1], 0x82);

  size_t seqLen;
  size_t headerLen;
  if (p[1] == 0x81) {
    seqLen = p[2];
    headerLen = 3;
  } else if (p[1] == 0x82) {
    seqLen = p[2] * 256 + p[3];
    headerLen = 4;
  } else {
    // FAIL
    AbortOrNot();
  }

  CHECK_LE(seqLen, certLen - headerLen);

  cert->assign(reinterpret_cast<const char*>(p), seqLen + headerLen);
  return true;
}

bool getSignature(const U2F_REGISTER_RESP& rsp,
                  std::string* sig) {
  std::string cert;
  CHECK_NE(false, getCertificate(rsp, &cert));

  size_t sigOff = rsp.keyHandleLen + cert.size();
  CHECK_LE(sigOff, sizeof(rsp.keyHandleCertSig));

  size_t sigLen = sizeof(rsp.keyHandleCertSig) - sigOff;
  const uint8_t* p = &rsp.keyHandleCertSig[sigOff];

  CHECK_GE(sigLen, 2);
  CHECK_EQ(p[0], 0x30);

  size_t seqLen = p[1];
  CHECK_LE(seqLen, sigLen - 2);

  sig->assign(reinterpret_cast<const char*>(p), seqLen + 2);
  return true;
}

bool getSubjectPublicKey(const std::string& cert,
                         std::string* pk) {
  CHECK_GE(cert.size(), U2F_EC_POINT_SIZE);

  // Explicitly search for asn1 lead-in sequence of p256-ecdsa public key.
  const char asn1[] = "3059301306072A8648CE3D020106082A8648CE3D030107034200";
  std::string pkStart(a2b(asn1));

  size_t off = cert.find(pkStart);
  CHECK_NE(off, std::string::npos);

  off += pkStart.size();
  CHECK_LE(off, cert.size() - U2F_EC_POINT_SIZE);

  pk->assign(cert, off, U2F_EC_POINT_SIZE);
  return true;
}

bool getCertSignature(const std::string& cert,
                      std::string* sig) {
  // Explicitly search asn1 lead-in sequence of p256-ecdsa signature.
  const char asn1[] = "300A06082A8648CE3D04030203";
  std::string sigStart(a2b(asn1));

  size_t off = cert.find(sigStart);
  CHECK_NE(off, std::string::npos);

  off += sigStart.size();
  CHECK_LE(off, cert.size() - 8);

  size_t bitStringLen = cert[off] & 255;
  CHECK_EQ(bitStringLen, cert.size() - off - 1);
  CHECK_EQ(cert[off + 1], 0);

  sig->assign(cert, off + 2, cert.size() - off - 2);
  return true;
}

void enrollCheckSignature(U2F_REGISTER_REQ regReq, U2F_REGISTER_RESP regRsp) {
  CHECK_EQ(regRsp.registerId, U2F_REGISTER_ID);
  CHECK_EQ(regRsp.pubKey.pointFormat, U2F_POINT_UNCOMPRESSED);

  std::string cert;
  CHECK_EQ(getCertificate(regRsp, &cert), true);

  std::string pk;
  CHECK_EQ(getSubjectPublicKey(cert, &pk), true);

  std::string sig;
  CHECK_EQ(getSignature(regRsp, &sig), true);

  // Log values if required
  if (log_Crypto == flagON) {
    std::cout << "Attestation Cert:\n" << b2a(cert) << "\n";
    std::cout << "Attestation Public Key:\n" << b2a(pk)<< "\n";
    std::cout << "Attestation Signature :\n" << b2a(sig)<< "\n";
  }

  // Parse signature into two integers.
  p256_int sig_r, sig_s;
  CHECK_EQ(1, dsa_sig_unpack((uint8_t*)(sig.data()), sig.size(),
                             &sig_r, &sig_s));

  // Compute hash as integer.
  p256_int h;
  SHA256_CTX sha;
  SHA256_init(&sha);
  uint8_t rfu = 0;  // TEST
  SHA256_update(&sha, &rfu, sizeof(rfu));  // 0x00
  SHA256_update(&sha, regReq.appId, sizeof(regReq.appId));  // O
  SHA256_update(&sha, regReq.nonce, sizeof(regReq.nonce));  // d
  SHA256_update(&sha, regRsp.keyHandleCertSig, regRsp.keyHandleLen);  // hk
  SHA256_update(&sha, &regRsp.pubKey, sizeof(regRsp.pubKey));  // pk
  p256_from_bin(SHA256_final(&sha), &h);

  // Parse subject public key into two integers.
  CHECK_EQ(pk.size(), U2F_EC_POINT_SIZE);
  p256_int pk_x, pk_y;
  p256_from_bin((uint8_t*)(pk.data()) + 1, &pk_x);
  p256_from_bin((uint8_t*)(pk.data()) + 1 + U2F_EC_KEY_SIZE,
                &pk_y);

  // Verify signature.
  CHECK_EQ(1, p256_ecdsa_verify(&pk_x, &pk_y, &h, &sig_r, &sig_s));
}

void signCheckSignature(U2F_REGISTER_REQ regReq,
                        U2F_REGISTER_RESP regRsp,
                        U2F_AUTHENTICATE_REQ authReq,
                        U2F_AUTHENTICATE_RESP authResp,
                        int respLength) {
  CHECK_EQ(authResp.flags, 0x01);

  if (log_Crypto == flagON) {
    std::cout << "Authentication Signature:\n" << b2a(authResp.sig, respLength -
              sizeof(authResp.flags) - sizeof(authResp.ctr)) << "\n";
  }
  // Parse signature from authenticate response.
  p256_int sig_r, sig_s;
  CHECK_EQ(1, dsa_sig_unpack(authResp.sig, respLength - sizeof(authResp.flags) -
                             sizeof(authResp.ctr), &sig_r, &sig_s));

  // Compute hash as integer.
  p256_int h;
  SHA256_CTX sha;
  SHA256_init(&sha);
  SHA256_update(&sha, regReq.appId, sizeof(regReq.appId));  // O
  SHA256_update(&sha, &authResp.flags, sizeof(authResp.flags));  // T
  SHA256_update(&sha, &authResp.ctr, sizeof(authResp.ctr));  // CTR
  SHA256_update(&sha, authReq.nonce, sizeof(authReq.nonce));  // d
  p256_from_bin(SHA256_final(&sha), &h);

  // Parse public key from registration response.
  p256_int pk_x, pk_y;
  p256_from_bin(regRsp.pubKey.x, &pk_x);
  p256_from_bin(regRsp.pubKey.y, &pk_y);

  // Verify signature.
  CHECK_EQ(1, p256_ecdsa_verify(&pk_x, &pk_y, &h, &sig_r, &sig_s));
}
