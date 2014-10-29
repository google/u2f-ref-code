// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

#include <stdio.h>
#include <stdlib.h>

#include "hidapi.h"

#ifdef __OS_WIN
#define QUOTE "\""
#else
#define QUOTE ""
#endif


int main(int argc, char* argv[]) {
  // Enumerate and print the HID devices on the system
  struct hid_device_info *devs, *cur_dev;

  hid_init();
  devs = hid_enumerate(0x0, 0x0);
  cur_dev = devs;
  while (cur_dev) {
    printf("Device Found\n");
    printf("  VID PID:      %04hx %04hx\n",
        cur_dev->vendor_id, cur_dev->product_id);
    printf("  Page/Usage:   0x%x/0x%x (%d/%d)\n",
        cur_dev->usage_page, cur_dev->usage,
        cur_dev->usage_page, cur_dev->usage);
    printf("\n");
    printf("  Manufacturer: %ls\n", cur_dev->manufacturer_string);
    printf("  Product:      %ls\n", cur_dev->product_string);
    printf("  Device path:  %s%s%s\n",
        QUOTE, cur_dev->path, QUOTE);
    printf("\n");

    cur_dev = cur_dev->next;
  }
  hid_free_enumeration(devs);

  hid_exit();
  return 0;
}
