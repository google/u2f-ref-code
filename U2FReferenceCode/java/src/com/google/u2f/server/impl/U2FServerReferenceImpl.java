package com.google.u2f.server.impl;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import com.google.u2f.U2FConsts;
import com.google.u2f.U2FException;
import com.google.u2f.codec.RawMessageCodec;
import com.google.u2f.key.UserPresenceVerifier;
import com.google.u2f.key.messages.AuthenticateResponse;
import com.google.u2f.key.messages.RegisterResponse;
import com.google.u2f.server.ChallengeGenerator;
import com.google.u2f.server.Crypto;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.SessionIdGenerator;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SessionData;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignRequest;
import com.google.u2f.server.messages.SignResponse;

public class U2FServerReferenceImpl implements U2FServer {
	private static final Logger Log = Logger.getLogger(U2FServerReferenceImpl.class.getName());

	private final String appId;
	private final ChallengeGenerator challengeGenerator;
	private final SessionIdGenerator sessionIdGenerator;
	private final DataStore dataStore;
	private final Crypto cryto;

	public U2FServerReferenceImpl(String appId, ChallengeGenerator challengeGenerator,
	    SessionIdGenerator sessionIdGenerator, DataStore dataStore, Crypto cryto) {
		this.appId = appId;
		this.challengeGenerator = challengeGenerator;
		this.sessionIdGenerator = sessionIdGenerator;
		this.dataStore = dataStore;
		this.cryto = cryto;
	}

	@Override
	public RegistrationRequest getRegistrationRequest(String accountName) {
		Log.info(">> getRegistrationRequest " + accountName);

		String sessionId = sessionIdGenerator.generateSessionId(accountName);
		byte[] challenge = challengeGenerator.generateChallenge(accountName);
		SessionData sessionData = new SessionData(accountName, challenge);

		dataStore.storeSessionData(sessionId, sessionData);

		String challengeBase64 = Base64.encodeBase64URLSafeString(challenge);

		Log.info("-- Output --");
		Log.info("  sessionId: " + sessionId);
		Log.info("  challenge: " + Hex.encodeHexString(challenge));

		Log.info("<< getRegistrationRequest " + accountName);

		return new RegistrationRequest(U2FConsts.U2F_V2, challengeBase64, appId, sessionId);
	}

	@Override
	public void processRegistrationResponse(RegistrationResponse registrationResponse)
	    throws U2FException {
		Log.info(">> processRegistrationResponse");

		String sessionId = registrationResponse.getSessionId();
		String browserDataBase64 = registrationResponse.getBd();
		String rawRegistrationDataBase64 = registrationResponse.getRegistrationData();

		SessionData sessionData = dataStore.getSessionData(sessionId);

		if (sessionData == null) {
			throw new U2FException("Unknown session_id");
		}

		String browserData = new String(Base64.decodeBase64(browserDataBase64));
		byte[] rawRegistrationData = Base64.decodeBase64(rawRegistrationDataBase64);

		Log.info("-- Input --");
		Log.info("  sessionId: " + sessionId);
		Log.info("  challenge: " + Hex.encodeHexString(sessionData.getChallenge()));
		Log.info("  accountName: " + sessionData.getAccountName());
		Log.info("  browserData: " + browserData);
		Log.info("  rawRegistrationData: " + Hex.encodeHexString(rawRegistrationData));

		RegisterResponse registerResponse = RawMessageCodec.decodeRegisterResponse(rawRegistrationData);
		byte[] userPublicKey = registerResponse.getUserPublicKey();
		byte[] keyHandle = registerResponse.getKeyHandle();
		X509Certificate attestationCertificate = registerResponse.getAttestationCertificate();
		byte[] signature = registerResponse.getSignature();

		Log.info("-- Parsed rawRegistrationResponse --");
		Log.info("  userPublicKey: " + Hex.encodeHexString(userPublicKey));
		Log.info("  keyHandle: " + Hex.encodeHexString(keyHandle));
		Log.info("  attestationCertificate: " + attestationCertificate.toString());
		try {
			Log.info("  attestationCertificate bytes: "
			    + Hex.encodeHexString(attestationCertificate.getEncoded()));
		} catch (CertificateEncodingException e) {
			throw new U2FException("Cannot encode certificate", e);
		}
		Log.info("  signature: " + Hex.encodeHexString(signature));

		byte[] appIdSha256 = cryto.computeSha256(appId.getBytes());
		byte[] browserDataSha256 = cryto.computeSha256(browserData.getBytes());
		byte[] signedBytes = RawMessageCodec.encodeRegistrationSignedBytes(appIdSha256, browserDataSha256,
		    keyHandle, userPublicKey);

		Set<X509Certificate> trustedCertificates = dataStore.getTrustedCertificates();
		if (!trustedCertificates.contains(attestationCertificate)) {
			throw new U2FException("Certificate is not trusted");
		}

		Log.info("Verifying signature of bytes " + Hex.encodeHexString(signedBytes));
		if (!cryto.verifySignature(attestationCertificate, signedBytes, signature)) {
			throw new U2FException("Signature is invalid");
		}

		dataStore.storeSecurityKeyData(sessionData.getAccountName(), new SecurityKeyData(keyHandle,
		    userPublicKey));

		Log.info("<< processRegistrationResponse");
	}

