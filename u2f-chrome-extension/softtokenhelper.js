/**
 * @fileoverview A helper that generates signatures in-browser using E2E
 * crypto. FOR TESTING ONLY.
 * @author arnarb@google.com (Arnar Birgisson)
 */
'use strict';


function generateKey(opt_sk) {
  var key = e2e.ecc.Protocol.generateKeyPair(e2e.ecc.PrimeCurve.P_256, opt_sk);
  return new e2e.ecc.Ecdsa(e2e.ecc.PrimeCurve.P_256, key);
}

/**
 * @constructor
 * @param {!SoftTokenProfile} profile
 * @extends {GenericRequestHelper}
 */
function SoftTokenHelper(profile) {
  this.profile_ = profile;
  GenericRequestHelper.apply(this, []);

  this.registerHandlerFactory('enroll_helper_request', function(request) {
    return new SoftTokenEnrollHandler(profile, request);
  });
  this.registerHandlerFactory('sign_helper_request', function(request) {
    return new SoftTokenSignHandler(profile, request);
  });
}

inherits(SoftTokenHelper, GenericRequestHelper);


/** @typedef {{pub: ?string, sec: string}} */
var SoftTokenKeyPair;


/** @typedef {{
 *    appIdHash: string,
 *    keyHandle: string,
 *    keys: !SoftTokenKeyPair,
 *    counter: number
 * }}
 */
var SoftTokenRegistration;


/**
 * A key pair for testing. When non-null, will be used instead of generating
 * a random key pair on enroll.
 * @type {?SoftTokenKeyPair}
 */
SoftTokenHelper.keyPairForTesting = null;



/**
 * A soft token profile represents the state of a single token.
 * @constructor
 */
function SoftTokenProfile() {
  /**
   * The attestation private key as a hex string
   * @type {string}
   */
  this.attestationKey = SoftTokenProfile.DEFAULT_ATTESTATION_KEY;
  /**
   * The attestation certificate in X.509 as a hex string
   * @type {string}
   */
  this.attestationCert = SoftTokenProfile.DEFAULT_ATTESTATION_CERT;
  /**
   * Registrations (i.e. appId/keyHandle pairs) known to this key.
   * Keys are hex-encoded and appIdHash and keyHandle are base64-urlsafe.
   * @type {!Array<!SoftTokenRegistration>}
   */
  this.registrations = [];
}


/**
 * A default attestation key, from U2F Raw Message Formats example section.
 * @const {string}
 */
SoftTokenProfile.DEFAULT_ATTESTATION_KEY =
    'f3fccc0d00d8031954f90864d43c247f4bf5f0665c6b50cc17749a27d1cf7664';


/**
 * A default attestation cert, from U2F Raw Message Formats example section.
 * @const {string}
 */
SoftTokenProfile.DEFAULT_ATTESTATION_CERT =
    '3082013c3081e4a003020102020a47901280001155957352300a06082a8648ce' +
    '3d0403023017311530130603550403130c476e756262792050696c6f74301e17' +
    '0d3132303831343138323933325a170d3133303831343138323933325a303131' +
    '2f302d0603550403132650696c6f74476e756262792d302e342e312d34373930' +
    '313238303030313135353935373335323059301306072a8648ce3d020106082a' +
    '8648ce3d030107034200048d617e65c9508e64bcc5673ac82a6799da3c144668' +
    '2c258c463fffdf58dfd2fa3e6c378b53d795c4a4dffb4199edd7862f23abaf02' +
    '03b4b8911ba0569994e101300a06082a8648ce3d0403020347003044022060cd' +
    'b6061e9c22262d1aac1d96d8c70829b2366531dda268832cb836bcd30dfa0220' +
    '631b1459f09e6330055722c8d89b7f48883b9089b88d60d1d9795902b30410df';


/**
 * Initializes a new registration record and stores in the profile.
 * @param {string} appIdHash in base64-urlsafe
 * @param {SoftTokenKeyPair} keypair
 * @return {!SoftTokenRegistration}
 */
