// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Handles web page requests for gnubby enrollment.
 */

'use strict';

/**
 * Handles a web enroll request.
 * @param {!RequestHelper} helper Helper for handling requests.
 * @param {!CountdownTimerFactory} timerFactory Factory for creating timers.
 * @param {MessageSender} sender The sender of the message.
 * @param {Object} request The web page's enroll request.
 * @param {Function} sendResponse Called back with the result of the enroll.
 * @return {Closeable} A handler object to be closed when the browser channel
 *     closes.
 */
function handleWebEnrollRequest(helper, timerFactory, sender, request,
    sendResponse) {
  var sentResponse = false;
  var closeable;

  function sendErrorResponse(u2fCode) {
    var response = makeWebErrorResponse(request,
        mapErrorCodeToGnubbyCodeType(u2fCode, false /* forSign */));
    sendResponseOnce(sentResponse, closeable, response, sendResponse);
  }

  function sendSuccessResponse(u2fVersion, info, browserData) {
    var enrollChallenges = request['enrollChallenges'];
    var enrollChallenge =
        findEnrollChallengeOfVersion(enrollChallenges, u2fVersion);
    if (!enrollChallenge) {
      sendErrorResponse(ErrorCodes.OTHER_ERROR);
      return;
    }
    var responseData =
        makeEnrollResponseData(enrollChallenge, u2fVersion,
            'enrollData', info, 'browserData', browserData);
    var response = makeWebSuccessResponse(request, responseData);
    sendResponseOnce(sentResponse, closeable, response, sendResponse);
  }

  closeable =
      validateAndBeginEnrollRequest(helper, timerFactory, sender, request,
      'enrollChallenges', 'signData', sendErrorResponse, sendSuccessResponse);
  return closeable;
}

/**
 * Handles a U2F enroll request.
 * @param {!RequestHelper} helper Helper for handling requests.
 * @param {!CountdownTimerFactory} timerFactory Factory for creating timers.
 * @param {MessageSender} sender The sender of the message.
 * @param {Object} request The web page's enroll request.
 * @param {Function} sendResponse Called back with the result of the enroll.
 * @return {Closeable} A handler object to be closed when the browser channel
 *     closes.
 */
function handleU2fEnrollRequest(helper, timerFactory, sender, request,
    sendResponse) {
  var sentResponse = false;
  var closeable;

  function sendErrorResponse(u2fCode) {
    var response = makeU2fErrorResponse(request, u2fCode);
    sendResponseOnce(sentResponse, closeable, response, sendResponse);
  }

  function sendSuccessResponse(u2fVersion, info, browserData) {
    var enrollChallenges = request['registerRequests'];
    var enrollChallenge =
        findEnrollChallengeOfVersion(enrollChallenges, u2fVersion);
    if (!enrollChallenge) {
      sendErrorResponse(ErrorCodes.OTHER_ERROR);
      return;
    }
    var responseData =
        makeEnrollResponseData(enrollChallenge, u2fVersion,
            'registrationData', info, 'clientData', browserData);
    var response = makeU2fSuccessResponse(request, responseData);
    sendResponseOnce(sentResponse, closeable, response, sendResponse);
  }

  closeable =
      validateAndBeginEnrollRequest(helper, timerFactory, sender, request,
      'registerRequests', 'signRequests', sendErrorResponse,
      sendSuccessResponse);
  return closeable;
}

/**
 * Validates an enroll request using the given parameters, and, if valid, begins
 * handling the enroll request.
 * @param {!RequestHelper} helper Helper for handling requests.
 * @param {!CountdownTimerFactory} timerFactory Factory for creating timers.
 * @param {MessageSender} sender The sender of the message.
 * @param {Object} request The web page's enroll request.
 * @param {string} enrollChallengesName The name of the enroll challenges value
 *     in the request.
 * @param {string} signChallengesName The name of the sign challenges value in
 *     the request.
 * @param {function(ErrorCodes)} errorCb Error callback.
 * @param {function(string, string, (string|undefined))} successCb Success
 *     callback.
 * @return {Closeable} Request handler that should be closed when the browser
 *     message channel is closed.
 */
