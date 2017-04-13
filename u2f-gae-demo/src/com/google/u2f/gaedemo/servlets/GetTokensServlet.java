// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo.servlets;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.base.Objects;
import com.google.gson.JsonArray;
import com.google.inject.Singleton;
import com.google.u2f.gaedemo.storage.TokenStorageData;
import com.google.u2f.gaedemo.storage.UserTokens;

@SuppressWarnings("serial")
@Singleton
public class GetTokensServlet extends HttpServlet {
	
	private UserService userService =  UserServiceFactory.getUserService();
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		User user = userService.getCurrentUser();
		UserTokens tokens = Objects.firstNonNull(
				ofy().load().type(UserTokens.class).id(user.getEmail()).now(),
				new UserTokens(user.getEmail()));
				
		JsonArray resultList = new JsonArray();
		
		for (TokenStorageData token : tokens.getTokens()) {
			resultList.add(token.toJson());
		}
		resp.setContentType("application/json");				
		resp.getWriter().println(resultList.toString());
	}
}
