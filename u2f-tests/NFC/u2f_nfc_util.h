// Based on code from Google & Yubico.

#ifndef __U2F_UTIL_H_INCLUDED__
#define __U2F_UTIL_H_INCLUDED__

#include <stdint.h>
#include <stdio.h>
#include <stdarg.h>
#include <time.h>

#include <string>
#include <iostream>

#include "u2f.h"
#include "u2f_nfc.h"



#ifdef __cplusplus
extern "C" {
#endif

int U2FNFC_connect(void);
uint xchgAPDUShort(uint cla, uint ins, uint p1, uint p2, uint lc,
                   const void *data, uint *rapduLen, void *rapdu);

uint xchgAPDUExtended(uint cla, uint ins, uint p1, uint p2, uint lc,
                      const void *data, uint *rapduLen, void *rapdu);

#ifdef __cplusplus
}
#endif

#endif  // __U2F_UTIL_H_INCLUDED__
