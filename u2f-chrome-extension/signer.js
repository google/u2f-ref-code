// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Handles web page requests for gnubby sign requests.
 *
 */

'use strict';

var signRequestQueue = new OriginKeyedRequestQueue();

/**
 * Handles a web sign request.
 * @param {MessageSender} sender The sender of the message.
 * @param {Object} request The web page's sign request.
 * @param {Function} sendResponse Called back with the result of the sign.
 * @return {Closeable} Request handler that should be closed when the browser
 *     message channel is closed.
 */
function handleWebSignRequest(sender, request, sendResponse) {
  var sentResponse = false;
  var queuedSignRequest;

  function sendErrorResponse(u2fCode) {
    sendResponseOnce(sentResponse, queuedSignRequest,
        makeWebErrorResponse(request,
            mapErrorCodeToGnubbyCodeType(u2fCode, true /* forSign */)),
        sendResponse);
  }

  function sendSuccessResponse(challenge, info, browserData) {
    var responseData = makeWebSignResponseDataFromChallenge(challenge);
    addSignatureAndBrowserDataToResponseData(responseData, info, browserData,
        'browserData');
    var response = makeWebSuccessResponse(request, responseData);
    sendResponseOnce(sentResponse, queuedSignRequest, response, sendResponse);
  }

  queuedSignRequest =
      validateAndEnqueueSignRequest(
          sender, request, 'signData', sendErrorResponse,
          sendSuccessResponse);
  return queuedSignRequest;
}

/**
 * Handles a U2F sign request.
 * @param {MessageSender} sender The sender of the message.
 * @param {Object} request The web page's sign request.
 * @param {Function} sendResponse Called back with the result of the sign.
 * @return {Closeable} Request handler that should be closed when the browser
 *     message channel is closed.
 */
function handleU2fSignRequest(sender, request, sendResponse) {
  var sentResponse = false;
  var queuedSignRequest;

  function sendErrorResponse(u2fCode) {
    sendResponseOnce(sentResponse, queuedSignRequest,
        makeU2fErrorResponse(request, u2fCode), sendResponse);
  }

  function sendSuccessResponse(challenge, info, browserData) {
    var responseData = makeU2fSignResponseDataFromChallenge(challenge);
    addSignatureAndBrowserDataToResponseData(responseData, info, browserData,
        'clientData');
    var response = makeU2fSuccessResponse(request, responseData);
    sendResponseOnce(sentResponse, queuedSignRequest, response, sendResponse);
  }

  queuedSignRequest =
      validateAndEnqueueSignRequest(
          sender, request, 'signRequests', sendErrorResponse,
          sendSuccessResponse);
  return queuedSignRequest;
}

/**
 * Creates a base U2F responseData object from the server challenge.
 * @param {SignChallenge} challenge The server challenge.
 * @return {Object} The responseData object.
 */
function makeU2fSignResponseDataFromChallenge(challenge) {
  var responseData = {
    'keyHandle': challenge['keyHandle']
  };
  return responseData;
}

/**
 * Creates a base web responseData object from the server challenge.
 * @param {SignChallenge} challenge The server challenge.
 * @return {Object} The responseData object.
 */
function makeWebSignResponseDataFromChallenge(challenge) {
  var responseData = {};
  for (var k in challenge) {
    responseData[k] = challenge[k];
  }
  return responseData;
}

/**
 * Adds the browser data and signature values to a responseData object.
 * @param {Object} responseData The "base" responseData object.
 * @param {string} signatureData The signature data.
 * @param {string} browserData The browser data generated from the challenge.
 * @param {string} browserDataName The name of the browser data key in the
 *     responseData object.
 */
function addSignatureAndBrowserDataToResponseData(responseData, signatureData,
    browserData, browserDataName) {
  responseData[browserDataName] = B64_encode(UTIL_StringToBytes(browserData));
  responseData['signatureData'] = signatureData;
}

/**
 * Validates a sign request using the given sign challenges name, and, if valid,
 * enqueues the sign request for eventual processing.
 * @param {MessageSender} sender The sender of the message.
 * @param {Object} request The web page's sign request.
 * @param {string} signChallengesName The name of the sign challenges value in
 *     the request.
 * @param {function(ErrorCodes)} errorCb Error callback.
 * @param {function(SignChallenge, string, string)} successCb Success callback.
 * @return {Closeable} Request handler that should be closed when the browser
 *     message channel is closed.
 */
