// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Does common handling for requests coming from web pages and
 * routes them to the provided handler.
 */

/**
 * Gets the scheme + origin from a web url.
 * @param {string} url Input url
 * @return {?string} Scheme and origin part if url parses
 */
function getOriginFromUrl(url) {
  var re = new RegExp('^(https?://)[^/]*/?');
  var originarray = re.exec(url);
  if (originarray == null) return originarray;
  var origin = originarray[0];
  while (origin.charAt(origin.length - 1) == '/') {
    origin = origin.substring(0, origin.length - 1);
  }
  if (origin == 'http:' || origin == 'https:')
    return null;
  return origin;
}

/**
 * Returns whether the array of SignChallenges appears to be valid.
 * @param {Array.<SignChallenge>} signChallenges The array of sign challenges.
 * @return {boolean} Whether the array appears valid.
 */
function isValidSignChallengeArray(signChallenges) {
  for (var i = 0; i < signChallenges.length; i++) {
    var incomingChallenge = signChallenges[i];
    if (!incomingChallenge.hasOwnProperty('challenge'))
      return false;
    if (!incomingChallenge.hasOwnProperty('appId')) {
      return false;
    }
    if (!incomingChallenge.hasOwnProperty('keyHandle'))
      return false;
    if (incomingChallenge['version']) {
      if (incomingChallenge['version'] != 'U2F_V1' &&
          incomingChallenge['version'] != 'U2F_V2') {
        return false;
      }
    }
  }
  return true;
}

/** Posts the log message to the log url.
 * @param {string} logMsg the log message to post.
 * @param {string=} opt_logMsgUrl the url to post log messages to.
 */
function logMessage(logMsg, opt_logMsgUrl) {
  console.log(UTIL_fmt('logMessage("' + logMsg + '")'));

  if (!opt_logMsgUrl) {
    return;
  }
  // Image fetching is not allowed per packaged app CSP.
  // But video and audio is.
  var audio = new Audio();
  audio.src = opt_logMsgUrl + logMsg;
}

/**
 * Makes a response to a request.
 * @param {Object} request The request to make a response to.
 * @param {string} responseSuffix How to name the response's type.
 * @param {string=} opt_defaultType The default response type, if none is
 *     present in the request.
 * @return {Object} The response object.
 */
function makeResponseForRequest(request, responseSuffix, opt_defaultType) {
  var type;
  if (request && request.type) {
    type = request.type.replace(/_request$/, responseSuffix);
  } else {
    type = opt_defaultType;
  }
  var reply = { 'type': type };
  if (request && request.requestId) {
    reply.requestId = request.requestId;
  }
  return reply;
}

/**
 * Makes a response to a U2F request with an error code.
 * @param {Object} request The request to make a response to.
 * @param {ErrorCodes} code The error code to return.
 * @param {string=} opt_detail An error detail string.
 * @param {string=} opt_defaultType The default response type, if none is
 *     present in the request.
 * @return {Object} The U2F error.
 */
function makeU2fErrorResponse(request, code, opt_detail, opt_defaultType) {
  var reply = makeResponseForRequest(request, '_response', opt_defaultType);
  var error = {'errorCode': code};
  if (opt_detail) {
    error['errorMessage'] = opt_detail;
  }
  reply['responseData'] = error;
  return reply;
}

/**
 * Makes a success response to a web request with a responseData object.
 * @param {Object} request The request to make a response to.
 * @param {Object} responseData The response data.
 * @return {Object} The web error.
 */
function makeU2fSuccessResponse(request, responseData) {
  var reply = makeResponseForRequest(request, '_response');
  reply['responseData'] = responseData;
  return reply;
}

/**
 * Makes a response to a web request with an error code.
 * @param {Object} request The request to make a response to.
 * @param {GnubbyCodeTypes} code The error code to return.
 * @param {string=} opt_defaultType The default response type, if none is
 *     present in the request.
 * @return {Object} The web error.
 */
function makeWebErrorResponse(request, code, opt_defaultType) {
  var reply = makeResponseForRequest(request, '_reply', opt_defaultType);
  reply['code'] = code;
  return reply;
}

/**
 * Makes a success response to a web request with a responseData object.
 * @param {Object} request The request to make a response to.
 * @param {Object} responseData The response data.
 * @return {Object} The web error.
 */
