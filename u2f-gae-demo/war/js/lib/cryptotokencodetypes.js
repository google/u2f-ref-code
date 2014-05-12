// Copyright 2013 Google Inc. All Rights Reserved.
/**
 * @fileoverview This provides the different code types for the crypto token
 * operations.
 */

goog.cryptotoken = goog.cryptotoken || {};

/**
 * Crypto Token code types.
 * @const
 * @enum {number}
 * @export
 */
goog.cryptotoken.CryptoTokenCodeTypes = {
  // Request succeeded.
  OK: 0,

  //Sign operation succeeded. (DEPRECATED)
  SIGN_OK: 1,

  // All plugged in devices are already enrolled.
  ALREADY_ENROLLED: 2,

  // None of the plugged in devices are enrolled.
  NONE_PLUGGED_ENROLLED: 3,

  // One or more devices are waiting for touch.
  WAIT_TOUCH: 4,

  // No gnubbies found.
  NO_GNUBBIES: 5,

  // Time out waiting for touch.
  TOUCH_TIMEOUT: 6,

  // Unknown error during enrollment.
  UNKNOWN_ERROR: 7,

  // Extension not found.
  NO_EXTENSION: 8,

  // No devices enrolled for this user.
  NO_DEVICES_ENROLLED: 9,

  // Gnubby errors due to chrome issues.
  BROWSER_ERROR: 10,

  // Gnubbyd taking too long.
  LONG_WAIT: 11,

  // Bad request.
  BAD_REQUEST: 12,

  // All gnubbies are too busy to handle your request.
  BUSY: 13,

  // There is a bad app_id in the request.
  BAD_APP_ID: 14,

  // NFC not supported by device
  NFC_UNSUPPORTED: 15,

  // Api not supported
  API_UNSUPPORTED: 16,

  // Authenticator not installed
  AUTHENTICATOR_NOT_INSTALLED: 17,

  // user cancelled operation
  USER_CANCELLED: 18
};
