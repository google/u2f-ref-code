package com.google.u2f.gaedemo.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.u2f.U2FException;
import com.google.u2f.gaedemo.storage.TokenStorageData;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.messages.RegistrationResponse;

@SuppressWarnings("serial")
@Singleton
public class FinishEnrollServlet extends HttpServlet {

  private final U2FServer u2fServer;

  @Inject
  public FinishEnrollServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  public void doPost(HttpServletRequest req, HttpServletResponse resp) 
      throws IOException, ServletException {

    RegistrationResponse registrationResponse = new RegistrationResponse(
        req.getParameter("enrollData"), 
        req.getParameter("browserData"),
        req.getParameter("sessionId"));

    SecurityKeyData newSecurityKeyData;
    try {
      newSecurityKeyData = u2fServer.processRegistrationResponse(
          registrationResponse, 
          System.currentTimeMillis());
    } catch (U2FException e) {
      throw new ServletException(e);
    }

    resp.setContentType("application/json");
    resp.getWriter().println(new TokenStorageData(newSecurityKeyData).toJson().toString());
  }
}
