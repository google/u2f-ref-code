// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Provides an implementation of origins that's based on explicit
 * user consent.
 *
 */
'use strict';

/**
 * Allows the caller to check whether the user has approved the use of
 * security keys from an origin.
 * @constructor
 * @implements {ApprovedOrigins}
 */
function UserApprovedOrigins() {
  /** @private {!Object.<number, Array.<function(boolean)>>} */
  this.pendingApprovals_ = {};
}

/**
 * Checks whether the origin is approved to use security keys. (If not, an
 * approval prompt may be shown.)
 * @param {string} origin The origin to approve.
 * @param {number=} opt_tabId A tab id to display approval prompt in, if
 *     necessary.
 * @return {Promise.<boolean>} A promise for the result of the check.
 */
UserApprovedOrigins.prototype.isApprovedOrigin = function(origin, opt_tabId) {
  var etldOriginChecker =
      /** @type {EtldOriginChecker} */ (FACTORY_REGISTRY.getOriginChecker());
  var etldFetcher = etldOriginChecker.getFetcher();
  var self = this;
  return new Promise(function(resolve, reject) {
      if (!opt_tabId) {
        resolve(false);
        return;
      }
      var tabId = /** @type {number} */ (opt_tabId);
      etldFetcher.getEffectiveTldPlusOne(origin).then(function(etldPlusOne) {
          if (!self.pendingApprovals_.hasOwnProperty(tabId)) {
            self.pendingApprovals_[tabId] = [];
          }
          self.pendingApprovals_[tabId].push(resolve);
          var url = 'infobar.html?' + etldPlusOne + '&' + tabId;
          var options = {'path': url, 'tabId': tabId};
          chrome.infobars.show(options);
      });
  });
};

/**
 * Approves pending requests for the given tab.
 * @param {number} tabId The tab id on which to approve the origin.
 */
UserApprovedOrigins.prototype.approveOrigin = function(tabId) {
  if (this.pendingApprovals_.hasOwnProperty(tabId)) {
    var originPendingApprovals = this.pendingApprovals_[tabId];
    for (var i = 0; i < originPendingApprovals.length; i++) {
      originPendingApprovals[i](true);
    }
    delete this.pendingApprovals_[tabId];
  }
};

/**
 * Denies all pending requests for the given tab.
 * @param {number} tabId The tab id on which to deny the origin.
 */
UserApprovedOrigins.prototype.denyOrigin = function(tabId) {
  if (this.pendingApprovals_.hasOwnProperty(tabId)) {
    var originPendingApprovals = this.pendingApprovals_[tabId];
    for (var i = 0; i < originPendingApprovals.length; i++) {
      originPendingApprovals[i](false);
    }
    delete this.pendingApprovals_[tabId];
  }
};
