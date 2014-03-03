package com.google.u2f.tools.httpserver.servlets;

import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;

public abstract class HtmlServlet implements Container {

	@Override
  public void handle(Request req, Response resp) {
		try {
	    PrintStream body = resp.getPrintStream();
	    try {
		    long time = System.currentTimeMillis();
		    
		    resp.setValue("Content-Type", "text/html");
		    resp.setValue("Server", "HelloWorld/1.0 (Simple 4.0)");
		    resp.setDate("Date", time);
		    resp.setDate("Last-Modified", time);
		    
			  body.println("<html><body>");
			  generateBody(req, resp, body);
				body.println("</body></html>");
	    } finally {
	    	body.close();
	    }
		} catch (Exception e) {
			e.printStackTrace();
			resp.setStatus(Status.INTERNAL_SERVER_ERROR);
			return;
		}
  }
	
	public abstract void generateBody(Request req, Response resp, PrintStream body) throws Exception;
}
