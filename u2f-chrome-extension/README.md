# U2F Chrome extension

This is an implementation of a U2F client for Chrome, intended for
experimentation and testing. For general testing, we recommend installing it
from the Chrome Web Store [here][webstore]. In this case, the extension will
automatically update when new versions are released. Any origin may call the
extension; side-loading is not necessary for this.

[webstore]: https://chrome.google.com/webstore/detail/fido-u2f-universal-2nd-fa/pfboblefjcgdjicmnffhdgionmgcdmne

To experiment with modifications to the extension, this folder can be loaded
directly, but the Web Store instance must be disabled or uninstalled. It is
important that the extension id remains the same, as it is whitelisted by
Chrome to allow USB access, which is normally reserved to packaged apps. Thus,
do not modify/remove the key in ``manifest.json``.

## Communicating with the extension

The simplest way for web pages to talk to the extension is to load the script
``chrome-extension://pfboblefjcgdjicmnffhdgionmgcdmne/u2f-api.js``. This installs
a namespace ``u2f`` as described in U2F JavaScript API draft sent to the mailing
list. If you are willing to load this script in your pages, you can safely skip
the remainder of this section.

The U2F JavaScript API draft also describes how websites can talk directly to
the extension via a [MessagePort][messageport], in cases where they do not
whish to load a script from the extension. How a port to this extension is
obtained depends on whether the source origin is whitelisted as *externally
connectable* in ``manifest.json``.

[messageport]: http://www.w3.org/TR/webmessaging/#message-ports

### Non-whitelisted origins

For non-whitelisted origins, messages pass through an iframe trampoline, which
must be loaded manually from the website, with the source
``chrome-extension://pfboblefjcgdjicmnffhdgionmgcdmne/u2f-comms.html``. Since
this iframe runs under a different origin, its scripts will not have access to
the context of the containing web page. However, the web page can message it by
creating a MessageChannel to obtain two entangled MessagePorts, and delivering
one of them to the iframe via a postMessage with the body ``"init"``. 

```javascript
function getIframePort(callback) {
  // Create the iframe
  var iframeOrigin = 'chrome-extension://pfboblefjcgdjicmnffhdgionmgcdmne';
  var iframe = document.createElement('iframe');
  iframe.src = iframeOrigin + '/u2f-comms.html';
  iframe.setAttribute('style', 'display:none');
  document.body.appendChild(iframe);

  // Prepare a channel
  var channel = new MessageChannel();
  var ready = function(message) {
    // When the iframe is ready to receive U2F messages,
    // it will send the string 'ready'
    if (message.data == 'ready') {
      channel.port1.removeEventListener('message', ready);
      callback(channel.port1);
    } else {
      console.error('First event on iframe port was not "ready"');
    }
  };
  channel.port1.addEventListener('message', ready);
  channel.port1.start();

  iframe.addEventListener('load', function() {
    // Deliver the port to the iframe and initialize
    iframe.contentWindow.postMessage('init', iframeOrigin, [channel.port2]);
  });
};
```

For a full example refer to ``u2f-api.js``.

The drawback of this transport is that the websites [TLS channel id][channelid]
will not be available to the extension, and thus not included in signed U2F
assertions.

[channelid]: https://tools.ietf.org/html/draft-balfanz-tls-channelid-01

### Whitelisted origins

Developers that want to test with channel ids must add their domains to the
``externally-connectable`` whitelist in the extension manifest. In this case,
obtaining a message port to the extension is simpler:

```javascript
 var port = chrome.runtime.connect(
     'pfboblefjcgdjicmnffhdgionmgcdmne',
     {'includeTlsChannelId': true});
```

The returned port will be a Chrome runtime port object, which has slightly
different syntax for how event handlers are added. Again, see ``u2f-api.js``
for a full example and how to wrap this in a HTML5 MessagePort compatible
interface.

### Extending the extension with an external helper

The extension splits the handling of messages into a "top half" and a
"bottom half". The responsibilities of the top half include validating
the request, checking that the origin and appId match, and building the
clientData used in registration and sign requests. The bottom half is
responsible for finding security keys and performing the low-level
register and sign requests on them. The bottom half included in the
extension supports USB security keys.

The extension includes support for deferring to an another extension
as another bottom half helper. This can be useful for prototyping other
token form factors or transports, e.g. Bluetooth.

To register an extension with the U2F extension as a bottom half helper,
the U2F extension needs to have the extension's id added to its whitelist.
E.g. if the helper's extension id is 'mycoolnewhelper', modify the
U2F extension's externally_connectable section in the manifest to include
the helper's extension id, like so:

```javascript
"externally_connectable": {
  "ids": [
    "mycoolnewhelper",
  ],
  "matches": [
  ...
```

Also modify the whitelist in u2fbackground.js like so:

```javascript
HELPER_WHITELIST.addAllowedExtension('mycoolnewhelper');
```

(Doing so will require that you side-load your modified copy of the U2F
extension.)

In your helper extension, at startup, notify the U2F extension of its
presence by sending a message to the U2F extension with helper's extension
id as the body of the message, like so:

chrome.runtime.sendMessage('pfboblefjcgdjicmnffhdgionmgcdmne',
    chrome.runtime.id);

At this point, whenever the U2F extension receives a register or sign
request, it'll send helper messages to your helper.

A bottom half helper is expected to handle messages of the following format:

```javascript
var enroll_helper_request = {
  "type": "enroll_helper_request",
  "enrollChallenges": [
    {
      "appIdHash": URI
      "challengeHash": websafe-b64
      "version": undefined || "U2F_V1" || "U2F_V2",
    }+
  ],
  "signData": [
      // see sign_helper_request.signData
  ],
  "timeoutSeconds": float
};

var sign_helper_request = {
  "type": "sign_helper_request",
  "timeoutSeconds": float
  "signData": [
    {
      "version": undefined || "U2F_V1" || "U2F_V2",
      "appIdHash": websafe-b64
      "challengeHash": websafe-b64
      "keyHandle": websafe-b64
    }+
  ],
};
```

It is expected to send in reply:

```javascript
var enroll_helper_reply = {
  "type": "enroll_helper_reply",
  "code": result,  // from DeviceStatusCodes
  "version": undefined || "U2F_V1" || "U2F_V2",
  "enrollData": websafe-b64
};

var sign_helper_reply = {
  "type": "sign_helper_reply",
  "code": result,  // from DeviceStatusCodes
  "errorDetail": undefined || string,
  "responseData": undefined || {
      "version": undefined || "U2F_V1" || "U2F_V2",
      "appIdHash": websafe-b64
      "challengeHash": websafe-b64
      "keyHandle": websafe-b64
      "signatureData": websafe-b64
  }
};
```
