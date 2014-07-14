// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Low level usb cruft to talk gnubby.
 */

'use strict';

/**
 * Creates a worker Gnubby instance.
 * @constructor
 * @param {number=} opt_busySeconds to retry an exchange upon a BUSY result.
 */
function usbGnubby(opt_busySeconds) {
  this.dev = null;
  this.gnubbyInstance = ++usbGnubby.gnubbyId_;
  this.cid = usbGnubby.BROADCAST_CID;
  this.rxframes = [];
  this.synccnt = 0;
  this.rxcb = null;
  this.closed = false;
  this.commandPending = false;
  this.notifyOnClose = [];
  this.busyMillis = (opt_busySeconds ? opt_busySeconds * 1000 : 2500);
}

/**
 * Global Gnubby instance counter.
 * @private {number}
 */
usbGnubby.gnubbyId_ = 0;

/**
 * Sets usbGnubby's Gnubbies singleton.
 * @param {Gnubbies} gnubbies Gnubbies singleton instance
 */
usbGnubby.setGnubbies = function(gnubbies) {
  /** @private {Gnubbies} */
  usbGnubby.gnubbies_ = gnubbies;
};

/**
 * Opens the gnubby with the given index, or the first found gnubby if no
 * index is specified.
 * @param {llGnubbyDeviceId} which The device to open. If null, the first
 *     gnubby found is opened.
 * @param {function(number)|undefined} opt_cb Called with result of opening the
 *     gnubby.
 */
usbGnubby.prototype.open = function(which, opt_cb) {
  var cb = opt_cb ? opt_cb : usbGnubby.defaultCallback;
  if (this.closed) {
    cb(-llGnubby.NODEVICE);
    return;
  }
  this.closingWhenIdle = false;

  var self = this;

  function setCid(which) {
    // Set a default channel ID, in case the caller never sets a better one.
    self.cid = usbGnubby.defaultChannelId_(self.gnubbyInstance, which);
  }

  var enumerateRetriesRemaining = 3;
  function enumerated(rc, devs) {
    if (!devs.length)
      rc = -llGnubby.NODEVICE;
    if (rc) {
      cb(rc);
      return;
    }
    which = devs[0];
    setCid(which);
    self.which = which;
    usbGnubby.gnubbies_.addClient(which, self, function(rc, device) {
      if (rc == -llGnubby.NODEVICE && enumerateRetriesRemaining-- > 0) {
        // We were trying to open the first device, but now it's not there?
        // Do over.
        usbGnubby.gnubbies_.enumerate(enumerated);
        return;
      }
      self.dev = device;
      cb(rc);
    });
  }

  if (which) {
    setCid(which);
    self.which = which;
    usbGnubby.gnubbies_.addClient(which, self, function(rc, device) {
      self.dev = device;
      cb(rc);
    });
  } else {
    usbGnubby.gnubbies_.enumerate(enumerated);
  }
};

/**
 * Generates a default channel id value for a gnubby instance that won't
 * collide within this application, but may when others simultaneously access
 * the device.
 * @param {number} gnubbyInstance An instance identifier for a gnubby.
 * @param {llGnubbyDeviceId} which The device identifer for the gnubby device.
 * @return {number} The channel id.
 * @private
 */
usbGnubby.defaultChannelId_ = function(gnubbyInstance, which) {
  var cid = (gnubbyInstance) & 0x00ffffff;
  cid |= ((which.device + 1) << 24);  // For debugging.
  return cid;
};

/**
 * @return {boolean} Whether this gnubby has any command outstanding.
 * @private
 */
usbGnubby.prototype.inUse_ = function() {
  return this.commandPending;
};