function validateAndEnqueueSignRequest(sender, request,
    signChallengesName, errorCb, successCb) {
  var origin = getOriginFromUrl(/** @type {string} */ (sender.url));
  if (!origin) {
    errorCb(ErrorCodes.BAD_REQUEST);
    return null;
  }
  // More closure type inference fail.
  var nonNullOrigin = /** @type {string} */ (origin);

  if (!isValidSignRequest(request, signChallengesName)) {
    errorCb(ErrorCodes.BAD_REQUEST);
    return null;
  }

  var signChallenges = request[signChallengesName];
  // A valid sign data has at least one challenge, so get the first appId from
  // the first challenge.
  var firstAppId = signChallenges[0]['appId'];
  var timer = createTimerForRequest(
      FACTORY_REGISTRY.getCountdownFactory(), request);
  var logMsgUrl = request['logMsgUrl'];

  // Queue sign requests from the same origin, to protect against simultaneous
  // sign-out on many tabs resulting in repeated sign-in requests.
  var queuedSignRequest = new QueuedSignRequest(signChallenges,
      timer, nonNullOrigin, errorCb, successCb, sender.tlsChannelId,
      logMsgUrl);
  var requestToken = signRequestQueue.queueRequest(firstAppId, nonNullOrigin,
      queuedSignRequest.begin.bind(queuedSignRequest), timer);
  queuedSignRequest.setToken(requestToken);
  return queuedSignRequest;
}

/**
 * Returns whether the request appears to be a valid sign request.
 * @param {Object} request The request.
 * @param {string} signChallengesName The name of the sign challenges value in
 *     the request.
 * @return {boolean} Whether the request appears valid.
 */
function isValidSignRequest(request, signChallengesName) {
  if (!request.hasOwnProperty(signChallengesName))
    return false;
  var signChallenges = request[signChallengesName];
  // If a sign request contains an empty array of challenges, it could never
  // be fulfilled. Fail.
  if (!signChallenges.length)
    return false;
  return isValidSignChallengeArray(signChallenges);
}

/**
 * Adapter class representing a queued sign request.
 * @param {!Array.<SignChallenge>} signChallenges The sign challenges.
 * @param {Countdown} timer Timeout timer
 * @param {string} origin Signature origin
 * @param {function(ErrorCodes)} errorCb Error callback
 * @param {function(SignChallenge, string, string)} successCb Success callback
 * @param {string|undefined} opt_tlsChannelId TLS Channel Id
 * @param {string|undefined} opt_logMsgUrl Url to post log messages to
 * @constructor
 * @implements {Closeable}
 */
function QueuedSignRequest(signChallenges, timer, origin, errorCb,
    successCb, opt_tlsChannelId, opt_logMsgUrl) {
  /** @private {!Array.<SignChallenge>} */
  this.signChallenges_ = signChallenges;
  /** @private {Countdown} */
  this.timer_ = timer;
  /** @private {string} */
  this.origin_ = origin;
  /** @private {function(ErrorCodes)} */
  this.errorCb_ = errorCb;
  /** @private {function(SignChallenge, string, string)} */
  this.successCb_ = successCb;
  /** @private {string|undefined} */
  this.tlsChannelId_ = opt_tlsChannelId;
  /** @private {string|undefined} */
  this.logMsgUrl_ = opt_logMsgUrl;
  /** @private {boolean} */
  this.begun_ = false;
  /** @private {boolean} */
  this.closed_ = false;
}

/** Closes this sign request. */
QueuedSignRequest.prototype.close = function() {
  if (this.closed_) return;
  if (this.begun_ && this.signer_) {
    this.signer_.close();
  }
  if (this.token_) {
    this.token_.complete();
  }
  this.closed_ = true;
};

/**
 * @param {QueuedRequestToken} token Token for this sign request.
 */
QueuedSignRequest.prototype.setToken = function(token) {
  /** @private {QueuedRequestToken} */
  this.token_ = token;
};

/**
 * Called when this sign request may begin work.
 * @param {QueuedRequestToken} token Token for this sign request.
 */
QueuedSignRequest.prototype.begin = function(token) {
  this.begun_ = true;
  this.setToken(token);
  this.signer_ = new Signer(this.timer_, this.origin_,
      this.signerFailed_.bind(this), this.signerSucceeded_.bind(this),
      this.tlsChannelId_, this.logMsgUrl_);
  if (!this.signer_.setChallenges(this.signChallenges_)) {
    token.complete();
    this.errorCb_(ErrorCodes.BAD_REQUEST);
  }
};

