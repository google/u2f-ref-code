// Based on code from Google & Yubico.
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <ctype.h>
#include "u2f.h"
#include "u2f_nfc_crypto.h"
#include "u2f_nfc.h"

#if defined(__GLIBC__)
#include <wintypes.h>
#include <pcsclite.h>
#include <reader.h>
#endif

#include <winscard.h>
const char* printError(uint err);

// Gloabl variables shared with top level routine
flag log_Apdu = flagOFF;  // default
flag log_Crypto = flagOFF;
flag arg_Pause = flagOFF;
flag arg_Abort = flagON;
cmd_apdu_type cmd_apdu;

// Chaining Blocksize from reader - Le
static uint16_t blockSize = 256;

// Shared between PC/SC Card Access Routines
static SCARDHANDLE hCard;

void setChainingLc(uint16_t size) {
  blockSize = (size <= 256 ? size : 256);
}

static void pausePrompt(const char* prompt) {
  printf("\n%s", prompt);
  fflush(stdin);
  getchar();
}

void checkPause(const char* prompt) {
  if (arg_Pause) pausePrompt(prompt);
}

void AbortOrNot(void) {
  checkPause(arg_Abort == flagOFF ? "\nHit Enter to Continue..." : "\nHit Enter to Exit...");
  if (arg_Abort) exit(0);
  printf("%s" , "Continuing... (-a option)");
}

int check(const char *func, long rc) {
  if (rc == SCARD_S_SUCCESS) return 1;
  // Don't try to continue after PC/SC error
  printf("%s: PC/SC error %08lx:%s\n", func, rc, printError(rc));
  checkPause("Hit Enter to Exit...");
  exit(0);
}

double getTimestampMs(void) {
#ifdef _MSC_VER
  FILETIME gtime;
  ULONGLONG tret;
  GetSystemTimeAsFileTime(&gtime);
  tret = (((ULONGLONG)gtime.dwHighDateTime << 32) | ((ULONGLONG)gtime.dwLowDateTime));
  return (double) (tret / 10000.0);
#else
  struct timespec st;
  clock_gettime(CLOCK_MONOTONIC, &st);
  return ((double)(st.tv_sec * 1000.0)  + (double)(st.tv_nsec / 1000000.0));
#endif
}

int printTransactionTime(double start, double stop) {
  double elapsed;
  elapsed = stop-start;
  if((elapsed > 0.0) && (elapsed < NFC_TIMEOUT_MS)) {
    printf("Transaction Time: %.0f ms\n", elapsed);
    return SUCCESS;
  } else {
    printf("!!Transaction Time FAIL!!: %.0f ms\n", elapsed);
    return SW_ERROR_ANY;
  }
}

void  printCmdAPDU(uint8_t apduin[], ulong lenin) {
  uint8_t i;
  uint Lc, Le, DataOffset;
  printf("\n");
  if (log_Apdu == flagON) {
    // Determine case of Command APDU
    if (lenin == 4) {
      printf("Cmd APDU, Case 1\n");
      Lc = 0;
      Le = 0;
      DataOffset = 0;
    } else if (lenin == 5) {
      printf("Cmd APDU, Case 2S\n");
      Lc = 0;
      Le = (uint) apduin[4];
      DataOffset = 0;
    } else if ((lenin == (5u + apduin[4])) && (apduin[4] != 0)) {
      printf("Cmd APDU, Case 3S\n");
      Lc = apduin[4];
      Le = 0;
      DataOffset = 5u;
    } else if ((lenin == (6u + apduin[4])) && (apduin[4] != 0)) {
      printf("Cmd APDU, Case 4S\n");
      Lc = apduin[4];
      Le = apduin[lenin-1];
      DataOffset = 5;
    } else if ((lenin == 7u) && (apduin[4] == 0)) {
      printf("Cmd APDU, Case 2Extended\n");
      Lc = 0;
      Le = (uint) (apduin[5]*256u + apduin[6]);
      DataOffset = 0;
    } else if ((lenin ==  7u + ((uint) (apduin[5]*256 + apduin[6]))) &&
             (apduin[4] == 0)) {
      printf("Cmd APDU, Case 3Extended\n");
      Lc = (uint) (apduin[5]*256u + apduin[6]);
      Le = 0;
      DataOffset = 7;
    } else if ((lenin ==  9u + ((uint) (apduin[5]*256 + apduin[6]))) &&
             (apduin[4] == 0)) {
      printf("Cmd APDU, Case 4Extended\n");
      Lc = (uint) (apduin[5]*256 + apduin[6]);
      Le = (uint) 256u*apduin[lenin-2] +  apduin[lenin-1];
      DataOffset = 7u;
    }

    printf("Length: %lu(0x%04lX)\n", lenin, lenin);
    printf("Cla:%02X ", apduin[CLA]);
    printf("Ina:%02X ", apduin[INS]);
    printf("p1:%02X ", apduin[P1]);
    printf("p2:%02X\n", apduin[P2]);
    printf("Lc: %u(0x%04X) ", Lc, Lc);
    printf("Le: %u(0x%04X)", Le, Le);
    if (Le == 0) {
      printf("(Le=256)");
    }
    printf("\n");
    for (i = 0; i < Lc; i++) {
      printf("%02X", apduin[i+DataOffset]);
      if (((i & 0xf) == 0xf) || (i == Lc-1)) {
        printf("\n");
      } else {
        printf(":");
      }
    }
  }
}