function makeWebSuccessResponse(request, responseData) {
  var reply = makeResponseForRequest(request, '_reply');
  reply['code'] = GnubbyCodeTypes.OK;
  reply['responseData'] = responseData;
  return reply;
}

/**
 * Maps an error code from the ErrorCodes namespace to the GnubbyCodeTypes
 * namespace.
 * @param {ErrorCodes} errorCode Error in the ErrorCodes namespace.
 * @param {boolean} forSign Whether the error is for a sign request.
 * @return {GnubbyCodeTypes} Error code in the GnubbyCodeTypes namespace.
 */
function mapErrorCodeToGnubbyCodeType(errorCode, forSign) {
  var code;
  switch (errorCode) {
    case ErrorCodes.BAD_REQUEST:
      return GnubbyCodeTypes.BAD_REQUEST;

    case ErrorCodes.DEVICE_INELIGIBLE:
      return forSign ? GnubbyCodeTypes.NONE_PLUGGED_ENROLLED :
          GnubbyCodeTypes.ALREADY_ENROLLED;

    case ErrorCodes.TIMEOUT:
      return GnubbyCodeTypes.WAIT_TOUCH;
  }
  return GnubbyCodeTypes.UNKNOWN_ERROR;
}

/**
 * Maps a helper's error code from the DeviceStatusCodes namespace to the
 * ErrorCodes namespace.
 * @param {number} code Error code from DeviceStatusCodes namespace.
 * @return {ErrorCodes} A ErrorCodes error code.
 */
function mapDeviceStatusCodeToErrorCode(code) {
  var reportedError = ErrorCodes.OTHER_ERROR;
  switch (code) {
    case DeviceStatusCodes.WRONG_DATA_STATUS:
      reportedError = ErrorCodes.DEVICE_INELIGIBLE;
      break;

    case DeviceStatusCodes.TIMEOUT_STATUS:
    case DeviceStatusCodes.WAIT_TOUCH_STATUS:
      reportedError = ErrorCodes.TIMEOUT;
      break;
  }
  return reportedError;
}

/**
 * Sends a response, using the given sentinel to ensure at most one response is
 * sent. Also closes the closeable, if it's given.
 * @param {boolean} sentResponse Whether a response has already been sent.
 * @param {?Closeable} closeable A thing to close.
 * @param {*} response The response to send.
 * @param {Function} sendResponse A function to send the response.
 */
function sendResponseOnce(sentResponse, closeable, response, sendResponse) {
  if (closeable) {
    closeable.close();
  }
  if (!sentResponse) {
    sentResponse = true;
    try {
      // If the page has gone away or the connection has otherwise gone,
      // sendResponse fails.
      sendResponse(response);
    } catch (exception) {
      console.warn('sendResponse failed: ' + exception);
    }
  } else {
    console.warn(UTIL_fmt('Tried to reply more than once! Juan, FIX ME'));
  }
}

/**
 * @param {!string} string Input string
 * @return {Array.<number>} SHA256 hash value of string.
 */
function sha256HashOfString(string) {
  var s = new SHA256();
  s.update(UTIL_StringToBytes(string));
  return s.digest();
}

/**
 * Normalizes the TLS channel ID value:
 * 1. Converts semantically empty values (undefined, null, 0) to the empty
 *     string.
 * 2. Converts valid JSON strings to a JS object.
 * 3. Otherwise, returns the input value unmodified.
 * @param {Object|string|undefined} opt_tlsChannelId TLS Channel id
 * @return {Object|string} The normalized TLS channel ID value.
 */
function tlsChannelIdValue(opt_tlsChannelId) {
  if (!opt_tlsChannelId) {
    // Case 1: Always set some value for  TLS channel ID, even if it's the empty
    // string: this browser definitely supports them.
    return '';
  }
  if (typeof opt_tlsChannelId === 'string') {
    try {
      var obj = JSON.parse(opt_tlsChannelId);
      if (!obj) {
        // Case 1: The string value 'null' parses as the Javascript object null,
        // so return an empty string: the browser definitely supports TLS
        // channel id.
        return '';
      }
      // Case 2: return the value as a JS object.
      return /** @type {Object} */ (obj);
    } catch (e) {
      console.warn('Unparseable TLS channel ID value ' + opt_tlsChannelId);
      // Case 3: return the value unmodified.
    }
  }
  return opt_tlsChannelId;
}