/**
 * Called when this request's signer fails.
 * @param {ErrorCodes} code The failure code reported by the signer.
 * @private
 */
QueuedSignRequest.prototype.signerFailed_ = function(code) {
  this.token_.complete();
  this.errorCb_(code);
};

/**
 * Called when this request's signer succeeds.
 * @param {SignChallenge} challenge The challenge that was signed.
 * @param {string} info The sign result.
 * @param {string} browserData Browser data JSON
 * @private
 */
QueuedSignRequest.prototype.signerSucceeded_ =
    function(challenge, info, browserData) {
  this.token_.complete();
  this.successCb_(challenge, info, browserData);
};

/**
 * Creates an object to track signing with a gnubby.
 * @param {Countdown} timer Timer for sign request.
 * @param {string} origin The origin making the request.
 * @param {function(ErrorCodes)} errorCb Called when the sign operation fails.
 * @param {function(SignChallenge, string, string)} successCb Called when the
 *     sign operation succeeds.
 * @param {string=} opt_tlsChannelId the TLS channel ID, if any, of the origin
 *     making the request.
 * @param {string=} opt_logMsgUrl The url to post log messages to.
 * @constructor
 */
function Signer(timer, origin, errorCb, successCb,
    opt_tlsChannelId, opt_logMsgUrl) {
  /** @private {Countdown} */
  this.timer_ = timer;
  /** @private {string} */
  this.origin_ = origin;
  /** @private {function(ErrorCodes)} */
  this.errorCb_ = errorCb;
  /** @private {function(SignChallenge, string, string)} */
  this.successCb_ = successCb;
  /** @private {string|undefined} */
  this.tlsChannelId_ = opt_tlsChannelId;
  /** @private {string|undefined} */
  this.logMsgUrl_ = opt_logMsgUrl;

  /** @private {boolean} */
  this.challengesSet_ = false;
  /** @private {boolean} */
  this.done_ = false;

  /** @private {Object.<string, string>} */
  this.browserData_ = {};
  /** @private {Object.<string, SignChallenge>} */
  this.serverChallenges_ = {};
  // Allow http appIds for http origins. (Broken, but the caller deserves
  // what they get.)
  /** @private {boolean} */
  this.allowHttp_ = this.origin_ ? this.origin_.indexOf('http://') == 0 : false;
  /** @private {Closeable} */
  this.handler_ = null;
}

/**
 * Sets the challenges to be signed.
 * @param {Array.<SignChallenge>} signChallenges The challenges to set.
 * @return {boolean} Whether the challenges could be set.
 */
Signer.prototype.setChallenges = function(signChallenges) {
  if (this.challengesSet_ || this.done_)
    return false;
  /** @private {Array.<SignChallenge>} */
  this.signChallenges_ = signChallenges;
  /** @private {boolean} */
  this.challengesSet_ = true;

  this.checkAppIds_();
  return true;
};

/**
 * Checks the app ids of incoming requests.
 * @private
 */
Signer.prototype.checkAppIds_ = function() {
  var appIds = getDistinctAppIds(this.signChallenges_);
  if (!appIds || !appIds.length) {
    this.notifyError_(ErrorCodes.BAD_REQUEST);
    return;
  }
  FACTORY_REGISTRY.getOriginChecker().canClaimAppIds(this.origin_, appIds)
      .then(this.originChecked_.bind(this, appIds));
};

/**
 * Called with the result of checking the origin. When the origin is allowed
 * to claim the app ids, begins checking whether the app ids also list the
 * origin.
 * @param {!Array.<string>} appIds The app ids.
 * @param {boolean} result Whether the origin could claim the app ids.
 * @private
 */
Signer.prototype.originChecked_ = function(appIds, result) {
  if (!result) {
    this.notifyError_(ErrorCodes.BAD_REQUEST);
    return;
  }
  /** @private {!AppIdChecker} */
  this.appIdChecker_ = new AppIdChecker(FACTORY_REGISTRY.getTextFetcher(),
      this.timer_.clone(), this.origin_,
      /** @type {!Array.<string>} */ (appIds), this.allowHttp_,
      this.logMsgUrl_);
  this.appIdChecker_.doCheck().then(this.appIdChecked_.bind(this));
};

