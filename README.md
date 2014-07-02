# Reference code for U2F specifications

This code implements the FIDO U2F specifications being developed at
http://fidoalliance.org/.  This code is intended as a reference and resource
for developers who are interested in exploring U2F.  The code consists of the
following components:

## Java U2F implementation

This code can verify U2F registrations and signatures. A web application built
to accept U2F 2nd factor is built on top of a code base such as this. The code
base includes a trivial web application so the user can experiment with
registration and signatures (also see the sample web app below).

## A virtual (software) U2F device

This is a Java implementation of a U2F device. It generates registration and
signature statements and is meant for testing against your server
implementation. A physical U2F device will generate similar statements. 

## A sample web app that uses U2F
This is a sample application built on the Google Appengine web platform which
demonstrates a possible UX for  user interaction with U2F in a web page.  The
sample application is deployed and available live at
https://u2fdemo.appspot.com/. The underlying U2F capability is provided by the
Java U2F implementation.  A developer can take the core ideas from here and
integrate U2F into a web application on their own favorite web app platform.

## A U2F extension for the Chrome browser
This extension brings U2F capability to the Chrome browser. A web application
is able to access USB U2F devices using the U2F API provided by this extension.
The extension is [available from the Chrome
store](https://chrome.google.com/webstore/detail/fido-u2f-universal-2nd-fa/pfboblefjcgdjicmnffhdgionmgcdmne)
for direct use. The source will be provided here shortly for the interested
developer.

* * *

To experience the end-to-end user experience you will need to get a physical
USB device since the virtual device *does not* simulate the USB layer at this
time. Please write to <asfas@asdads> to get pointers on how to acquire devices
for testing.

## Getting started

u2f-ref-code is a self contained java project that includes a basic web server
and includes packages for all crypto, utilities, etc.  It does *not* need to run
in a container or application server like Tomcat.  To run the demo server, run
the main class in ``com.google.u2f.tools.httpserver.U2fHttpServer``

To compile and run the server in Eclipse, import the project into your
workspace. You may need to fix the classpath if your version of JDK is
different (this has been tested with Java 1.7).  The simple demo web server is
in ``com.google.u2f.tools.httpserver.UtfHttpServer.java`` and runs on port
8080. Run this class as a regular Java application (right click, select *Run
As* and *Java Application*). Note that you need to have the U2F extension
installed in Chrome in order for the demo app to talk to your U2F token.
