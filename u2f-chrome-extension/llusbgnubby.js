// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Implements a low-level gnubby driver based on chrome.usb.
 */
'use strict';

/**
 * Low level gnubby 'driver'. One per physical USB device.
 * @param {Gnubbies} gnubbies The gnubbies instances this device is enumerated
 *     in.
 * @param {!chrome.usb.ConnectionHandle} dev The device.
 * @param {number} id The device's id.
 * @param {number} inEndpoint The device's in endpoint.
 * @param {number} outEndpoint The device's out endpoint.
 * @constructor
 * @implements {llGnubby}
 */
function llUsbGnubby(gnubbies, dev, id, inEndpoint, outEndpoint) {
  /** @private {Gnubbies} */
  this.gnubbies_ = gnubbies;
  this.dev = dev;
  this.id = id;
  this.inEndpoint = inEndpoint;
  this.outEndpoint = outEndpoint;
  this.txqueue = [];
  this.clients = [];
  this.lockCID = 0;     // channel ID of client holding a lock, if != 0.
  this.lockMillis = 0;  // current lock period.
  this.lockTID = null;  // timer id of lock timeout.
  this.closing = false;  // device to be closed by receive loop.
  this.updating = false;  // device firmware is in final stage of updating.
  this.inTransferPending = false;
  this.outTransferPending = false;
}

/**
 * Namespace for the llUsbGnubby implementation.
 * @const
 */
llUsbGnubby.NAMESPACE = 'usb';

/** Destroys this low-level device instance. */
llUsbGnubby.prototype.destroy = function() {
  if (!this.dev) return;  // Already dead.

  this.gnubbies_.removeOpenDevice(
      {namespace: llUsbGnubby.NAMESPACE, device: this.id});
  this.closing = true;

  console.log(UTIL_fmt('llUsbGnubby.destroy()'));

  // Synthesize a close error frame to alert all clients,
  // some of which might be in read state.
  //
  // Use magic CID 0 to address all.
  this.publishFrame_(new Uint8Array([
        0, 0, 0, 0,  // broadcast CID
        llGnubby.CMD_ERROR,
        0, 1,  // length
        llGnubby.GONE]).buffer);

  // Set all clients to closed status and remove them.
  while (this.clients.length != 0) {
    var client = this.clients.shift();
    if (client) client.closed = true;
  }

  if (this.lockTID) {
    window.clearTimeout(this.lockTID);
    this.lockTID = null;
  }

  var dev = this.dev;
  this.dev = null;

  chrome.usb.releaseInterface(dev, 0, function() {
    console.log(UTIL_fmt('Device ' + dev.handle + ' released'));
    chrome.usb.closeDevice(dev, function() {
      console.log(UTIL_fmt('Device ' + dev.handle + ' closed'));
    });
  });
};

/**
 * Push frame to all clients.
 * @param {ArrayBuffer} f Data frame
 * @private
 */
llUsbGnubby.prototype.publishFrame_ = function(f) {
  var old = this.clients;

  var remaining = [];
  var changes = false;
  for (var i = 0; i < old.length; ++i) {
    var client = old[i];
    if (client.receivedFrame(f)) {
      // Client still alive; keep on list.
      remaining.push(client);
    } else {
      changes = true;
      console.log(UTIL_fmt(
          '[' + client.cid.toString(16) + '] left?'));
    }
  }
  if (changes) this.clients = remaining;
};

/**
 * @return {boolean} whether this device is open and ready to use.
 * @private
 */
llUsbGnubby.prototype.readyToUse_ = function() {
  if (this.closing) return false;
  if (!this.dev) return false;

  return true;
};

/**
 * Reads one reply from the low-level device.
 * @private
 */
