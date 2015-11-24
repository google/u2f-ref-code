// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview The U2F api.
 */

'use strict';


/**
 * Namespace for the U2F api.
 * @type {Object}
 */
var u2f = u2f || {};

/**
 * FIDO U2F Javascript API Version
 * @number
 */
var js_api_version;

/**
 * The U2F extension id
 * @const {string}
 */
u2f.EXTENSION_ID = 'pfboblefjcgdjicmnffhdgionmgcdmne';


/**
 * Message types for messsages to/from the extension
 * @const
 * @enum {string}
 */
u2f.MessageTypes = {
  'U2F_REGISTER_REQUEST': 'u2f_register_request',
  'U2F_SIGN_REQUEST': 'u2f_sign_request',
  'U2F_REGISTER_RESPONSE': 'u2f_register_response',
  'U2F_SIGN_RESPONSE': 'u2f_sign_response',
  'U2F_GET_API_VERSION_REQUEST': 'u2f_get_api_version_request',
  'U2F_GET_API_VERSION_RESPONSE': 'u2f_get_api_version_response'
};


/**
 * Response status codes
 * @const
 * @enum {number}
 */
u2f.ErrorCodes = {
  'OK': 0,
  'OTHER_ERROR': 1,
  'BAD_REQUEST': 2,
  'CONFIGURATION_UNSUPPORTED': 3,
  'DEVICE_INELIGIBLE': 4,
  'TIMEOUT': 5
};


/**
 * A message type for registration requests
 * @typedef {{
 *   type: u2f.MessageTypes,
 *   appId: ?string,
 *   timeoutSeconds: ?number,
 *   requestId: ?number
 * }}
 */
u2f.U2fRequest;


/**
 * A message for registration responses
 * @typedef {{
 *   type: u2f.MessageTypes,
 *   responseData: (u2f.Error | u2f.RegisterResponse | u2f.SignResponse),
 *   requestId: ?number
 * }}
 */
u2f.U2fResponse;


/**
 * An error object for responses
 * @typedef {{
 *   errorCode: u2f.ErrorCodes,
 *   errorMessage: ?string
 * }}
 */
u2f.Error;

/**
 * Data object for a single sign request.
 * @typedef {enum {BLUETOOTH_RADIO, BLUETOOTH_LOW_ENERGY, USB, NFC}}
 */
u2f.Transport;


/**
 * Data object for a single sign request.
 * @typedef {Array<u2f.Transport>}
 */
u2f.Transports;

/**
 * Data object for a single sign request.
 * @typedef {{
 *   version: string,
 *   challenge: string,
 *   keyHandle: string,
 *   appId: string
 * }}
 */
u2f.SignRequest;


/**
 * Data object for a sign response.
 * @typedef {{
 *   keyHandle: string,
 *   signatureData: string,
 *   clientData: string
 * }}
 */
u2f.SignResponse;


/**
 * Data object for a registration request.
 * @typedef {{
 *   version: string,
 *   challenge: string
 * }}
 */
u2f.RegisterRequest;


/**
 * Data object for a registration response.
 * @typedef {{
 *   version: string,
 *   keyHandle: string,
 *   transports: Transports,
 *   appId: string
 * }}
 */
u2f.RegisterResponse;

/**
 * Data object for a registered key.
 * @typedef {{
 *   version: string,
 *   keyHandle: string,
 *   transports: ?Transports,
 *   appId: ?string
 * }}
 */
u2f.RegisteredKey;

/**
 * Data object for a get API register response.
 * @typedef {{
 *   js_api_version: number
 * }}
 */
u2f.GetJsApiVersionResponse;


// Low level MessagePort API support

/**
  * A message type for low-level MessagePort API registration requests
  * @typedef {{
  *   type: u2f.MessageTypes,
  *   appId: ?string,
  *   timeoutSeconds: ?number,
  *   requestId: ?number,
  *   registerRequests: Array<u2f.RegisterRequest>,
  *   registeredKeys: Array<u2f.RegisteredKey>
  * }}
 */
u2f.U2fRegisterRequest;


/**
 * Sets up a MessagePort to the U2F extension using the
 * available mechanisms.
 * @param {function((MessagePort|u2f.WrappedChromeRuntimePort_))} callback
 */