/** Closes this gnubby. */
usbGnubby.prototype.close = function() {
  this.closed = true;

  if (this.dev) {
    console.log(UTIL_fmt('usbGnubby.close()'));
    this.rxframes = [];
    this.rxcb = null;
    var dev = this.dev;
    this.dev = null;
    var self = this;
    // Wait a bit in case simpleton client tries open next gnubby.
    // Without delay, gnubbies would drop all idle devices, before client
    // gets to the next one.
    window.setTimeout(
        function() {
          usbGnubby.gnubbies_.removeClient(dev, self);
        }, 300);
  }
};

/**
 * Asks this gnubby to close when it gets a chance.
 * @param {Function=} cb called back when closed.
 */
usbGnubby.prototype.closeWhenIdle = function(cb) {
  if (!this.inUse_()) {
    this.close();
    if (cb) cb();
    return;
  }
  this.closingWhenIdle = true;
  if (cb) this.notifyOnClose.push(cb);
};

/**
 * Close and notify every caller that it is now closed.
 * @private
 */
usbGnubby.prototype.idleClose_ = function() {
  this.close();
  while (this.notifyOnClose.length != 0) {
    var cb = this.notifyOnClose.shift();
    cb();
  }
};

/**
 * Notify callback for every frame received.
 * @param {function()} cb Callback
 * @private
 */
usbGnubby.prototype.notifyFrame_ = function(cb) {
  if (this.rxframes.length != 0) {
    // Already have frames; continue.
    if (cb) window.setTimeout(cb, 0);
  } else {
    this.rxcb = cb;
  }
};

/**
 * Called by low level driver with a frame.
 * @param {ArrayBuffer|Uint8Array} frame Data frame
 * @return {boolean} Whether this client is still interested in receiving
 *     frames from its device.
 */
usbGnubby.prototype.receivedFrame = function(frame) {
  if (this.closed) return false;  // No longer interested.

  if (!this.checkCID_(frame)) {
    // Not for me, ignore.
    return true;
  }

  this.rxframes.push(frame);

  // Callback self in case we were waiting. Once.
  var cb = this.rxcb;
  this.rxcb = null;
  if (cb) window.setTimeout(cb, 0);

  return true;
};

/**
 * @return {ArrayBuffer|Uint8Array} oldest received frame. Throw if none.
 * @private
 */
usbGnubby.prototype.readFrame_ = function() {
  if (this.rxframes.length == 0) throw 'rxframes empty!';

  var frame = this.rxframes.shift();
  return frame;
};

/** Poll from rxframes[].
 * @param {number} cmd Command
 * @param {number} timeout timeout in seconds.
 * @param {?function(...)} cb Callback
 * @private
 */
