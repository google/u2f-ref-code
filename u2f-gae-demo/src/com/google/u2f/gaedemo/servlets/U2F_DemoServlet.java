// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo.servlets;
import java.io.IOException;

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
public class U2F_DemoServlet extends HttpServlet {
	
	private SoyTofu tofu;
	private UserService userService =  UserServiceFactory.getUserService();

	@Inject
	public U2F_DemoServlet(SoyTofu tofu) {
		this.tofu = tofu;
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		User user = userService.getCurrentUser();
		
		Renderer renderer = tofu.newRenderer(".main")
				.setData(new SoyMapData(
						"username",  user.getNickname(),
						"email", user.getEmail(),
						"logoutUrl", userService.createLogoutURL(req.getRequestURI())));
		resp.setContentType("text/html");
		resp.getWriter().println(renderer.render());
	}
}
