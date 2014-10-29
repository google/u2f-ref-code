# Copyright 2014 Google Inc. All rights reserved.
#
# Use of this source code is governed by a BSD-style
# license that can be found in the LICENSE file or at
# https://developers.google.com/open-source/licenses/bsd

all: list HIDTest U2FTest

UNAME := $(shell uname)

ifeq ($(UNAME), Linux)

CFLAGS=-Ihidapi/hidapi -D__OS_LINUX -Icore/include
LDFLAGS=-lrt -ludev
HIDAPI=hid.o
hid.o: hidapi/linux/hid.c
	gcc -c $(CFLAGS) -o hid.o hidapi/linux/hid.c

endif  # Linux

ifeq ($(UNAME), Darwin)

CFLAGS=-Ihidapi/hidapi -Icore/include -D__OS_MAC
LDFLAGS=-framework IOKit -framework CoreFoundation
HIDAPI=hid.o
hid.o: hidapi/mac/hid.c
	gcc -c $(CFLAGS) -o hid.o hidapi/mac/hid.c

endif  # Darwin

# Crypto code.
LIBMINCRYPT=dsa_sig.o p256.o p256_ec.o p256_ecdsa.o sha256.o

dsa_sig.o: core/libmincrypt/dsa_sig.c
	gcc -c $(CFLAGS) -Wall $^

p256.o: core/libmincrypt/p256.c
	gcc -c $(CFLAGS) -Wall $^

p256_ec.o: core/libmincrypt/p256_ec.c
	gcc -c $(CFLAGS) -Wall $^

p256_ecdsa.o: core/libmincrypt/p256_ecdsa.c
	gcc -c $(CFLAGS) -Wall $^

sha256.o: core/libmincrypt/sha256.c
	gcc -c $(CFLAGS) -Wall $^

# utility tools.
u2f_util.o: u2f_util.cc u2f_util.h u2f.h u2f_hid.h
	g++ -c $(CFLAGS) -Wall -o u2f_util.o u2f_util.cc

# simple hidapi tool to list devices to see paths.
list: list.c $(HIDAPI)
	gcc $(CFLAGS) -Wall -o $@ $^ $(LDFLAGS)

# Low-level HID framing test.
HIDTest: HIDTest.cc u2f_util.o $(HIDAPI)
	g++ $(CFLAGS) -Wall -o $@ $^ $(LDFLAGS)

# U2F messaging crypto test.
U2FTest: U2FTest.cc u2f_util.o $(HIDAPI) $(LIBMINCRYPT)
	g++ $(CFLAGS) -Wall -o $@ $^ $(LDFLAGS)
