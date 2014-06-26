// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f;

@SuppressWarnings("serial")
public class U2FException extends Exception {

  public U2FException(String message) {
    super(message);
  }

  public U2FException(String message, Throwable cause) {
    super(message, cause);
  }
}
