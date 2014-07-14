// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Interface for representing a low-level gnubby device.
 */
'use strict';

/**
 * Low level gnubby 'driver'. One per physical USB device.
 * @interface
 */
function llGnubby() {}

// Commands of the USB interface.
/** Echo data through local processor only */
llGnubby.CMD_PING = 0x81;
/** Perform reset action and read ATR string */
llGnubby.CMD_ATR = 0x82;
/** Send raw APDU */
llGnubby.CMD_APDU = 0x83;
/** Send lock channel command */
llGnubby.CMD_LOCK = 0x84;
/** Obtain system information record */
llGnubby.CMD_SYSINFO = 0x85;
/** Obtain an unused channel ID */
llGnubby.CMD_INIT = 0x86;
/** Control prompt flashing */
llGnubby.CMD_PROMPT = 0x87;
/** Send device identification wink */
llGnubby.CMD_WINK = 0x88;
/** USB test */
llGnubby.CMD_USB_TEST = 0xb9;
/** Device Firmware Upgrade */
llGnubby.CMD_DFU = 0xba;
/** Protocol resync command */
llGnubby.CMD_SYNC = 0xbc;
/** Error response */
llGnubby.CMD_ERROR = 0xbf;

// Low-level error codes.
/** No error */
llGnubby.OK = 0;
/** Invalid command */
llGnubby.INVALID_CMD = 1;
/** Invalid parameter */
llGnubby.INVALID_PAR = 2;
/** Invalid message length */
llGnubby.INVALID_LEN = 3;
/** Invalid message sequencing */
llGnubby.INVALID_SEQ = 4;
/** Message has timed out */
llGnubby.TIMEOUT = 5;
/** CHannel is busy */
llGnubby.BUSY = 6;
/** Access denied */
llGnubby.ACCESS_DENIED = 7;
/** Device is gone */
llGnubby.GONE = 8;
/** Verification error */
llGnubby.VERIFY_ERROR = 9;
/** Command requires channel lock */
llGnubby.LOCK_REQUIRED = 10;
/** Sync error */
llGnubby.SYNC_FAIL = 11;
/** Other unspecified error */
llGnubby.OTHER = 127;

// Remote helper errors.
/** Not a remote helper */
llGnubby.NOTREMOTE = 263;
/** Could not reach remote endpoint */
llGnubby.COULDNOTDIAL = 264;

// chrome.usb-related errors.
/** No device */
llGnubby.NODEVICE = 512;
/** Permission denied */
llGnubby.NOPERMISSION = 666;

/** Destroys this low-level device instance. */
llGnubby.prototype.destroy = function() {};

/**
 * Register a client for this gnubby.
 * @param {*} who The client.
 */
llGnubby.prototype.registerClient = function(who) {};

/**
 * De-register a client.
 * @param {*} who The client.
 * @return {number} The number of remaining listeners for this device, or -1
 *     if this had no clients to start with.
 */
llGnubby.prototype.deregisterClient = function(who) {};

/**
 * @param {*} who The client.
 * @return {boolean} Whether this device has who as a client.
 */
llGnubby.prototype.hasClient = function(who) {};

/**
 * Queue command to be sent.
 * If queue was empty, initiate the write.
 * @param {number} cid The client's channel ID.
 * @param {number} cmd The command to send.
 * @param {ArrayBuffer|Uint8Array} data Command data
 */
llGnubby.prototype.queueCommand = function(cid, cmd, data) {};

/**
 * @typedef {{
 *   vendorId: number,
 *   productId: number
 * }}
 */
var UsbDeviceSpec;

/**
 * Gets the list of USB devices permitted by this app.
 * @param {function(!Array.<!UsbDeviceSpec>)} cb Called back with a list of USB
 *     device specifiers.
 */
llGnubby.getPermittedUsbDevices = function(cb) {
  chrome.permissions.getAll(function(perms) {
    if (!perms.hasOwnProperty('permissions')) {
      cb([]);
      return;
    }
    var devs = [];
    var permissions = perms['permissions'];
    for (var i = 0; i < permissions.length; i++) {
      var permission = permissions[i];
      if (typeof permission === 'object' &&
          permission.hasOwnProperty('usbDevices')) {
        for (var j = 0; j < permission['usbDevices'].length; j++) {
          var dev = permission['usbDevices'][j];
          devs.push(
              {'vendorId': dev['vendorId'], 'productId': dev['productId']});
        }
      }
    }
    cb(devs);
  });
};