void printRespAPDU(uint8_t apduin[], ulong lenin) {
  ulong i;
  if (log_Apdu == flagON) {
    printf("Response APDU, Length: %lu(0x%04lX)\n", lenin, lenin);
    printf("Status=>%02X:%02X\n", apduin[lenin-2], apduin[lenin-1]);
    for (i = 0; i < lenin-2; i++) {
      printf("%02X", apduin[i]);
      if ((i & 0xf) == 0xf) {
        printf("\n");
      } else {
        printf(":");
      }
    }
    printf("\n");
  }
}

void dumpHex(const char *descr, uint8_t *buf, int bcnt) {
  int i, j;
  uint8_t *p = buf;

  printf("%s: %d uint8_ts\n", descr, bcnt);

  for (i = 0; ; i += 0x10) {
    printf("%04x:", i);
    for (j = 0; j < 0x10; j++) {
      if (j < bcnt)
        printf("%c%02x", (j == 8) ? '-' : ' ', *p++);
      else
        printf("   ");
    }
    printf(" <");
    for (j = 0; j < 0x10; j++, buf++) {
      if (j < bcnt)
        putchar(isprint(*buf) ? *buf : '.');
      else
        putchar(' ');
    }
    printf(">\n");
    if (bcnt <= 0x10) break;
    bcnt -= 0x10;
  }
}

uint xchgAPDUShort(uint cla, uint ins, uint p1, uint p2, uint lc,
    const void *data, uint *rapduLen, void *rapdu) {
  double start, stop;
  uint8_t capdu[APDU_BUFFER_SIZE];
  uint8_t *dp = (uint8_t *) data;
  uint8_t rapduBuf[APDU_BUFFER_SIZE];
  ulong rlen;
  long rc;
  uint len;
  uint sw12;

  cmd_apdu = SHORT_APDU;

  // Setup and send cAPDU. Perform output chaining if necessary
  capdu[INS] = (uint8_t) (ins & 0xff);
  capdu[P1] =  (uint8_t) (p1 & 0xff);
  capdu[P2] =  (uint8_t) (p2 & 0xff);

  for (;;) {
    capdu[CLA] = (uint8_t) (cla & 0xff);
    if (lc > blockSize) cla |= 0x10;
    if (lc) {
      capdu[LC] = (lc > blockSize) ? blockSize : lc;
      memcpy((void*)&capdu[DATA_NON_EXTENDED], (const void *) dp,
          (size_t)capdu[LC]);

      capdu[DATA_NON_EXTENDED + capdu[LC]] = (blockSize == 256 ? 0 : blockSize);
      len = 6 + capdu[LC];
      dp += blockSize;
      lc -= capdu[LC];
    } else {
      capdu[LC] = (blockSize == 256 ? 0 : blockSize);
      len = 5;
    }

    rlen = sizeof(rapduBuf);
    printCmdAPDU(capdu, len);
    start = getTimestampMs();
    rc = SCardTransmit(hCard, SCARD_PCI_T1, (uint8_t *) &capdu, len,
      NULL, rapduBuf, &rlen);
    stop = getTimestampMs();

    if (!check("SCardTransmit (1)", rc)) return PCSC_ERROR;
    printRespAPDU(rapduBuf, rlen);
    if (rlen > (ulong)(blockSize) + 2) {
      printf("!! ERROR !!, Response Longer than Le (Extended Response to Short APDU Input?) \n");
      return SW_ERROR_ANY;
    }
    if (printTransactionTime(start, stop) != SUCCESS) {
      return SW_ERROR_ANY;
    }
    if (!lc) break;

    // If chaining, verify expected response
    if (rlen != 2 || rapduBuf[0] != 0x90 || rapduBuf[1] != 0x00) {
      printf("Invalid cAPDU chain block response\n");
    }
  }

  dp = (uint8_t *) rapdu;
  len = 0;

  for (;;) {
    if (rlen < 2) {
      printf("Malformed Response APDU. Expected at least SW12. Got %lu uint8_ts\n", rlen);
      return SW_ERROR_ANY;
    }
    rlen -= 2;
    sw12 = (int) (rapduBuf[rlen] << 8) | rapduBuf[rlen + 1];
    len += rlen;
    if (len > *rapduLen) {
      printf("Response APDU buffer overflow\n");
      return SW_ERROR_ANY;
    }
    if (rlen) {
      memcpy((void*)dp, (const void *)rapduBuf, (size_t) rlen);
      dp += rlen;
    }

    // More to read ?
    if (rapduBuf[rlen] != 0x61) break;

    // Read next block
    capdu[0] = 0;
    capdu[1] = 0xc0;
    capdu[2] = 0;
    capdu[3] = 0;
    capdu[4] = (uint8_t)(blockSize == 256 ? 0 : blockSize);

    rlen = sizeof(rapduBuf);
    printCmdAPDU(capdu, 5);

    start = getTimestampMs();
    rc = SCardTransmit(hCard, SCARD_PCI_T1, (uint8_t *) &capdu, 5, NULL, rapduBuf, &rlen);
    if (!check("SCardTransmit (2)", rc)) {return PCSC_ERROR;}
    stop = getTimestampMs();

    printRespAPDU(rapduBuf, rlen);
    if (rlen > (ulong)(blockSize) + 2) {
      printf("!! ERROR !!, Response Longer than Le (Extended Response to Short APDU Input?) \n");
      return SW_ERROR_ANY;
    }
    if (printTransactionTime(start, stop) != SUCCESS) {
      return SW_ERROR_ANY;
    }
  }
  *rapduLen = len;
  return sw12;
}

