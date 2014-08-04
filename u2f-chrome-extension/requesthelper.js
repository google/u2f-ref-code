// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Provides a "bottom half" helper to assist with raw requests.
 * This fills the same role as the Authenticator-Specific Module component of
 * U2F documents, although the API is different.
 */
'use strict';

/**
 * @typedef {{
 *   type: string,
 *   timeout: number
 * }}
 */
var HelperRequest;

/**
 * @typedef {{
 *   type: string,
 *   code: (number|undefined)
 * }}
 */
var HelperReply;

/**
 * A helper to process requests.
 * @interface
 */
function RequestHelper() {}

/**
 * Gets a handler for a request.
 * @param {HelperRequest} request The request to handle.
 * @return {RequestHandler} A handler for the request.
 */
RequestHelper.prototype.getHandler = function(request) {};

/**
 * A handler to track an outstanding request.
 * @extends {Closeable}
 * @interface
 */
function RequestHandler() {}

/** @typedef {function(HelperReply, string=)} */
var RequestHandlerCallback;

/**
 * @param {RequestHandlerCallback} cb Called with the result of the request,
 *     and an optional source for the result.
 * @return {boolean} Whether this handler could be run.
 */
RequestHandler.prototype.run = function(cb) {};

/** Closes this handler. */
RequestHandler.prototype.close = function() {};