usbGnubby.prototype.read_ = function(cmd, timeout, cb) {
  if (this.closed) { cb(-llGnubby.GONE); return; }
  if (!this.dev) { cb(-llGnubby.GONE); return; }

  var tid = null;  // timeout timer id.
  var callback = cb;
  var self = this;

  var msg = null;
  var seqno = 0;
  var count = 0;

  /**
   * Schedule call to cb if not called yet.
   * @param {number} a Return code.
   * @param {Object=} b Optional data.
   */
  function schedule_cb(a, b) {
    self.commandPending = false;
    if (tid) {
      // Cancel timeout timer.
      window.clearTimeout(tid);
      tid = null;
    }
    var c = callback;
    if (c) {
      callback = null;
      window.setTimeout(function() { c(a, b); }, 0);
    }
    if (self.closingWhenIdle) self.idleClose_();
  };

  function read_timeout() {
    if (!callback || !tid) return;  // Already done.

    console.error(UTIL_fmt(
        '[' + self.cid.toString(16) + '] timeout!'));

    if (self.dev) {
      self.dev.destroy();  // Stop pretending this thing works.
    }

    tid = null;

    schedule_cb(-llGnubby.TIMEOUT);
  };

  function cont_frame() {
    if (!callback || !tid) return;  // Already done.

    var f = new Uint8Array(self.readFrame_());
    var rcmd = f[4];
    var totalLen = (f[5] << 8) + f[6];

    if (rcmd == llGnubby.CMD_ERROR && totalLen == 1) {
      // Error from device; forward.
      console.log(UTIL_fmt(
          '[' + self.cid.toString(16) + '] error frame ' +
          UTIL_BytesToHex(f)));
      if (f[7] == llGnubby.GONE) {
        self.closed = true;
      }
      schedule_cb(-f[7]);
      return;
    }

    if ((rcmd & 0x80)) {
      // Not an CONT frame, ignore.
      console.log(UTIL_fmt(
          '[' + self.cid.toString(16) + '] ignoring non-cont frame ' +
          UTIL_BytesToHex(f)));
      self.notifyFrame_(cont_frame);
      return;
    }

    var seq = (rcmd & 0x7f);
    if (seq != seqno++) {
      console.log(UTIL_fmt(
          '[' + self.cid.toString(16) + '] bad cont frame ' +
          UTIL_BytesToHex(f)));
      schedule_cb(-llGnubby.INVALID_SEQ);
      return;
    }

    // Copy payload.
    for (var i = 5; i < f.length && count < msg.length; ++i) {
      msg[count++] = f[i];
    }

    if (count == msg.length) {
      // Done.
      schedule_cb(-llGnubby.OK, msg.buffer);
    } else {
      // Need more CONT frame(s).
      self.notifyFrame_(cont_frame);
    }
  }

  function init_frame() {
    if (!callback || !tid) return;  // Already done.

    var f = new Uint8Array(self.readFrame_());

    var rcmd = f[4];
    var totalLen = (f[5] << 8) + f[6];

    if (rcmd == llGnubby.CMD_ERROR && totalLen == 1) {
      // Error from device; forward.
      // Don't log busy frames, they're "normal".
      if (f[7] != llGnubby.BUSY) {
        console.log(UTIL_fmt(
            '[' + self.cid.toString(16) + '] error frame ' +
            UTIL_BytesToHex(f)));
      }
      if (f[7] == llGnubby.GONE) {
        self.closed = true;
      }
      schedule_cb(-f[7]);
      return;
    }

    if (!(rcmd & 0x80)) {
      // Not an init frame, ignore.
      console.log(UTIL_fmt(
          '[' + self.cid.toString(16) + '] ignoring non-init frame ' +
          UTIL_BytesToHex(f)));
      self.notifyFrame_(init_frame);
      return;
    }

    if (rcmd != cmd) {
      // Not expected ack, read more.
      console.log(UTIL_fmt(
          '[' + self.cid.toString(16) + '] ignoring non-ack frame ' +
          UTIL_BytesToHex(f)));
      self.notifyFrame_(init_frame);
      return;
    }

    // Copy payload.
    msg = new Uint8Array(totalLen);
    for (var i = 7; i < f.length && count < msg.length; ++i) {
      msg[count++] = f[i];
    }

    if (count == msg.length) {
      // Done.
      schedule_cb(-llGnubby.OK, msg.buffer);
    } else {
      // Need more CONT frame(s).
      self.notifyFrame_(cont_frame);
    }
  }

  // Start timeout timer.
  tid = window.setTimeout(read_timeout, 1000.0 * timeout);

  // Schedule read of first frame.
  self.notifyFrame_(init_frame);
};

/**
  * @const
  */
usbGnubby.NOTIFICATION_CID = 0;

/**
  * @const
  */
usbGnubby.BROADCAST_CID = (0xff << 24) | (0xff << 16) | (0xff << 8) | 0xff;

/**
 * @param {ArrayBuffer|Uint8Array} frame Data frame
 * @return {boolean} Whether frame is for my channel.
 * @private
 */
usbGnubby.prototype.checkCID_ = function(frame) {
  var f = new Uint8Array(frame);
  var c = (f[0] << 24) |
          (f[1] << 16) |
          (f[2] << 8) |
          (f[3]);
  return c === this.cid ||
         c === usbGnubby.NOTIFICATION_CID ||
         c === usbGnubby.BROADCAST_CID;
};

