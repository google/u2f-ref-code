// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Implements a check whether an app id lists an origin.
 */
'use strict';

/**
 * Parses the text as JSON and returns it as an array of strings.
 * @param {string} text Input JSON
 * @return {!Array<string>} Array of origins
 */
function getOriginsFromJson(text) {
  try {
    var urls, i;
    var appIdData = JSON.parse(text);
    if (Array.isArray(appIdData)) {
      // Older format where it is a simple list of facets
      urls = appIdData;
    } else {
      var trustedFacets = appIdData['trustedFacets'];
      if (trustedFacets) {
        var versionBlock;
        for (i = 0; versionBlock = trustedFacets[i]; i++) {
          if (versionBlock['version'] &&
              versionBlock['version']['major'] == 1 &&
              versionBlock['version']['minor'] == 0) {
            urls = versionBlock['ids'];
            break;
          }
        }
      }
      if (typeof urls == 'undefined') {
        throw Error('Could not find trustedFacets for version 1.0');
      }
    }
    var origins = {};
    var url;
    for (i = 0; url = urls[i]; i++) {
      var origin = getOriginFromUrl(url);
      if (origin) {
        origins[origin] = origin;
      }
    }
    return Object.keys(origins);
  } catch (e) {
    console.error(UTIL_fmt('could not parse ' + text));
    return [];
  }
}

/**
 * Retrieves a set of distinct app ids from the sign challenges.
 * @param {Array<SignChallenge>=} signChallenges Input sign challenges.
 * @return {Array<string>} array of distinct app ids.
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
 * Provides an object to track checking a list of appIds.
 * @param {!TextFetcher} fetcher A URL fetcher.
 * @param {!Countdown} timer A timer by which to resolve all provided app ids.
 * @param {string} origin The origin to check.
 * @param {!Array<string>} appIds The app ids to check.
 * @param {boolean} allowHttp Whether to allow http:// URLs.
 * @param {string=} opt_logMsgUrl A log message URL.
 * @constructor
 */
function AppIdChecker(fetcher, timer, origin, appIds, allowHttp, opt_logMsgUrl)
    {
  /** @private {!TextFetcher} */
  this.fetcher_ = fetcher;
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
  /** @private {Array<string>} */
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
 * Checks whether all the app ids provided can be asserted by the given origin.
 * @return {Promise<boolean>} A promise for the result of the check
 */
AppIdChecker.prototype.doCheck = function() {
  if (!this.distinctAppIds_.length)
    return Promise.resolve(false);

  if (this.allAppIdsEqualOrigin_()) {
    // Trivially allowed.
    return Promise.resolve(true);
  } else {
    var self = this;
    // Begin checking remaining app ids.
    var appIdChecks = self.distinctAppIds_.map(self.checkAppId_.bind(self));
    return Promise.all(appIdChecks).then(function(results) {
      return results.every(function(result) {
        if (!result)
          self.anyInvalidAppIds_ = true;
        return result;
      });
    });
  }
};

/**
 * Checks if a single appId can be asserted by the given origin.
 * @param {string} appId The appId to check
 * @return {Promise<boolean>} A promise for the result of the check
 * @private
 */
AppIdChecker.prototype.checkAppId_ = function(appId) {
  if (appId == this.origin_) {
    // Trivially allowed
    return Promise.resolve(true);
  }
  var p = this.fetchAllowedOriginsForAppId_(appId);
  var self = this;
  return p.then(function(allowedOrigins) {
    if (allowedOrigins.indexOf(self.origin_) == -1) {
      console.warn(UTIL_fmt('Origin ' + self.origin_ +
            ' not allowed by app id ' + appId));
      return false;
    }
    return true;
  });
};

/**
 * Closes this checker. No callback will be called after this checker is closed.
 */
AppIdChecker.prototype.close = function() {
  this.closed_ = true;
};

/**
 * @return {boolean} Whether all the app ids being checked are equal to the
 * calling origin.
 * @private
 */
AppIdChecker.prototype.allAppIdsEqualOrigin_ = function() {
  var self = this;
  return this.distinctAppIds_.every(function(appId) {
    return appId == self.origin_;
  });
};

/**
 * Fetches the allowed origins for an appId.
 * @param {string} appId Application id
 * @return {Promise<!Array<string>>} A promise for a list of allowed origins
 *     for appId
 * @private
 */
AppIdChecker.prototype.fetchAllowedOriginsForAppId_ = function(appId) {
  if (!appId) {
    return Promise.resolve([]);
  }

  if (appId.indexOf('http://') == 0 && !this.allowHttp_) {
    console.log(UTIL_fmt('http app ids disallowed, ' + appId + ' requested'));
    return Promise.resolve([]);
  }

  var origin = getOriginFromUrl(appId);
  if (!origin) {
    return Promise.resolve([]);
  }

  var p = this.fetcher_.fetch(appId);
  var self = this;
  return p.then(getOriginsFromJson, function(rc_) {
    var rc = /** @type {number} */(rc_);
    console.log(UTIL_fmt('fetching ' + appId + ' failed: ' + rc));
    if (!(rc >= 400 && rc < 500) && !self.timer_.expired()) {
      // Retry
      return self.fetchAllowedOriginsForAppId_(appId);
    }
    return [];
  });
};
