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

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.u2f.U2FException;
import com.google.u2f.gaedemo.storage.TokenStorageData;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SignSessionData;
import com.google.u2f.server.messages.SignResponse;

@SuppressWarnings("serial")
@Singleton
public class FinishSignServlet extends HttpServlet {

  private final UserService userService =  UserServiceFactory.getUserService();
  private final U2FServer u2fServer;
  private final DataStore dataStore;

  @Inject
  public FinishSignServlet(U2FServer u2fServer, DataStore dataStore) {
    this.u2fServer = u2fServer;
    this.dataStore = dataStore;
  }

  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {

    SignSessionData sessionData = dataStore
        .getSignSessionData(req.getParameter("sessionId"));

    // Simple XSRF protection. We don't want users to be tricked into
    // submitting other people's enrollment data. Here we're just checking
    // that it's the same user that also started the enrollment - you might
    // want to do something more sophisticated.
    String currentUser = userService.getCurrentUser().getEmail();
    String expectedUser = sessionData.getAccountName();
    if (!currentUser.equals(expectedUser)) {
      throw new ServletException("Cross-site request prohibited");
    }


    SignResponse signResponse = new SignResponse(
        req.getParameter("keyHandle"),
        req.getParameter("signatureData"),
        req.getParameter("clientData"),
        req.getParameter("sessionId")
        );

    SecurityKeyData securityKeyData;
    try {
      securityKeyData = u2fServer.processSignResponse(signResponse);
    } catch (U2FException e) {
      throw new ServletException("signature didn't verify", e);
    }

    resp.setContentType("application/json");
    resp.getWriter().println(new TokenStorageData(securityKeyData).toJson().toString());
  }
}
