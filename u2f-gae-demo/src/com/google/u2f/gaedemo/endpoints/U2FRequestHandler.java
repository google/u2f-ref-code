package com.google.u2f.gaedemo.endpoints;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.u2f.U2FException;
import com.google.u2f.gaedemo.impl.ChallengeGeneratorImpl;
import com.google.u2f.gaedemo.impl.DataStoreImpl;
import com.google.u2f.gaedemo.storage.TokenStorageData;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.impl.BouncyCastleCrypto;
import com.google.u2f.server.impl.U2FServerReferenceImpl;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignResponse;
import com.google.u2f.server.messages.U2fSignRequest;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.util.List;
import javax.inject.Named;
import javax.servlet.ServletException;

@Api(
  name = "u2fRequestHandler",
  version = "v1",
  scopes = {Constants.EMAIL_SCOPE, Constants.OPENID_SCOPE},
  clientIds = {Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID},
  audiences = {Constants.ANDROID_AUDIENCE},
  namespace =
      @ApiNamespace(ownerName = "gaedemo.u2f.google.com", ownerDomain = "gaedemo.u2f.google.com")
)
/**
 * An endpoint class for handling U2F requests.
 * 
 * Google Cloud Endpoints generate APIs and client libraries from API backend, to simplify client
 * access to data from other applications.
 * https://cloud.google.com/appengine/docs/standard/java/endpoints/
 */
public class U2FRequestHandler {
  private static final String KEY_APP_ID = "appId";
  private static final String KEY_SESSION_ID = "sessionId"; 
  private static final String KEY_CHALLENGE = "challenge"; 
  private static final String KEY_VERSION = "version"; 
  private static final String KEY_KEYHANDLE = "keyHandle"; 
  private static final String KEY_CLIENTDATA = "clientData"; 
  private static final String KEY_REGISTER_REQUESTS = "registerRequests";
  private static final String KEY_REGISTER_KEYS = "registeredKeys";
  private static final String KEY_REGISTRATION_DATA = "registrationData";
  private static final String KEY_SIGNATURE_DATA = "signatureData"; 

  private U2FServer u2fServer = null;
  private DataStore dataStore = null;

  // https://cloud.google.com/appengine/docs/standard/java/endpoints/annotate-code
  // https://cloud.google.com/appengine/docs/standard/java/endpoints/parameter-and-return-types
  @ApiMethod(name = "getRegistrationRequest")
  public String[] getRegistrationRequest(
      User user, @Named("allowReregistration") boolean allowReregistration)
      throws OAuthRequestException, ServletException {
    if (user == null) {
      throw new OAuthRequestException("user is not authenticated");
    }

    if (u2fServer == null) {
      initU2fServer();
    }

    RegistrationRequest registrationRequest;
    U2fSignRequest signRequest;
    try {
      registrationRequest = u2fServer.getRegistrationRequest(user.getEmail(), Constants.APP_ID);
      signRequest = u2fServer.getSignRequest(user.getEmail(), Constants.APP_ID);
    } catch(U2FException e) {
      throw new ServletException("couldn't get registration request", e);
    }

    JsonObject result = new JsonObject();
    result.addProperty(KEY_APP_ID, Constants.APP_ID);
    result.addProperty(KEY_SESSION_ID, registrationRequest.getSessionId());

    JsonArray registerRequests = new JsonArray();
    JsonObject registerRequest = new JsonObject();
    registerRequest.addProperty(KEY_CHALLENGE, registrationRequest.getChallenge());
    registerRequest.addProperty(KEY_VERSION, registrationRequest.getVersion());
    registerRequests.add(registerRequest);
    result.add(KEY_REGISTER_REQUESTS, registerRequests);

    if (allowReregistration) {
      result.add(KEY_REGISTER_KEYS, new JsonArray());
    } else {
      result.add(KEY_REGISTER_KEYS, signRequest.getRegisteredKeysAsJson(Constants.APP_ID));
    }

    return new String[] {result.toString()};
  }

  @ApiMethod(name = "processRegistrationResponse")
  public String[] processRegistrationResponse(
      @Named("responseData") String responseData, User user)
      throws OAuthRequestException, ServletException {
    if (user == null) {
      throw new OAuthRequestException("user is not authenticated");
    }

    JsonObject responseDataJson = (JsonObject) new JsonParser().parse(responseData);

    String currentUser = user.getEmail();
    String expectedUser = dataStore.getEnrollSessionData(
        responseDataJson.get(KEY_SESSION_ID).toString()).getAccountName();
    if (!currentUser.equals(expectedUser)) {
      throw new ServletException(
          "Current user differs from the expected one. Cross-site request prohibited!");
    }

    RegistrationResponse registrationResponse =
        new RegistrationResponse(
            responseDataJson.get(KEY_REGISTRATION_DATA).toString(),
            responseDataJson.get(KEY_CLIENTDATA).toString(),
            responseDataJson.get(KEY_SESSION_ID).toString());

    if (u2fServer == null) {
      initU2fServer();
    }

    SecurityKeyData securityKeyData;

    try {
      securityKeyData =
          u2fServer.processRegistrationResponse(registrationResponse, System.currentTimeMillis());
    } catch (U2FException e) {
      throw new ServletException(e);
    }

    return new String[] {new TokenStorageData(securityKeyData).toJson().toString()};
  }

