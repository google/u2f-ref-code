// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.tools.httpserver.servlets;

import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

import com.google.gson.JsonObject;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.U2fSignRequest;

public class SignDataServlet extends JavascriptServlet {

  private final U2FServer u2fServer;

  public SignDataServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  @Override
  public void generateJavascript(Request req, Response resp, PrintStream body) throws Exception {
    String userName = req.getParameter("userName");
    if (userName == null) {
      resp.setStatus(Status.BAD_REQUEST);
      return;
    }

    String appId = "http://localhost:8080";
    U2fSignRequest signRequest = u2fServer.getSignRequest(userName, appId);
    JsonObject result = new JsonObject();
    result.addProperty("challenge", signRequest.getChallenge());
    result.addProperty("appId", appId);
    result.add("registeredKeys", signRequest.getRegisteredKeysAsJson(appId));

    body.println("var signData = " + result.toString() + ";");
  }
}
