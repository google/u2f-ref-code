// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Implements a sign handler using USB gnubbies.
 */
'use strict';

var CORRUPT_sign = false;

/**
 * @param {!SignHelperRequest} request The sign request.
 * @constructor
 * @implements {RequestHandler}
 */
function UsbSignHandler(request) {
  /** @private {!SignHelperRequest} */
  this.request_ = request;

  /** @private {boolean} */
  this.notified_ = false;
  /** @private {boolean} */
  this.anyGnubbiesFound_ = false;
}

/**
 * Default timeout value in case the caller never provides a valid timeout.
 * @const
 */
UsbSignHandler.DEFAULT_TIMEOUT_MILLIS = 30 * 1000;

/**
 * Attempts to run this handler's request.
 * @param {RequestHandlerCallback} cb Called with the result of the request and
 *     an optional source for the sign result.
 * @return {boolean} whether this set of challenges was accepted.
 */
UsbSignHandler.prototype.run = function(cb) {
  if (this.cb_) {
    // Can only handle one request.
    return false;
  }
  /** @private {RequestHandlerCallback} */
  this.cb_ = cb;
  if (!this.request_.signData || !this.request_.signData.length) {
    // Fail a sign request with an empty set of challenges.
    this.notifyError_(DeviceStatusCodes.INVALID_DATA_STATUS);
    return false;
  }
  var timeoutMillis =
      this.request_.timeoutSeconds ?
      this.request_.timeoutSeconds * 1000 :
      UsbSignHandler.DEFAULT_TIMEOUT_MILLIS;
  /** @private {MultipleGnubbySigner} */
  this.signer_ = new MultipleGnubbySigner(
      false /* forEnroll */,
      this.signerCompleted_.bind(this),
      this.signerFoundGnubby_.bind(this),
      timeoutMillis,
      this.request_.logMsgUrl);
  return this.signer_.doSign(this.request_.signData);
};


/**
 * Called when a MultipleGnubbySigner completes.
 * @param {boolean} anyPending Whether any gnubbies are pending.
 * @private
 */
UsbSignHandler.prototype.signerCompleted_ = function(anyPending) {
  if (!this.anyGnubbiesFound_ || anyPending) {
    this.notifyError_(DeviceStatusCodes.TIMEOUT_STATUS);
  } else if (this.signerError_ !== undefined) {
    this.notifyError_(this.signerError_);
  } else {
    // Do nothing: signerFoundGnubby_ will have returned results from other
    // gnubbies.
  }
};

/**
 * Called when a MultipleGnubbySigner finds a gnubby that has completed signing
 * its challenges.
 * @param {MultipleSignerResult} signResult Signer result object
 * @param {boolean} moreExpected Whether the signer expects to produce more
 *     results.
 * @private
 */
UsbSignHandler.prototype.signerFoundGnubby_ =
    function(signResult, moreExpected) {
  this.anyGnubbiesFound_ = true;
  if (!signResult.code) {
    var gnubby = signResult['gnubby'];
    var challenge = signResult['challenge'];
    var info = new Uint8Array(signResult['info']);
    this.notifySuccess_(gnubby, challenge, info);
  } else if (!moreExpected) {
    // If the signer doesn't expect more results, return the error directly to
    // the caller.
    this.notifyError_(signResult.code);
  } else {
    // Record the last error, to report from the complete callback if no other
    // eligible gnubbies are found.
    /** @private {number} */
    this.signerError_ = signResult.code;
  }
};

/**
 * Reports the result of a successful sign operation.
 * @param {Gnubby} gnubby Gnubby instance
 * @param {SignHelperChallenge} challenge Challenge signed
 * @param {Uint8Array} info Result data
 * @private
 */
UsbSignHandler.prototype.notifySuccess_ = function(gnubby, challenge, info) {
  if (this.notified_)
    return;
  this.notified_ = true;

  gnubby.closeWhenIdle();
  this.close();

  if (CORRUPT_sign) {
    CORRUPT_sign = false;
    info[info.length - 1] = info[info.length - 1] ^ 0xff;
  }
  var responseData = {
    'appIdHash': B64_encode(challenge['appIdHash']),
    'challengeHash': B64_encode(challenge['challengeHash']),
    'keyHandle': B64_encode(challenge['keyHandle']),
    'signatureData': B64_encode(info)
  };
  var reply = {
    'type': 'sign_helper_reply',
    'code': DeviceStatusCodes.OK_STATUS,
    'responseData': responseData
  };
  this.cb_(reply, 'USB');
};

/**
 * Reports error to the caller.
 * @param {number} code error to report
 * @private
 */
UsbSignHandler.prototype.notifyError_ = function(code) {
  if (this.notified_)
    return;
  this.notified_ = true;
  this.close();
  var reply = {
    'type': 'sign_helper_reply',
    'code': code
  };
  this.cb_(reply);
};

/**
 * Closes the MultipleGnubbySigner, if any.
 */
UsbSignHandler.prototype.close = function() {
  if (this.signer_) {
    this.signer_.close();
    this.signer_ = null;
  }
};
