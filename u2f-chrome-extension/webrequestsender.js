// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Provides a representation of a web request sender, and
 * utility functions for creating them.
 */
'use strict';

/**
 * @typedef {{
 *   origin: string,
 *   tlsChannelId: (string|undefined),
 *   tabId: (number|undefined)
 * }}
 */
var WebRequestSender;

/**
 * Creates an object representing the sender's origin, and, if available,
 * tab.
 * @param {MessageSender} messageSender The message sender.
 * @return {?WebRequestSender} The sender's origin and tab, or null if the
 *     sender is invalid.
 */
function createSenderFromMessageSender(messageSender) {
  var origin = getOriginFromUrl(/** @type {string} */ (messageSender.url));
  if (!origin) {
    return null;
  }
  var sender = {
    origin: origin
  };
  if (messageSender.tlsChannelId) {
    sender.tlsChannelId = messageSender.tlsChannelId;
  }
  if (messageSender.tab) {
    sender.tabId = messageSender.tab.id;
  }
  return sender;
}

/**
 * Attempts to ensure that the tabId of the sender is set, using chrome.tabs
 * when available.
 * @param {WebRequestSender} sender The request sender.
 * @return {Promise} A promise resolved once the tabId retrieval is done.
 *     The promise is rejected if the tabId is untrustworthy, e.g. if the
 *     user rapidly switched tabs.
 */
function getTabIdWhenPossible(sender) {
  if (sender.tabId) {
    // Already got it? Done.
    return Promise.resolve(true);
  } else if (!chrome.tabs) {
    // Can't get it? Done. (This happens to packaged apps, which can't access
    // chrome.tabs.)
    return Promise.resolve(true);
  } else {
    return new Promise(function(resolve, reject) {
      chrome.tabs.query({active: true, lastFocusedWindow: true},
          function(tabs) {
            if (!tabs.length) {
              // Safety check.
              reject(false);
              return;
            }
            var tab = tabs[0];
            // Safety check: only trust the tab id if its origin matches the
            // sender's.
            if (getOriginFromUrl(tab.url) == sender.origin) {
              sender.tabId = tab.id;
              resolve(true);
              return;
            }
            // Didn't match? Check if the debugger is open.
            if (tab.url.indexOf('chrome-devtools://') != 0) {
              reject(false);
              return;
            }
            // Debugger active: find first tab with the sender's origin.
            chrome.tabs.query({active: true}, function(tabs) {
              if (!tabs.length) {
                // Safety check.
                reject(false);
                return;
              }
              for (var i = 0; i < tabs.length; i++) {
                tab = tabs[i];
                if (getOriginFromUrl(tab.url) == sender.origin) {
                  sender.tabId = tab.id;
                  resolve(true);
                  return;
                }
              }
              // No match: reject.
              reject(false);
              return;
            });
          });
    });
  }
}
