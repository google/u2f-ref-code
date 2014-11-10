// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Inspects iframes in a page to find an iframe whose origin
 * matches one the extension is looking for.
 */
'use strict';

chrome.runtime.onMessage.addListener(
    function(request, sender, sendResponse) {
      if (!request.origin) {
        sendResponse(false);
        return;
      }
      var frames = document.getElementsByTagName('iframe');
      for (var i = 0; i < frames.length; i++) {
        var anchor = document.createElement('a');
        anchor.href = frames[i].src;
        if (request.origin == anchor.origin) {
          sendResponse(true);
          return;
        }
      }
      sendResponse(false);
    });
