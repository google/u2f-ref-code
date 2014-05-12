// Copyright 2013 Google Inc. All Rights Reserved.
'use strict';

goog.cryptotoken = goog.cryptotoken || {};

/**
 * Constructs cryptoTokenHandler object that handles the crypto token login
 * page sign requests. It creates a json object from the identity assertion
 * challenge and passes it to the cryptotokenrequestsender and sends the
 * response updates back to the page.
 * @param {function(?goog.cryptotoken.CryptoTokenHandler.ResponseData):void}
 *     successCallback callback when operation succeeds.
 * @param {function(number):void} errorCallback callback when error is
 *     encountered.
 * @constructor
 * @export
 */
goog.cryptotoken.CryptoTokenHandler =
    function(successCallback, errorCallback) {
  /**
   * Callback when operation succeeds.
   * @private
   * {function(?goog.cryptotoken.CryptoTokenHandler.ResponseData):void}
   */
  this.successCallback_ = successCallback;
  /**
   * Callback when operation fails.
   * @private {function(number):void}
   */
  this.errorCallback_ = errorCallback;
};

/**
 * @typedef {{
 *   keyHandle: string,
 *   challenge: string,
 *   sessionId: string,
 *   appId: ?string,
 *   version: ?string
 * }}
 */
goog.cryptotoken.CryptoTokenHandler.Challenge;

/**
 * @typedef {{
 *   appId: (string|null|undefined),
 *   version : string,
 *   challenges: !Array.<goog.cryptotoken.CryptoTokenHandler.Challenge>
 * }}
 */
goog.cryptotoken.CryptoTokenHandler.SignData_;

/**
 * @typedef {{
 *   appId: (string|null|undefined),
 *   sessionId: string,
 *   challenge : ?string,
 *   version: ?string
 * }}
 */
goog.cryptotoken.CryptoTokenHandler.EnrollData_;

/**
 * @typedef {{
 *   type: goog.cryptotoken.CryptoTokenMsgTypes,
 *   signData: !Array.<goog.cryptotoken.CryptoTokenHandler.Challenge>,
 *   enrollData:
 *   (goog.cryptotoken.CryptoTokenHandler.EnrollData_|undefined),
 *   enrollChallenges:
 *   (Array.<goog.cryptotoken.CryptoTokenHandler.EnrollData_>|undefined),
 *   requestId: (string|null|undefined),
 *   logMsgUrl: (string|null|undefined),
 *   timeout: (number|undefined)
 * }}
 */
goog.cryptotoken.CryptoTokenHandler.RequestObject;

/**
 * @typedef {{
 *   bd: ?string,
 *   sign: ?string,
 *   challenge: ?string,
 *   sessionId: string,
 *   appId: string,
 *   enrollData: ?string
 * }}
 */
goog.cryptotoken.CryptoTokenHandler.ResponseData;

/**
 * @typedef {{
 *   type: goog.cryptotoken.CryptoTokenMsgTypes,
 *   code: goog.cryptotoken.CryptoTokenCodeTypes,
 *   responseData: ?goog.cryptotoken.CryptoTokenHandler.ResponseData
 * }}
 */
goog.cryptotoken.CryptoTokenHandler.ResponseObject;

/**
 * Touch timeout (2 minutes) in milliseconds.
 * @const
 */
goog.cryptotoken.CryptoTokenHandler.TOUCH_TIMEOUT_MILLIS = 2 * 60 * 1000;

/**
 * Extension response timeout in seconds.
 * @const
 */
goog.cryptotoken.CryptoTokenHandler.EXTENSION_TIMEOUT_SEC = 30;

/**
 * This is a helper function provided to clients to be able to create challenge
 * lists where the app id is included as part of each challenge. The clients
 * will pass the sign data json object to this method and then pass the returned
 * sign data list to handleAuthenticationRequest
 * @param {!goog.cryptotoken.CryptoTokenHandler.SignData_} signData sign data
 *     object.
 * @param {string} appId application identity for the challenges
 * @return {!Array.<goog.cryptotoken.CryptoTokenHandler.Challenge>} list of
 *     challenges in the sign data
 * @export
 */
goog.cryptotoken.CryptoTokenHandler.getSignDataList =
    function(signData, appId) {
  return signData['challenges'].map(function(challenge) {
    challenge['appId'] = appId;
    return challenge;
  });
};

/**
 * Handles a sign request. This sends a request of type sign_web_req to
 * the extension as defined by security/tools/gnubby/gnubbyd/doc/api.json.
 * @param {!Array.<goog.cryptotoken.CryptoTokenHandler.Challenge>} signDataList
 *     object containing challenge list.
 * @param {?string} logMsgUrl the url to log messages to.
 * @export
 */