/**
 * Creates a browser data object with the given values.
 * @param {!string} type A string representing the "type" of this browser data
 *     object.
 * @param {!string} serverChallenge The server's challenge, as a base64-
 *     encoded string.
 * @param {!string} origin The server's origin, as seen by the browser.
 * @param {Object|string|undefined} opt_tlsChannelId TLS Channel Id
 * @return {string} A string representation of the browser data object.
 */
function makeBrowserData(type, serverChallenge, origin, opt_tlsChannelId) {
  var browserData = {
    'typ' : type,
    'challenge' : serverChallenge,
    'origin' : origin
  };
  browserData['cid_pubkey'] = tlsChannelIdValue(opt_tlsChannelId);
  return JSON.stringify(browserData);
}

/**
 * Creates a browser data object for an enroll request with the given values.
 * @param {!string} serverChallenge The server's challenge, as a base64-
 *     encoded string.
 * @param {!string} origin The server's origin, as seen by the browser.
 * @param {Object|string|undefined} opt_tlsChannelId TLS Channel Id
 * @return {string} A string representation of the browser data object.
 */
function makeEnrollBrowserData(serverChallenge, origin, opt_tlsChannelId) {
  return makeBrowserData(
      'navigator.id.finishEnrollment', serverChallenge, origin,
      opt_tlsChannelId);
}

/**
 * Creates a browser data object for a sign request with the given values.
 * @param {!string} serverChallenge The server's challenge, as a base64-
 *     encoded string.
 * @param {!string} origin The server's origin, as seen by the browser.
 * @param {Object|string|undefined} opt_tlsChannelId TLS Channel Id
 * @return {string} A string representation of the browser data object.
 */
function makeSignBrowserData(serverChallenge, origin, opt_tlsChannelId) {
  return makeBrowserData(
      'navigator.id.getAssertion', serverChallenge, origin, opt_tlsChannelId);
}

/**
 * Encodes the sign data as an array of sign helper challenges.
 * @param {Array.<SignChallenge>} signChallenges The sign challenges to encode.
 * @param {function(string, string): string=} opt_challengeHashFunction
 *     A function that produces, from a key handle and a raw challenge, a hash
 *     of the raw challenge. If none is provided, a default hash function is
 *     used.
 * @return {!Array.<SignHelperChallenge>} The sign challenges, encoded.
 */
function encodeSignChallenges(signChallenges, opt_challengeHashFunction) {
  function encodedSha256(keyHandle, challenge) {
    return B64_encode(sha256HashOfString(challenge));
  }
  var challengeHashFn = opt_challengeHashFunction || encodedSha256;
  var encodedSignChallenges = [];
  if (signChallenges) {
    for (var i = 0; i < signChallenges.length; i++) {
      var challenge = signChallenges[i];
      var challengeHash =
          challengeHashFn(challenge['keyHandle'], challenge['challenge']);
      var encodedChallenge = {
        'challengeHash': challengeHash,
        'appIdHash': B64_encode(sha256HashOfString(challenge['appId'])),
        'keyHandle': challenge['keyHandle'],
        'version': (challenge['version'] || 'U2F_V1')
      };
      encodedSignChallenges.push(encodedChallenge);
    }
  }
  return encodedSignChallenges;
}

/**
 * Makes a sign helper request from an array of challenges.
 * @param {Array.<SignHelperChallenge>} challenges The sign challenges.
 * @param {number=} opt_timeoutSeconds Timeout value.
 * @param {string=} opt_logMsgUrl URL to log to.
 * @return {SignHelperRequest} The sign helper request.
 */
function makeSignHelperRequest(challenges, opt_timeoutSeconds, opt_logMsgUrl) {
  var request = {
    'type': 'sign_helper_request',
    'signData': challenges,
    'timeout': opt_timeoutSeconds || 0,
    'timeoutSeconds': opt_timeoutSeconds || 0
  };
  if (opt_logMsgUrl !== undefined) {
    request.logMsgUrl = opt_logMsgUrl;
  }
  return request;
}