/**
 * Queue command for sending.
 * @param {number} cmd The command to send.
 * @param {ArrayBuffer|Uint8Array} data Command data
 * @private
 */
usbGnubby.prototype.write_ = function(cmd, data) {
  if (this.closed) return;
  if (!this.dev) return;

  this.commandPending = true;

  this.dev.queueCommand(this.cid, cmd, data);
};

/**
 * Writes the command, and calls back when the command's reply is received.
 * @param {number} cmd The command to send.
 * @param {ArrayBuffer|Uint8Array} data Command data
 * @param {number} timeout Timeout in seconds.
 * @param {function(number, ArrayBuffer=)} cb Callback
 * @private
 */
usbGnubby.prototype.exchange_ = function(cmd, data, timeout, cb) {
  var busyWait = new CountdownTimer(this.busyMillis);
  var self = this;

  function retryBusy(rc, rc_data) {
    if (rc == -llGnubby.BUSY && !busyWait.expired()) {
      if (usbGnubby.gnubbies_) {
        usbGnubby.gnubbies_.resetInactivityTimer(timeout * 1000);
      }
      self.write_(cmd, data);
      self.read_(cmd, timeout, retryBusy);
    } else {
      busyWait.clearTimeout();
      cb(rc, rc_data);
    }
  }

  retryBusy(-llGnubby.BUSY, undefined);  // Start work.
};

/** Default callback for commands. Simply logs to console.
 * @param {number} rc Result status code
 * @param {(ArrayBuffer|Uint8Array|Array.<number>|null)} data Result data
 */
usbGnubby.defaultCallback = function(rc, data) {
  var msg = 'defaultCallback(' + rc;
  if (data) {
    if (typeof data == 'string') msg += ', ' + data;
    else msg += ', ' + UTIL_BytesToHex(new Uint8Array(data));
  }
  msg += ')';
  console.log(UTIL_fmt(msg));
};