llUsbGnubby.prototype.readOneReply_ = function() {
  if (!this.readyToUse_()) return;  // No point in continuing.
  if (this.updating) return;  // Do not bother waiting for final update reply.

  var self = this;

  function inTransferComplete(x) {
    self.inTransferPending = false;

    if (!self.readyToUse_()) return;  // No point in continuing.

    if (chrome.runtime.lastError) {
      console.warn(UTIL_fmt('lastError: ' + chrome.runtime.lastError));
      console.log(chrome.runtime.lastError);
      window.setTimeout(function() { self.destroy(); }, 0);
      return;
    }

    if (x.data) {
      var u8 = new Uint8Array(x.data);
      console.log(UTIL_fmt('<' + UTIL_BytesToHex(u8)));

      self.publishFrame_(x.data);

      // Write another pending request, if any.
      window.setTimeout(
          function() {
            self.txqueue.shift();  // Drop sent frame from queue.
            self.writeOneRequest_();
          },
          0);
    } else {
      console.log(UTIL_fmt('no x.data!'));
      console.log(x);
      window.setTimeout(function() { self.destroy(); }, 0);
    }
  }

  if (this.inTransferPending == false) {
    this.inTransferPending = true;
    chrome.usb.bulkTransfer(
      /** @type {!chrome.usb.ConnectionHandle} */(this.dev),
      { direction: 'in', endpoint: this.inEndpoint, length: 2048 },
      inTransferComplete);
  } else {
    throw 'inTransferPending!';
  }
};

/**
 * Register a client for this gnubby.
 * @param {*} who The client.
 */
llUsbGnubby.prototype.registerClient = function(who) {
  for (var i = 0; i < this.clients.length; ++i) {
    if (this.clients[i] === who) return;  // Already registered.
  }
  this.clients.push(who);
};

/**
 * De-register a client.
 * @param {*} who The client.
 * @return {number} The number of remaining listeners for this device, or -1
 * Returns number of remaining listeners for this device.
 *     if this had no clients to start with.
 */
llUsbGnubby.prototype.deregisterClient = function(who) {
  var current = this.clients;
  if (current.length == 0) return -1;
  this.clients = [];
  for (var i = 0; i < current.length; ++i) {
    var client = current[i];
    if (client !== who) this.clients.push(client);
  }
  return this.clients.length;
};

/**
 * @param {*} who The client.
 * @return {boolean} Whether this device has who as a client.
 */
llUsbGnubby.prototype.hasClient = function(who) {
  if (this.clients.length == 0) return false;
  for (var i = 0; i < this.clients.length; ++i) {
    if (who === this.clients[i])
      return true;
  }
  return false;
};

/**
 * Stuff queued frames from txqueue[] to device, one by one.
 * @private
 */
llUsbGnubby.prototype.writeOneRequest_ = function() {
  if (!this.readyToUse_()) return;  // No point in continuing.

  if (this.txqueue.length == 0) return;  // Nothing to send.

  var frame = this.txqueue[0];

  var self = this;
  function OutTransferComplete(x) {
    self.outTransferPending = false;

    if (!self.readyToUse_()) return;  // No point in continuing.

    if (chrome.runtime.lastError) {
      console.warn(UTIL_fmt('lastError: ' + chrome.runtime.lastError));
      console.log(chrome.runtime.lastError);
      window.setTimeout(function() { self.destroy(); }, 0);
      return;
    }

    window.setTimeout(function() { self.readOneReply_(); }, 0);
  };

  var u8 = new Uint8Array(frame);

  // See whether this requires scrubbing before logging.
  var alternateLog = usbGnubby.hasOwnProperty('redactRequestLog') &&
                     usbGnubby['redactRequestLog'](u8);
  if (alternateLog) {
    console.log(UTIL_fmt('>' + alternateLog));
  } else {
    console.log(UTIL_fmt('>' + UTIL_BytesToHex(u8)));
  }

  if (this.outTransferPending == false) {
    this.outTransferPending = true;
    chrome.usb.bulkTransfer(
        /** @type {!chrome.usb.ConnectionHandle} */(this.dev),
        { direction: 'out', endpoint: this.outEndpoint, data: frame },
        OutTransferComplete);
  } else {
    throw 'outTransferPending!';
  }
};

/**
 * Check whether channel is locked for this request or not.
 * @param {number} cid Channel id
 * @param {number} cmd Command to be sent
 * @return {boolean} true if not locked for this request.
 * @private
 */
