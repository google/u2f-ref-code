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
  /** @private {!TextFetcher} */
  this.fetcher_ = fetcher;
  /** @private {boolean} */
  this.cacheEtlds_ = opt_cache || false;
  /** @private {(!Array.<string>)|undefined} */
  this.eTlds_ = undefined;
}

/** @const */
EffectiveTldFetcher.EFFECTIVE_TLD_LIST_URLS = [
  // Preferred for its HSTS pinning:
  'https://git.chromium.org/gitweb/?p=chromium/src/net.git;a=blob_plain;' +
  'f=base/registry_controlled_domains/effective_tld_names.dat;hb=HEAD',
  // Other authoritative source maintained by Mozilla.
  'https://publicsuffix.org/list/effective_tld_names.dat'
];

/**
 * A fixed list of known TLDs.
 * @private {!Array.<string>}
 */
EffectiveTldFetcher.fixed_tld_list_ = [];

/**
 * Sets a fixed list of known TLDs. This list is not canonical: if an origin
 * is not found to be in this list, a canonical list is fetched from
 * EffectiveTldFetcher.EFFECTIVE_TLD_LIST_URLS.
 * @param {!Array.<string>} tlds The list of known TLDs.
 */
EffectiveTldFetcher.setFixedTldList = function(tlds) {
  EffectiveTldFetcher.fixed_tld_list_ = tlds;
};

/**
 * @param {string} origin The origin.
 * @return {Promise.<?string>} A promise for the eTLD+1 of origin, or null if it
 *     doesn't have one (e.g. the origin is invalid.)
 */
EffectiveTldFetcher.prototype.getEffectiveTldPlusOne = function(origin) {
  var etld = this.getEffectiveTldPlusOne_(origin,
    EffectiveTldFetcher.fixed_tld_list_);
  if (etld) {
    return Promise.resolve(/** @type {?string} */(etld));
  }
  var p = this.loadEffectiveTlds_();
  var self = this;
  return p.then(function(eTlds) {
    return self.getEffectiveTldPlusOne_(origin, eTlds);
  });
};

/**
 * @param {string} origin The origin.
 * @param {!Array.<string>} eTlds The list of extended TLDs.
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

/**
 * Retrieves the list of extended TLDs.
 * @param {number=} opt_index Which url from EFFECTIVE_TLD_LIST_URLS
 *     to fetch, default 0.
 * @return {Promise.<!Array.<string>>} A promise for an array of eTLDs.
 * @private
 */
EffectiveTldFetcher.prototype.loadEffectiveTlds_ = function(opt_index) {
  var index = opt_index === undefined ? 0 : opt_index;
  if (this.eTlds_) {
    return Promise.resolve(this.eTlds_);
  }
  var p = this.fetcher_.fetch(
    EffectiveTldFetcher.EFFECTIVE_TLD_LIST_URLS[index]);
  var self = this;
  return p.then(function(text) {
    var eTlds = self.getEffectiveTldsFromText_(/** @type {string} */ (text));
    if (self.cacheEtlds_) {
      self.eTlds_ = eTlds;
    }
    return eTlds;
  }, function(rc) {
    if (rc == 404 &&
        index + 1 < EffectiveTldFetcher.EFFECTIVE_TLD_LIST_URLS.length) {
      // Retry with the next URL in the list
      return self.loadEffectiveTlds_(index + 1);
    } else {
      return [];
    }
  });
};

/**
 * Parses the text input as a sequence of newline-delimited extended TLDs.
 * @param {string} text The text to parse.
 * @return {!Array.<string>} The list of extended TLDs.
 * @private
 */
EffectiveTldFetcher.prototype.getEffectiveTldsFromText_ = function(text) {
  var origins = [];
  var lines = text.split('\n');
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i];
    if (!line) {
      continue;
    }
    if (line.substring(0, 2) == '//') {
      continue;
    }
    // For now, ignore non-alpha first characters, e.g. *.mz or !teledata.mz,
    // because interpreting them correctly is more difficult: wildcard matching
    // rules imply precedence within the origins list. See the rule definition
    // at https://publicsuffix.org/list/
    // TODO: This test also doesn't match non-alpha starting
    // characters. Implement for these cases too.
    if (!/^[a-zA-Z]/.test(line)) {
      continue;
    }
    origins.push(line);
  }
  return origins;
};

