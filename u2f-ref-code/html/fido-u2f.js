// Note: not a reference implementation. Google specific implementation
// for tests only
var FidoU2fGnubbydChromeExtension = function(exensionId) {
  var enroll = function(enrollData, callback) {
    // Inputs from the server
    var sessionId = enrollData.sessionId;
    var appId = enrollData.appId;
    var challenge = enrollData.challenge;
    var version = enrollData.version;
    
    var enrollRequest = {
        type: "enroll_web_request",
        enrollChallenges: [
            {
              appId: appId,
              challenge: challenge,
              version: version
            }
        ]
    };
    
    function extensionCallback(extensionResult) {
      if (!extensionResult.responseData) {
        callback({errorCode: extensionResult.code})
        return;
      } 
      var responseData = extensionResult.responseData;
      callback({
        browserData: responseData.browserData,
        challenge: responseData.challenge,
        enrollData: responseData.enrollData,
        sessionId: sessionId
      });
    }
    
    chrome.runtime.sendMessage(extensionId, enrollRequest, extensionCallback);
  }

  var sign = function() {
    // Inputs from the server are in a global object    
    var signRequest = {
        type: "sign_web_request",
        signData: signData
    };
    
    function extensionCallback(extensionResult) {
      if (!extensionResult.responseData) {
        callback({errorCode: extensionResult.code})
        return;
      } 
      var responseData = extensionResult.responseData;
      callback({
        browserData: responseData.browserData,
        challenge: responseData.challenge,
        signData: responseData.signatureData,
        appId: responseData.appId,
        sessionId: responseData.sessionId
      });
    }
    
    chrome.runtime.sendMessage(extensionId, signRequest, extensionCallback);
  }
  
  return {
    enroll: enroll,
    sign: sign
  };
};

//Note: not a reference implementation. Google specific implementation
//for tests only
var FidoU2fGAAndroid = function() {
  var enroll = function(enrollData, callback) {
    // Inputs from the server
    var sessionId = enrollData.sessionId;
    var appId = enrollData.appId;
    var challenge = enrollData.challenge;
    var version = enrollData.version;
    
    var enrollRequest = {
        challenge: {
          typ: "navigator.id.getAssertion",
          challenge: challenge
        }
    };
    
    var intentUrl =
        "intent:#Intent;"
        + "action=com.google.android.apps.authenticator.ENROLL;"
        + "S.appId=" + appId + ";"
        + "S.challenges=" + JSON.stringify(enrollRequest) + ";"
        + "end";
    
    window.addEventListener("message", callback, !1);
        
    document.location = intentUrl;
  }
  
  var sign = function() {
    // Inputs from the server
    var sessionId = signData.sessionId;
    var appId = signData.appId;
    var challenge = signData.challenge;
    var version = signData.version;
    var keyHandle = signData.keyHandle;
    
    var signRequest = {
        challenge: {
          typ: "navigator.id.getAssertion",
          challenge: challenge
        },
        keyHandle: keyHandle
    };
    
    var intentUrl =
        "intent:#Intent;"
        + "action=com.google.android.apps.authenticator.AUTHENTICATE;"
        + "S.appId=" + appId + ";"
        + "S.challenges=" + JSON.stringify(signRequest) + ";"
        + "end";
    
    function androidCallback(androidResult) {
      var messageData = JSON.parse(androidResult.data);
      if (messageData.errorCode != 0) {
        callback({errorCode: messageData.errorCode});
        return;
      }
      
      var resultData = JSON.parse(messageData.data);
      if (resultData.resultCode != 0) {
        callback({errorCode: resultData.resultCode});
        return;
      }
      
      callback({
        browserData: responseData.browserData,
        challenge: responseData.challenge,
        signData: responseData.signatureData,
        appId: responseData.appId,
        sessionId: sessionId
      });
    }
    
    window.addEventListener("message", androidCallback, !1);
        
    document.location = intentUrl;
  }
  
  return {
    enroll: enroll,
    sign: sign
  };
};

var FidoU2f = function(exensionId) {
  var isBrowserChrome = chrome != undefined;
  var isPlatformAndroid = navigator.userAgent.indexOf("Android") != -1;
  
  // TODO: check and refuse unsupported environments.
  
  if (!isBrowserChrome) {
    throw "Unsupported configuration";
  }
  
  if (isPlatformAndroid) {
    return new FidoU2fGAAndroid();
  }
  
  return new FidoU2fGnubbydChromeExtension(exensionId);
};