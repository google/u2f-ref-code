// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview U2F gnubbyd background page
 */

'use strict';

// Singleton tracking available devices.
var gnubbies = new Gnubbies();
llUsbGnubby.register(gnubbies);
// Only include HID support if it's available in this browser.
if (chrome.hid) {
  llHidGnubby.register(gnubbies);
}

var GNUBBY_FACTORY = new UsbGnubbyFactory(gnubbies);
var TIMER_FACTORY = new CountdownTimerFactory();
var USB_HELPER = new UsbHelper(GNUBBY_FACTORY, TIMER_FACTORY);

var REQUEST_HELPER = new DelegatingHelper();
REQUEST_HELPER.addHelper(USB_HELPER);

/**
 * Whitelist of allowed external request helpers.
 * (This is currently empty, and is left here to document how you might insert
 * your own helper.)
 */
var HELPER_WHITELIST = new RequestHelperWhitelist();

/**
 * Registers the given extension as an external helper.
 * @param {string} id Extension id.
 */
function registerExternalHelper(id) {
  var helperAppConfig = {
    appId: id,
    sendMessage: chrome.runtime.sendMessage,
    defaultError: DeviceStatusCodes.TIMEOUT_STATUS
  };
  var source = HELPER_WHITELIST.getExtensionMnemonic(id);
  if (source) {
    helperAppConfig.source = source;
  }
  var externalHelper = new ExternalHelper(helperAppConfig);
  REQUEST_HELPER.addHelper(externalHelper);
}

/**
 * @param {Object} request Request object
 * @param {MessageSender} sender Sender frame
 * @param {Function} sendResponse Response callback
 * @return {?Closeable} Optional handler object that should be closed when port
 *     closes
 */
function handleWebPageRequest(request, sender, sendResponse) {
  switch (request.type) {
    case MessageTypes.U2F_REGISTER_REQUEST:
      return handleU2fEnrollRequest(REQUEST_HELPER, TIMER_FACTORY, sender,
          request, sendResponse);

    case MessageTypes.U2F_SIGN_REQUEST:
      return handleU2fSignRequest(REQUEST_HELPER, TIMER_FACTORY, sender,
          request, sendResponse);

    default:
      sendResponse(
          makeU2fErrorResponse(request, ErrorCodes.BAD_REQUEST, undefined,
              MessageTypes.U2F_REGISTER_RESPONSE));
      return null;
  }
}

// Listen to individual messages sent from (whitelisted) webpages via
// chrome.runtime.sendMessage
function messageHandlerExternal(request, sender, sendResponse) {
  if (sender.id) {
    // An external helper registers itself by sending its id as the message.
    // Check whether it's a whitelisted helper.
    if (request === sender.id &&
        HELPER_WHITELIST.isExtensionAllowed(sender.id)) {
      registerExternalHelper(sender.id);
    }
    return true;
  }
  var closeable = handleWebPageRequest(request, sender, function(response) {
    response['requestId'] = request['requestId'];
    sendResponse(response);
  });
}
chrome.runtime.onMessageExternal.addListener(messageHandlerExternal);

// Listen to direct connection events, and wire up a message handler on the port
chrome.runtime.onConnectExternal.addListener(function(port) {
  var closeable;
  port.onMessage.addListener(function(request) {
    if (port.sender.id) {
      // An external helper registers itself by sending its id as the message.
      // Check whether it's a whitelisted helper.
      if (request === port.sender.id &&
          HELPER_WHITELIST.isExtensionAllowed(port.sender.id)) {
        registerExternalHelper(port.sender.id);
      }
      return;
    }
    closeable = handleWebPageRequest(request, port.sender,
        function(response) {
          response['requestId'] = request['requestId'];
          port.postMessage(response);
        });
  });
  port.onDisconnect.addListener(function() {
    if (closeable) {
      closeable.close();
    }
  });
});

/**
 * Makes a MessageSender representing a web origin sending a message.
 * @param {string} origin The origin sending the message.
 * @return {MessageSender} A MessageSender for the origin.
 */
function makeMessageSenderFromOrigin(origin) {
  var sender;
  // Make Closure happy by using the constructor if one's available, otherwise
  // use a raw object.
  if (window.hasOwnProperty('MessageSender')) {
    sender = new MessageSender();
  } else {
    sender = {};
  }
  sender['url'] = origin;
  sender['tlsChannelId'] = '';  // Can't deliver channelId over the iframe link
  return /** @type {MessageSender} */ (sender);
}

// Listen to connection events from our own web accessible scripts
chrome.runtime.onConnect.addListener(function(port) {
  var closeable;
  port.onMessage.addListener(function(message) {
    var sender =
        makeMessageSenderFromOrigin(/** @type {string} */ (message.origin));
    var request = message.request;
    closeable = handleWebPageRequest(request,
        sender,
        function(response) {
          response['requestId'] = request['requestId'];
          port.postMessage(response);
        });
  });
  port.onDisconnect.addListener(function() {
    if (closeable) {
      closeable.close();
    }
  });
});
