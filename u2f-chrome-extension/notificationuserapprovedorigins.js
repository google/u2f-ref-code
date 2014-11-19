// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Provides an implementation of origins that's based on explicit
 * user consent via Chrome's notifications API.
 *
 */
'use strict';

/**
 * Allows the caller to check whether the user has approved the use of
 * security keys from an origin.
 * @constructor
 * @implements {ApprovedOrigins}
 */
function NotificationUserApprovedOrigins() {
  /** @private {number} */
  this.notificationInstance_ = 0;
  /** @private {!Object.<string, function(boolean)>} */
  this.pendingNotifications_ = {};
  chrome.notifications.onClosed.addListener(
      this.notificationClosedListener_.bind(this));
  chrome.notifications.onButtonClicked.addListener(
      this.notificationButtonClickedListener_.bind(this));
}

/**
 * Checks whether the origin is approved to use security keys. (If not, an
 * approval prompt may be shown.)
 * @param {string} origin The origin to approve.
 * @param {number=} opt_tabId A tab id to display approval prompt in, if
 *     necessary.
 * @return {Promise.<boolean>} A promise for the result of the check.
 */
NotificationUserApprovedOrigins.prototype.isApprovedOrigin =
    function(origin, opt_tabId) {
  var etldOriginChecker =
      /** @type {EtldOriginChecker} */ (FACTORY_REGISTRY.getOriginChecker());
  var etldFetcher = etldOriginChecker.getFetcher();
  var self = this;
  var p = etldFetcher.getEffectiveTldPlusOne(origin);
  var p2 =
      p.then(self.showNotification_.bind(self, ++self.notificationInstance_));
  // Closure can't infer that the first promise (getEffectiveTldPlusOne) chains
  // immediately to a second promise of the correct type, so help it.
  return /** @type {Promise.<boolean>} */ (p2);
};

/**
 * Displays a notification to gather the user's consent to use security keys.
 * @param {number} instance A unique identifier for this notification.
 * @param {?string} etldPlusOne The etld + 1 of the origin to display.
 * @return {Promise.<boolean>} A promise for the user's response to the
 *     notification.
 * @private
 */
NotificationUserApprovedOrigins.prototype.showNotification_ =
    function(instance, etldPlusOne) {
  if (!etldPlusOne) {
    return Promise.resolve(false);
  }
  var buttons = [
    { title: chrome.i18n.getMessage('approveButton') },
    { title: chrome.i18n.getMessage('denyButton') }
  ];
  var notificationOptions = {
    type: 'basic',
    iconUrl: chrome.runtime.getURL('/u2f-128.ico'),
    message: chrome.i18n.getMessage('notificationMessage', etldPlusOne),
    title: chrome.i18n.getMessage('notificationTitle'),
    buttons: buttons
  };
  var self = this;
  return new Promise(function(resolve, reject) {
    chrome.notifications.create('userapprovalnotification-' + instance,
        notificationOptions, function(id) {
          console.log(UTIL_fmt('created notification ' + id));
          self.pendingNotifications_[id] = resolve;
        });
  });
};

/**
 * Called when a notification is closed.
 * @param {string} id An identifier for the notification.
 * @param {boolean} byUser Whether the user closed the notification (true) or
 *     the system did (false).
 * @private
 */
NotificationUserApprovedOrigins.prototype.notificationClosedListener_ =
    function(id, byUser) {
  console.log(UTIL_fmt(id + ' closed by ' + (byUser ? 'user' : 'system')));
  if (this.pendingNotifications_.hasOwnProperty(id)) {
    this.pendingNotifications_[id](false);
    delete this.pendingNotifications_[id];
  }
  chrome.notifications.clear(id, function() {});
};

/**
 * Called when a user clicks on a button in a notification.
 * @param {string} id An identifier for the notification.
 * @param {number} buttonIndex Which button was clicked.
 * @private
 */
NotificationUserApprovedOrigins.prototype.notificationButtonClickedListener_ =
    function(id, buttonIndex) {
  console.log(id + ' got button click on ' + buttonIndex);
  if (this.pendingNotifications_.hasOwnProperty(id)) {
    if (buttonIndex == 0) {
      this.pendingNotifications_[id](true);
    } else if (buttonIndex == 1) {
      this.pendingNotifications_[id](false);
    }
    delete this.pendingNotifications_[id];
  }
  chrome.notifications.clear(id, function() {});
};

/**
 * Approves pending requests for the given tab.
 * @param {number} tabId The tab id on which to approve the origin.
 */
NotificationUserApprovedOrigins.prototype.approveOrigin = function(tabId) {
  // No-op: this should never be called.
  console.warn(
      UTIL_fmt('approveOrigin called on NotificationUserApprovedOrigins?'));
};

/**
 * Denies all pending requests for the given tab.
 * @param {number} tabId The tab id on which to deny the origin.
 */
NotificationUserApprovedOrigins.prototype.denyOrigin = function(tabId) {
  // No-op: this should never be called.
  console.warn(
      UTIL_fmt('denyOrigin called on NotificationUserApprovedOrigins?'));
};
