// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Implements handling of appIds.
 */
'use strict';

/**
 * Parses the text as JSON and returns it as an array of strings.
 * @param {string} text Input JSON
 * @return {!Array.<string>} Array of origins
 */
function getOriginsFromJson(text) {
  try {
    var urls = JSON.parse(text);
    var origins = {};
    for (var i = 0, url; url = urls[i]; i++) {
      var origin = getOriginFromUrl(url);
      if (origin) {
        origins[origin] = origin;
      }
    }
    return Object.keys(origins);
  } catch (e) {
    console.log(UTIL_fmt('could not parse ' + text));
    return [];
  }
}

/**
 * Fetches the app id, and calls a callback with list of allowed origins for it.
 * @param {string} appId The app id to fetch.
 * @param {function(number, Array.<string>=)} cb
 *     Called with the result of the app id fetch, and, if the fetch was
 *     successful, a list of allowed origins for the app id.
 */
function fetchAppId(appId, cb) {
  var origin = getOriginFromUrl(appId);
  if (!origin) {
    cb(404);
    return;
  }
  var xhr = new XMLHttpRequest();
  var origins = [];
  xhr.open('GET', appId, true);
  xhr.onloadend = function() {
    if (xhr.status != 200) {
      cb(xhr.status);
      return;
    }
    cb(xhr.status, getOriginsFromJson(xhr.responseText));
  };
  xhr.send();
}

/**
 * Retrieves a set of distinct app ids from the sign challenges.
 * @param {Array.<SignChallenge>=} signChallenges Input sign challenges.
 * @return {Array.<string>} array of distinct app ids.
 */
function getDistinctAppIds(signChallenges) {
  if (!signChallenges) {
    return [];
  }
  var appIds = {};
  for (var i = 0, request; request = signChallenges[i]; i++) {
    var appId = request['appId'];
    if (appId) {
      appIds[appId] = appId;
    }
  }
  return Object.keys(appIds);
}

/**
 * Checks whether an appId is valid for a given origin.
 * @param {!string} appId Application id
 * @param {!string} origin Origin
 * @param {!Array.<string>} allowedOrigins the list of allowed origins for each
 *    appId.
 * @return {boolean} whether the appId is allowed for the origin.
 */
function isValidAppIdForOrigin(appId, origin, allowedOrigins) {
  if (!appId)
    return false;
  if (appId == origin) {
    // trivially allowed
    return true;
  }
  if (!allowedOrigins)
    return false;
  return allowedOrigins.indexOf(origin) >= 0;
}

/**
 * Provides an object to track checking a list of appIds.
 * @param {!Countdown} timer A timer by which to resolve all provided app ids.
 * @param {string} origin The origin to check.
 * @param {!Array.<string>} appIds The app ids to check.
 * @param {boolean} allowHttp Whether to allow http:// URLs.
 * @param {string=} opt_logMsgUrl A log message URL.
 * @constructor
 */
function AppIdChecker(timer, origin, appIds, allowHttp, opt_logMsgUrl) {
  /** @private {!Countdown} */
  this.timer_ = timer;
  /** @private {string} */
  this.origin_ = origin;
  var appIdsMap = {};
  if (appIds) {
    for (var i = 0; i < appIds.length; i++) {
      appIdsMap[appIds[i]] = appIds[i];
    }
  }
  /** @private {Array.<string>} */
  this.distinctAppIds_ = Object.keys(appIdsMap);
  /** @private {boolean} */
  this.allowHttp_ = allowHttp;
  /** @private {string|undefined} */
  this.logMsgUrl_ = opt_logMsgUrl;

  /** @private {boolean} */
  this.closed_ = false;
  /** @private {boolean} */
  this.anyInvalidAppIds_ = false;
  /** @private {number} */
  this.fetchedAppIds_ = 0;
}

/**
 * A fixed mapping from app id to allowed origins for that app id.
 * @private {Object.<string, !Array.<string>>}
 */
AppIdChecker.fixedAllowedOrigins_ = {};

/**
 * Sets pre-configured allowed app ids for origins, to work around legacy
 * app ids for which app id fetching will fail.
 * @param {Object.<string, !Array.<string>>} allowedOrigins A new set of
 *     allowed origins, keyed by app id.
 */
AppIdChecker.setFixedAllowedOrigins = function(allowedOrigins) {
  AppIdChecker.fixedAllowedOrigins_ = allowedOrigins;
};

/**
 * Checks all the app ids provided, and calls a callback indicating whether
 * all of them can be asserted by the given orign.
 * @param {function(boolean)} cb Called with the result of the check.
 */
