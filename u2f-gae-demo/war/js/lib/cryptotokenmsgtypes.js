// Copyright 2013 Google Inc. All Rights Reserved.

goog.cryptotoken = goog.cryptotoken || {};

/**
 * Crypto Token Messages Types
 * @const
 * @enum {string}
 * @export
 */
goog.cryptotoken.CryptoTokenMsgTypes = {
  // Response error msg type.
  RES_ERROR: 'res_error',

  // enrollment web request
  ENROLL_WEB_REQ: 'enroll_web_request',

  // sign web request
  SIGN_WEB_REQ: 'sign_web_request',

  // enrollment web reply
  ENROLL_WEB_REPLY: 'enroll_web_reply',

  // sign web reply
  SIGN_WEB_REPLY: 'sign_web_reply',

  // enroll web notification
  ENROLL_WEB_NOTIFICATION: 'enroll_web_notification',

  // sign web notification
  SIGN_WEB_NOTIFICATION: 'sign_web_notification'
};
