package com.google.u2f.tools.httpserver.servlets;

import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

import com.google.gson.JsonObject;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.RegistrationRequest;

public class EnrollDataServlet extends JavascriptServlet {

  private final U2FServer u2fServer;

  public EnrollDataServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  @Override
  public void generateJavascript(Request req, Response resp, PrintStream body) throws Exception {
    String userName = req.getParameter("userName");
    if (userName == null) {
      resp.setStatus(Status.BAD_REQUEST);
      return;
    }
    RegistrationRequest registrationRequest = u2fServer.getRegistrationRequest(userName, "http://localhost:8080");

    JsonObject enrollServerData = new JsonObject();
    enrollServerData.addProperty("appId", registrationRequest.getAppId());
    enrollServerData.addProperty("challenge", registrationRequest.getChallenge());
    enrollServerData.addProperty("version", registrationRequest.getVersion());
    enrollServerData.addProperty("sessionId", registrationRequest.getSessionId());

    body.println("var enrollData = " + enrollServerData.toString() + ";");
  }
}