/**
 * Ensures this device has temporary ownership of the USB device, by:
 * 1. Using the INIT command to allocate an unique channel id, if one hasn't
 *    been retrieved before, or
 * 2. Sending a nonce to device, flushing read queue until match.
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.sync = function(cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  if (this.closed) {
    cb(-llGnubby.GONE);
    return;
  }

  var done = false;
  var trycount = 6;
  var tid = null;
  var self = this;

  function returnValue(rc) {
    done = true;
    cb(rc);
    if (self.closingWhenIdle) self.idleClose_();
  }

  function callback(rc, opt_frame) {
    self.commandPending = false;
    if (tid) {
      window.clearTimeout(tid);
      tid = null;
    }
    completionAction(rc, opt_frame);
  }

  function sendSyncSentinel() {
    var cmd = llGnubby.CMD_SYNC;
    var data = new Uint8Array(1);
    data[0] = ++self.synccnt;
    self.dev.queueCommand(self.cid, cmd, data.buffer);
  }

  function syncSentinelEquals(f) {
    return (f[4] == llGnubby.CMD_SYNC &&
        (f.length == 7 || /* fw pre-0.2.1 bug: does not echo sentinel */
         f[7] == self.synccnt));
  }

  function syncCompletionAction(rc, opt_frame) {
    if (rc) console.warn(UTIL_fmt('sync failed: ' + rc));
    returnValue(rc);
  }

  function sendInitSentinel() {
    var cid = usbGnubby.BROADCAST_CID;
    var cmd = llGnubby.CMD_INIT;
    self.dev.queueCommand(cid, cmd, nonce);
  }

  function initSentinelEquals(f) {
    return (f[4] == llGnubby.CMD_INIT &&
        f.length >= nonce.length + 7 &&
        UTIL_equalArrays(f.subarray(7, nonce.length + 7), nonce));
  }

  function initCmdUnsupported(rc) {
    // Different firmwares fail differently on different inputs, so treat any
    // of the following errors as indicating the INIT command isn't supported.
    return rc == -llGnubby.INVALID_CMD ||
        rc == -llGnubby.INVALID_PAR ||
        rc == -llGnubby.INVALID_LEN;
  }

  function initCompletionAction(rc, opt_frame) {
    // Actual failures: bail out.
    if (rc && !initCmdUnsupported(rc)) {
      console.warn(UTIL_fmt('init failed: ' + rc));
      returnValue(rc);
    }

    var HEADER_LENGTH = 7;
    var MIN_LENGTH = HEADER_LENGTH + 4;  // 4 bytes for the channel id
    if (rc || !opt_frame || opt_frame.length < nonce.length + MIN_LENGTH) {
      // INIT command not supported or is missing the returned channel id:
      // Pick a random cid to try to prevent collisions on the USB bus.
      var rnd = UTIL_getRandom(2);
      self.cid ^= (rnd[0] << 16) | (rnd[1] << 8);
      // Now sync with that cid, to make sure we've got it.
      setSync();
      timeoutLoop();
      return;
    }
    // Accept the provided cid.
    var offs = HEADER_LENGTH + nonce.length;
    self.cid = (opt_frame[offs] << 24) |
               (opt_frame[offs + 1] << 16) |
               (opt_frame[offs + 2] << 8) |
               opt_frame[offs + 3];
    returnValue(rc);
  }

  function checkSentinel() {
    var f = new Uint8Array(self.readFrame_());

    // Stop on errors and return them.
    if (f[4] == llGnubby.CMD_ERROR &&
        f[5] == 0 && f[6] == 1) {
      if (f[7] == llGnubby.GONE) {
        // Device disappeared on us.
        self.closed = true;
      }
      callback(-f[7]);
      return;
    }

    // Eat everything else but expected sentinel reply.
    if (!sentinelEquals(f)) {
      // Read more.
      self.notifyFrame_(checkSentinel);
      return;
    }

    // Done.
    callback(-llGnubby.OK, f);
  };

  function timeoutLoop() {
    if (done) return;

    if (trycount == 0) {
      // Failed.
      callback(-llGnubby.TIMEOUT);
      return;
    }

    --trycount;  // Try another one.
    sendSentinel();
    self.notifyFrame_(checkSentinel);
    tid = window.setTimeout(timeoutLoop, 500);
  };

  var sendSentinel;
  var sentinelEquals;
  var nonce;
  var completionAction;

  function setInit() {
    sendSentinel = sendInitSentinel;
    nonce = UTIL_getRandom(8);
    sentinelEquals = initSentinelEquals;
    completionAction = initCompletionAction;
  }

  function setSync() {
    sendSentinel = sendSyncSentinel;
    sentinelEquals = syncSentinelEquals;
    completionAction = syncCompletionAction;
  }

  if (this.cid ==
      usbGnubby.defaultChannelId_(this.gnubbyInstance, this.which)) {
    setInit();
  } else {
    setSync();
  }
  timeoutLoop();
};

/** Short timeout value in seconds */
usbGnubby.SHORT_TIMEOUT = 1;
/** Normal timeout value in seconds */
usbGnubby.NORMAL_TIMEOUT = 3;
// Max timeout usb firmware has for smartcard response is 30 seconds.
// Make our application level tolerance a little longer.
/** Maximum timeout in seconds */
usbGnubby.MAX_TIMEOUT = 31;