u2f.getMessagePort = function(callback) {
  if (typeof chrome != 'undefined' && chrome.runtime) {
    // The actual message here does not matter, but we need to get a reply
    // for the callback to run. Thus, send an empty signature request
    // in order to get a failure response.
    var msg = {
      type: u2f.MessageTypes.U2F_SIGN_REQUEST,
      signRequests: []
    };
    chrome.runtime.sendMessage(u2f.EXTENSION_ID, msg, function() {
      if (!chrome.runtime.lastError) {
        // We are on a whitelisted origin and can talk directly
        // with the extension.
        u2f.getChromeRuntimePort_(callback);
      } else {
        // chrome.runtime was available, but we couldn't message
        // the extension directly, use iframe
        u2f.getIframePort_(callback);
      }
    });
  } else {
    // chrome.runtime was not available at all, which is normal
    // when this origin doesn't have access to any extensions.
    u2f.getIframePort_(callback);
  }
};


/**
 * Connects directly to the extension via chrome.runtime.connect.
 * @param {function(u2f.WrappedChromeRuntimePort_)} callback
 * @private
 */
u2f.getChromeRuntimePort_ = function(callback) {
  var port = chrome.runtime.connect(u2f.EXTENSION_ID,
      {'includeTlsChannelId': true});
  setTimeout(function() {
    callback(new u2f.WrappedChromeRuntimePort_(port));
  }, 0);
};



/**
 * A wrapper for chrome.runtime.Port that is compatible with MessagePort.
 * @param {Port} port
 * @constructor
 * @private
 */
u2f.WrappedChromeRuntimePort_ = function(port) {
  this.port_ = port;
};


/**
 * Posts a message on the underlying channel.
 * @param {Object} message
 */
u2f.WrappedChromeRuntimePort_.prototype.postMessage = function(message) {
  this.port_.postMessage(message);
};


/**
 * Emulates the HTML 5 addEventListener interface. Works only for the
 * onmessage event, which is hooked up to the chrome.runtime.Port.onMessage.
 * @param {string} eventName
 * @param {function({data: Object})} handler
 */
u2f.WrappedChromeRuntimePort_.prototype.addEventListener =
    function(eventName, handler) {
  var name = eventName.toLowerCase();
  if (name == 'message' || name == 'onmessage') {
    this.port_.onMessage.addListener(function(message) {
      // Emulate a minimal MessageEvent object
      handler({'data': message});
    });
  } else {
    console.error('WrappedChromeRuntimePort only supports onMessage');
  }
};


/**
 * Sets up an embedded trampoline iframe, sourced from the extension.
 * @param {function(MessagePort)} callback
 * @private
 */
u2f.getIframePort_ = function(callback) {
  // Create the iframe
  var iframeOrigin = 'chrome-extension://' + u2f.EXTENSION_ID;
  var iframe = document.createElement('iframe');
  iframe.src = iframeOrigin + '/u2f-comms.html';
  iframe.setAttribute('style', 'display:none');
  document.body.appendChild(iframe);

  var channel = new MessageChannel();
  var ready = function(message) {
    if (message.data == 'ready') {
      channel.port1.removeEventListener('message', ready);
      callback(channel.port1);
    } else {
      console.error('First event on iframe port was not "ready"');
    }
  };
  channel.port1.addEventListener('message', ready);
  channel.port1.start();

  iframe.addEventListener('load', function() {
    // Deliver the port to the iframe and initialize
    iframe.contentWindow.postMessage('init', iframeOrigin, [channel.port2]);
  });
};


// High-level JS API


/**
 * Default extension response timeout in seconds.
 * @const
 */
u2f.EXTENSION_TIMEOUT_SEC = 30;


/**
 * A singleton instance for a MessagePort to the extension.
 * @private {MessagePort|u2f.WrappedChromeRuntimePort_}
 */
u2f.port_ = null;


/**
 * Callbacks waiting for a port
 * @private {Array<function((MessagePort|u2f.WrappedChromeRuntimePort_))>}
 */
u2f.waitingForPort_ = [];


/**
 * A counter for requestIds.
 * @private {number}
 */
u2f.reqCounter_ = 0;


/**
 * A map from requestIds to client callbacks
 * @private {Object<number,(function((u2f.Error|u2f.RegisterResponse))
 *                       |function((u2f.Error|u2f.SignResponse)))>}
 */
u2f.callbackMap_ = {};


/**
 * Creates or retrieves the MessagePort singleton to use.
 * @param {function((MessagePort|u2f.WrappedChromeRuntimePort_))} callback
 * @private
 */
