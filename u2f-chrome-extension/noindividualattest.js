// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Provides a default implementation of IndividualAttestation.
 */
'use strict';

/**
 * Default implementation of IndividualAttestation that always returns no,
 * i.e. only requests the batch attestation certificate.
 * @constructor
 * @implements IndividualAttestation
 */
function NoIndividualAttestation() {}

/**
 * @param {string} appIdHash The app id hash.
 * @return {boolean} Whether to request the individual attestation certificate
 *     for this app id.
 */
NoIndividualAttestation.prototype.requestIndividualAttestation =
    function(appIdHash) {
  return false;
};