SoftTokenProfile.prototype.createRegistration = function(appIdHash, keypair) {
  var registration = {
    appIdHash: appIdHash,
    keyHandle: B64_encode(new SHA256().digest(keypair.pub)),
    keys: {
      sec: keypair.sec,
      pub: keypair.pub
    },
    counter: 1
  };
  this.registrations.push(registration);
  return registration;
};


/**
 * Looks up an existing registration by appId and keyHandle.
 * Returns null if not found.
 * @param {string} appIdHash in base64-urlsafe
 * @param {string} keyHandle in base64-urlsafe
 * @return {?SoftTokenRegistration}
 */
SoftTokenProfile.prototype.getRegistration = function(appIdHash, keyHandle) {
  var reg = null;
  for (var i = 0; i < this.registrations.length; ++i) {
    reg = this.registrations[i];
    if (reg.appIdHash == appIdHash && reg.keyHandle == keyHandle)
      return reg;
  }
  return null;
};



/**
 * @param {!Object} profile
 * @param {*} request
 * @constructor
 * @implements {RequestHandler}
 */
function SoftTokenEnrollHandler(profile, request) {
  this.profile_ = profile;
  this.request_ = request;
}


/**
 * @param {RequestHandlerCallback} cb Called with the result of the request
 * @return {boolean} Whether this handler could be run.
 */
SoftTokenEnrollHandler.prototype.run = function(cb) {
  console.log('SoftTokenEnrollHandler.run', this.request_);

  var i;

  // First go through signData and see if we already own one of the keyhandles
  for (i = 0; i < this.request_.signData.length; ++i) {
    var sd = this.request_.signData[i];
    if (sd.version != 'U2F_V2')
      continue;
    if (this.profile_.getRegistration(sd.appIdHash, sd.keyHandle)) {
      cb({
        'type': 'enroll_helper_reply',
        'code': DeviceStatusCodes.WRONG_DATA_STATUS
      });
      return true;
    }
  }

  // Not yet registered, look for an enroll challenge with our version
  var challenge;
  for (i = 0; i < this.request_.enrollChallenges.length; ++i) {
    var c = this.request_.enrollChallenges[i];
    if (c.version && c.version == 'U2F_V2') {
      challenge = c;
      break;
    }
  }
  if (!challenge) {
    cb({
      'type': 'enroll_helper_reply',
      'code': DeviceStatusCodes.TIMEOUT_STATUS
    });
    return true;
  }

  // Found a challenge, lets register it
  var keyPair;
  if (SoftTokenHelper.keyPairForTesting) {
    keyPair = SoftTokenHelper.keyPairForTesting;
  } else {
    var tmpEcdsa = generateKey();
    keyPair = {
      sec: UTIL_BytesToHex(tmpEcdsa.getPrivateKey()),
      pub: UTIL_BytesToHex(tmpEcdsa.getPublicKey())
    };
  }
  var registration =
      this.profile_.createRegistration(challenge.appIdHash, keyPair);

  var appIdBytes = new Uint8Array(B64_decode(challenge.appIdHash));
  var challengeBytes = new Uint8Array(B64_decode(challenge.challengeHash));
  var keyHandleBytes = new Uint8Array(B64_decode(registration.keyHandle));
  var publicKeyBytes = UTIL_HexToBytes(registration.keys.pub);

  var signBuf = new Uint8Array(1 + 32 + 32 + keyHandleBytes.length + 65);
  signBuf[0] = 0x00;
  signBuf.set(appIdBytes, 1);
  signBuf.set(challengeBytes, 33);
  signBuf.set(keyHandleBytes, 65);
  signBuf.set(publicKeyBytes, 65 + keyHandleBytes.length);

  // E2E SHA-256 requires a regular Array<number>
  signBuf = Array.prototype.slice.call(signBuf);

  var ecdsa = generateKey(UTIL_HexToArray(this.profile_.attestationKey));
  var signature = UTIL_JsonSignatureToAsn1(ecdsa.sign(signBuf));

  var certBytes = UTIL_HexToBytes(this.profile_.attestationCert);

  var regData = new Uint8Array(1 + 65 + 1 +
      keyHandleBytes.length + certBytes.length + signature.length);

  var offset = 0;
  regData[offset++] = 0x05;
  regData.set(publicKeyBytes, offset);
  offset += publicKeyBytes.length;
  regData[offset++] = keyHandleBytes.length;
  regData.set(keyHandleBytes, offset);
  offset += keyHandleBytes.length;
  regData.set(certBytes, offset);
  offset += certBytes.length;
  regData.set(signature, offset);

  cb({
    'type': 'enroll_helper_reply',
    'code': DeviceStatusCodes.OK_STATUS,
    'version': 'U2F_V2',
    'enrollData': B64_encode(regData)
  });
  return true;
};


