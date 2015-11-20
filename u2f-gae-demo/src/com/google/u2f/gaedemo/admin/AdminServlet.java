// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo.admin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.google.template.soy.tofu.SoyTofu.Renderer;

@SuppressWarnings("serial")
@Singleton
public class AdminServlet extends HttpServlet {
  private SoyTofu tofu;
  private UserService userService =  UserServiceFactory.getUserService();

  @Inject
  public AdminServlet(SoyTofu tofu) {
    this.tofu = tofu;
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    if (!userService.isUserAdmin()) {
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must be admin");
      return;
    }

    User user = userService.getCurrentUser();

    Renderer renderer = tofu.newRenderer(".admin")
        .setData(new SoyMapData(
            "username",  user.getNickname(),
            "email", user.getEmail(),
            "logoutUrl", userService.createLogoutURL(req.getRequestURI())));
    resp.setContentType("text/html");
    resp.getWriter().println(renderer.render());
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String command = req.getParameter("command");
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "unknown command: " + command);
  }
}