	@Override
	public SignRequest getSignRequest(String accountName) throws U2FException {
		Log.info(">> getSignRequest " + accountName);

		String sessionId = sessionIdGenerator.generateSessionId(accountName);
		byte[] challenge = challengeGenerator.generateChallenge(accountName);
		SessionData sessionData = new SessionData(accountName, challenge);
		SecurityKeyData securityKeyData = dataStore.getSecurityKeyData(accountName);

		if (securityKeyData == null) {
			throw new U2FException("No security keys registered for this user");
		}

		dataStore.storeSessionData(sessionId, sessionData);

		byte[] keyHandle = securityKeyData.getKeyHandle();

		Log.info("-- Output --");
		Log.info("  sessionId: " + sessionId);
		Log.info("  challenge: " + Hex.encodeHexString(challenge));
		Log.info("  keyHandle: " + Hex.encodeHexString(keyHandle));

		String challengeBase64 = Base64.encodeBase64URLSafeString(challenge);
		String keyHandleBase64 = Base64.encodeBase64URLSafeString(keyHandle);

		Log.info("<< getSignRequest " + accountName);

		return new SignRequest(U2FConsts.U2F_V2, challengeBase64, appId, keyHandleBase64, sessionId);
	}

	@Override
	public void processSignResponse(SignResponse signResponse) throws U2FException {
		Log.info(">> processSignResponse");

		String sessionId = signResponse.getSessionId();
		String browserDataBase64 = signResponse.getBd();
		String rawSignDataBase64 = signResponse.getSign();

		SessionData sessionData = dataStore.getSessionData(sessionId);

		if (sessionData == null) {
			throw new U2FException("Unknown session_id");
		}

		SecurityKeyData securityKeyData = dataStore.getSecurityKeyData(sessionData.getAccountName());

		if (securityKeyData == null) {
			throw new U2FException("No security keys registered for this user");
		}

		String browserData = new String(Base64.decodeBase64(browserDataBase64));
		byte[] rawSignData = Base64.decodeBase64(rawSignDataBase64);

		Log.info("-- Input --");
		Log.info("  sessionId: " + sessionId);
		Log.info("  challenge: " + Hex.encodeHexString(sessionData.getChallenge()));
		Log.info("  accountName: " + sessionData.getAccountName());
		Log.info("  browserData: " + browserData);
		Log.info("  rawSignData: " + Hex.encodeHexString(rawSignData));
		
		// TODO: verify browserData

		AuthenticateResponse authenticateResponse = RawMessageCodec.decodeAuthenticateResponse(rawSignData);
		byte userPresence = authenticateResponse.getUserPresence();
		int counter = authenticateResponse.getCounter();
		byte[] signature = authenticateResponse.getSignature();

		Log.info("-- Parsed rawSignData --");
		Log.info("  userPresence: " + Integer.toHexString(userPresence & 0xFF));
		Log.info("  counter: " + counter);
		Log.info("  signature: " + Hex.encodeHexString(signature));
		
		if (userPresence != UserPresenceVerifier.USER_PRESENT_FLAG) {
			throw new U2FException("User presence invalid during authentication");
		}

		byte[] appIdSha256 = cryto.computeSha256(appId.getBytes());
		byte[] browserDataSha256 = cryto.computeSha256(browserData.getBytes());
		byte[] signedBytes = RawMessageCodec.encodeAuthenticateSignedBytes(appIdSha256, userPresence,
		    counter, browserDataSha256);

		Log.info("Verifying signature of bytes " + Hex.encodeHexString(signedBytes));
		if (!cryto.verifySignature(cryto.decodePublicKey(securityKeyData.getPublicKey()), signedBytes,
		    signature)) {
			throw new U2FException("Signature is invalid");
		}

		Log.info("<< processSignResponse");
	}
}