/**
 * Closes this handler.
 */
SoftTokenEnrollHandler.prototype.close = function() {
  // No-op
};



/**
 * @param {!Object} profile
 * @param {*} request
 * @constructor
 * @implements {RequestHandler}
 */
function SoftTokenSignHandler(profile, request) {
  this.profile_ = profile;
  this.request_ = request;
}


/**
 * @param {RequestHandlerCallback} cb Called with the result of the request
 * @return {boolean} Whether this handler could be run.
 */
SoftTokenSignHandler.prototype.run = function(cb) {
  console.log('SoftTokenSignHandler.run', this.request_);

  var i;

  // See if we know any of the keyHandles
  var registration, signData = null;
  for (i = 0; i < this.request_.signData.length; ++i) {
    var sd = this.request_.signData[i];
    if (sd.version != 'U2F_V2')
      continue;
    registration = this.profile_.getRegistration(sd.appIdHash, sd.keyHandle);
    if (registration) {
      signData = sd;
      break;
    }
  }

  if (!signData) {
    cb({
      'type': 'sign_helper_reply',
      'code': DeviceStatusCodes.WRONG_DATA_STATUS
    });
    return true;
  }

  // Increment the counter
  ++registration.counter;

  var signBuffer = new Uint8Array(32 + 1 + 4 + 32);
  signBuffer.set(B64_decode(registration.appIdHash), 0);
  signBuffer[32] = 0x01;  // user presence
  // Sadly, JS TypedArrays are whatever-endian the platform is,
  // so Uint32Array is not at all useful here (or anywhere?),
  // and we must manually pack the counter (big endian as per spec).
  signBuffer[33] = 0xFF & registration.counter >>> 24;
  signBuffer[34] = 0xFF & registration.counter >>> 16;
  signBuffer[35] = 0xFF & registration.counter >>> 8;
  signBuffer[36] = 0xFF & registration.counter;
  signBuffer.set(B64_decode(signData.challengeHash), 37);

  // E2E SHA-256 requires a regular Array<number>
  var signBufferArray = Array.prototype.slice.call(signBuffer);

  var ecdsa = generateKey(UTIL_HexToArray(registration.keys.sec));
  var signature = UTIL_JsonSignatureToAsn1(ecdsa.sign(signBufferArray));

  var signatureData = new Uint8Array(1 + 4 + signature.length);
  // Grab user presence byte and counter from the sign base buffer
  signatureData.set(signBuffer.subarray(32, 37), 0);
  signatureData.set(signature, 5);

  cb({
    'type': 'sign_helper_reply',
    'code': DeviceStatusCodes.OK_STATUS,
    'responseData': {
      'version': 'U2F_V2',
      'appIdHash': registration.appIdHash,
      'challengeHash': signData.challengeHash,
      'keyHandle': registration.keyHandle,
      'signatureData': B64_encode(signatureData)
    }
  });
  return true;
};


/**
 * Closes this handler.
 */
SoftTokenSignHandler.prototype.close = function() {
  // No-op
};