void utilInit(void) {
  srand((unsigned int) time(0));
}

void getRandom(uint8_t *buf, size_t size) {
  while (size--) *buf++ = (uint8_t) rand();
}

uint xchgAPDUExtended(uint cla, uint ins, uint p1, uint p2, uint lc,
    const void *data, uint *rapduLen, void *rapdu ) {
  double start, stop;
  uint8_t capdu[APDU_BUFFER_SIZE];
  ulong rlen = *rapduLen + 2;  // Add Buffer for Status
  ulong len;
  long rc;
  int sw12;

  cmd_apdu = EXTENDED_APDU;

  // Setup and send extended  cAPDU
  capdu[CLA] = cla & 0xff;
  capdu[INS] = ins & 0xff;
  capdu[P1] = p1 & 0xff;
  capdu[P2] = p2 & 0xff;
  capdu[4] = 0;
  capdu[5] = (lc>>8) & 0xff;
  capdu[6]= lc & 0xff;
  memcpy((void*)&capdu[7], (const void*)data, lc);
  capdu[7+lc]=(uint8_t) ((*rapduLen/256) & 0xff);
  capdu[8+lc]=(uint8_t) (*rapduLen & 0xff);
  len = lc+9;

  start  = getTimestampMs();
  printCmdAPDU(capdu, len);
  rc = SCardTransmit(hCard, SCARD_PCI_T1, capdu, len, NULL, (uint8_t*) rapdu, &rlen);
  stop = getTimestampMs();

  if (!check("SCardTransmit (3)", rc)) return PCSC_ERROR;
  printRespAPDU((uint8_t*) rapdu, rlen);
  if (rlen >= 2) {
    if (((uint8_t*)rapdu)[rlen-2] == 0x61) {
      printf("!! ERROR !!, DATA AVAILABLE (Chained) Response to Extended APDU Input\n");
      return SW_ERROR_ANY;
    }
  }
  if (printTransactionTime(start, stop) != SUCCESS) {
    return SW_ERROR_ANY;
  }
  *rapduLen = rlen-2;
  sw12 = (int) (((uint8_t*)rapdu)[rlen-2] << 8) | ((uint8_t*)rapdu)[rlen-1];
  return sw12;
}

