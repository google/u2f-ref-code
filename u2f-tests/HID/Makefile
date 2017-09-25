# Copyright 2014 Google Inc. All rights reserved.
#
# Use of this source code is governed by a BSD-style
# license that can be found in the LICENSE file or at
# https://developers.google.com/open-source/licenses/bsd

all: list HIDTest U2FTest

CC ?= gcc
CXX ?= g++

UNAME := $(shell uname)

ifeq ($(UNAME), Linux)

CPPFLAGS+=-Ihidapi/hidapi -D__OS_LINUX -Icore/include
LDLIBS+=-lrt -ludev
HIDAPI=hid.o
hid.o: hidapi/linux/hid.c
	$(CC) -c $(CPPFLAGS) $(CFLAGS) -o hid.o hidapi/linux/hid.c

endif  # Linux

ifeq ($(UNAME), Darwin)

CPPFLAGS+=-Ihidapi/hidapi -Icore/include -D__OS_MAC
LDLIBS+=-framework IOKit -framework CoreFoundation
HIDAPI=hid.o
hid.o: hidapi/mac/hid.c
	$(CC) -c $(CPPFLAGS) $(CFLAGS) -o hid.o hidapi/mac/hid.c

endif  # Darwin

# Crypto code.
LIBMINCRYPT=dsa_sig.o p256.o p256_ec.o p256_ecdsa.o sha256.o

dsa_sig.o: core/libmincrypt/dsa_sig.c
	$(CC) -c $(CPPFLAGS) $(CFLAGS) -Wall $^

p256.o: core/libmincrypt/p256.c
	$(CC) -c $(CPPFLAGS) $(CFLAGS) -Wall $^

p256_ec.o: core/libmincrypt/p256_ec.c
	$(CC) -c $(CPPFLAGS) $(CFLAGS) -Wall $^

p256_ecdsa.o: core/libmincrypt/p256_ecdsa.c
	$(CC) -c $(CPPFLAGS) $(CFLAGS) -Wall $^

sha256.o: core/libmincrypt/sha256.c
	$(CC) -c $(CPPFLAGS) $(CFLAGS) -Wall $^

# utility tools.
u2f_util.o: u2f_util.cc u2f_util.h u2f.h u2f_hid.h
	$(CXX) -c $(CPPFLAGS) $(CFLAGS) -Wall -o u2f_util.o u2f_util.cc

# simple hidapi tool to list devices to see paths.
list: list.c $(HIDAPI)
	$(CC) $(CPPFLAGS) $(CFLAGS) $(LDFLAGS) -Wall -o $@ $^ $(LDLIBS)

# Low-level HID framing test.
HIDTest: HIDTest.cc u2f_util.o $(HIDAPI)
	$(CXX) $(CPPFLAGS) $(CXXFLAGS) $(LDFLAGS) -Wall -o $@ $^ $(LDLIBS)

# U2F messaging crypto test.
U2FTest: U2FTest.cc u2f_util.o $(HIDAPI) $(LIBMINCRYPT)
	$(CXX) $(CPPFLAGS) $(CXXFLAGS) $(LDFLAGS) -Wall -o $@ $^ $(LDLIBS)
