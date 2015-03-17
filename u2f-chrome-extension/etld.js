// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Fetches the extended TLD + 1 of an origin.
 */
'use strict';

/**
 * A class to fetch the extended TLD + 1 of an origin.
 * @param {!TextFetcher} fetcher A URL fetcher.
 * @param {boolean=} opt_cache Whether to cache fetched results for the
 *     lifetime of this object.
 * @constructor
 */
function EffectiveTldFetcher(fetcher, opt_cache) {
  /** @private {(!Array<string>)|undefined} */
  this.eTlds_ = ETLD_NAMES_LIST;
}

/** @private {!Array<string>} */
EffectiveTldFetcher.effectiveTldListUrls_ = [];

/**
 * A fixed list of known TLDs.
 * @private {!Array<string>}
 */
EffectiveTldFetcher.fixed_tld_list_ = [];

/**
 * Sets a fixed list of known TLDs. This list is not canonical: if an origin
 * is not found to be in this list, a canonical list is fetched from
 * EffectiveTldFetcher.effectiveTldListUrls_.
 * @param {!Array<string>} tlds The list of known TLDs.
 */
EffectiveTldFetcher.setFixedTldList = function(tlds) {
  EffectiveTldFetcher.fixed_tld_list_ = tlds;
};

/**
 * @param {string} origin The origin.
 * @return {Promise<?string>} A promise for the eTLD+1 of origin, or null if it
 *     doesn't have one (e.g. the origin is invalid.)
 */
EffectiveTldFetcher.prototype.getEffectiveTldPlusOne = function(origin) {
  var etld = this.getEffectiveTldPlusOne_(origin,
    EffectiveTldFetcher.fixed_tld_list_);
  if (etld) {
    return Promise.resolve(/** @type {?string} */(etld));
  }
  return Promise.resolve(this.getEffectiveTldPlusOne_(origin, this.eTlds_));
};

/**
 * @param {string} origin The origin.
 * @param {!Array<string>} eTlds The list of extended TLDs.
 * @return {?string} The eTLD + 1 of the origin, or null if it doesn't have one
 *     (e.g. is invalid.)
 * @private
 */
EffectiveTldFetcher.prototype.getEffectiveTldPlusOne_ =
    function(origin, eTlds) {
  var prev = '';
  var host;
  if (origin.indexOf('http://') == 0) {
    host = origin.substring(7);
  } else if (origin.indexOf('https://') == 0) {
    host = origin.substring(8);
  } else {
    host = origin;
  }
  if (host.indexOf(':') != -1) {
    host = host.substring(0, host.indexOf(':'));
  }
  if (host == 'localhost') {
    return host;
  }
  // Loop over each possible subdomain, from longest to shortest, in order to
  // find the longest matching eTLD first.
  var next = host;
  while (true) {
    var dot = next.indexOf('.');
    if (dot == -1) return null;
    prev = next;
    next = next.substring(dot + 1);
    if (eTlds.indexOf(next) >= 0) {
      return prev;
    }
  }
};

