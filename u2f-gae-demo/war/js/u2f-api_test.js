/**
 * @fileoverview Unit tests for the U2F client api.
 */
'use strict';

goog.require('goog.testing.AsyncTestCase');
goog.require('goog.testing.MockControl');
goog.require('goog.testing.PropertyReplacer');
goog.require('goog.testing.messaging.MockMessageEvent');
goog.require('goog.testing.messaging.MockMessagePort');
goog.require('goog.testing.mockmatchers');

var mocks_;  // Global MockControl instance
var stubs_;  // Global PropertyReplacer instance

var isFunction = goog.testing.mockmatchers.isFunction;
var isObject = goog.testing.mockmatchers.isObject;

function setUp() {
  mocks_ = new goog.testing.MockControl();
  stubs_ = new goog.testing.PropertyReplacer();
}

function tearDown() {
  mocks_.$tearDown();
  stubs_.reset();

  // Each test should recreate their mock port
  u2f.port_ = null;
  u2f.waitingForPort_ = [];

  var iframes = document.getElementsByTagName('iframe');
  // Sigh, live NodeLists... must walk backwards
  for (var i = iframes.length - 1; i >= 0; --i) {
    iframes[i].remove();
  }
}

var asyncTestCase = goog.testing.AsyncTestCase.createAndInstall();

function test_getMessagePort() {
  var chromePort = {}, iframePort = {};

  function setupMocks(useIframe, hasChromeRuntime) {
    stubs_.reset();
    mocks_.$resetAll();
    var mockGetPort = mocks_.createMethodMock(u2f,
        useIframe ? 'getIframePort_' : 'getChromeRuntimePort_');
    mockGetPort(isFunction).$does(function(cb) {
      setTimeout(function() {
        cb(useIframe ? iframePort : chromePort);
      }, 0);
    });
    if (hasChromeRuntime) {
      var mockSendMessage = mocks_.createFunctionMock();
      stubs_.setPath('chrome.runtime.sendMessage', mockSendMessage);
      mockSendMessage(u2f.EXTENSION_ID, isObject, isFunction).$does(
          function(extId, msg, callback) {
            callback();
          });
    }
    mocks_.$replayAll();
  }

  function step1_whitelisted() {
    setupMocks(false, true);
    stubs_.remove(chrome.runtime, 'lastError');
    u2f.getMessagePort(function(port) {
      assertEquals(chromePort, port);
      mocks_.$verifyAll();
      step2_runtimeAvailableButNotWhitelisted();
    });
    asyncTestCase.waitForAsync('step1');
  }

  function step2_runtimeAvailableButNotWhitelisted() {
    setupMocks(true, true);
    stubs_.set(chrome.runtime, 'lastError', 1);
    u2f.getMessagePort(function(port) {
      assertEquals(iframePort, port);
      mocks_.$verifyAll();
      step3_runtimeUnavailable();
    });
    asyncTestCase.waitForAsync('step2');
  }

  function step3_runtimeUnavailable() {
    setupMocks(true, false);
    if (typeof chrome !== 'undefined')
      stubs_.remove(goog.global, 'chrome');
    u2f.getMessagePort(function(port) {
      assertEquals(iframePort, port);
      mocks_.$verifyAll();
      asyncTestCase.continueTesting();
    });
    asyncTestCase.waitForAsync('step3');
  }

  step1_whitelisted();
}

function test_getChromeRuntimePort() {
  var thePort = {};
  var mockConnect = mocks_.createFunctionMock();
  stubs_.setPath('chrome.runtime.connect', mockConnect);
  mockConnect(u2f.EXTENSION_ID, isObject).$returns(thePort);
  mocks_.$replayAll();

  u2f.getChromeRuntimePort_(function(wrappedPort) {
    assertEquals(wrappedPort.port_, thePort);
    mocks_.$verifyAll();
    asyncTestCase.continueTesting();
  });
  asyncTestCase.waitForAsync();
}

function test_WrappedChromeRuntimePort() {
  var port = {
    postMessage: null,
    onMessage: {
      addListener: null
    }
  };
  var mockPostMessage = mocks_.createMethodMock(port, 'postMessage');
  var mockAddListener = mocks_.createMethodMock(port.onMessage, 'addListener');

  mockPostMessage('a message');
  mockAddListener(isFunction).$does(function(listener) {
    setTimeout(function() {
      listener('a reply');
    }, 0);
  });

  mocks_.$replayAll();

  var wrapped = new u2f.WrappedChromeRuntimePort_(port);
  wrapped.postMessage('a message');
  wrapped.addEventListener('message', function(event) {
    assertNotUndefined(event.data);
    assertEquals(event.data, 'a reply');
    mocks_.$verifyAll();
    asyncTestCase.continueTesting();
  });
  asyncTestCase.waitForAsync();
}

function test_getIframePort() {
  var mockPort = new goog.testing.messaging.MockMessagePort('local', mocks_);
  var channel = {
    port1: mockPort,
    port2: new ArrayBuffer(1)  // Need a transferable object here
  };
  var mockMCCtor = mocks_.createGlobalFunctionMock('MessageChannel');
  mockMCCtor().$returns(channel);

  mocks_.$replayAll();

  // Note: The iframe will be created in the test page, but the load event
  // will not fire since the extension isn't available.
  u2f.getIframePort_(function(port) {
    assertEquals(port, mockPort);
    assertTrue(mockPort.started);
    assertFalse(mockPort.hasListener('message'));

    var iframes = document.getElementsByTagName('iframe');
    assertEquals(iframes.length, 1);
    var iframe = iframes[0];

    assertEquals(iframe.src,
        'chrome-extension://' + u2f.EXTENSION_ID + '/u2f-comms.html');
    assertNotVisible(iframe);

    asyncTestCase.continueTesting();
  });
  asyncTestCase.waitForAsync();
  setTimeout(function() {
    var event = new goog.testing.messaging.MockMessageEvent('ready');
    mockPort.dispatchEvent(event);
  }, 0);
}

