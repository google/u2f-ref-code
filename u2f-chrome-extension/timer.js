// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Provides an interface representing the browser/extension
 * system's timer interface.
 */
'use strict';

/**
 * An interface representing the browser/extension system's timer interface.
 * @interface
 */
function SystemTimer() {}

/**
 * Sets a single-shot timer.
 * @param {function()} func Called back when the timer expires.
 * @param {number} timeoutMillis How long until the timer fires, in
 *     milliseconds.
 * @return {number} A timeout ID, which can be used to cancel the timer.
 */
SystemTimer.prototype.setTimeout = function(func, timeoutMillis) {};

/**
 * Clears a previously set timer.
 * @param {number} timeoutId The ID of the timer to clear.
 */
SystemTimer.prototype.clearTimeout = function(timeoutId) {};

/**
 * Sets a repeating interval timer.
 * @param {function()} func Called back each time the timer fires.
 * @param {number} timeoutMillis How long until the timer fires, in
 *     milliseconds.
 * @return {number} A timeout ID, which can be used to cancel the timer.
 */
SystemTimer.prototype.setInterval = function(func, timeoutMillis) {};

/**
 * Clears a previously set interval timer.
 * @param {number} timeoutId The ID of the timer to clear.
 */
SystemTimer.prototype.clearInterval = function(timeoutId) {};