goog.cryptotoken.CryptoTokenHandler.prototype.handleAuthenticationRequest =
    function(signDataList, logMsgUrl) {
  if (signDataList.length == 0) {
    this.errorCallback_(
        goog.cryptotoken.CryptoTokenCodeTypes.NO_DEVICES_ENROLLED);
    return;
  }
  var signJsonObject = {
    'type': goog.cryptotoken.CryptoTokenMsgTypes.SIGN_WEB_REQ,
    'signData': signDataList,
    'timeout' : goog.cryptotoken.CryptoTokenHandler.EXTENSION_TIMEOUT_SEC
  };
  if (logMsgUrl) {
    signJsonObject['logMsgUrl'] = logMsgUrl;
  }
  this.sendRequest_(signJsonObject, new Date(Date.now() +
      goog.cryptotoken.CryptoTokenHandler.TOUCH_TIMEOUT_MILLIS));
};

/**
 * Sends the request to the extension
 * @param {!goog.cryptotoken.CryptoTokenHandler.RequestObject} requestData
 *     request data to be sent to the extension.
 * @param {Date} touchTimeout timeout waiting for touch.
 * @private
 */
goog.cryptotoken.CryptoTokenHandler.prototype.sendRequest_ =
    function(requestData, touchTimeout) {
  var self = this;
  var extensionIds = [
    'dlfcjilkjfhdnfiecknlnddkmmiofjbg', // gnubbyd-dev
    'beknehfpfkghjoafdifaflglpjkojoco',  // gnubbyd-stable
    'kmendfapggjehodndflmmgagdbamhnfd' // component extension
  ];
  if (!this.sender_) {
    goog.cryptotoken.requestSender.createCryptoTokenRequestSender(
        extensionIds, function(sender) {
      if (sender) {
        self.sender_ = sender;
        self.sender_.sendRequest(requestData,
          self.onUpdateCallback_.bind(self), touchTimeout);
      } else {
        self.errorCallback_(
            goog.cryptotoken.CryptoTokenCodeTypes.NO_EXTENSION);
      }
    });
  } else {
    this.sender_.sendRequest(requestData, this.onUpdateCallback_.bind(this),
        touchTimeout);
  }
};

/**
 * Handles the registration (aka enrollment) request
 * @param {!Array.<goog.cryptotoken.CryptoTokenHandler.EnrollData_>}
 *     enrollDataList list of enrollment data challenges
 * @param {!Array.<goog.cryptotoken.CryptoTokenHandler.Challenge>}
 *     signDataList list of challenges of sign data
 * @param {?string} logMsgUrl url to log messages to
 * @export
 */
goog.cryptotoken.CryptoTokenHandler.prototype.handleRegistrationRequest =
    function(enrollDataList, signDataList, logMsgUrl) {
  var enrollJsonObject = {
    'type': goog.cryptotoken.CryptoTokenMsgTypes.ENROLL_WEB_REQ,
    'enrollChallenges': enrollDataList,
    'signData': signDataList,
    'timeout' : goog.cryptotoken.CryptoTokenHandler.EXTENSION_TIMEOUT_SEC
  };
  if (logMsgUrl) {
    enrollJsonObject['logMsgUrl'] = logMsgUrl;
  }
  this.sendRequest_(enrollJsonObject, new Date(Date.now() +
      goog.cryptotoken.CryptoTokenHandler.TOUCH_TIMEOUT_MILLIS));
};

/**
 * Callback for response update.
 * @param {!goog.cryptotoken.CryptoTokenHandler.ResponseObject} responseObject
 *     response object.
 * @private
 */
goog.cryptotoken.CryptoTokenHandler.prototype.onUpdateCallback_ =
    function(responseObject) {
  var type = responseObject['type'];
  if (type == goog.cryptotoken.CryptoTokenMsgTypes.ENROLL_WEB_REPLY ||
      type == goog.cryptotoken.CryptoTokenMsgTypes.SIGN_WEB_REPLY ||
      type == goog.cryptotoken.CryptoTokenMsgTypes.ENROLL_WEB_NOTIFICATION ||
      type == goog.cryptotoken.CryptoTokenMsgTypes.SIGN_WEB_NOTIFICATION) {
    this.onRequestUpdate_(responseObject['code'],
        responseObject['responseData']);
  } else {
    // responseObject['type'] is goog.cryptotoken.CryptoTokenMsgTypes.RES_ERROR
    this.errorCallback_(goog.cryptotoken.CryptoTokenCodeTypes.NO_EXTENSION);
  }
};

/**
 * Handles the updates for sign or enroll operation.
 * @param {number} code status code.
 * @param {?goog.cryptotoken.CryptoTokenHandler.ResponseData} responseData
 *     response object.
 * @private
 */
goog.cryptotoken.CryptoTokenHandler.prototype.onRequestUpdate_ =
    function(code, responseData) {
  // Accept SIGN_OK, which is deprecated but sent by the gnubbyd app,
  // until a version of gnubbyd is rolled out that sends OK.
  if ((code == goog.cryptotoken.CryptoTokenCodeTypes.SIGN_OK ||
      code == goog.cryptotoken.CryptoTokenCodeTypes.OK) &&
      responseData != null) {
    this.successCallback_(responseData);
  } else {
    this.errorCallback_(code);
  }
};

// also export this in the u2f namespace:
var u2f = goog.cryptotoken;
