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
  // Always use Chrome notifications for now, while chrome.infobars is non-
  // operable on Mac.
  // TODO: make it possible to switch the two easily, e.g. with a
  // stored permission.
  this.impl_ = new NotificationUserApprovedOrigins();
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
  return this.impl_.isApprovedOrigin(origin, opt_tabId);
};

/**
 * Approves pending requests for the given tab.
 * @param {number} tabId The tab id on which to approve the origin.
 */
UserApprovedOrigins.prototype.approveOrigin = function(tabId) {
  return this.impl_.approveOrigin(tabId);
};

/**
 * Denies all pending requests for the given tab.
 * @param {number} tabId The tab id on which to deny the origin.
 */
UserApprovedOrigins.prototype.denyOrigin = function(tabId) {
  return this.impl_.denyOrigin(tabId);
};
