// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.tools.httpserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import com.google.common.collect.ImmutableSet;
import com.google.u2f.server.ChallengeGenerator;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.SessionIdGenerator;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.impl.BouncyCastleCrypto;
import com.google.u2f.server.impl.MemoryDataStore;
import com.google.u2f.server.impl.U2FServerReferenceImpl;
import com.google.u2f.tools.httpserver.servlets.EnrollDataServlet;
import com.google.u2f.tools.httpserver.servlets.EnrollFinishServlet;
import com.google.u2f.tools.httpserver.servlets.RequestDispatcher;
import com.google.u2f.tools.httpserver.servlets.SignDataServlet;
import com.google.u2f.tools.httpserver.servlets.SignFinishServlet;
import com.google.u2f.tools.httpserver.servlets.StaticHandler;

public class U2fHttpServer {
  private final static Logger Log = Logger.getLogger(U2fHttpServer.class.getSimpleName());

  private final Object lock = new Object();
  private final U2FServer u2fServer;

  private long sessionIdCounter = 0;

  public static void main(String[] args) throws InterruptedException {
    new U2fHttpServer();
  }

  public U2fHttpServer() {
    ChallengeGenerator challengeGenerator = new ChallengeGenerator() {
      @Override
      public byte[] generateChallenge(String accountName) {
        try {
          return Hex.decodeHex("1234".toCharArray());
        } catch (DecoderException e) {
          throw new RuntimeException(e);
        }
      }
    };

    SessionIdGenerator sessionIdGenerator = new SessionIdGenerator() {
      @Override
      public String generateSessionId(String accountName) {
        return new StringBuilder()
          .append("sessionId_")
          .append(sessionIdCounter++)
          .append("_")
          .append(accountName)
          .toString();
      }
    };

    X509Certificate trustedCertificate;
    try {
      trustedCertificate = (X509Certificate) CertificateFactory.getInstance("X.509")
          .generateCertificate(new ByteArrayInputStream(Hex.decodeHex((
              "308201433081eaa0030201020209012333009941964658300a06082a8648ce3d"
                  + "040302301b3119301706035504031310476e756262792048534d2043412030"
                  + "303022180f32303132303630313030303030305a180f323036323035333132"
                  + "33353935395a30303119301706035504031310476f6f676c6520476e756262"
                  + "7920763031133011060355042d030a00012333009941964658305930130607"
                  + "2a8648ce3d020106082a8648ce3d03010703420004aabc1b97a7c391f8b1fe"
                  + "5280a65cf27890409bdc392e181ff00ccf39599461d583f3351b21602cf99e"
                  + "2fe71e7f838658b42df49f06b8446d375d2aaaa8e317a1300a06082a8648ce"
                  + "3d0403020348003045022037788207c2239373b289169cfd3500b54fe92903"
                  + "e6772ea995cd2ce4a670fba5022100dfbfe7da528600be0d6125060d029f40"
                  + "c647bc053e35226fffb66cd7f4609b49").toCharArray())));
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    } catch (DecoderException e) {
      throw new RuntimeException(e);
    }
    DataStore dataStore = new MemoryDataStore(sessionIdGenerator);
    dataStore.addTrustedCertificate(trustedCertificate);

    // this implementation will only accept signatures from http://localhost:8080
    u2fServer = new U2FServerReferenceImpl(challengeGenerator, dataStore,
        new BouncyCastleCrypto(), ImmutableSet.of("http://localhost:8080"));
    Container dispatchContainer = new RequestDispatcher()
        .registerContainer("/", new StaticHandler("text/html","html/index.html"))
        .registerContainer("/enroll", new StaticHandler("text/html","html/enroll.html"))
        .registerContainer("/enrollData.js", new EnrollDataServlet(u2fServer))
        .registerContainer("/enrollFinish", new EnrollFinishServlet(u2fServer))
        .registerContainer("/sign", new StaticHandler("text/html","html/sign.html"))
        .registerContainer("/signData.js", new SignDataServlet(u2fServer))
        .registerContainer("/signFinish", new SignFinishServlet(u2fServer));

    try {
      Connection connection = new SocketConnection(new ContainerServer(dispatchContainer));

      try {
        connection.connect(new InetSocketAddress("0.0.0.0", 8080));

        synchronized (lock) {
          lock.wait();
        }
      } finally {
        connection.close();
      }
    } catch (IOException e) {
      Log.severe("Error with HTTP server: " + e);
      return;
    } catch (InterruptedException e) {
      Log.info("Interrupted");
      return;
    }
  }
}