  @ApiMethod(name = "getSignRequest", path = "getSignRequest")
  public String[] getSignRequest(User user) throws OAuthRequestException, ServletException {
    if (user == null) {
      throw new OAuthRequestException("user is not authenticated");
    }

    if (u2fServer == null) {
      initU2fServer();
    }

    U2fSignRequest u2fSignRequest;
    try {
      u2fSignRequest = u2fServer.getSignRequest(user.getEmail(), Constants.APP_ID);
    } catch (U2FException e) {
      throw new ServletException("couldn't get sign request", e);
    }

    JsonObject result = new JsonObject();
    result.addProperty(KEY_CHALLENGE, u2fSignRequest.getChallenge());
    result.addProperty(KEY_APP_ID, Constants.APP_ID);
    result.add(KEY_REGISTER_KEYS, u2fSignRequest.getRegisteredKeysAsJson(Constants.APP_ID));

    return new String[] {result.toString()};
  }

  @ApiMethod(name = "processSignResponse")
  public String[] processSignResponse(@Named("responseData") String responseData, User user)
      throws OAuthRequestException, ServletException {
    if (user == null) {
      throw new OAuthRequestException("user is not authenticated");
    }

    JsonObject responseDataJson = (JsonObject) new JsonParser().parse(responseData);
    String currentUser = user.getEmail();
    String expectedUser = dataStore
        .getSignSessionData(responseDataJson.get(KEY_SESSION_ID).toString()).getAccountName();

    if (!currentUser.equals(expectedUser)) {
      throw new ServletException(
          "Current user differs from the expected one. Cross-site request prohibited!");
    }

    SignResponse signResponse =
        new SignResponse(
            responseDataJson.get(KEY_KEYHANDLE).toString(),
            responseDataJson.get(KEY_SIGNATURE_DATA).toString(),
            responseDataJson.get(KEY_CLIENTDATA).toString(),
            responseDataJson.get(KEY_SESSION_ID).toString());

    if (u2fServer == null) {
      initU2fServer();
    }

    SecurityKeyData securityKeyData;
    try {
      securityKeyData = u2fServer.processSignResponse(signResponse);
    } catch (U2FException e) {
      throw new ServletException("signature didn't verify", e);
    }

    return new String[] {new TokenStorageData(securityKeyData).toJson().toString()};
  }

  @ApiMethod(name = "getAllSecurityKeys", path = "getAllSecurityKeys")
  public String[] getAllSecurityKeys(User user) throws OAuthRequestException {
    if (user == null) {
      throw new OAuthRequestException("user is not authenticated");
    }

    JsonArray resultList = new JsonArray();
    if (u2fServer == null) {
      initU2fServer();
    }

    List<SecurityKeyData> tokens = u2fServer.getAllSecurityKeys(user.getEmail());
    for (SecurityKeyData token : tokens) {
      resultList.add(new TokenStorageData(token).toJson());
    }
    return new String[] {resultList.toString()};
  }

  @ApiMethod(name = "removeSecurityKey")
  public String[] removeSecurityKey(User user, @Named("publicKey") String publicKey)
      throws OAuthRequestException, U2FException, DecoderException {
    if (user == null) {
      throw new OAuthRequestException("user is not authenticated");
    }

    if (u2fServer == null) {
      initU2fServer();
    }

    u2fServer.removeSecurityKey(user.getEmail(), Hex.decodeHex(publicKey.toCharArray()));
    return new String[] {"OK"};
  }

  private void initU2fServer() {
    if (dataStore == null) {
      dataStore = new DataStoreImpl();
    }

    u2fServer =
        new U2FServerReferenceImpl(
            new ChallengeGeneratorImpl(),
            dataStore,
            new BouncyCastleCrypto(),
            ImmutableSet.of(
                // This implementation will only accept signatures from the following origins,
                // as this class is for generating endpoints APIs for the below Android client app.
                // This list should always be in sync with the content in Constants.APP_ID.
                "android:apk-key-hash:bkHnlWEV_jRCPdYGJfwOl7Sn_CLC_2TE3h4TO1_n34I"));
  }
}
