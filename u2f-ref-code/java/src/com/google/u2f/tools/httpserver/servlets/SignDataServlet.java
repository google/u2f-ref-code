package com.google.u2f.tools.httpserver.servlets;

import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

import com.google.gson.JsonObject;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.SignRequest;

public class SignDataServlet extends JavascriptServlet {

  private final U2FServer u2fServer;

  public SignDataServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  @Override
  public void generateJavascript(Request req, Response resp, PrintStream body) throws Exception {
    String userName = req.getParameter("userName");
    if (userName == null) {
      resp.setStatus(Status.BAD_REQUEST);
      return;
    }

    SignRequest signRequest = u2fServer.getSignRequest(userName);

    JsonObject signServerData = new JsonObject();
    signServerData.addProperty("appId", signRequest.getAppId());
    signServerData.addProperty("challenge", signRequest.getChallenge());
    signServerData.addProperty("version", signRequest.getVersion());
    signServerData.addProperty("sessionId", signRequest.getSessionId());
    signServerData.addProperty("keyHandle", signRequest.getKeyHandle());

    body.println("var signData = " + signServerData.toString() + ";");
  }
}
