// Copyright 2013 Google Inc. All Rights Reserved.
 /**
 * @fileoverview  It sends request messages to the extension and listens for
 * response updates.
 */

'use strict';

goog.cryptotoken = goog.cryptotoken || {};
goog.cryptotoken.requestSender = goog.cryptotoken.requestSender || {};

/**
 * Creates a chromeruntimerequestsender.
 * @param {!Array.<string>} extensionIds the list of extension ids
 * @param {function((goog.cryptotoken.ChromeRuntimeRequestSender))} callback
 *     callback to call with the sender object.
 */
goog.cryptotoken.requestSender.createCryptoTokenRequestSender =
    function(extensionIds, callback) {
  if (typeof chrome != 'undefined' && chrome.runtime) {
    // This means an extension has been registered to receive
    // messages from this origin.
    var extensionId = extensionIds.shift();
    if (!extensionId) {
      callback(null);
      return;
    }
    var sender = new goog.cryptotoken.ChromeRuntimeRequestSender(extensionId);
    sender.ping(function(result) {
      if (result) {
        callback(sender);
      } else {
        goog.cryptotoken.requestSender.createCryptoTokenRequestSender(
            extensionIds, callback);
      }
    });
  } else {
    // No extension is present to handle chrome runtime messages.
    callback(null);
  }
};

/**
 * Handles the response object by calling the callback with the response.
 * Optionally, also sends message back to extension.
 * @param {!goog.cryptotoken.CryptoTokenHandler.ResponseObject} responseObject
 *     response object.
 * @param {!goog.cryptotoken.CryptoTokenHandler.RequestObject} requestObject
 *     request object.
 * @param {function(!goog.cryptotoken.CryptoTokenHandler.ResponseObject)}
 *     callback callback to call.
 * @param {(!goog.cryptotoken.ChromeRuntimeRequestSender)} sender request
 *     sender object.
 * @param {Date} touchTimeout timeout waiting for touch.
 * @private
 */
goog.cryptotoken.requestSender.handleResponse_ =
    function(responseObject, requestObject, callback, sender, touchTimeout) {
  if (goog.cryptotoken.requestSender.isValidResponse_(responseObject)) {
    callback(responseObject);
    if (responseObject['type'] !=
        goog.cryptotoken.CryptoTokenMsgTypes.ENROLL_WEB_NOTIFICATION &&
        responseObject['type'] !=
        goog.cryptotoken.CryptoTokenMsgTypes.SIGN_WEB_NOTIFICATION) {
      if (responseObject['code'] ==
          goog.cryptotoken.CryptoTokenCodeTypes.NO_GNUBBIES ||
          responseObject['code'] ==
          goog.cryptotoken.CryptoTokenCodeTypes.WAIT_TOUCH) {
        // We need to post these message back to the extension as we expect the
        // user to insert or touch a crypto token.
        var now = new Date(goog.now());
        if (touchTimeout != undefined && now > touchTimeout) {
          var timeoutResponseObject = {
            'type': responseObject['type'],
            'code': goog.cryptotoken.CryptoTokenCodeTypes.TOUCH_TIMEOUT,
            'responseData': null
          };
          callback(timeoutResponseObject);
          return;
        }
        setTimeout(function() {
          sender.sendRequest(requestObject, callback, touchTimeout);
        }, 500);
        return;
      }
    }
  }
};

/**
 * Returns whether a response is valid or not.
 * @param {!goog.cryptotoken.CryptoTokenHandler.ResponseObject} responseObject
 *     response object.
 * @return {boolean} whether the response is valid
 * @private
 */
goog.cryptotoken.requestSender.isValidResponse_ = function(responseObject) {
  return responseObject['type'] ==
      goog.cryptotoken.CryptoTokenMsgTypes.ENROLL_WEB_REPLY ||
      responseObject['type'] ==
      goog.cryptotoken.CryptoTokenMsgTypes.ENROLL_WEB_NOTIFICATION ||
      responseObject['type'] ==
      goog.cryptotoken.CryptoTokenMsgTypes.SIGN_WEB_NOTIFICATION ||
      responseObject['type'] ==
      goog.cryptotoken.CryptoTokenMsgTypes.SIGN_WEB_REPLY ||
      responseObject['type'] ==
      goog.cryptotoken.CryptoTokenMsgTypes.RES_ERROR;
};

/**
 * Get the request challenge object corresponding to the challenge in the
 * response object
 * @param {goog.cryptotoken.CryptoTokenHandler.RequestObject} requestObject
 *     request Object
 * @param {Object} responseObject authenticator response object
 * @param {goog.cryptotoken.CryptoTokenMsgTypes} type response type
 * @return {(goog.cryptotoken.CryptoTokenHandler.Challenge|null)} challenge
 *     object from the request
 */
goog.cryptotoken.requestSender.getRequestChallengeObject =
    function(requestObject, responseObject, type) {
  var challenge = responseObject['challenge'];
  var challengesList;
  if (type == goog.cryptotoken.CryptoTokenMsgTypes.SIGN_WEB_REPLY ||
      type == goog.cryptotoken.CryptoTokenMsgTypes.SIGN_WEB_NOTIFICATION) {
    challengesList = requestObject['signData'];
  } else {
    challengesList = requestObject['enrollChallenges'];
  }
  for (var i = 0; i < challengesList.length; i++) {
    var challengeObject = challengesList[i];
    if (challengeObject['challenge'] == challenge) {
      return challengeObject;
    }
  }
  return null;
};

/**
 * Creates a sender that talks to the extension using chrome runtime messaging.
 * Since there are multiple possible extensions, we first try to connect to
 * each one. Once we find one that the browser can connect to, the
 * ChromeRuntimeRequestSender is created with that extension id.
 * @param {string} extensionId extension id.
 * @constructor
 */
