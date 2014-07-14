// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Implements a helper using USB gnubbies.
 */
'use strict';

/**
 * @param {!GnubbyFactory} gnubbyFactory Factory to create gnubbies.
 * @param {!CountdownFactory} timerFactory A factory to create timers.
 * @constructor
 * @extends {GenericRequestHelper}
 */
function UsbHelper(gnubbyFactory, timerFactory) {
  GenericRequestHelper.apply(this, arguments);
  /** @private {!GnubbyFactory} */
  this.gnubbyFactory_ = gnubbyFactory;
  /** @private {!CountdownFactory} */
  this.timerFactory_ = timerFactory;

  var self = this;
  this.registerHandlerFactory('enroll_helper_request', function(request) {
    return new UsbEnrollHandler(/** @type {EnrollHelperRequest} */ (request),
        self.gnubbyFactory_, self.timerFactory_);
  });
  this.registerHandlerFactory('sign_helper_request', function(request) {
    return new UsbSignHandler(/** @type {SignHelperRequest} */ (request),
        self.gnubbyFactory_, self.timerFactory_);
  });
}

UsbHelper.prototype = GenericRequestHelper.prototype;
UsbHelper.prototype.constructor = UsbHelper;
