// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Implements a low-level gnubby driver based on chrome.hid.
 */
'use strict';

/**
 * Low level gnubby 'driver'. One per physical USB device.
 * @param {Gnubbies} gnubbies The gnubbies instances this device is enumerated
 *     in.
 * @param {!chrome.hid.HidConnectInfo} dev The connection to the device.
 * @param {number} id The device's id.
 * @constructor
 * @implements {llGnubby}
 */
function llHidGnubby(gnubbies, dev, id) {
  /** @private {Gnubbies} */
  this.gnubbies_ = gnubbies;
  this.dev = dev;
  this.id = id;
  this.txqueue = [];
  this.clients = [];
  this.lockCID = 0;     // channel ID of client holding a lock, if != 0.
  this.lockMillis = 0;  // current lock period.
  this.lockTID = null;  // timer id of lock timeout.
  this.closing = false;  // device to be closed by receive loop.
  this.updating = false;  // device firmware is in final stage of updating.
}

/**
 * Namespace for the llHidGnubby implementation.
 * @const
 */
llHidGnubby.NAMESPACE = 'hid';

/** Destroys this low-level device instance. */
llHidGnubby.prototype.destroy = function() {
  if (!this.dev) return;  // Already dead.

  this.gnubbies_.removeOpenDevice(
      {namespace: llHidGnubby.NAMESPACE, device: this.id});
  this.closing = true;

  console.log(UTIL_fmt('llHidGnubby.destroy()'));

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

  chrome.hid.disconnect(dev.connectionId, function() {
    console.log(UTIL_fmt('Device ' + dev.connectionId + ' closed'));
  });
};

/**
 * Push frame to all clients.
 * @param {ArrayBuffer} f Data to push
 * @private
 */
