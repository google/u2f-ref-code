// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Implements a check whether an origin is allowed to assert an
 * app id based on whether they share the same effective TLD + 1.
 *
 */
'use strict';

/**
 * Implements half of the app id policy: whether an origin is allowed to claim
 * an app id. For checking whether the app id also lists the origin,
 * @see AppIdChecker.
 * @implements OriginChecker
 * @constructor
 */
function EtldOriginChecker() {
  // The instance of this class is managed by FactoryRegistry, which also
  // manages a TextFetcher on which EffectiveTldFetcher depends. Thus, we must
  // initialize the EffectiveTldFetcher lazily (see getFetcher).
  /** @private {EffectiveTldFetcher} */
  this.etldFetcher_ = null;
}

/**
 * Gets an EffectiveTldFetcher instance, creating one if necessary.
 * @return {!EffectiveTldFetcher}
 */
EtldOriginChecker.prototype.getFetcher = function() {
  if (!this.etldFetcher_) {
    var fetcher = FACTORY_REGISTRY.getTextFetcher();
    this.etldFetcher_ = new EffectiveTldFetcher(fetcher, true);
  }
  return this.etldFetcher_;
};

/**
 * Checks whether the origin is allowed to claim the app ids.
 * @param {string} origin The origin claiming the app id.
 * @param {!Array<string>} appIds The app ids being claimed.
 * @return {Promise<boolean>} A promise for the result of the check.
 */
EtldOriginChecker.prototype.canClaimAppIds = function(origin, appIds) {
  // First make sure we know the origin's eTLD + 1, to know whether the origin
  // can assert the app ids.
  var p = this.getFetcher().getEffectiveTldPlusOne(origin);
  var self = this;
  return p.then(function(originEtldPlusOne) {
    if (!originEtldPlusOne)
      return Promise.resolve(false);
    var appIdChecks = appIds.map(
        self.checkAppId_.bind(self, origin, originEtldPlusOne));
    return Promise.all(appIdChecks).then(function(results) {
      return results.every(function(result) {
        return result;
      });
    });
  });
};

/**
 * Checks if a single appId can be asserted by the given origin.
 * @param {string} origin The origin.
 * @param {string} originEtldPlusOne The origin's etld + 1.
 * @param {string} appId The appId to check
 * @return {Promise<boolean>} A promise for the result of the check
 * @private
 */
EtldOriginChecker.prototype.checkAppId_ =
    function(origin, originEtldPlusOne, appId) {
  if (appId == origin) {
    // Trivially allowed
    return Promise.resolve(true);
  }
  var appIdOrigin = getOriginFromUrl(appId);
  if (!appIdOrigin)
    return Promise.resolve(false);
  var appIdOriginString = /** @type {string} */ (appIdOrigin);
  var p = this.getFetcher().getEffectiveTldPlusOne(appIdOriginString);
  return p.then(function(appIdEtldPlusOne) {
    if (originEtldPlusOne == appIdEtldPlusOne)
      return true;
    // As an exception, allow google.com to use gstatic.com appIds. These should
    // be implemented using the redirect mechanism described in the FIDO AppID
    // and Facet Specification, but Javascript doesn't allow us to implement it
    // correctly: the client can't ensure the presence of the
    // FIDO-AppID-Redirect-Authorized header prior to following the redirect.
    if (originEtldPlusOne == 'google.com')
      return appIdEtldPlusOne == 'gstatic.com';
    return false;
  });
};