llUsbGnubby.prototype.checkLock_ = function(cid, cmd) {
  if (this.lockCID) {
    // We have an active lock.
    if (this.lockCID != cid) {
      // Some other channel has active lock.

      if (cmd != llGnubby.CMD_SYNC) {
        // Anything but SYNC gets an immediate busy.
        var busy = new Uint8Array(
            [(cid >> 24) & 255,
             (cid >> 16) & 255,
             (cid >> 8) & 255,
             cid & 255,
             llGnubby.CMD_ERROR,
             0, 1,  // length
             llGnubby.BUSY]);
        // Log the synthetic busy too.
        console.log(UTIL_fmt('<' + UTIL_BytesToHex(busy)));
        this.publishFrame_(busy.buffer);
        return false;
      }

      // SYNC gets to go to the device to flush OS tx/rx queues.
      // The usb firmware always responds to SYNC, regardless of lock status.
    }
  }
  return true;
};

/**
 * Update or grab lock.
 * @param {number} cid Channel id
 * @param {number} cmd Command
 * @param {number} arg Command argument
 * @private
 */
llUsbGnubby.prototype.updateLock_ = function(cid, cmd, arg) {
  if (this.lockCID == 0 || this.lockCID == cid) {
    // It is this caller's or nobody's lock.
    if (this.lockTID) {
      window.clearTimeout(this.lockTID);
      this.lockTID = null;
    }

    if (cmd == llGnubby.CMD_LOCK) {
      var nseconds = arg;
      if (nseconds != 0) {
        this.lockCID = cid;
        // Set tracking time to be .1 seconds longer than usb device does.
        this.lockMillis = nseconds * 1000 + 100;
      } else {
        // Releasing lock voluntarily.
        this.lockCID = 0;
      }
    }

    // (re)set the lock timeout if we still hold it.
    if (this.lockCID) {
      var self = this;
      this.lockTID = window.setTimeout(
          function() {
            console.warn(UTIL_fmt(
                'lock for CID ' + cid.toString(16) + ' expired!'));
            self.lockTID = null;
            self.lockCID = 0;
          },
          this.lockMillis);
    }
  }
};

/**
 * Queue command to be sent.
 * If queue was empty, initiate the write.
 * @param {number} cid The client's channel ID.
 * @param {number} cmd The command to send.
 * @param {ArrayBuffer|Uint8Array} data Command argument data
 */
llUsbGnubby.prototype.queueCommand = function(cid, cmd, data) {
  if (!this.dev) return;
  if (!this.checkLock_(cid, cmd)) return;

  var u8 = new Uint8Array(data);
  var frame = new Uint8Array(u8.length + 7);

  frame[0] = cid >>> 24;
  frame[1] = cid >>> 16;
  frame[2] = cid >>> 8;
  frame[3] = cid;
  frame[4] = cmd;
  frame[5] = (u8.length >> 8);
  frame[6] = (u8.length & 255);

  frame.set(u8, 7);

  var lockArg = (u8.length > 0) ? u8[0] : 0;
  this.updateLock_(cid, cmd, lockArg);

  var wasEmpty = (this.txqueue.length == 0);
  this.txqueue.push(frame.buffer);
  if (wasEmpty) this.writeOneRequest_();
};

/**
 * @param {function(Array)} cb Enumerate callback
 */
llUsbGnubby.enumerate = function(cb) {
  var permittedDevs;
  var numEnumerated = 0;
  var allDevs = [];

  function enumerated(devs) {
    allDevs = allDevs.concat(devs);
    if (++numEnumerated == permittedDevs.length) {
      cb(allDevs);
    }
  }

  llGnubby.getPermittedUsbDevices(function(devs) {
    permittedDevs = devs;
    for (var i = 0; i < devs.length; i++) {
      chrome.usb.getDevices(devs[i], enumerated);
    }
  });
};

/**
 * @param {Gnubbies} gnubbies The gnubbies instances this device is enumerated
 *     in.
 * @param {number} which The index of the device to open.
 * @param {!chrome.usb.Device} dev The device to open.
 * @param {function(number, llGnubby=)} cb Called back with the
 *     result of opening the device.
 */
