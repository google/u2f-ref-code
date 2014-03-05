package com.google.u2f.tools.httpserver.servlets;

import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import com.google.u2f.U2FException;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.SignResponse;

public class SignFinishServlet extends HtmlServlet {

  private final U2FServer u2fServer;

  public SignFinishServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  @Override
  public void generateBody(Request req, Response resp, PrintStream body) {
    SignResponse signResponse = new SignResponse(
        req.getParameter("browserData"),
        req.getParameter("signData"),
        req.getParameter("challenge"),
        req.getParameter("sessionId"),
        req.getParameter("appId"));
    try {
      u2fServer.processSignResponse(signResponse);
      body.println("Success!!!");
    } catch (U2FException e) {
      body.println("Failure: " + e.toString());
    }
  }
}