llHidGnubby.prototype.publishFrame_ = function(f) {
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
 * Register a client for this gnubby.
 * @param {*} who The client.
 */
llHidGnubby.prototype.registerClient = function(who) {
  for (var i = 0; i < this.clients.length; ++i) {
    if (this.clients[i] === who) return;  // Already registered.
  }
  this.clients.push(who);
  if (this.clients.length == 1) {
    // First client? Kick off read loop.
    this.readLoop_();
  }
};

/**
 * De-register a client.
 * @param {*} who The client.
 * @return {number} The number of remaining listeners for this device, or -1
 * Returns number of remaining listeners for this device.
 *     if this had no clients to start with.
 */
llHidGnubby.prototype.deregisterClient = function(who) {
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
llHidGnubby.prototype.hasClient = function(who) {
  if (this.clients.length == 0) return false;
  for (var i = 0; i < this.clients.length; ++i) {
    if (who === this.clients[i])
      return true;
  }
  return false;
};

/**
 * Reads all incoming frames and notifies clients of their receipt.
 * @private
 */
llHidGnubby.prototype.readLoop_ = function() {
  //console.log(UTIL_fmt('entering readLoop'));
  if (!this.dev) return;

  if (this.closing) {
    this.destroy();
    return;
  }

  // No interested listeners, yet we hit readLoop().
  // Must be clean-up. We do this here to make sure no transfer is pending.
  if (!this.clients.length) {
    this.closing = true;
    this.destroy();
    return;
  }

  // firmwareUpdate() sets this.updating when writing the last block before
  // the signature. We process that reply with the already pending
  // read transfer but we do not want to start another read transfer for the
  // signature block, since that request will have no reply.
  // Instead we will see the device drop and re-appear on the bus.
  // Current libusb on some platforms gets unhappy when transfer are pending
  // when that happens.
  // TODO: revisit once Chrome stabilizes its behavior.
  if (this.updating) {
    console.log(UTIL_fmt('device updating. Ending readLoop()'));
    return;
  }

  var self = this;
  chrome.hid.receive(
    this.dev.connectionId,
    64,
    function(x) {
      if (chrome.runtime.lastError || !x) {
        console.log(UTIL_fmt('got lastError'));
        console.log(chrome.runtime.lastError);
        window.setTimeout(function() { self.destroy(); }, 0);
        return;
      }
      var u8 = new Uint8Array(x);
      console.log(UTIL_fmt('<' + UTIL_BytesToHex(u8)));

      self.publishFrame_(x);

      // Read more.
      window.setTimeout(function() { self.readLoop_(); }, 0);
    }
  );
};

/**
 * Check whether channel is locked for this request or not.
 * @param {number} cid Channel id
 * @param {number} cmd Request command
 * @return {boolean} true if not locked for this request.
 * @private
 */
llHidGnubby.prototype.checkLock_ = function(cid, cmd) {
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
 * @param {number} cid Channel ID
 * @param {number} cmd Command
 * @param {number} arg Command argument
 * @private
 */
llHidGnubby.prototype.updateLock_ = function(cid, cmd, arg) {
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
 * @param {ArrayBuffer|Uint8Array} data Command arguments
 */
llHidGnubby.prototype.queueCommand = function(cid, cmd, data) {
  if (!this.dev) return;
  if (!this.checkLock_(cid, cmd)) return;

  var u8 = new Uint8Array(data);
  var f = new Uint8Array(64);

  llHidGnubby.setCid_(f, cid);
  f[4] = cmd;
  f[5] = (u8.length >> 8);
  f[6] = (u8.length & 255);

  var lockArg = (u8.length > 0) ? u8[0] : 0;

  // Fragment over our 64 byte frames.
  var n = 7;
  var seq = 0;
  for (var i = 0; i < u8.length; ++i) {
    f[n++] = u8[i];
    if (n == f.length) {
      this.queueFrame_(f.buffer, cid, cmd, lockArg);

      f = new Uint8Array(64);
      llHidGnubby.setCid_(f, cid);
      cmd = f[4] = seq++;
      n = 5;
    }
  }
  if (n != 5) {
    this.queueFrame_(f.buffer, cid, cmd, lockArg);
  }
};

/**
 * Sets the channel id in the frame.
 * @param {Uint8Array} frame Data frame
 * @param {number} cid The client's channel ID.
 * @private
 */
llHidGnubby.setCid_ = function(frame, cid) {
  frame[0] = cid >>> 24;
  frame[1] = cid >>> 16;
  frame[2] = cid >>> 8;
  frame[3] = cid;
};

/**
 * Updates the lock, and queues the frame for sending. Also begins sending if
 * no other writes are outstanding.
 * @param {ArrayBuffer} frame Data frame
 * @param {number} cid The client's channel ID.
 * @param {number} cmd The command to send.
 * @param {number} arg Command argument
 * @private
 */
llHidGnubby.prototype.queueFrame_ = function(frame, cid, cmd, arg) {
  this.updateLock_(cid, cmd, arg);
  var wasEmpty = (this.txqueue.length == 0);
  this.txqueue.push(frame);
  if (wasEmpty) this.writePump_();
};

/**
 * Stuff queued frames from txqueue[] to device, one by one.
 * @private
 */
llHidGnubby.prototype.writePump_ = function() {
  if (!this.dev) return;  // Ignore.

  if (this.txqueue.length == 0) return;  // Done with current queue.

  var frame = this.txqueue[0];

  var self = this;
  function transferComplete() {
    if (chrome.runtime.lastError) {
      console.log(UTIL_fmt('got lastError'));
      console.log(chrome.runtime.lastError);
      window.setTimeout(function() { self.destroy(); }, 0);
      return;
    }
    self.txqueue.shift();  // drop sent frame from queue.
    if (self.txqueue.length != 0) {
      window.setTimeout(function() { self.writePump_(); }, 0);
    }
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

  var u8f = new Uint8Array(64);
  for (var i = 0; i < u8.length; ++i) {
    u8f[i] = u8[i];
  }

  chrome.hid.send(
      this.dev.connectionId,
      0, // report Id
      u8f.buffer,
      transferComplete
  );
};

/**
 * @param {function(Array)} cb Enumeration callback
 */
llHidGnubby.enumerate = function(cb) {
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
      chrome.hid.getDevices(devs[i], enumerated);
    }
  });
};

/**
 * @param {Gnubbies} gnubbies The gnubbies instances this device is enumerated
 *     in.
 * @param {number} which The index of the device to open.
 * @param {!chrome.hid.HidDeviceInfo} dev The device to open.
 * @param {function(number, llGnubby=)} cb Called back with the
 *     result of opening the device.
 */
llHidGnubby.open = function(gnubbies, which, dev, cb) {
  chrome.hid.connect(dev.deviceId, function(handle) {
    if (chrome.runtime.lastError) {
      console.log(chrome.runtime.lastError);
    }
    if (!handle) {
      console.warn(UTIL_fmt('failed to connect device. permissions issue?'));
      cb(-llGnubby.NODEVICE);
      return;
    }
    var nonNullHandle = /** @type {!chrome.hid.HidConnectInfo} */ (handle);
    var gnubby = new llHidGnubby(gnubbies, nonNullHandle, which);
    cb(-llGnubby.OK, gnubby);
  });
};

/**
 * @param {*} dev A browser API device object
 * @return {llGnubbyDeviceId} A device identifier for the device.
 */
llHidGnubby.deviceToDeviceId = function(dev) {
  var hidDev = /** @type {!chrome.hid.HidDeviceInfo} */ (dev);
  var deviceId = { namespace: llHidGnubby.NAMESPACE, device: hidDev.deviceId };
  return deviceId;
};

/**
 * Registers this implementation with gnubbies.
 * @param {Gnubbies} gnubbies Gnubbies registry
 */
llHidGnubby.register = function(gnubbies) {
  var HID_GNUBBY_IMPL = {
    enumerate: llHidGnubby.enumerate,
    deviceToDeviceId: llHidGnubby.deviceToDeviceId,
    open: llHidGnubby.open
  };
  gnubbies.registerNamespace(llHidGnubby.NAMESPACE, HID_GNUBBY_IMPL);
};
