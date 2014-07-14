// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview This provides the different message types for the gnubby
 * operations.
 */

var GnubbyMsgTypes = {};

/**
 * Enroll request message type.
 * @const
 */
GnubbyMsgTypes.ENROLL_WEB_REQUEST = 'enroll_web_request';

/**
 * Enroll reply message type.
 * @const
 */
GnubbyMsgTypes.ENROLL_WEB_REPLY = 'enroll_web_reply';

/**
 * Enroll notification message type.
 * @const
 */
GnubbyMsgTypes.ENROLL_WEB_NOTIFICATION = 'enroll_web_notification';

/**
 * Sign request message type.
 * @const
 */
GnubbyMsgTypes.SIGN_WEB_REQUEST = 'sign_web_request';

/**
 * Sign reply message type.
 * @const
 */
GnubbyMsgTypes.SIGN_WEB_REPLY = 'sign_web_reply';

/**
 * Sign notification message type.
 * @const
 */
GnubbyMsgTypes.SIGN_WEB_NOTIFICATION = 'sign_web_notification';
