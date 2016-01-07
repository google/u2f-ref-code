# U2F Demo Deployment

To deploy https://noext-dot-u2fdemo.appspot.com and configure the appspot
server to the extension that is built into Chrome, you have to:
 (1) Set the version value to 'noext' in appengine-web.xml 
    <version>noext</version>
 (2) Set the extension id in u2f-api.js to 'pfboblefjcgdjicmnffhdgionmgcdmne'
     u2f.EXTENSION_ID = 'pfboblefjcgdjicmnffhdgionmgcdmne';
     
     
To deploy https://crxjs-dot-u2fdemo.appspot.com and configure the appspot
server to the U2F extension you have to:
 (1) Set the version value to 'crxjs' in appengine-web.xml 
    <version>crxjs</version>
 (2) Set the extension id in u2f-api.js to 'kmendfapggjehodndflmmgagdbamhnfd'
     u2f.EXTENSION_ID = 'kmendfapggjehodndflmmgagdbamhnfd';
     

