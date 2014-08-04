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
 * Provides an object to track checking a list of appIds.
 * @param {!TextFetcher} fetcher A URL fetcher.
 * @param {!Countdown} timer A timer by which to resolve all provided app ids.
 * @param {string} origin The origin to check.
 * @param {!Array.<string>} appIds The app ids to check.
 * @param {boolean} allowHttp Whether to allow http:// URLs.
 * @param {string=} opt_logMsgUrl A log message URL.
 * @constructor
 */
function AppIdChecker(fetcher, timer, origin, appIds, allowHttp, opt_logMsgUrl)
    {
  /** @private {!TextFetcher} */
  this.fetcher_ = fetcher;
  /** @private {!EffectiveTldFetcher} */
  this.etldChecker_ = new EffectiveTldFetcher(fetcher, true);
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
 * Checks whether all the app ids provided can be asserted by the given origin.
 * @return {Promise.<boolean>} A promise for the result of the check
 */
AppIdChecker.prototype.doCheck = function() {
  if (!this.distinctAppIds_.length)
    return Promise.resolve(false);

  if (this.allAppIdsEqualOrigin_()) {
    // Trivially allowed.
    return Promise.resolve(true);
  } else {
    // Begin checking remaining app ids. First make sure we know the origin's
    // eTLD + 1, to know whether the origin can assert them.
    var p = this.etldChecker_.getEffectiveTldPlusOne(this.origin_);
    var self = this;
    return p.then(function(originEtld) {
      if (!originEtld)
        return Promise.resolve(false);
      /** @private {string} */
      self.originEtld_ = originEtld;
      var appIdChecks = self.distinctAppIds_.map(self.checkAppId_.bind(self));
      return Promise.all(appIdChecks).then(function(results) {
        return results.every(function(result) {
          if (!result)
            self.anyInvalidAppIds_ = true;
          return result;
        });
      });
    });
  }
};

/**
 * Checks if a single appId can be asserted by the given origin.
 * @param {string} appId The appId to check
 * @return {Promise.<boolean>} A promise for the result of the check
 * @private
 */
AppIdChecker.prototype.checkAppId_ = function(appId) {
  if (appId == this.origin_) {
    // Trivially allowed
    return Promise.resolve(true);
  }
  var self = this;
  var p = this.checkOriginAllowedToAssertAppId_(appId);
  return p.then(function(allowed) {
    if (!allowed)
      return false;
    var p = self.fetchAllowedOriginsForAppId_(appId);
    return p.then(function(allowedOrigins) {
      if (allowedOrigins.indexOf(self.origin_) == -1) {
        console.warn(UTIL_fmt('Origin ' + self.origin_ +
              ' not allowed by app id ' + appId));
        return false;
      }
      return true;
    });
  });
};

/**
 * Closes this checker. No callback will be called after this checker is closed.
 */
AppIdChecker.prototype.close = function() {
  this.closed_ = true;
};

/**
 * Sets the app ID whitelist.
 * @param {!Object.<string, !Array.<string>>} whitelist The whitelist to set,
 *     as a map from eTLD + 1 of the asking origin to the app ID origins
 *     allowed from that eTLD + 1.
 */
AppIdChecker.setAppIdWhitelist = function(whitelist) {
  AppIdChecker.whitelistedAppIdOriginsByEtld_ = whitelist;
  // Set the fixed eTLDs known by the EffectiveTldFetcher, to avoid having to
  // fetch the canonical list if we don't have to.
  var etlds = {};
  for (var etldPlusOne in whitelist) {
    var dot = etldPlusOne.indexOf('.');
    if (dot >= 0) {
      var etld = etldPlusOne.substring(dot + 1);
      if (etld) {
        etlds[etld] = etld;
      }
    }
  }
  var etldList = Object.keys(etlds);
  if (etldList.length) {
    EffectiveTldFetcher.setFixedTldList(etldList);
  }
};

/** @private {!Object.<string, !Array.<string>>} */
AppIdChecker.whitelistedAppIdOriginsByEtld_ = {};

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
 * Checks whether this origin is allowed to assert the given app id.
 * @param {string} appId The app id to check.
 * @return {Promise.<boolean>} A promise for whether this origin is
 *     allowed to assert the given app id.
 * @private
 */
AppIdChecker.prototype.checkOriginAllowedToAssertAppId_ = function(appId) {
  var appIdOrigin = getOriginFromUrl(appId);
  if (!appIdOrigin)
    return Promise.resolve(false);
  var appIdOriginString = /** @type {string} */ (appIdOrigin);
  var p = this.etldChecker_.getEffectiveTldPlusOne(appIdOriginString);
  var self = this;
  return p.then(function(appIdEtld) {
    if (self.originEtld_ == appIdEtld) {
      // Origin and app id are from the same eTLD + 1: allowed.
      return true;
    }
    // Origin eTLD + 1 != app id eTLD + 1: only allowed if the app id's
    // origin is explicitly whitelisted for this origin's eTLD + 1.
    return self.isAppIdOriginWhitelistedForOrigin_(appIdOriginString);
  });
};

/**
 * Checks whether an origin is allowed to assert an app ID that doesn't belong
 * to the same eTLD + 1 as it, according to AppIdChecker's internal whitelist.
 * @param {string} appIdOrigin The app ID origin being requested.
 * @return {boolean} Whether this origin's eTLD + 1 is allowed to assert the
 *     given app ID.
 * @private
 */
AppIdChecker.prototype.isAppIdOriginWhitelistedForOrigin_ =
    function(appIdOrigin) {
  if (!AppIdChecker.whitelistedAppIdOriginsByEtld_
      .hasOwnProperty(this.originEtld_)) {
    return false;
  }
  var allowed = AppIdChecker.whitelistedAppIdOriginsByEtld_[this.originEtld_]
      .indexOf(appIdOrigin) >= 0;
  return allowed;
};

/**
 * Fetches the allowed origins for an appId.
 * @param {string} appId Application id
 * @return {Promise.<!Array.<string>>} A promise for a list of allowed origins
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
