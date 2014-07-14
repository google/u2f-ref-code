// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Provides a countdown-based timer implementation.
 */
'use strict';

/**
 * Constructs a new timer.  The timer has a very limited resolution, and does
 * not attempt to be millisecond accurate. Its intended use is as a
 * low-precision timer that pauses while debugging.
 * @param {number=} timeoutMillis how long, in milliseconds, the countdown
 *     lasts.
 * @param {Function=} cb called back when the countdown expires.
 * @constructor
 * @implements {Countdown}
 */
function CountdownTimer(timeoutMillis, cb) {
  this.remainingMillis = 0;
  this.setTimeout(timeoutMillis || 0, cb);
}

/** Timer interval */
CountdownTimer.TIMER_INTERVAL_MILLIS = 200;

/**
 * Sets a new timeout for this timer. Only possible if the timer is not
 * currently active.
 * @param {number} timeoutMillis how long, in milliseconds, the countdown lasts.
 * @param {Function=} cb called back when the countdown expires.
 * @return {boolean} whether the timeout could be set.
 */
CountdownTimer.prototype.setTimeout = function(timeoutMillis, cb) {
  if (this.timeoutId)
    return false;
  if (!timeoutMillis || timeoutMillis < 0)
    return false;
  this.remainingMillis = timeoutMillis;
  this.cb = cb;
  if (this.remainingMillis > CountdownTimer.TIMER_INTERVAL_MILLIS) {
    this.timeoutId =
        window.setInterval(this.timerTick.bind(this),
            CountdownTimer.TIMER_INTERVAL_MILLIS);
  } else {
    // Set a one-shot timer for the last interval.
    this.timeoutId =
        window.setTimeout(this.timerTick.bind(this), this.remainingMillis);
  }
  return true;
};

/** Clears this timer's timeout. Timers that are cleared become expired. */
CountdownTimer.prototype.clearTimeout = function() {
  if (this.timeoutId) {
    window.clearTimeout(this.timeoutId);
    this.timeoutId = undefined;
  }
  this.remainingMillis = 0;
};

/**
 * @return {number} how many milliseconds are remaining until the timer expires.
 */
CountdownTimer.prototype.millisecondsUntilExpired = function() {
  return this.remainingMillis > 0 ? this.remainingMillis : 0;
};

/** @return {boolean} whether the timer has expired. */
CountdownTimer.prototype.expired = function() {
  return this.remainingMillis <= 0;
};

/**
 * Constructs a new clone of this timer, while overriding its callback.
 * @param {Function=} cb callback for new timer.
 * @return {!Countdown} new clone.
 */
CountdownTimer.prototype.clone = function(cb) {
  return new CountdownTimer(this.remainingMillis, cb);
};

/** Timer callback. */
CountdownTimer.prototype.timerTick = function() {
  this.remainingMillis -= CountdownTimer.TIMER_INTERVAL_MILLIS;
  if (this.expired()) {
    window.clearTimeout(this.timeoutId);
    this.timeoutId = undefined;
    if (this.cb) {
      this.cb();
    }
  }
};

/**
 * A factory for creating CountdownTimers.
 * @constructor
 * @implements {CountdownFactory}
 */
function CountdownTimerFactory() {
}

/**
 * Creates a new timer.
 * @param {number} timeoutMillis How long, in milliseconds, the countdown lasts.
 * @param {function()=} opt_cb Called back when the countdown expires.
 * @return {!Countdown} The timer.
 */
CountdownTimerFactory.prototype.createTimer =
    function(timeoutMillis, opt_cb) {
  return new CountdownTimer(timeoutMillis, opt_cb);
};

/**
 * Minimum timeout attenuation, below which a response couldn't be reasonably
 * guaranteed, in seconds.
 * @const
 */
var MINIMUM_TIMEOUT_ATTENUATION_SECONDS = 0.5;

/**
 * @param {number} timeoutSeconds Timeout value in seconds.
 * @return {number} The timeout value, attenuated to ensure a response can be
 *     given before the timeout's expiration.
 * @private
 */
function attenuateTimeoutInSeconds_(timeoutSeconds) {
  if (timeoutSeconds < MINIMUM_TIMEOUT_ATTENUATION_SECONDS)
    return 0;
  return timeoutSeconds - MINIMUM_TIMEOUT_ATTENUATION_SECONDS;
}

/**
 * Default request timeout when none is present in the request, in seconds.
 * @const
 */
var DEFAULT_REQUEST_TIMEOUT_SECONDS = 30;

/**
 * Creates a new countdown from the given request using the given timer factory,
 * attenuated to ensure a response is given prior to the countdown's expiration.
 * @param {CountdownFactory} timerFactory The factory to use.
 * @param {Object} request The request containing the timeout.
 * @param {number=} opt_defaultTimeoutSeconds
 * @return {!Countdown} A countdown timer.
 */
function createTimerForRequest(timerFactory, request,
    opt_defaultTimeoutSeconds) {
  var timeoutValueSeconds;
  if (request.hasOwnProperty('timeoutSeconds')) {
    timeoutValueSeconds = request['timeoutSeconds'];
  } else if (request.hasOwnProperty('timeout')) {
    timeoutValueSeconds = request['timeout'];
  } else if (opt_defaultTimeoutSeconds !== undefined) {
    timeoutValueSeconds = opt_defaultTimeoutSeconds;
  } else {
    timeoutValueSeconds = DEFAULT_REQUEST_TIMEOUT_SECONDS;
  }
  timeoutValueSeconds = attenuateTimeoutInSeconds_(timeoutValueSeconds);
  return timerFactory.createTimer(timeoutValueSeconds * 1000);
}
