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
 * @return {!Promise.<string>} A promise for the fetched text. In case of an
 *     error, this promise is rejected with an HTTP status code.
 */
TextFetcher.prototype.fetch = function(url) {};

/**
 * @constructor
 * @implements {TextFetcher}
 */
function XhrTextFetcher() {
}

/**
 * @param {string} url The URL to fetch.
 * @return {!Promise.<string>} A promise for the fetched text. In case of an
 *     error, this promise is rejected with an HTTP status code.
 */
XhrTextFetcher.prototype.fetch = function(url) {
  return new Promise(function(resolve, reject) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.onloadend = function() {
      if (xhr.status != 200) {
        reject(xhr.status);
        return;
      }
      resolve(xhr.responseText);
    };
    xhr.send();
  });
};
