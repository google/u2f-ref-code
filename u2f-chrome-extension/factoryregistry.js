// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Class providing common dependencies for the extension's
 * top half.
 */
'use strict';

/**
 * @param {!CountdownFactory} countdownFactory A countdown timer factory.
 * @param {!OriginChecker} originChecker An origin checker.
 * @param {!RequestHelper} requestHelper A request helper.
 * @param {!TextFetcher} textFetcher A text fetcher.
 * @constructor
 */
function FactoryRegistry(countdownFactory, originChecker, requestHelper,
    textFetcher) {
  /** @private {!CountdownFactory} */
  this.countdownFactory_ = countdownFactory;
  /** @private {!OriginChecker} */
  this.originChecker_ = originChecker;
  /** @private {!RequestHelper} */
  this.requestHelper_ = requestHelper;
  /** @private {!TextFetcher} */
  this.textFetcher_ = textFetcher;
}

/** @return {!CountdownFactory} A countdown factory. */
FactoryRegistry.prototype.getCountdownFactory = function() {
  return this.countdownFactory_;
};

/** @return {!OriginChecker} An origin checker. */
FactoryRegistry.prototype.getOriginChecker = function() {
  return this.originChecker_;
};

/** @return {!RequestHelper} A request helper. */
FactoryRegistry.prototype.getRequestHelper = function() {
  return this.requestHelper_;
};

/** @return {!TextFetcher} A text fetcher. */
FactoryRegistry.prototype.getTextFetcher = function() {
  return this.textFetcher_;
};
