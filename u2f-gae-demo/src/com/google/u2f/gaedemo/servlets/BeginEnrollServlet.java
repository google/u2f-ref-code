// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo.servlets;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.u2f.U2FException;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.SignRequest;

@SuppressWarnings("serial")
@Singleton
public class BeginEnrollServlet extends HttpServlet {
	
	private final UserService userService =  UserServiceFactory.getUserService();
	private final U2FServer u2fServer;
	
	@Inject
	public BeginEnrollServlet(U2FServer u2fServer) {
		this.u2fServer = u2fServer;
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		User user = userService.getCurrentUser();
		
		boolean singleEnrollment = !Boolean.valueOf(req.getParameter("reregistration"));
		
		RegistrationRequest registrationRequest;
		List<SignRequest> signRequests;
		try {
          registrationRequest = u2fServer.getRegistrationRequest(user.getUserId(),
              (req.isSecure() ? "https://" : "http://") + req.getHeader("Host"));

          if (singleEnrollment) {
            signRequests = u2fServer.getSignRequest(user.getUserId(),
                (req.isSecure() ? "https://" : "http://") + req.getHeader("Host"));
          } else {
            signRequests = ImmutableList.of();
          }
		} catch (U2FException e) {
		  throw new ServletException("couldn't get registration request", e);
		}
				
        JsonArray signData = new JsonArray();

        for (SignRequest signRequest : signRequests) {
          JsonObject signServerData = new JsonObject();
          signServerData.addProperty("appId", signRequest.getAppId());
          signServerData.addProperty("challenge", signRequest.getChallenge());
          signServerData.addProperty("version", signRequest.getVersion());
          signServerData.addProperty("keyHandle", signRequest.getKeyHandle());
          signData.add(signServerData);
        }
		
	    JsonObject enrollData = new JsonObject();
	    enrollData.addProperty("appId", registrationRequest.getAppId());
	    enrollData.addProperty("challenge", registrationRequest.getChallenge());
	    enrollData.addProperty("version", registrationRequest.getVersion());
		
	    JsonObject result = new JsonObject();
	    result.add("enroll_data", enrollData);
	    result.add("sign_data", signData);
	    result.addProperty("sessionId", registrationRequest.getSessionId());
	
		resp.setContentType("application/json");
		resp.getWriter().println(result.toString());
	}
}
