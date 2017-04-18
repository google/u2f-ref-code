// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.u2f.U2FException;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.U2fSignRequest;

@SuppressWarnings("serial")
@Singleton
public class BeginSignServlet extends HttpServlet {

  private final UserService userService =  UserServiceFactory.getUserService();
  private final U2FServer u2fServer;

  @Inject
  public BeginSignServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    User user = userService.getCurrentUser();

    U2fSignRequest signRequest;
    String appId =
        (req.isSecure() ? "https://" : "http://") + req.getHeader("Host") + "/origins.json";
    try {
      signRequest = u2fServer.getSignRequest(user.getEmail(), appId);
    } catch (U2FException e) {
      throw new ServletException("couldn't get sign request", e);
    }

    JsonObject result = new JsonObject();
    result.addProperty("challenge", signRequest.getChallenge());
    result.addProperty("appId", appId);
    result.add("registeredKeys", signRequest.getRegisteredKeysAsJson(appId));

    resp.setContentType("application/json");
    resp.getWriter().println(result.toString());
  }
}
