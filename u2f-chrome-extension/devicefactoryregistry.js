// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Class providing common dependencies for the extension's
 * bottom half.
 */
'use strict';

/**
 * @param {!GnubbyFactory} gnubbyFactory A Gnubby factory.
 * @param {!CountdownFactory} countdownFactory A countdown timer factory.
 * @constructor
 */
function DeviceFactoryRegistry(gnubbyFactory, countdownFactory) {
  /** @private {!GnubbyFactory} */
  this.gnubbyFactory_ = gnubbyFactory;
  /** @private {!CountdownFactory} */
  this.countdownFactory_ = countdownFactory;
}

/** @return {!GnubbyFactory} A Gnubby factory. */
DeviceFactoryRegistry.prototype.getGnubbyFactory = function() {
  return this.gnubbyFactory_;
};

/** @return {!CountdownFactory} A countdown factory. */
DeviceFactoryRegistry.prototype.getCountdownFactory = function() {
  return this.countdownFactory_;
};
