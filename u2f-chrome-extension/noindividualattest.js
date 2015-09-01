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