llUsbGnubby.open = function(gnubbies, which, dev, cb) {
  /** @param {chrome.usb.ConnectionHandle=} handle Connection handle */
  function deviceOpened(handle) {
    if (!handle) {
      console.warn(UTIL_fmt('failed to open device. permissions issue?'));
      cb(-llGnubby.NODEVICE);
      return;
    }
    var nonNullHandle = /** @type {!chrome.usb.ConnectionHandle} */ (handle);
    chrome.usb.listInterfaces(nonNullHandle, function(descriptors) {
      var inEndpoint, outEndpoint;
      for (var i = 0; i < descriptors.length; i++) {
        var descriptor = descriptors[i];
        for (var j = 0; j < descriptor.endpoints.length; j++) {
          var endpoint = descriptor.endpoints[j];
          if (inEndpoint == undefined && endpoint.type == 'bulk' &&
              endpoint.direction == 'in') {
            inEndpoint = endpoint.address;
          }
          if (outEndpoint == undefined && endpoint.type == 'bulk' &&
              endpoint.direction == 'out') {
            outEndpoint = endpoint.address;
          }
        }
      }
      if (inEndpoint == undefined || outEndpoint == undefined) {
        console.warn(UTIL_fmt('device lacking an endpoint (broken?)'));
        chrome.usb.closeDevice(nonNullHandle);
        cb(-llGnubby.NODEVICE);
        return;
      }
      // Try getting it claimed now.
      chrome.usb.claimInterface(nonNullHandle, 0, function() {
        if (chrome.runtime.lastError) {
          console.warn(UTIL_fmt('lastError: ' + chrome.runtime.lastError));
          console.log(chrome.runtime.lastError);
        }
        var claimed = !chrome.runtime.lastError;
        if (!claimed) {
          console.warn(UTIL_fmt('failed to claim interface. busy?'));
          // Claim failed? Let the callers know and bail out.
          chrome.usb.closeDevice(nonNullHandle);
          cb(-llGnubby.BUSY);
          return;
        }
        var gnubby = new llUsbGnubby(gnubbies, nonNullHandle, which, inEndpoint,
            outEndpoint);
        cb(-llGnubby.OK, gnubby);
      });
    });
  }

  if (llUsbGnubby.runningOnCrOS === undefined) {
    llUsbGnubby.runningOnCrOS =
        (window.navigator.appVersion.indexOf('; CrOS ') != -1);
  }
  if (llUsbGnubby.runningOnCrOS) {
    chrome.usb.requestAccess(dev, 0, function(success) {
      // Even though the argument to requestAccess is a chrome.usb.Device, the
      // access request is for access to all devices with the same vid/pid.
      // Curiously, if the first chrome.usb.requestAccess succeeds, a second
      // call with a separate device with the same vid/pid fails. Since
      // chrome.usb.openDevice will fail if a previous access request really
      // failed, just ignore the outcome of the access request and move along.
      chrome.usb.openDevice(dev, deviceOpened);
    });
  } else {
    chrome.usb.openDevice(dev, deviceOpened);
  }
};

/**
 * @param {*} dev Chrome usb device
 * @return {llGnubbyDeviceId} A device identifier for the device.
 */
llUsbGnubby.deviceToDeviceId = function(dev) {
  var usbDev = /** @type {!chrome.usb.Device} */ (dev);
  var deviceId = { namespace: llUsbGnubby.NAMESPACE, device: usbDev.device };
  return deviceId;
};

/**
 * Registers this implementation with gnubbies.
 * @param {Gnubbies} gnubbies Gnubbies singleton instance
 */
llUsbGnubby.register = function(gnubbies) {
  var USB_GNUBBY_IMPL = {
    enumerate: llUsbGnubby.enumerate,
    deviceToDeviceId: llUsbGnubby.deviceToDeviceId,
    open: llUsbGnubby.open
  };
  gnubbies.registerNamespace(llUsbGnubby.NAMESPACE, USB_GNUBBY_IMPL);
};
