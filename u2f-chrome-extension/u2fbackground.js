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

/** @const */
var HTTP_ORIGINS_ALLOWED = true;

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

var REQUEST_HELPER = new DelegatingHelper();
REQUEST_HELPER.addHelper(new UsbHelper());

var FACTORY_REGISTRY = (function() {
  var windowTimer = new WindowTimer();
  var xhrTextFetcher = new XhrTextFetcher();
  return new FactoryRegistry(
      new XhrAppIdCheckerFactory(xhrTextFetcher),
      new CryptoTokenApprovedOrigin(),
      new CountdownTimerFactory(windowTimer),
      new EtldOriginChecker(),
      REQUEST_HELPER,
      windowTimer,
      xhrTextFetcher);
})();

var DEVICE_FACTORY_REGISTRY = new DeviceFactoryRegistry(
    new UsbGnubbyFactory(gnubbies),
    FACTORY_REGISTRY.getCountdownFactory(),
    new NoIndividualAttestation());

var BLE_APP_HASH = 'sdpeHuOBPmz9tQ9Dk_q9GlO3fN4rbKST08sQ6X1f2rQ';

/**
 * @param {string} senderId of app that sent a message
 * @return {boolean} Whether sender app is the remote helper app.
 */
function isBleApp(senderId) {
  return BLE_APP_HASH == B64_encode(sha256HashOfString(senderId));
}

function setBleAppId(app_id) {
 chrome.storage.local.set({ble_app_id: app_id});
}

/**
 * Whitelist of allowed external request helpers.
 * (This is currently empty, and is left here to document how you might insert
 * your own helper.)
 */
var HELPER_WHITELIST = new RequestHelperWhitelist();
HELPER_WHITELIST.addAllowedBlindedExtension(BLE_APP_HASH, 'BLE');

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

chrome.storage.local.get('ble_app_id', function(stored) {
  if (!chrome.runtime.lastError && stored && stored.ble_app_id) {
    registerExternalHelper(stored.ble_app_id);
  }
});

/**
 * @param {*} request The received request
 * @return {boolean} Whether the request is a register/enroll request.
 */
function isRegisterRequest(request) {
  if (!request) {
    return false;
  }
  switch (request.type) {
    case MessageTypes.U2F_REGISTER_REQUEST:
      return true;

    default:
      return false;
  }
}

/**
 * Default response callback to deliver a response to a request.
 * @param {*} request The received request.
 * @param {function(*): void} sendResponse A callback that delivers a response.
 * @param {*} response The response to return.
 */
function defaultResponseCallback(request, sendResponse, response) {
  response['requestId'] = request['requestId'];
  try {
    sendResponse(response);
  } catch (e) {
    console.warn(UTIL_fmt('caught: ' + e.message));
  }
}

/**
 * Response callback that delivers a response to a request only when the
 * sender is a foreground tab.
 * @param {*} request The received request.
 * @param {!MessageSender} sender The message sender.
 * @param {function(*): void} sendResponse A callback that delivers a response.
 * @param {*} response The response to return.
 */
function sendResponseToActiveTabOnly(request, sender, sendResponse, response) {
  tabInForeground(sender.tab.id).then(function(result) {
    // If the tab is no longer in the foreground, drop the result: the user
    // is no longer interacting with the tab that originated the request.
    if (result) {
      defaultResponseCallback(request, sendResponse, response);
    }
  });
}

/**
 * Common handler for messages received from chrome.runtime.sendMessage and
 * chrome.runtime.connect + postMessage.
 * @param {*} request The received request
 * @param {!MessageSender} sender The message sender
 * @param {function(*): void} sendResponse A callback that delivers a response
 * @return {Closeable} A Closeable request handler.
 */
function messageHandler(request, sender, sendResponse) {
  var responseCallback;
  if (isRegisterRequest(request)) {
    responseCallback =
        sendResponseToActiveTabOnly.bind(null, request, sender, sendResponse);
  } else {
    responseCallback =
        defaultResponseCallback.bind(null, request, sendResponse);
  }
  var closeable = handleWebPageRequest(/** @type {Object} */(request),
      sender, responseCallback);
  return closeable;
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
      if (isBleApp(sender.id)) {
        setBleAppId(sender.id);
        sendResponse(/** @type {ExternalHelperAck} */ ({rc: 0}));
      }
    }
    return false;  // We won't call sendResponse, Chrome may discard it
  }
  messageHandler(request, sender, sendResponse);
  return true;
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
    var sender = /** @type {!MessageSender} */ (port.sender);
    closeable = messageHandler(request, sender, port.postMessage.bind(port));
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
 * @return {!MessageSender} A MessageSender for the origin.
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
  return /** @type {!MessageSender} */ (sender);
}

// Listen to connection events from our own web accessible scripts
chrome.runtime.onConnect.addListener(function(port) {
  var closeable;
  port.onMessage.addListener(function(message) {
    var sender =
        makeMessageSenderFromOrigin(/** @type {string} */ (message.origin));
    if (port.sender && port.sender.tab) {
      sender.tab = port.sender.tab;
    }
    var request = message.request;
    closeable = messageHandler(request,
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
