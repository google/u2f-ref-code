// Based on code from Google & Yubico.

// General U2F EC constants
#define U2F_EC_KEY_SIZE   32  // EC key size in bytes
#define U2F_EC_POINT_SIZE ((U2F_EC_KEY_SIZE * 2) + 1)  // Size of EC point

// EC (uncompressed) point
#define U2F_POINT_UNCOMPRESSED  0x04    // Uncompressed point format
typedef struct {
    uint8_t pointFormat;                // Point type
    uint8_t x[U2F_EC_KEY_SIZE];         // X-value
    uint8_t y[U2F_EC_KEY_SIZE];         // Y-value
} U2F_EC_POINT;
