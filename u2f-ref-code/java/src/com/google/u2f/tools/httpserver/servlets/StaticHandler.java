// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.tools.httpserver.servlets;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;

public class StaticHandler implements Container {

  private final String mimeType;
  private final String path;

  public StaticHandler(String mimeType, String path) {
    this.mimeType = mimeType;
    this.path = path;
  }

  @Override
  public void handle(Request req, Response resp) {
    try {
      InputStream inputStream = new BufferedInputStream(new FileInputStream(path));
      try {
        OutputStream outputStream = resp.getOutputStream();
        try {
          long time = System.currentTimeMillis();
          resp.setValue("Content-Type", mimeType);
          resp.setValue("Server", "HelloWorld/1.0 (Simple 4.0)");
          resp.setDate("Date", time);
          resp.setDate("Last-Modified", time);

          int len;
          byte[] buffer = new byte[65536];
          while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
          }
          outputStream.flush();
        } finally {
          outputStream.close();
        }
      } finally {
        inputStream.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
      resp.setStatus(Status.INTERNAL_SERVER_ERROR);
      return;
    }
  }
}