function test_getPortSingleton() {
  var mockGetMessagePort = mocks_.createMethodMock(u2f, 'getMessagePort');
  mockGetMessagePort(isFunction).$does(function(cb) {
    setTimeout(function() {
      cb(new goog.testing.messaging.MockMessagePort('u2fport', mocks_));
    });
  });
  mocks_.$replayAll();
  u2f.getPortSingleton_(function(port1) {
    u2f.getPortSingleton_(function(port2) {
      assertEquals(port1, port2);
      mocks_.$verifyAll();  // also checks getMessagePort was only called once
      asyncTestCase.continueTesting();
    });
  });
  asyncTestCase.waitForAsync();
}

function setupMockPort() {
  var port = new goog.testing.messaging.MockMessagePort('u2fport', mocks_);
  var mockGetMessagePort = mocks_.createMethodMock(u2f, 'getMessagePort');
  mockGetMessagePort(isFunction).$does(function(cb) {
    setTimeout(function() {
      cb(port);
    });
  });
  return port;
}

function setupMockRequestResponse(mockPort, reqType,
    signRequests, registerRequests, responseData) {
  mockPort.postMessage(isObject).$does(function(req) {
    switch (reqType) {
      case u2f.MessageTypes.U2F_REGISTER_REQUEST:
        assertObjectEquals(signRequests, req.signRequests);
        assertObjectEquals(registerRequests, req.registerRequests);
        break;
      case u2f.MessageTypes.U2F_SIGN_REQUEST:
        assertObjectEquals(signRequests, req.signRequests);
        assertUndefined(req.registerRequests);
        break;
      case u2f.MessageTypes.U2F_GET_API_VERSION_REQUEST:
        assertUndefined(req.signRequests);
        assertUndefined(req.registerRequests);
        break;
    }
    var response = {
        type: getRespTypeFor(reqType),
        responseData: responseData,
        requestId: req.requestId
    };
    setTimeout(function() {
      var event = new goog.testing.messaging.MockMessageEvent(response);
      mockPort.dispatchEvent(event);
    });
  });
}

function getRespTypeFor(reqType) {
  switch (reqType) {
    case u2f.MessageTypes.U2F_REGISTER_REQUEST:
      return u2f.MessageTypes.U2F_REGISTER_RESPONSE;
    case u2f.MessageTypes.U2F_SIGN_REQUEST:
      return u2f.MessageTypes.U2F_SIGN_RESPONSE;
    case u2f.MessageTypes.U2F_GET_API_VERSION_REQUEST:
      return u2f.MessageTypes.U2F_GET_API_VERSION_RESPONSE;
  }
}

function test_signOne() {
  var port = setupMockPort();
  setupMockRequestResponse(port, u2f.MessageTypes.U2F_SIGN_REQUEST, ['foo'],
    null /*registerRequests */, 'bar');
  mocks_.$replayAll();

  u2f.sign(['foo'], function(response) {
    assertEquals('bar', response);
    mocks_.$verifyAll();
    asyncTestCase.continueTesting();
  });
  asyncTestCase.waitForAsync();
}

function test_registerOne() {
  var port = setupMockPort();
  setupMockRequestResponse(port, u2f.MessageTypes.U2F_REGISTER_REQUEST, ['foo'],
    ['baz'], 'bar');
  mocks_.$replayAll();

  u2f.register(['baz'], ['foo'], function(response) {
    assertEquals('bar', response);
    mocks_.$verifyAll();
    asyncTestCase.continueTesting();
  });
  asyncTestCase.waitForAsync();
}

function test_getApiVersion() {
  var port = setupMockPort();
  setupMockRequestResponse(port, u2f.MessageTypes.U2F_GET_API_VERSION_REQUEST,
    null /*signRequests */, null /* registerRequests */,
    { 'js_api_version': 1.0});
  mocks_.$replayAll();

  u2f.getApiVersion(function(response) {
    assertEquals(1.0, response['js_api_version']);
    mocks_.$verifyAll();
    asyncTestCase.continueTesting();
  });
  asyncTestCase.waitForAsync();
}

function test_multipleRequests() {
  var port = setupMockPort();
  setupMockRequestResponse(port, u2f.MessageTypes.U2F_SIGN_REQUEST, ['sreq1'],
    null, 'sresp1');
  setupMockRequestResponse(port, u2f.MessageTypes.U2F_REGISTER_REQUEST,
    ['rsreq1'], ['rrreq1'], 'rresp1');
  setupMockRequestResponse(port, u2f.MessageTypes.U2F_SIGN_REQUEST, ['sreq2'],
    null, 'sresp2');
  mocks_.$replayAll();

  var n = 3;

  u2f.sign(['sreq1'], function(response) {
    assertEquals('sresp1', response);
    if (!--n) done();
  });

  u2f.register(['rrreq1'], ['rsreq1'], function(response) {
    assertEquals('rresp1', response);
    if (!--n) done();
  });

  u2f.sign(['sreq2'], function(response) {
    assertEquals('sresp2', response);
    if (!--n) done();
  });

  function done() {
    mocks_.$verifyAll();
    asyncTestCase.continueTesting();
  }
  asyncTestCase.waitForAsync();
}
