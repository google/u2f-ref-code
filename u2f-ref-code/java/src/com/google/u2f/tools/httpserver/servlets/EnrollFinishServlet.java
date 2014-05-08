package com.google.u2f.tools.httpserver.servlets;

import java.io.PrintStream;
import java.security.cert.X509Certificate;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import com.google.u2f.U2FException;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.RegistrationResponse;

public class EnrollFinishServlet extends HtmlServlet {

  private final U2FServer u2fServer;

  public EnrollFinishServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  @Override
  public void generateBody(Request req, Response resp, PrintStream body) {
    RegistrationResponse registrationResponse = new RegistrationResponse(
        req.getParameter("enrollData"), req.getParameter("browserData"),
        req.getParameter("sessionId"));

    try {
      X509Certificate cert = u2fServer.processRegistrationResponse(registrationResponse);
      body.println("Success!!!\n\nAttestation cert is:\n" + cert.toString());
    } catch (U2FException e) {
      body.println("Failure: " + e.toString());
    }
  }
}
