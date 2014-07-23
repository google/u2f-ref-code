// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Implements a simple XmlHttpRequest-based text document
 * fetcher.
 *
 */
'use strict';

/**
 * A fetcher of text files.
 * @interface
 */
function TextFetcher() {}

/**
 * @param {string} url The URL to fetch.
 * @param {function(number, string=)} cb Called back with the HTTP status code
 *     of the fetch, and, if the fetch was successful, the text that was
 *     fetched.
 */
TextFetcher.prototype.fetch = function(url, cb) {};

/**
 * @constructor
 * @implements {TextFetcher}
 */
function XhrTextFetcher() {
}

/**
 * @param {string} url The URL to fetch.
 * @param {function(number, string=)} cb Called back with the HTTP status code
 *     of the fetch, and, if the fetch was successful, the text that was
 *     fetched.
 */
XhrTextFetcher.prototype.fetch = function(url, cb) {
  var xhr = new XMLHttpRequest();
  var origins = [];
  xhr.open('GET', url, true);
  xhr.onloadend = function() {
    if (xhr.status != 200) {
      cb(xhr.status);
      return;
    }
    cb(xhr.status, xhr.responseText);
  };
  xhr.send();
};