AppIdChecker.prototype.doCheck = function(cb) {
  if (this.cb_) {
    // Check already in progress: no go.
    this.notify_(false);
    return;
  }
  /** @private {function(boolean)} */
  this.cb_ = cb;
  if (!this.distinctAppIds_.length) {
    this.notify_(false);
    return;
  }
  for (var i = 0; i < this.distinctAppIds_.length; i++) {
    var appId = this.distinctAppIds_[i];
    if (appId == this.origin_) {
      // Trivially allowed.
      this.fetchedAppIds_++;
      if (this.fetchedAppIds_ == this.distinctAppIds_.length &&
          !this.anyInvalidAppIds_) {
        // Last app id was fetched, and they were all valid: we're done.
        // (Note that the case when anyInvalidAppIds_ is true doesn't need to
        // be handled here: the callback was already called with false at that
        // point, see fetchedAllowedOriginsForAppId_.)
        this.notify_(true);
      }
    } else {
      var start = new Date();
      this.fetchAllowedOriginsForAppId_(appId,
          this.fetchedAllowedOriginsForAppId_.bind(this, appId, start));
    }
  }
};

/**
 * Closes this checker. No callback will be called after this checker is closed.
 */
AppIdChecker.prototype.close = function() {
  this.closed_ = true;
};

/**
 * Notifies the callback with the result.
 * @param {boolean} result The result to notify.
 * @private
 */
AppIdChecker.prototype.notify_ = function(result) {
  if (!this.closed_) {
    this.closed_ = true;
    if (this.cb_) {
      this.cb_(result);
    }
  }
};

/**
 * Fetches the allowed origins for an appId.
 * @param {string} appId Application id
 * @param {function(number, !Array.<string>)} cb Called back with an HTTP
 *     response code and a list of allowed origins for appId.
 * @private
 */
AppIdChecker.prototype.fetchAllowedOriginsForAppId_ = function(appId, cb) {
  var allowedOrigins = [];
  if (!appId) {
    cb(200, allowedOrigins);
    return;
  }
  if (appId.indexOf('http://') == 0 && !this.allowHttp_) {
    console.log(UTIL_fmt('http app ids disallowed, ' + appId + ' requested'));
    cb(200, allowedOrigins);
    return;
  }
  // Fixed app ids get special-case treatment.
  if (AppIdChecker.fixedAllowedOrigins_.hasOwnProperty(appId)) {
    allowedOrigins = AppIdChecker.fixedAllowedOrigins_[appId];
    cb(200, allowedOrigins);
    return;
  }
  // Termination of this function relies in fetchAppId completing.
  // (Not completing would be a bug in XMLHttpRequest.)
  // TODO: provide a termination guarantee, e.g. with a timer?
  fetchAppId(appId, function(rc, origins) {
    if (rc != 200) {
      console.log(UTIL_fmt('fetching ' + appId + ' failed: ' + rc));
      allowedOrigins = [];
    } else {
      allowedOrigins = /** @type {!Array.<string>} */ (origins);
    }
    cb(rc, allowedOrigins);
  });
};

/**
 * Called with the result of an app id fetch.
 * @param {string} appId the app id that was fetched.
 * @param {Date} start the time the fetch request started.
 * @param {number} rc The HTTP response code for the app id fetch.
 * @param {!Array.<string>} allowedOrigins The origins allowed for this app id.
 * @private
 */
AppIdChecker.prototype.fetchedAllowedOriginsForAppId_ =
    function(appId, start, rc, allowedOrigins) {
  var end = new Date();
  this.fetchedAppIds_++;
  this.logFetchAppIdResult_(appId, end - start, allowedOrigins);
  if (rc != 200 && !(rc >= 400 && rc < 500)) {
    if (this.timer_.expired()) {
      this.notify_(false);
    } else {
      start = new Date();
      this.fetchAllowedOriginsForAppId_(appId,
          this.fetchedAllowedOriginsForAppId_.bind(this, appId, start));
    }
    return;
  }
  if (!isValidAppIdForOrigin(appId, this.origin_, allowedOrigins)) {
    console.warn(UTIL_fmt('Origin ' + this.origin_ + ' not allowed by app id ' +
          appId));
    this.logInvalidOriginForAppId_(appId);
    this.anyInvalidAppIds_ = true;
    this.notify_(false);
  }
  if (this.fetchedAppIds_ == this.distinctAppIds_.length &&
      !this.anyInvalidAppIds_) {
    // Last app id was fetched, and they were all valid: we're done.
    this.notify_(true);
  }
};

/**
 * Logs the result of fetching an appId.
 * @param {!string} appId Application Id
 * @param {number} millis elapsed time while fetching the appId.
 * @param {Array.<string>} allowedOrigins the allowed origins retrieved.
 * @private
 */
AppIdChecker.prototype.logFetchAppIdResult_ =
    function(appId, millis, allowedOrigins) {
  var logMsg = 'log=fetchappid&appid=' + appId + '&millis=' + millis +
      '&numorigins=' + allowedOrigins.length;
  logMessage(logMsg, this.logMsgUrl_);
};

/**
 * Logs a mismatch between an origin and an appId.
 * @param {!string} appId Application id
 * @private
 */
AppIdChecker.prototype.logInvalidOriginForAppId_ = function(appId) {
  var logMsg = 'log=originrejected&origin=' + this.origin_ + '&appid=' + appId;
  logMessage(logMsg, this.logMsgUrl_);
};