/** Blink led
 * @param {number|ArrayBuffer|Uint8Array} data Command data or number
 *     of seconds to blink
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.blink = function(data, cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  if (typeof data == 'number') {
    var d = new Uint8Array([data]);
    data = d.buffer;
  }
  this.exchange_(llGnubby.CMD_PROMPT, data, usbGnubby.NORMAL_TIMEOUT, cb);
};

/** Lock the gnubby
 * @param {number|ArrayBuffer|Uint8Array} data Command data
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.lock = function(data, cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  if (typeof data == 'number') {
    var d = new Uint8Array([data]);
    data = d.buffer;
  }
  this.exchange_(llGnubby.CMD_LOCK, data, usbGnubby.NORMAL_TIMEOUT, cb);
};

/** Unlock the gnubby
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.unlock = function(cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  var data = new Uint8Array([0]);
  this.exchange_(llGnubby.CMD_LOCK, data.buffer,
      usbGnubby.NORMAL_TIMEOUT, cb);
};

/** Request system information data.
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.sysinfo = function(cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  this.exchange_(llGnubby.CMD_SYSINFO, new ArrayBuffer(0),
      usbGnubby.NORMAL_TIMEOUT, cb);
};

/** Send wink command
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.wink = function(cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  this.exchange_(llGnubby.CMD_WINK, new ArrayBuffer(0),
      usbGnubby.NORMAL_TIMEOUT, cb);
};

/** Send DFU (Device firmware upgrade) command
 * @param {ArrayBuffer|Uint8Array} data Command data
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.dfu = function(data, cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  this.exchange_(llGnubby.CMD_DFU, data, usbGnubby.NORMAL_TIMEOUT, cb);
};

/** Ping the gnubby
 * @param {number|ArrayBuffer|Uint8Array} data Command data
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.ping = function(data, cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  if (typeof data == 'number') {
    var d = new Uint8Array(data);
    window.crypto.getRandomValues(d);
    data = d.buffer;
  }
  this.exchange_(llGnubby.CMD_PING, data, usbGnubby.NORMAL_TIMEOUT, cb);
};

/** Send a raw APDU command
 * @param {ArrayBuffer|Uint8Array} data Command data
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.apdu = function(data, cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  this.exchange_(llGnubby.CMD_APDU, data, usbGnubby.MAX_TIMEOUT, cb);
};

/** Reset gnubby
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.reset = function(cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  this.exchange_(llGnubby.CMD_ATR, new ArrayBuffer(0),
      usbGnubby.NORMAL_TIMEOUT, cb);
};

// byte args[3] = [delay-in-ms before disabling interrupts,
//                 delay-in-ms before disabling usb (aka remove),
//                 delay-in-ms before reboot (aka insert)]
/** Send usb test command
 * @param {ArrayBuffer|Uint8Array} args Command data
 * @param {?function(...)} cb Callback
 */
usbGnubby.prototype.usb_test = function(args, cb) {
  if (!cb) cb = usbGnubby.defaultCallback;
  var u8 = new Uint8Array(args);
  this.exchange_(llGnubby.CMD_USB_TEST, u8.buffer,
      usbGnubby.NORMAL_TIMEOUT, cb);
};

/** APDU command with reply
 * @param {ArrayBuffer|Uint8Array} request The request
 * @param {?function(...)} cb Callback
 * @param {boolean=} opt_nowink Do not wink
 */
usbGnubby.prototype.apduReply = function(request, cb, opt_nowink) {
  if (!cb) cb = usbGnubby.defaultCallback;
  var self = this;

  this.apdu(request, function(rc, data) {
    if (rc == 0) {
      var r8 = new Uint8Array(data);
      if (r8[r8.length - 2] == 0x90 && r8[r8.length - 1] == 0x00) {
        // strip trailing 9000
        var buf = new Uint8Array(r8.subarray(0, r8.length - 2));
        cb(-llGnubby.OK, buf.buffer);
        return;
      } else {
        // return non-9000 as rc
        rc = r8[r8.length - 2] * 256 + r8[r8.length - 1];
        // wink gnubby at hand if it needs touching.
        if (rc == 0x6985 && !opt_nowink) {
          self.wink(function() { cb(rc); });
          return;
        }
      }
    }
    // Warn on errors other than waiting for touch, wrong data, and
    // unrecognized command.
    if (rc != 0x6985 && rc != 0x6a80 && rc != 0x6d00) {
      console.warn(UTIL_fmt('apduReply_ fail: ' + rc.toString(16)));
    }
    cb(rc);
  });
};