goog.cryptotoken.ChromeRuntimeRequestSender = function(extensionId) {
  /**
   * Extension id.
   * @private {string}
   */
  this.extensionId_ = extensionId;
  this.map_ = {};
  this.eventId_ = 0;
};

/**
 * Sends a message to the extension to determine if it can connect to
 * the extension.
 * @param {function(boolean)} callback callback to call with status of connect
 * attempt to the extension.
 */
goog.cryptotoken.ChromeRuntimeRequestSender.prototype.ping =
    function(callback) {
  var self = this;
  // Only Chrome m32 and higher support sending the TLS channel ID.
  if (self.isChromeM32OrHigher_()) {
    var opts = { 'includeTlsChannelId': true };
    self.port_ = chrome.runtime.connect(self.extensionId_, opts);
  } else {
    self.port_ = chrome.runtime.connect(self.extensionId_);
  }

  chrome.runtime.sendMessage(this.extensionId_, 'hello',
      function() {
    if (!chrome.runtime.lastError) {
      // If send message succeeded, port can be used to connect to extension.
      self.port_.onMessage.addListener(self.handleResponse.bind(self));
      self.port_.onDisconnect.addListener(self.handleDisconnect.bind(self));
    }
    callback(!chrome.runtime.lastError);
  });
};

/**
 * @return {boolean} Whether the Chrome version is M32 or higher.
 * @private
 */
goog.cryptotoken.ChromeRuntimeRequestSender.prototype.isChromeM32OrHigher_ =
    function() {
  var matches = navigator.userAgent.match(/Chrome\/([0-9]+)/);
  if (!matches || matches.length != 2)
    return false;
  return matches[1] >= 32;
};

/**
 * Sends the request to the extension using chrome runtime messaging.
 * @param {!goog.cryptotoken.CryptoTokenHandler.RequestObject} requestObject
 *     request object.
 * @param {function(!goog.cryptotoken.CryptoTokenHandler.ResponseObject)}
 *     callback callback for the request.
 * @param {Date} touchTimeout timeout waiting for touch.
 */
goog.cryptotoken.ChromeRuntimeRequestSender.prototype.sendRequest =
    function(requestObject, callback, touchTimeout) {
  if (this.port_) {
    var requestId = this.getNextEventId_();
    requestObject['requestId'] = requestId;
    this.map_[requestId] = {};
    this.map_[requestId]['callback'] = callback;
    this.map_[requestId]['requestObject'] = requestObject;
    this.map_[requestId]['touchTimeout'] = touchTimeout;
    this.port_.postMessage(requestObject);
  } else {
    this.sendErrorResponse(callback);
  }
};

/**
 * Gets the next event id.
 * @return {number} the next event id.
 * @private
 */
goog.cryptotoken.ChromeRuntimeRequestSender.prototype.getNextEventId_ =
    function() {
  return ++this.eventId_;
};
/**
 * Send an unknown error response to the callback.
 * @param {function(goog.cryptotoken.CryptoTokenHandler.ResponseObject)}
 *    callback callback function to send response to.
 */
goog.cryptotoken.ChromeRuntimeRequestSender.prototype.sendErrorResponse =
    function(callback) {
  var responseObject = {
    'type': goog.cryptotoken.CryptoTokenMsgTypes.RES_ERROR,
    'code': goog.cryptotoken.CryptoTokenCodeTypes.UNKNOWN_ERROR,
    'responseData': null
  };
  callback(responseObject);
};

/**
 * Handle the response from the extension.
 * @param {!goog.cryptotoken.CryptoTokenHandler.ResponseObject} response
 *     response object.
 */
goog.cryptotoken.ChromeRuntimeRequestSender.prototype.handleResponse =
    function(response) {
  var self = this;
  var requestId = response['requestId'];
  if (requestId == undefined) {
    goog.global.console.log('invalid requestid:' + requestId);
    return;
  }
  var callback = this.map_[requestId]['callback'];
  if (callback == undefined) {
    goog.global.console.log('invalid requestid:' + requestId);
    return;
  }
  var requestObject = this.map_[requestId]['requestObject'];
  var touchTimeout = this.map_[requestId]['touchTimeout'];
  if (chrome.runtime.lastError || requestObject == null) {
    self.sendErrorResponse(callback);
  } else {
    if (response['type'] !=
        goog.cryptotoken.CryptoTokenMsgTypes.ENROLL_WEB_NOTIFICATION &&
        response['type'] !=
        goog.cryptotoken.CryptoTokenMsgTypes.SIGN_WEB_NOTIFICATION) {
      // Since it is not a notification, the extension will not send any more
      // messages for this request id so remove the map entry.
      delete this.map_[requestId];
    }
    if (response['code'] == goog.cryptotoken.CryptoTokenCodeTypes.OK ||
        response['code'] == goog.cryptotoken.CryptoTokenCodeTypes.SIGN_OK) {
      var responseData = response['responseData'];
      var requestChallengeObject =
          goog.cryptotoken.requestSender.getRequestChallengeObject(
              requestObject, responseData, response['type']);
      if (requestChallengeObject == null) {
        self.sendErrorResponse(callback);
        return;
      }
      responseData['sessionId'] = requestChallengeObject['sessionId'];
      response['responseData'] = responseData;
    }
    goog.cryptotoken.requestSender.handleResponse_(
      /** @type {goog.cryptotoken.CryptoTokenHandler.ResponseObject} */
      (response),
      requestObject, callback, self, touchTimeout);
  }
};

/**
 * Handle the disconnect from extension.
 */
goog.cryptotoken.ChromeRuntimeRequestSender.prototype.handleDisconnect =
    function() {
  goog.global.console.log('port disconnected');
  this.port_ = null;
  this.map_ = {};
};
