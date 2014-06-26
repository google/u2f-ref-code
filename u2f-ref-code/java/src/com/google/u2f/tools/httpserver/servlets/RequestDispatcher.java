// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.tools.httpserver.servlets;

import java.io.IOException;
import java.util.HashMap;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;

public class RequestDispatcher implements Container {

  private final HashMap<String, Container> servletMap = new HashMap<String, Container>();

  public RequestDispatcher registerContainer(String path, Container container) {
    servletMap.put(path, container);
    return this;
  }

  @Override
  public void handle(Request req, Response resp) {
    Container container = servletMap.get(req.getPath().toString());

    if (container == null) {
      resp.setStatus(Status.NOT_FOUND);
      try {
        resp.close();
      } catch (IOException ignored) {}
      return;
    }

    container.handle(req, resp);
  }
}