function validateAndBeginEnrollRequest(helper, timerFactory, sender, request,
    enrollChallengesName, signChallengesName, errorCb, successCb) {
  var origin = getOriginFromUrl(/** @type {string} */ (sender.url));
  if (!origin) {
    errorCb(ErrorCodes.BAD_REQUEST);
    return null;
  }

  if (!isValidEnrollRequest(request, enrollChallengesName,
      signChallengesName)) {
    errorCb(ErrorCodes.BAD_REQUEST);
    return null;
  }

  var enrollChallenges = request[enrollChallengesName];
  var signChallenges = request[signChallengesName];
  var logMsgUrl = request['logMsgUrl'];

  var timer = createTimerForRequest(timerFactory, request);
  var enroller = new Enroller(helper, timer, origin, errorCb, successCb,
      sender.tlsChannelId, logMsgUrl);
  enroller.doEnroll(enrollChallenges, signChallenges);
  return /** @type {Closeable} */ (enroller);
}

/**
 * Returns whether the request appears to be a valid enroll request.
 * @param {Object} request The request.
 * @param {string} enrollChallengesName The name of the enroll challenges value
 *     in the request.
 * @param {string} signChallengesName The name of the sign challenges value in
 *     the request.
 * @return {boolean} Whether the request appears valid.
 */
function isValidEnrollRequest(request, enrollChallengesName,
    signChallengesName) {
  if (!request.hasOwnProperty(enrollChallengesName))
    return false;
  var enrollChallenges = request[enrollChallengesName];
  if (!enrollChallenges.length)
    return false;
  if (!isValidEnrollChallengeArray(enrollChallenges))
    return false;
  var signChallenges = request[signChallengesName];
  // A missing sign challenge array is ok, in the case the user is not already
  // enrolled.
  if (signChallenges && !isValidSignChallengeArray(signChallenges))
    return false;
  return true;
}

/**
 * @typedef {{
 *   version: (string|undefined),
 *   challenge: string,
 *   appId: string
 * }}
 */
var EnrollChallenge;

/**
 * @param {Array.<EnrollChallenge>} enrollChallenges The enroll challenges to
 *     validate.
 * @return {boolean} Whether the given array of challenges is a valid enroll
 *     challenges array.
 */
function isValidEnrollChallengeArray(enrollChallenges) {
  var seenVersions = {};
  for (var i = 0; i < enrollChallenges.length; i++) {
    var enrollChallenge = enrollChallenges[i];
    var version = enrollChallenge['version'];
    if (!version) {
      // Version is implicitly V1 if not specified.
      version = 'U2F_V1';
    }
    if (version != 'U2F_V1' && version != 'U2F_V2') {
      return false;
    }
    if (seenVersions[version]) {
      // Each version can appear at most once.
      return false;
    }
    seenVersions[version] = version;
    if (!enrollChallenge['appId']) {
      return false;
    }
    if (!enrollChallenge['challenge']) {
      // The challenge is required.
      return false;
    }
  }
  return true;
}

/**
 * Finds the enroll challenge of the given version in the enroll challlenge
 * array.
 * @param {Array.<EnrollChallenge>} enrollChallenges The enroll challenges to
 *     search.
 * @param {string} version Version to search for.
 * @return {?EnrollChallenge} The enroll challenge with the given versions, or
 *     null if it isn't found.
 */
function findEnrollChallengeOfVersion(enrollChallenges, version) {
  for (var i = 0; i < enrollChallenges.length; i++) {
    if (enrollChallenges[i]['version'] == version) {
      return enrollChallenges[i];
    }
  }
  return null;
}

/**
 * Makes a responseData object for the enroll request with the given parameters.
 * @param {EnrollChallenge} enrollChallenge The enroll challenge used to
 *     register.
 * @param {string} u2fVersion Version of gnubby that enrolled.
 * @param {string} enrollDataName The name of the enroll data key in the
 *     responseData object.
 * @param {string} enrollData The enroll data.
 * @param {string} browserDataName The name of the browser data key in the
 *     responseData object.
 * @param {string=} browserData The browser data, if available.
 * @return {Object} The responseData object.
 */
function makeEnrollResponseData(enrollChallenge, u2fVersion, enrollDataName,
    enrollData, browserDataName, browserData) {
  var responseData = {};
  responseData[enrollDataName] = enrollData;
  // Echo the used challenge back in the reply.
  for (var k in enrollChallenge) {
    responseData[k] = enrollChallenge[k];
  }
  if (u2fVersion == 'U2F_V2') {
    // For U2F_V2, the challenge sent to the gnubby is modified to be the
    // hash of the browser data. Include the browser data.
    responseData[browserDataName] = browserData;
  }
  return responseData;
}

