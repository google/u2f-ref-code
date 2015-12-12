// Based on code from Google & Yubico.

// Define Local Values for errors in routines that should replay with APDU
// Status
#define SW_NO_ERROR     0x9000
#define SW_ERROR_ANY    0xffff
#define PCSC_ERROR      0xfffe
#define SUCCESS         0x0

#define NFC_TIMEOUT_MS  800.0

// Convert big-endian U2F to little-endian
#define MAKE_UINT32(x)  ( (uint32_t) (((x << 24) & 0xff000000) | ((x << 8) & 0x00ff0000) | ((x>> 8) & 0x0000ff00) | ((x>>24) & 0x000000ff)) )

// Command APDU Offsets
#define CLA 0
#define INS 1
#define P1  2
#define P2  3
#define LC  4
#define DATA_NON_EXTENDED 5
#define DATA_EXTENDED 7

typedef enum {SHORT_APDU, EXTENDED_APDU} cmd_apdu_type;

#ifndef __NO_PRAGMA_PACK
#pragma pack(push, 1)
#endif

typedef struct {
  uint8_t nonce[U2F_NONCE_SIZE];
  uint8_t appId[U2F_APPID_SIZE];
} U2F_REGISTER_REQ;

typedef struct {
  uint8_t registerId;
  U2F_EC_POINT pubKey;
  uint8_t keyHandleLen;
  uint8_t keyHandleCertSig[
      MAX_KH_SIZE +
      MAX_CERT_SIZE +
      MAX_ECDSA_SIG_SIZE];
} U2F_REGISTER_RESP;

typedef struct {
  uint8_t nonce[U2F_NONCE_SIZE];
  uint8_t appId[U2F_APPID_SIZE];
  uint8_t keyHandleLen;
  uint8_t keyHandle[MAX_KH_SIZE];
} U2F_AUTHENTICATE_REQ;

// Flags values
#define U2F_TOUCHED  0x01
#define U2F_ALTERNATE_INTERFACE  0x02

typedef struct {
  uint8_t flags;
  uint32_t ctr;
  uint8_t sig[MAX_ECDSA_SIG_SIZE];
} U2F_AUTHENTICATE_RESP;


#ifndef __NO_PRAGMA_PACK
#pragma pack(pop)
#endif

// AID for U2F applet
#define U2F_APPLET_AID          {0xA0, 0x00, 0x00, 0x06, 0x47, 0x2F, 0x00, 0x01 }
#define U2F_APPLET_AID_LEN      8

// Response Message
#define U2F_VERSION             {'U', '2', 'F', '_', 'V', '2' }
#define U2F_VERSION_LEN         6
