// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview This provides the different code types for the gnubby
 * operations.
 */

/**
 * @const
 * @enum {number}
 */
var GnubbyCodeTypes = {
  /** Request succeeded. */
  'OK': 0,

  /** All plugged in devices are already enrolled. */
  'ALREADY_ENROLLED': 2,

  /** None of the plugged in devices are enrolled. */
  'NONE_PLUGGED_ENROLLED': 3,

  /** One or more devices are waiting for touch. */
  'WAIT_TOUCH': 4,

  /** Unknown error. */
  'UNKNOWN_ERROR': 7,

  /** Bad request. */
  'BAD_REQUEST': 12

};
