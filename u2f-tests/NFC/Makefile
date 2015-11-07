# Copyright 2014 Google Inc. All rights reserved.
#
# Use of this source code is governed by a BSD-style
# license that can be found in the LICENSE file or at
# https://developers.google.com/open-source/licenses/bsd

all: u2f_nfc_test

UNAME := $(shell uname)

ifeq ($(UNAME), Linux)
CFLAGS= -D__OS_LINUX -I../HID/core/include -I../HID/core/include/mincrypt -I/usr/include/PCSC
LDFLAGS=-lpcsclite
endif  # Linux

MINCRYPT_PATH=../HID/core/libmincrypt

# Crypto code.
LIBMINCRYPT=dsa_sig.o p256.o p256_ec.o p256_ecdsa.o sha256.o

dsa_sig.o: $(MINCRYPT_PATH)/dsa_sig.c
	gcc -c $(CFLAGS) -Wall $^

p256.o: $(MINCRYPT_PATH)/p256.c
	gcc -c $(CFLAGS) -Wall $^

p256_ec.o: $(MINCRYPT_PATH)/p256_ec.c
	gcc -c $(CFLAGS) -Wall $^

p256_ecdsa.o: $(MINCRYPT_PATH)/p256_ecdsa.c
	gcc -c $(CFLAGS) -Wall $^

sha256.o: $(MINCRYPT_PATH)/sha256.c
	gcc -c $(CFLAGS) -Wall $^

# utility tools.
u2f_nfc_util.o: u2f_nfc_util.c u2f_nfc_util.h u2f.h u2f_nfc_crypto.h
	gcc -c $(CFLAGS) -Wall -o u2f_nfc_util.o u2f_nfc_util.c

# crypto lib
u2f_nfc_crypto.o: u2f_nfc_crypto.cc u2f_nfc_util.h u2f.h u2f_nfc_crypto.h
	g++ -c $(CFLAGS) -Wall -o u2f_nfc_crypto.o u2f_nfc_crypto.cc

# U2F messaging crypto test.
u2f_nfc_test: u2f_nfc_test.cc u2f_nfc_util.o u2f_nfc_crypto.o $(LIBMINCRYPT)
	g++ $(CFLAGS) -Wall -o $@ $^ $(LDFLAGS)