/**
 * Creates a new object to track enrolling with a gnubby.
 * @param {!RequestHelper} helper Helper for handling requests.
 * @param {!Countdown} timer Timer for enroll request.
 * @param {string} origin The origin making the request.
 * @param {function(ErrorCodes)} errorCb Called upon enroll failure with an
 *     error code.
 * @param {function(string, string, (string|undefined))} successCb Called upon
 *     enroll success with the version of the succeeding gnubby, the enroll
 *     data, and optionally the browser data associated with the enrollment.
 * @param {string=} opt_tlsChannelId the TLS channel ID, if any, of the origin
 *     making the request.
 * @param {string=} opt_logMsgUrl The url to post log messages to.
 * @constructor
 */
function Enroller(helper, timer, origin, errorCb, successCb, opt_tlsChannelId,
    opt_logMsgUrl) {
  /** @private {RequestHelper} */
  this.helper_ = helper;
  /** @private {Countdown} */
  this.timer_ = timer;
  /** @private {string} */
  this.origin_ = origin;
  /** @private {function(ErrorCodes)} */
  this.errorCb_ = errorCb;
  /** @private {function(string, string, (string|undefined))} */
  this.successCb_ = successCb;
  /** @private {string|undefined} */
  this.tlsChannelId_ = opt_tlsChannelId;
  /** @private {string|undefined} */
  this.logMsgUrl_ = opt_logMsgUrl;

  /** @private {boolean} */
  this.done_ = false;

  /** @private {Object.<string, string>} */
  this.browserData_ = {};
  /** @private {Array.<EnrollHelperChallenge>} */
  this.encodedEnrollChallenges_ = [];
  /** @private {Array.<SignHelperChallenge>} */
  this.encodedSignChallenges_ = [];
  // Allow http appIds for http origins. (Broken, but the caller deserves
  // what they get.)
  /** @private {boolean} */
  this.allowHttp_ = this.origin_ ? this.origin_.indexOf('http://') == 0 : false;
  /** @private {Closeable} */
  this.handler_ = null;
}

/**
 * Default timeout value in case the caller never provides a valid timeout.
 */
Enroller.DEFAULT_TIMEOUT_MILLIS = 30 * 1000;

/**
 * Performs an enroll request with the given enroll and sign challenges.
 * @param {Array.<EnrollChallenge>} enrollChallenges A set of enroll challenges.
 * @param {Array.<SignChallenge>} signChallenges A set of sign challenges for
 *     existing enrollments for this user and appId.
 */
Enroller.prototype.doEnroll = function(enrollChallenges, signChallenges) {
  var encodedEnrollChallenges = this.encodeEnrollChallenges_(enrollChallenges);
  var encodedSignChallenges = encodeSignChallenges(signChallenges);
  var request = {
    type: 'enroll_helper_request',
    enrollChallenges: encodedEnrollChallenges,
    signData: encodedSignChallenges,
    logMsgUrl: this.logMsgUrl_
  };
  if (!this.timer_.expired()) {
    request.timeout = this.timer_.millisecondsUntilExpired() / 1000.0;
    request.timeoutSeconds = this.timer_.millisecondsUntilExpired() / 1000.0;
  }

  // Begin fetching/checking the app ids.
  var enrollAppIds = [];
  for (var i = 0; i < enrollChallenges.length; i++) {
    enrollAppIds.push(enrollChallenges[i]['appId']);
  }
  var self = this;
  this.checkAppIds_(enrollAppIds, signChallenges, function(result) {
    if (result) {
      self.handler_ = self.helper_.getHandler(request);
      if (self.handler_) {
        var helperComplete =
            /** @type {function(HelperReply)} */
            (self.helperComplete_.bind(self));
        self.handler_.run(helperComplete);
      } else {
        self.notifyError_(ErrorCodes.OTHER_ERROR);
      }
    } else {
      self.notifyError_(ErrorCodes.BAD_REQUEST);
    }
  });
};

/**
 * Encodes the enroll challenge as an enroll helper challenge.
 * @param {EnrollChallenge} enrollChallenge The enroll challenge to encode.
 * @return {EnrollHelperChallenge} The encoded challenge.
 * @private
 */
Enroller.encodeEnrollChallenge_ = function(enrollChallenge) {
  var encodedChallenge = {};
  var version;
  if (enrollChallenge['version']) {
    version = enrollChallenge['version'];
  } else {
    // Version is implicitly V1 if not specified.
    version = 'U2F_V1';
  }
  encodedChallenge['version'] = version;
  encodedChallenge['challenge'] = enrollChallenge['challenge'];
  encodedChallenge['appIdHash'] =
      B64_encode(sha256HashOfString(enrollChallenge['appId']));
  return /** @type {EnrollHelperChallenge} */ (encodedChallenge);
};

