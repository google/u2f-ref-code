// Copyright 2014 Google Inc. All rights reserved
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

/**
 * @fileoverview Javascript to direct user actions to the extension.
 */
'use strict';
var originAndTab = window.location.search.substring(1).split('&');
var origin = originAndTab[0];
var tabId = parseInt(originAndTab[1]);

function deny() {
  chrome.runtime.sendMessage({
    'type': 'originDenied',
    'tab': tabId
  });
  window.close();
}

function approve() {
  chrome.runtime.sendMessage({
    'type': 'originApproved',
    'tab': tabId
  });
  window.close();
}

window.onload = function() {
  if (origin) {
    var domOrigin = document.querySelector('#origin');
    domOrigin.innerText = origin;
    document.getElementById('deny').addEventListener('click', deny);
    document.getElementById('approve').addEventListener('click', approve);
  }
}
