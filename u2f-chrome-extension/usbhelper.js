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
 * @constructor
 * @extends {GenericRequestHelper}
 */
function UsbHelper() {
  GenericRequestHelper.apply(this, arguments);

  var self = this;
  this.registerHandlerFactory('enroll_helper_request', function(request) {
    return new UsbEnrollHandler(/** @type {EnrollHelperRequest} */ (request));
  });
  this.registerHandlerFactory('sign_helper_request', function(request) {
    return new UsbSignHandler(/** @type {SignHelperRequest} */ (request));
  });
}

inherits(UsbHelper, GenericRequestHelper);
