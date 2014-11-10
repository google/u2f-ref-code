// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview U2F gnubbyd background page
 */

'use strict';

/** @const */
var BROWSER_SUPPORTS_TLS_CHANNEL_ID = true;

// Singleton tracking available devices.
var gnubbies = new Gnubbies();
// Only include HID support if it's available in this browser. Register it
// first, though, because it's more likely to succeed on HID devices than
// chrome.usb is, on platforms where chrome.usb can see HID devices as well as
// non-HID ones (Linux in particular.)
// Only include HID support if it's available in this browser.
if (chrome.hid) {
  HidGnubbyDevice.register(gnubbies);
}
UsbGnubbyDevice.register(gnubbies);

var TIMER_FACTORY = new CountdownTimerFactory();

var REQUEST_HELPER = new DelegatingHelper();
REQUEST_HELPER.addHelper(new UsbHelper());

var FACTORY_REGISTRY = new FactoryRegistry(
    new UserApprovedOrigins(),
    TIMER_FACTORY,
    new EtldOriginChecker(),
    REQUEST_HELPER,
    new XhrTextFetcher());

var DEVICE_FACTORY_REGISTRY = new DeviceFactoryRegistry(
    new UsbGnubbyFactory(gnubbies),
    TIMER_FACTORY,
    new NoIndividualAttestation());

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
 * Listen to individual messages sent from (whitelisted) webpages via
 * chrome.runtime.sendMessage
 * @return {boolean}
 */
function messageHandlerExternal(request, sender, sendResponse) {
  if (sender.id) {
    // An external helper registers itself by sending its id as the message.
    // Check whether it's a whitelisted helper.
    if (request === sender.id &&
        HELPER_WHITELIST.isExtensionAllowed(sender.id)) {
      registerExternalHelper(sender.id);
    }
    return false;  // We won't call sendResponse, Chrome may discard it
  }
  var closeable = handleWebPageRequest(request, sender, function(response) {
    response['requestId'] = request['requestId'];
    try {
      sendResponse(response);
    } catch (e) {
      console.warn(UTIL_fmt('caught: ' + e.message));
    }
  });
  return true;
}
chrome.runtime.onMessageExternal.addListener(messageHandlerExternal);

// Listen to individual messages sent from this extension via
// chrome.runtime.sendMessage.
function messageHandler(request, sender, sendResponse) {
  if (request && request.type) {
    switch (request.type) {
      case 'originApproved':
        var userApprovedOrigins =
            /** @type {UserApprovedOrigins} */ (FACTORY_REGISTRY.
                getApprovedOrigins());
        // TODO: Remove timeout when race with infobar is solved
        window.setTimeout(function() {
          userApprovedOrigins.approveOrigin(request.tab);
        }, 200);
        break;
      case 'originDenied':
        var userApprovedOrigins =
            /** @type {UserApprovedOrigins} */ (FACTORY_REGISTRY.
                getApprovedOrigins());
        // TODO: Remove timeout when race with infobar is solved
        window.setTimeout(function() {
          userApprovedOrigins.denyOrigin(request.tab);
        }, 200);
        break;
    }
  }
}
chrome.runtime.onMessage.addListener(messageHandler);

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
