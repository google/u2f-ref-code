// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

'use strict';

var u2fcomms = {};


/**
 * A message broker running in an embedded iframe. It routes messages between a
 * 3rd party U2F consumer page and the U2F extension.
 * @constructor
 */
u2fcomms.Forwarder = function() {
  /**
   * @private {Port}
   */
  this.extensionPort_ = null;

  /**
   * @private {MessagePort}
   */
  this.pagePort_ = null;

  /**
   * @private {string?}
   */
  this.pageOrigin_ = null;
};


/**
 * Initializes the forwarder
 */
u2fcomms.Forwarder.prototype.init = function() {
  var self = this;
  window.addEventListener('message', function(message) {
    if (message.data == 'init' && message.ports.length > 0) {
      self.pageOrigin_ = message.origin;
      self.pagePort_ = message.ports[0];
      self.pagePort_.addEventListener('message',
          self.onPageMessage_.bind(self));
      self.pagePort_.start();
      self.connectToExtension_();

      // Tell the page we are ready
      self.pagePort_.postMessage('ready');
    } else {
      console.error('U2F iframe received non-init message');
    }
  }, false);
};


/**
 * Handles messages from the page, forwarding to the extension.
 * @param {MessageEvent} event The message event
 * @private
 */
u2fcomms.Forwarder.prototype.onPageMessage_ = function(event) {
  if (!this.extensionPort_) {
    console.error("Couldn't connect to extension.");
    return;
  }

  var message = {
    origin: this.pageOrigin_,
    request: event.data
  };
  this.extensionPort_.postMessage(message);
};


/**
 * Handles messages from the extension, forwarding to the page.
 * @param {*} message
 * @private
 */
u2fcomms.Forwarder.prototype.onExtensionMessage_ = function(message) {
  this.pagePort_.postMessage(message);
};


/**
 * Connects to the extension if need be.
 * @private
 */
u2fcomms.Forwarder.prototype.connectToExtension_ = function() {
  if (this.extensionPort_)
    return;
  this.extensionPort_ = chrome.runtime.connect();
  if (this.extensionPort_) {
    this.extensionPort_.onMessage.addListener(
        this.onExtensionMessage_.bind(this));
  }
};


/**
 * Initializes the forwarder.
 */
u2fcomms.initialize = function() {
  var fwdr = new u2fcomms.Forwarder();
  fwdr.init();

  // store the forwarder instance in the global scope for debugging
  u2fcomms.forwarderInstance = fwdr;
};

// Initialize only if we are in our frame
if (window.location.pathname == '/u2f-comms.html') {
  u2fcomms.initialize();
}