int U2FNFC_connect(void) {
  ulong dwActiveProtocol, dwRecvLength;
  uint8_t pbRecvBuffer[0x100];
  LPTSTR  pmszReaders = NULL;
  LPTSTR  readerNames[10] = {0};
  LPTSTR  p;
  ulong   cch = SCARD_AUTOALLOCATE;
  SCARDCONTEXT hContext;
  long rc;
  int i;
  int key;

  printf("Initalization, finding PC/SC Readers...\n");

  // Initialize util functions
  utilInit();

  // Establish context
  rc = SCardEstablishContext(SCARD_SCOPE_USER, NULL, NULL, &hContext);
  if (!check("SCardEstablishContext", rc)) return PCSC_ERROR;


  rc = SCardListReaders(hContext, NULL, (LPTSTR)&pmszReaders, &cch);
  check("SCardListReaders", rc);

  for (i = 0, p = pmszReaders; *p; readerNames[i] = p, i++, p += (strlen(p) + 1)) {
    printf("Reader %d name:", i);
    printf("%s", p);
    printf("\n");
  }

  if (!p) {
    checkPause("No PC/SC reader found");
    return 1;
  }

  printf("Select Reader <Enter>:");
  key = 10;
  while (key > 9) {
    key = getchar();
     if (key >= '0' && key <= '9') {
      if (readerNames[key-'0'] != 0) {
        key = key - '0';
      }
    } else {
      printf("Select Valid Reader <Enter>:");
    }
  }

  printf("\nConnecting to: %s \n", readerNames[key]);

  // Connect to card (if any)
  rc = SCardConnect(hContext, readerNames[key], SCARD_SHARE_EXCLUSIVE, SCARD_PROTOCOL_T1, &hCard, &dwActiveProtocol);
  if (!check("SCardConnect", rc)) return 0;

  // Get ATR string
  dwRecvLength = sizeof(pbRecvBuffer);
  rc = SCardGetAttrib(hCard, SCARD_ATTR_ATR_STRING, pbRecvBuffer, &dwRecvLength);
  if (!check("SCardGetAttrib[ATR]", rc)) return PCSC_ERROR;
  dumpHex("\nSCardGetAttrib[SCARD_ATTR_ATR_STRING]", pbRecvBuffer, dwRecvLength);

  return 0;
}

// Lookup PCSC error codes & display to user
const char* printError(uint err) {
  switch (err) {
    case SCARD_S_SUCCESS:return "OK";
    case SCARD_E_CANCELLED:return "Command cancelled";
    case SCARD_E_CANT_DISPOSE:return "Cannot dispose";
    case SCARD_E_INSUFFICIENT_BUFFER:return "Insufficient buffer allocated";
    case SCARD_E_INVALID_ATR: return "Invalid ATR";
    case SCARD_E_INVALID_HANDLE:return "Invalid handle";
    case SCARD_E_INVALID_PARAMETER:return "Invalid parameter given";
    case SCARD_E_INVALID_TARGET:return "Invalid target given";
    case SCARD_E_INVALID_VALUE:return "Invalid value given";
    case SCARD_E_NO_MEMORY:return "Not enough memory";
    case SCARD_F_COMM_ERROR:return "Comm error";
    case SCARD_F_INTERNAL_ERROR:return "Internal error";
    case SCARD_F_UNKNOWN_ERROR:return "Unknown error";
    case SCARD_F_WAITED_TOO_LONG:return "Waited too long";
    case SCARD_E_UNKNOWN_READER:return "Unknown reader";
    case SCARD_E_TIMEOUT:return "Timeout";
    case SCARD_E_SHARING_VIOLATION:return "Sharing violation";
    case SCARD_E_NO_SMARTCARD:return "No smart card inserted";
    case SCARD_E_UNKNOWN_CARD:return "Unknown card";
    case SCARD_E_PROTO_MISMATCH:return "Pprotocol mismatch";
    case SCARD_E_NOT_READY:return "Not ready";
    case SCARD_E_SYSTEM_CANCELLED:return "System cancelled";
    case SCARD_E_NOT_TRANSACTED:return "Not Transacted";
    case SCARD_E_READER_UNAVAILABLE:return "Reader is unavailable";
    case SCARD_W_UNSUPPORTED_CARD:return "Card not supported";
    case SCARD_W_UNRESPONSIVE_CARD:return "Card unresponsive";
    case SCARD_W_UNPOWERED_CARD:return "Card unpowered";
    case SCARD_W_RESET_CARD:return "Card reset";
    case SCARD_E_UNSUPPORTED_FEATURE:return "Unsupported Feature";
    case SCARD_E_PCI_TOO_SMALL:return "PCI too small";
    case SCARD_E_READER_UNSUPPORTED:return "Reader unsupported";
    case SCARD_E_DUPLICATE_READER:return "Duplicate Reader";
    case SCARD_E_CARD_UNSUPPORTED:return "Card unsupported";
    case SCARD_E_NO_SERVICE: return "No Service";
    case SCARD_E_SERVICE_STOPPED:return "Service stopped";
    case SCARD_E_NO_READERS_AVAILABLE:return "No Reader ";
    default:return "Unknown Error";
  }
}