/**
 * Called with the result of checking app ids.  When the app ids are valid,
 * adds the sign challenges to those being signed.
 * @param {boolean} result Whether the app ids are valid.
 * @private
 */
Signer.prototype.appIdChecked_ = function(result) {
  if (!result) {
    this.notifyError_(ErrorCodes.BAD_REQUEST);
    return;
  }
  if (!this.doSign_()) {
    this.notifyError_(ErrorCodes.BAD_REQUEST);
    return;
  }
};

/**
 * Begins signing this signer's challenges.
 * @return {boolean} Whether the challenge could be added.
 * @private
 */
Signer.prototype.doSign_ = function() {
  // Create the browser data for each challenge.
  for (var i = 0; i < this.signChallenges_.length; i++) {
    var challenge = this.signChallenges_[i];
    var serverChallenge = challenge['challenge'];
    var keyHandle = challenge['keyHandle'];

    var browserData =
        makeSignBrowserData(serverChallenge, this.origin_, this.tlsChannelId_);
    this.browserData_[keyHandle] = browserData;
    this.serverChallenges_[keyHandle] = challenge;
  }

  var encodedChallenges = encodeSignChallenges(this.signChallenges_,
      this.getChallengeHash_.bind(this));

  var timeoutSeconds = this.timer_.millisecondsUntilExpired() / 1000.0;
  var request = makeSignHelperRequest(encodedChallenges, timeoutSeconds,
      this.logMsgUrl_);
  this.handler_ =
      FACTORY_REGISTRY.getRequestHelper()
          .getHandler(/** @type {HelperRequest} */ (request));
  if (!this.handler_)
    return false;
  return this.handler_.run(this.helperComplete_.bind(this));
};

/**
 * @param {string} keyHandle The key handle used with the challenge.
 * @param {string} challenge The challenge.
 * @return {string} The hashed challenge associated with the key
 *     handle/challenge pair.
 * @private
 */
Signer.prototype.getChallengeHash_ = function(keyHandle, challenge) {
  return B64_encode(sha256HashOfString(this.browserData_[keyHandle]));
};

/** Closes this signer. */
Signer.prototype.close = function() {
  if (this.appIdChecker_) {
    this.appIdChecker_.close();
  }
  if (this.handler_) {
    this.handler_.close();
    this.handler_ = null;
  }
  this.timer_.clearTimeout();
};

/**
 * Notifies the caller of error with the given error code.
 * @param {ErrorCodes} code Error code
 * @private
 */
Signer.prototype.notifyError_ = function(code) {
  if (this.done_)
    return;
  this.close();
  this.done_ = true;
  this.errorCb_(code);
};

/**
 * Notifies the caller of success.
 * @param {SignChallenge} challenge The challenge that was signed.
 * @param {string} info The sign result.
 * @param {string} browserData Browser data JSON
 * @private
 */
Signer.prototype.notifySuccess_ = function(challenge, info, browserData) {
  if (this.done_)
    return;
  this.close();
  this.done_ = true;
  this.successCb_(challenge, info, browserData);
};

/**
 * Called by the helper upon completion.
 * @param {HelperReply} helperReply The result of the sign request.
 * @param {string=} opt_source The source of the sign result.
 * @private
 */
Signer.prototype.helperComplete_ = function(helperReply, opt_source) {
  if (helperReply.type != 'sign_helper_reply') {
    this.notifyError_(ErrorCodes.OTHER_ERROR);
    return;
  }
  var reply = /** @type {SignHelperReply} */ (helperReply);

  if (reply.code) {
    var reportedError = mapDeviceStatusCodeToErrorCode(reply.code);
    console.log(UTIL_fmt('helper reported ' + reply.code.toString(16) +
        ', returning ' + reportedError));
    this.notifyError_(reportedError);
  } else {
    if (this.logMsgUrl_ && opt_source) {
      var logMsg = 'signed&source=' + opt_source;
      logMessage(logMsg, this.logMsgUrl_);
    }

    var key = reply.responseData['keyHandle'];
    var browserData = this.browserData_[key];
    // Notify with server-provided challenge, not the encoded one: the
    // server-provided challenge contains additional fields it relies on.
    var serverChallenge = this.serverChallenges_[key];
    this.notifySuccess_(serverChallenge, reply.responseData.signatureData,
        browserData);
  }
};
