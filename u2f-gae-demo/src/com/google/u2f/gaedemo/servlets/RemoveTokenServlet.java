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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.u2f.U2FException;
import com.google.u2f.server.U2FServer;

@SuppressWarnings("serial")
@Singleton
public class RemoveTokenServlet extends HttpServlet {

    private final UserService userService =  UserServiceFactory.getUserService();
    private final U2FServer u2fServer;

    @Inject
    public RemoveTokenServlet(U2FServer u2fServer) {
      this.u2fServer = u2fServer;
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        User user = userService.getCurrentUser();
        String publicKey = req.getParameter("public_key");

        try {
          u2fServer.removeSecurityKey(user.getEmail(), Hex.decodeHex(publicKey.toCharArray()));
        } catch (U2FException e) {
          throw new ServletException("couldn't remove U2F token", e);
        } catch (DecoderException e) {
          throw new ServletException("invalid public key", e);
        }

        JsonObject result = new JsonObject();
        result.addProperty("status", "ok");

        resp.setContentType("application/json");
        resp.getWriter().println(result.toString());
    }
}
