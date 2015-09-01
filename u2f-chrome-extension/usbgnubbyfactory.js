// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Contains a simple factory for creating and opening Gnubby
 * instances.
 */
'use strict';

/**
 * @param {Gnubbies} gnubbies Gnubbies singleton instance
 * @constructor
 * @implements {GnubbyFactory}
 */
function UsbGnubbyFactory(gnubbies) {
  /** @private {Gnubbies} */
  this.gnubbies_ = gnubbies;
  Gnubby.setGnubbies(gnubbies);
}

/**
 * Creates a new gnubby object, and opens the gnubby with the given index.
 * @param {GnubbyDeviceId} which The device to open.
 * @param {boolean} forEnroll Whether this gnubby is being opened for enrolling.
 * @param {FactoryOpenCallback} cb Called with result of opening the gnubby.
 * @param {string=} opt_appIdHash The base64-encoded hash of the app id for
 *     which the gnubby being opened.
 * @param {string=} opt_logMsgUrl The url to post log messages to.
 * @override
 */
UsbGnubbyFactory.prototype.openGnubby =
    function(which, forEnroll, cb, opt_appIdHash, opt_logMsgUrl) {
  var gnubby = new Gnubby();
  gnubby.open(which, function(rc) {
    if (rc) {
      cb(rc, gnubby);
      return;
    }
    gnubby.sync(function(rc) {
      cb(rc, gnubby);
    });
  });
};

/**
 * Enumerates gnubbies.
 * @param {function(number, Array<GnubbyDeviceId>)} cb Enumerate callback
 */
UsbGnubbyFactory.prototype.enumerate = function(cb) {
  this.gnubbies_.enumerate(cb);
};

/**
 * No-op prerequisite check.
 * @param {Gnubby} gnubby The not-enrolled gnubby.
 * @param {string} appIdHash The base64-encoded hash of the app id for which
 *     the gnubby being enrolled.
 * @param {FactoryOpenCallback} cb Called with the result of the prerequisite
 *     check. (A non-zero status indicates failure.)
 */
UsbGnubbyFactory.prototype.notEnrolledPrerequisiteCheck =
    function(gnubby, appIdHash, cb) {
  cb(DeviceStatusCodes.OK_STATUS, gnubby);
};