/**
 * Encodes the given enroll challenges using this enroller's state.
 * @param {Array.<EnrollChallenge>} enrollChallenges The enroll challenges.
 * @return {!Array.<EnrollHelperChallenge>} The encoded enroll challenges.
 * @private
 */
Enroller.prototype.encodeEnrollChallenges_ = function(enrollChallenges) {
  var challenges = [];
  for (var i = 0; i < enrollChallenges.length; i++) {
    var enrollChallenge = enrollChallenges[i];
    var version = enrollChallenge.version;
    if (!version) {
      // Version is implicitly V1 if not specified.
      version = 'U2F_V1';
    }

    if (version == 'U2F_V2') {
      var modifiedChallenge = {};
      for (var k in enrollChallenge) {
        modifiedChallenge[k] = enrollChallenge[k];
      }
      // V2 enroll responses contain signatures over a browser data object,
      // which we're constructing here. The browser data object contains, among
      // other things, the server challenge.
      var serverChallenge = enrollChallenge['challenge'];
      var browserData = makeEnrollBrowserData(
          serverChallenge, this.origin_, this.tlsChannelId_);
      // Replace the challenge with the hash of the browser data.
      modifiedChallenge['challenge'] =
          B64_encode(sha256HashOfString(browserData));
      this.browserData_[version] =
          B64_encode(UTIL_StringToBytes(browserData));
      challenges.push(Enroller.encodeEnrollChallenge_(
          /** @type {EnrollChallenge} */ (modifiedChallenge)));
    } else {
      challenges.push(Enroller.encodeEnrollChallenge_(enrollChallenge));
    }
  }
  return challenges;
};

/**
 * Checks the app ids associated with this enroll request, and calls a callback
 * with the result of the check.
 * @param {!Array.<string>} enrollAppIds The app ids in the enroll challenge
 *     portion of the enroll request.
 * @param {Array.<SignChallenge>} signChallenges The sign challenges associated
 *     with the request.
 * @param {function(boolean)} cb Called with the result of the check.
 * @private
 */
Enroller.prototype.checkAppIds_ = function(enrollAppIds, signChallenges, cb) {
  var distinctAppIds =
      UTIL_unionArrays(enrollAppIds, getDistinctAppIds(signChallenges));
  /** @private {!AppIdChecker} */
  this.appIdChecker_ = new AppIdChecker(this.timer_.clone(), this.origin_,
      distinctAppIds, this.allowHttp_, this.logMsgUrl_);
  this.appIdChecker_.doCheck(cb);
};

/** Closes this enroller. */
Enroller.prototype.close = function() {
  if (this.appIdChecker_) {
    this.appIdChecker_.close();
  }
  if (this.handler_) {
    this.handler_.close();
  }
};

/**
 * Notifies the caller with the error code.
 * @param {ErrorCodes} code Error code
 * @private
 */
Enroller.prototype.notifyError_ = function(code) {
  if (this.done_)
    return;
  this.close();
  this.done_ = true;
  this.errorCb_(code);
};

/**
 * Notifies the caller of success with the provided response data.
 * @param {string} u2fVersion Protocol version
 * @param {string} info Response data
 * @param {string|undefined} opt_browserData Browser data used
 * @private
 */
Enroller.prototype.notifySuccess_ =
    function(u2fVersion, info, opt_browserData) {
  if (this.done_)
    return;
  this.close();
  this.done_ = true;
  this.successCb_(u2fVersion, info, opt_browserData);
};

/**
 * Called by the helper upon completion.
 * @param {EnrollHelperReply} reply The result of the enroll request.
 * @private
 */
Enroller.prototype.helperComplete_ = function(reply) {
  if (reply.code) {
    var reportedError = mapDeviceStatusCodeToErrorCode(reply.code);
    console.log(UTIL_fmt('helper reported ' + reply.code.toString(16) +
        ', returning ' + reportedError));
    this.notifyError_(reportedError);
  } else {
    console.log(UTIL_fmt('Gnubby enrollment succeeded!!!!!'));
    var browserData;

    if (reply.version == 'U2F_V2') {
      // For U2F_V2, the challenge sent to the gnubby is modified to be the hash
      // of the browser data. Include the browser data.
      browserData = this.browserData_[reply.version];
    }

    this.notifySuccess_(/** @type {string} */ (reply.version),
                        /** @type {string} */ (reply.enrollData),
                        browserData);
  }
};
