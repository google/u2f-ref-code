// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Contains a factory interface for creating and opening gnubbies.
 */
'use strict';

/**
 * A factory for creating and opening gnubbies.
 * @interface
 */
function GnubbyFactory() {}

/**
 * Enumerates gnubbies.
 * @param {function(number, Array<GnubbyDeviceId>)} cb Enumerate callback
 */
GnubbyFactory.prototype.enumerate = function(cb) {
};

/** @typedef {function(number, Gnubby=)} */
var FactoryOpenCallback;

/**
 * Creates a new gnubby object, and opens the gnubby with the given index.
 * @param {GnubbyDeviceId} which The device to open.
 * @param {boolean} forEnroll Whether this gnubby is being opened for enrolling.
 * @param {FactoryOpenCallback} cb Called with result of opening the gnubby.
 * @param {string=} opt_appIdHash The base64-encoded hash of the app id for
 *     which the gnubby being opened.
 * @param {string=} opt_logMsgUrl The url to post log messages to.
 */
GnubbyFactory.prototype.openGnubby =
    function(which, forEnroll, cb, opt_appIdHash, opt_logMsgUrl) {
};

/**
 * Called during enrollment to check whether a gnubby known not to be enrolled
 * is allowed to enroll in its present state. Upon completion of the check, the
 * callback is called.
 * @param {Gnubby} gnubby The not-enrolled gnubby.
 * @param {string} appIdHash The base64-encoded hash of the app id for which
 *     the gnubby being enrolled.
 * @param {FactoryOpenCallback} cb Called with the result of the prerequisite
 *     check. (A non-zero status indicates failure.)
 */
GnubbyFactory.prototype.notEnrolledPrerequisiteCheck =
    function(gnubby, appIdHash, cb) {
};