u2f.getPortSingleton_ = function(callback) {
  if (u2f.port_) {
    callback(u2f.port_);
  } else {
    if (u2f.waitingForPort_.length == 0) {
      u2f.getMessagePort(function(port) {
        u2f.port_ = port;
        u2f.port_.addEventListener('message',
            /** @type {function(Event)} */ (u2f.responseHandler_));

        // Careful, here be async callbacks. Maybe.
        while (u2f.waitingForPort_.length)
          u2f.waitingForPort_.shift()(u2f.port_);
      });
    }
    u2f.waitingForPort_.push(callback);
  }
};


/**
 * Handles response messages from the extension.
 * @param {MessageEvent.<u2f.U2fResponse>} message
 * @private
 */
u2f.responseHandler_ = function(message) {
  var response = message.data;
  var reqId = response['requestId'];
  if (!reqId || !u2f.callbackMap_[reqId]) {
    console.error('Unknown or missing requestId in response.');
    return;
  }
  var cb = u2f.callbackMap_[reqId];
  delete u2f.callbackMap_[reqId];
  cb(response['responseData']);
};


/**
 * Dispatches an array of sign requests to available U2F tokens.
 * If the JS API version supported by the extension is not known, it first sends a
 * message to the extension to find out the supported API version and then it sends
 * the sign request.
 * @param {string=} appId
 * @param {string=} challenge
 * @param {Array<u2f.RegisteredKey>} registeredKeys
 * @param {function((u2f.Error|u2f.SignResponse))} callback
 * @param {number=} opt_timeoutSeconds
 */
u2f.sign = function(appId, challenge, registeredKeys, callback, opt_timeoutSeconds) {
  console.log("js_api_version: " + js_api_version);
  if (js_api_version === undefined) {
    // Send a message to get the extension to JS API version, then send the actual sign request.
    u2f.getApiVersion(
        function (response) {
          js_api_version = response['js_api_version'] === undefined ? 0 : response['js_api_version'];
          console.log("Extension JS API Version: ", js_api_version);
          u2f.sendSignRequest(appId, challenge, registeredKeys, callback, opt_timeoutSeconds);
        });
  } else {
    // We know the JS API version. Send the actual sign request in the supported API version.
    u2f.sendSignRequest(appId, challenge, registeredKeys, callback, opt_timeoutSeconds);
  }
};

/**
 * Dispatches an array of sign requests to available U2F tokens.
 * @param {string=} appId
 * @param {string=} challenge
 * @param {Array<u2f.RegisteredKey>} registeredKeys
 * @param {function((u2f.Error|u2f.SignResponse))} callback
 * @param {number=} opt_timeoutSeconds
 */
u2f.sendSignRequest = function(appId, challenge, registeredKeys, callback, opt_timeoutSeconds) {
  u2f.getPortSingleton_(function(port) {
    var reqId = ++u2f.reqCounter_;
    u2f.callbackMap_[reqId] = callback;
    var timeoutSeconds = typeof opt_timeoutSeconds !== 'undefined' ?
        opt_timeoutSeconds : u2f.EXTENSION_TIMEOUT_SEC;
    var req = u2f.formatSignRequest(appId, challenge, registeredKeys, timeoutSeconds, reqId);
    port.postMessage(req);
  });
}

/**
 * Creates a sign request object compatible with the JS version supported by the extension.
 * @param {string=} appId
 * @param {string=} challenge
 * @param {Array<u2f.RegisteredKey>} registeredKeys
 * @param {number=} timeoutSeconds
 * @param {number=} reqId
 * @return Object
 */
u2f.formatSignRequest = function(appId, challenge, registeredKeys, timeoutSeconds, reqId) {
  if (js_api_version === undefined || js_api_version < 1.1) {
    // Adapt request to the 1.0 JS API
    var signRequests = [];
    for (var i = 0; i < registeredKeys.length; i++) { 
      signRequests[i] = {
          version: registeredKeys[i].version,
          challenge: challenge,
          keyHandle: registeredKeys[i].keyHandle,
          appId: appId
      };
    }
    return {
      type: u2f.MessageTypes.U2F_SIGN_REQUEST,
      signRequests: signRequests,
      timeoutSeconds: timeoutSeconds,
      requestId: reqId
    };
  }
  // JS 1.1 API
  return {
    type: u2f.MessageTypes.U2F_SIGN_REQUEST,
    appId: appId,
    challenge: challenge,
    registeredKeys: registeredKeys,
    timeoutSeconds: timeoutSeconds,
    requestId: reqId
  };
};

