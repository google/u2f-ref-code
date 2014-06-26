// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.tools.httpserver.servlets;

import java.io.PrintStream;
import java.util.List;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.SignRequest;

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

    List<SignRequest> signRequests = u2fServer.getSignRequest(userName, "http://localhost:8080");
    JsonArray result = new JsonArray();
    
    for (SignRequest signRequest : signRequests) {
      JsonObject signServerData = new JsonObject();
      signServerData.addProperty("appId", signRequest.getAppId());
      signServerData.addProperty("challenge", signRequest.getChallenge());
      signServerData.addProperty("version", signRequest.getVersion());
      signServerData.addProperty("sessionId", signRequest.getSessionId());
      signServerData.addProperty("keyHandle", signRequest.getKeyHandle());
      result.add(signServerData);
    }

    body.println("var signData = " + result.toString() + ";");
  }
}
