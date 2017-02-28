package com.google.u2f.gaedemo.endpoints;

public class Constants {
  // Scopes: https://developers.google.com/identity/protocols/googlescopes
  public static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";
  public static final String OPENID_SCOPE = "openid";
  
  // ClientIds:
  // https://cloud.google.com/appengine/docs/standard/java/endpoints/add-authorization-backend
  public static final String WEB_CLIENT_ID =
      "533916084229-qmk7s8pcv7u4uhj2coctv9h75rbp66d3.apps.googleusercontent.com";
  public static final String ANDROID_CLIENT_ID =
      "533916084229-a1v1p3l6vhtmo0h5931lc90oo10274da.apps.googleusercontent.com";
  public static final String ANDROID_AUDIENCE = WEB_CLIENT_ID;

  public static final String APP_ID = "https://u2fdemo.appspot.com/origins.json";
}