/**
 * Dispatches register requests to available U2F tokens. An array of registered
 * keys identifies already registered tokens.
 * If the JS API version supported by the extension is not known, it first sends a
 * message to the extension to find out the supported API version and then it sends
 * the register request.
 * @param {string=} appId
 * @param {Array<u2f.RegisterRequest>} registerRequests
 * @param {Array<u2f.RegisteredKey>} registeredKeys
 * @param {function((u2f.Error|u2f.RegisterResponse))} callback
 * @param {number=} opt_timeoutSeconds
 */
u2f.register = function(appId, registerRequests, registeredKeys, callback, opt_timeoutSeconds) {
  if (js_api_version === undefined) {
    // Send a message to get the extension to JS API version, then send the actual register request.
    u2f.getApiVersion(
        function (response) {
          js_api_version = response['js_api_version'] === undefined ? 0: response['js_api_version'];
          console.log("Extension JS API Version: ", js_api_version);
          u2f.sendRegisterRequest(appId, registerRequests, registeredKeys,
              callback, opt_timeoutSeconds);
        });
  } else {
    // We know the JS API version. Send the actual register request in the supported API version.
    u2f.sendRegisterRequest(appId, registerRequests, registeredKeys,
        callback, opt_timeoutSeconds);
  }
};


/**
 * Dispatches register requests to available U2F tokens. An array of registered keys
 * identifies already registered tokens.
 * @param {string=} appId
 * @param {Array<u2f.RegisterRequest>} registerRequests
 * @param {Array<u2f.RegisteredKey>} registeredKeys
 * @param {function((u2f.Error|u2f.RegisterResponse))} callback
 * @param {number=} opt_timeoutSeconds
 */
u2f.sendRegisterRequest = function(appId, registerRequests, registeredKeys, callback, opt_timeoutSeconds) {
  u2f.getPortSingleton_(function(port) {
    var reqId = ++u2f.reqCounter_;
    u2f.callbackMap_[reqId] = callback;
    var timeoutSeconds = typeof opt_timeoutSeconds !== 'undefined' ?
        opt_timeoutSeconds : u2f.EXTENSION_TIMEOUT_SEC;
    var req = u2f.formatRegisterRequest(appId, registerRequests, registeredKeys, timeoutSeconds, reqId);
    port.postMessage(req);
  });
};


/**
 * Creates a register request object compatible with the JS version supported by the extension.
 * @param {string=} appId
 * @param {Array<u2f.RegisterRequest>} registerRequests
 * @param {Array<u2f.RegisteredKey>} registeredKeys
 * @param {number=} timeoutSeconds
 * @param {number=} reqId
 * @return Object
 */
u2f.formatRegisterRequest = function(appId, registerRequests, registeredKeys, timeoutSeconds, reqId) {
  if (js_api_version === undefined || js_api_version < 1.1) {
    // Adapt request to the 1.0 JS API
    for (var i = 0; i < registerRequests.length; i++) { 
      registerRequests[i].appId = appId;
    } 
    var signRequests = []; 
    for (var i = 0; i < registeredKeys.length; i++) { 
      signRequests[i] = {
          version: registeredKeys[i].version,
          challenge: registerRequests[0],
          keyHandle: registeredKeys[i].keyHandle,
          appId: appId
      };
    }
    return {
      type: u2f.MessageTypes.U2F_REGISTER_REQUEST,
      signRequests: signRequests,
      registerRequests: registerRequests,
      timeoutSeconds: timeoutSeconds,
      requestId: reqId
    };
  }
  // JS 1.1 API
  return {
    type: u2f.MessageTypes.U2F_REGISTER_REQUEST,
    appId: appId,
    registerRequests: registerRequests,
    registeredKeys: registeredKeys,
    timeoutSeconds: timeoutSeconds,
    requestId: reqId
  }
};


/**
 * Dispatches a message to the extension to find out the supported
 * JS API version.
 * @param {function((u2f.Error|u2f.GetJsApiVersionResponse))} callback
 * @param {number=} opt_timeoutSeconds
 */
u2f.getApiVersion = function(callback, opt_timeoutSeconds) {
 u2f.getPortSingleton_(function(port) {
    var reqId = ++u2f.reqCounter_;
    u2f.callbackMap_[reqId] = callback;
    var req = {
      type: u2f.MessageTypes.U2F_GET_API_VERSION_REQUEST,
      timeoutSeconds: (typeof opt_timeoutSeconds !== 'undefined' ?
          opt_timeoutSeconds : u2f.EXTENSION_TIMEOUT_SEC),
      requestId: reqId
    };
    port.postMessage(req);
  });
};
