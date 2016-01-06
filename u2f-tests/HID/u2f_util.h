// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

#ifndef __U2F_UTIL_H_INCLUDED__
#define __U2F_UTIL_H_INCLUDED__

#include <stdint.h>
#include <stdio.h>
#include <stdarg.h>
#include <time.h>

#include <string>
#include <iostream>

#include "u2f.h"
#include "u2f_hid.h"

#include "hidapi.h"

#ifdef _MSC_VER
#include <windows.h>
#define usleep(x) Sleep((x + 999) / 1000)
#else
#include <unistd.h>
#define max(a,b) \
           ({ __typeof__ (a) _a = (a); \
                       __typeof__ (b) _b = (b); \
                       _a > _b ? _a : _b; })

#define min(a,b) \
           ({ __typeof__ (a) _a = (a); \
                       __typeof__ (b) _b = (b); \
                       _a < _b ? _a : _b; })
#endif

#define CHECK_INFO __FUNCTION__ << "[" << __LINE__ << "]:"

#define CHECK_EQ(a,b) do { if ((a)!=(b)) { std::cerr << "\x1b[31mCHECK_EQ fail at " << CHECK_INFO#a << " != "#b << ":\x1b[0m "; AbortOrNot(); }} while(0)
#define CHECK_NE(a,b) do { if ((a)==(b)) { std::cerr << "\x1b[31mCHECK_NE fail at " << CHECK_INFO#a << " == "#b << ":\x1b[0m "; AbortOrNot(); }} while(0)
#define CHECK_GE(a,b) do { if ((a)<(b)) { std::cerr << "\x1b[31mCHECK_GE fail at " << CHECK_INFO#a << " < "#b << ":\x1b[0m "; AbortOrNot(); }} while(0)
#define CHECK_GT(a,b) do { if ((a)<=(b)) { std::cerr << "\x1b[31mCHECK_GT fail at " << CHECK_INFO#a << " < "#b << ":\x1b[0m "; AbortOrNot(); }} while(0)
#define CHECK_LT(a,b) do { if ((a)>=(b)) { std::cerr << "\x1b[31mCHECK_LT fail at " << CHECK_INFO#a << " >= "#b << ":\x1b[0m "; AbortOrNot(); }} while(0)
#define CHECK_LE(a,b) do { if ((a)>(b)) { std::cerr << "\x1b[31mCHECK_LE fail at " << CHECK_INFO#a << " > "#b << ":\x1b[0m "; AbortOrNot(); }} while(0)

#define PASS(x) do { (x); std::cout << "\x1b[32mPASS("#x")\x1b[0m" << std::endl; } while(0)

class U2F_info {
 public:
  U2F_info(const char* func, int line) {
    std::cout << func << "[" << line << "]";
  }
  ~U2F_info() {
    std::cout << std::endl;
  }
  std::ostream& operator<<(const char* s) {
    std::cout << s;
    return std::cout;
  }
};

extern int arg_Verbose;
#define INFO if (arg_Verbose) U2F_info(__FUNCTION__, __LINE__) << ": "

std::string b2a(const void* ptr, size_t size);
std::string b2a(const std::string& s);
std::string a2b(const std::string& s);

float U2Fob_deltaTime(uint64_t* state);

struct U2Fob {
  hid_device* dev;
  char* path;
  uint32_t cid;
  int loglevel;
  uint8_t nonce[INIT_NONCE_SIZE];
  uint64_t logtime;
  FILE* logfp;
  char logbuf[BUFSIZ];
};

struct U2Fob* U2Fob_create();

void U2Fob_destroy(struct U2Fob* device);

void U2Fob_setLog(struct U2Fob* device, FILE* fd, int logMask);

int U2Fob_open(struct U2Fob* device, const char* pathname);

void U2Fob_close(struct U2Fob* device);

int U2Fob_reopen(struct U2Fob* device);

int U2Fob_init(struct U2Fob* device);

uint32_t U2Fob_getCid(struct U2Fob* device);

int U2Fob_sendHidFrame(struct U2Fob* device, U2FHID_FRAME* out);

int U2Fob_receiveHidFrame(struct U2Fob* device, U2FHID_FRAME* in,
                          float timeoutSeconds);

int U2Fob_send(struct U2Fob* device, uint8_t cmd,
               const void* data, size_t size);

int U2Fob_recv(struct U2Fob* device, uint8_t* cmd,
               void* data, size_t size,
               float timeoutSeconds);

// returns
//   negative error
//   positive sw12, e.g. 0x9000, 0x6985 etc.
int U2Fob_exchange(struct U2Fob* device,
                   void* data,
                   size_t size,
                   std::string* in);

// returns
//   negative error
//   positive sw12, e.g. 0x9000, 0x6985 etc.
int U2Fob_apdu(struct U2Fob* device,
               uint8_t CLA, uint8_t INS, uint8_t P1, uint8_t P2,
               const std::string& out,
               std::string* in);

bool getCertificate(const U2F_REGISTER_RESP& rsp,
                    std::string* cert);

bool getSignature(const U2F_REGISTER_RESP& rsp,
                  std::string* sig);

bool getSubjectPublicKey(const std::string& cert,
                         std::string* pk);

bool getCertSignature(const std::string& cert,
                      std::string* sig);

bool verifyCertificate(const std::string& pk,
                       const std::string& cert);

#endif  // __U2F_UTIL_H_INCLUDED__
