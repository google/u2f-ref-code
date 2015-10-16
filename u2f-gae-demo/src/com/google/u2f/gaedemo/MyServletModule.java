// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo;

import com.google.inject.servlet.ServletModule;
import com.google.u2f.gaedemo.admin.AdminServlet;
import com.google.u2f.gaedemo.servlets.BeginEnrollServlet;
import com.google.u2f.gaedemo.servlets.BeginSignServlet;
import com.google.u2f.gaedemo.servlets.FinishEnrollServlet;
import com.google.u2f.gaedemo.servlets.FinishSignServlet;
import com.google.u2f.gaedemo.servlets.GetTokensServlet;
import com.google.u2f.gaedemo.servlets.RemoveTokenServlet;
import com.google.u2f.gaedemo.servlets.U2F_DemoServlet;
import com.googlecode.objectify.ObjectifyFilter;

public class MyServletModule extends ServletModule {

  @Override
  protected void configureServlets() {
    serve("/").with(U2F_DemoServlet.class);
    serve("/BeginEnroll").with(BeginEnrollServlet.class);
    serve("/FinishEnroll").with(FinishEnrollServlet.class);

    serve("/BeginSign").with(BeginSignServlet.class);
    serve("/FinishSign").with(FinishSignServlet.class);

    serve("/GetTokens").with(GetTokensServlet.class);
    serve("/RemoveToken").with(RemoveTokenServlet.class);

    serve("/admin").with(AdminServlet.class);

    filter("/*").through(ObjectifyFilter.class);
  }
}
