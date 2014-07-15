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

