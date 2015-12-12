// Based on code from Google & Yubico.

#ifndef __U2F_H_INCLUDED__
#define __U2F_H_INCLUDED__

#ifdef __cplusplus
extern "C" {
#endif

// Data Types
#ifdef _MSC_VER  // Windows
typedef unsigned char     uint8_t;
typedef unsigned short    uint16_t;
typedef unsigned int      uint32_t;
#else
#include <stdint.h>
#endif

typedef unsigned int  uint;
typedef unsigned long ulong;

// Define ourselves to pass between C++ / C as MSVC c doesn't support bool
typedef enum {flagOFF = 0, flagON} flag;

// General U2F constants
#define MAX_ECDSA_SIG_SIZE  72    // asn1 DER format
#define MIN_KH_SIZE         32
#define MAX_KH_SIZE         255   // key handle
#define MAX_CERT_SIZE       2048  // attestation certificate

#define APDU_BUFFER_SIZE  5000

#define U2F_APPID_SIZE  32
#define U2F_NONCE_SIZE  32

// U2Fv2 NFC instructions
#define U2F_INS_REGISTER      0x01
#define U2F_INS_AUTHENTICATE  0x02

// U2F_REGISTER instruction defines
#define U2F_REGISTER_ID       0x05  // magic constant
#define U2F_REGISTER_HASH_ID  0x00  // magic constant

// Authentication parameter byte
#define U2F_AUTH_ENFORCE     0x03  // Require user presence
#define U2F_AUTH_CHECK_ONLY  0x07  // Test but do not consume

#define U2F_CTR_SIZE            4     // Size of counter field
#define U2F_APPID_SIZE          32    // Size of application id
#define U2F_CHAL_SIZE           32    // Size of challenge

#define U2F_MAX_KH_SIZE         128   // Max size of key handle
#define U2F_MAX_ATT_CERT_SIZE   1024  // Max size of attestation certificate
#define U2F_MAX_EC_SIG_SIZE     72    // Max size of DER coded EC signature


#ifdef _MSC_VER
#include <windows.h>
#define usleep(x) Sleep((x + 999) / 1000)
#else
#include <unistd.h>
#define max(a, b) ({ __typeof__ (a) _a = (a); \
                     __typeof__ (b) _b = (b); \
                     _a > _b ? _a : _b; })

#define min(a, b) ({ __typeof__ (a) _a = (a); \
                     __typeof__ (b) _b = (b); \
                     _a < _b ? _a : _b; })
#endif

#define CHECK_INFO "FILE:" << __FILE__ " FUNCTION:" << __FUNCTION__ << " LINE:" << __LINE__ << "\n"

#ifdef _MSC_VER  // ANSI codes are a pain on PC
#define CHECK_EQ(a, b) do { if ((a) != (b)) { std::cerr << "CHECK_EQ fail at " << CHECK_INFO#a << " != "#b << ":"; AbortOrNot(); }} while (0)
#define CHECK_NE(a, b) do { if ((a) == (b)) { std::cerr << "CHECK_NE fail at " << CHECK_INFO#a << " == "#b << ":"; AbortOrNot(); }} while (0)
#define CHECK_GE(a, b) do { if ((a) < (b)) { std::cerr << "CHECK_GE fail at " << CHECK_INFO#a << " < "#b << ":"; AbortOrNot(); }} while (0)
#define CHECK_GT(a, b) do { if ((a) <= (b)) { std::cerr << "CHECK_GT fail at " << CHECK_INFO#a << " < "#b << ":"; AbortOrNot(); }} while (0)
#define CHECK_LT(a, b) do { if ((a) >= (b)) { std::cerr << "CHECK_LT fail at " << CHECK_INFO#a << " >= "#b << ":"; AbortOrNot(); }} while (0)
#define CHECK_LE(a, b) do { if ((a) > (b)) { std::cerr << "CHECK_LE fail at " << CHECK_INFO#a << " > "#b << ":"; AbortOrNot(); }} while (0)
#define PASS(x) do { (x); std::cout << "PASS("#x")" << std::endl; } while (0)
#else
#define CHECK_EQ(a, b) do { if ((a) != (b)) { std::cerr << "\x1b[31mCHECK_EQ fail at " << CHECK_INFO#a << " != "#b << ":\x1b[0m "; AbortOrNot(); }} while (0)
#define CHECK_NE(a, b) do { if ((a) == (b)) { std::cerr << "\x1b[31mCHECK_NE fail at " << CHECK_INFO#a << " == "#b << ":\x1b[0m "; AbortOrNot(); }} while (0)
#define CHECK_GE(a, b) do { if ((a) < (b)) { std::cerr << "\x1b[31mCHECK_GE fail at " << CHECK_INFO#a << " < "#b << ":\x1b[0m "; AbortOrNot(); }} while (0)
#define CHECK_GT(a, b) do { if ((a) <= (b)) { std::cerr << "\x1b[31mCHECK_GT fail at " << CHECK_INFO#a << " < "#b << ":\x1b[0m "; AbortOrNot(); }} while (0)
#define CHECK_LT(a, b) do { if ((a) >= (b)) { std::cerr << "\x1b[31mCHECK_LT fail at " << CHECK_INFO#a << " >= "#b << ":\x1b[0m "; AbortOrNot(); }} while (0)
#define CHECK_LE(a, b) do { if ((a) > (b)) { std::cerr << "\x1b[31mCHECK_LE fail at " << CHECK_INFO#a << " > "#b << ":\x1b[0m "; AbortOrNot(); }} while (0)
#define PASS(x) do { (x); std::cout << "\x1b[32mPASS("#x")\x1b[0m" << std::endl; } while (0)
#endif

#define INFO if (arg_Verbose) U2F_info(__FUNCTION__, __LINE__) << ": "

#ifdef __cplusplus
}
#endif

#endif  // __U2F_H_INCLUDED__
