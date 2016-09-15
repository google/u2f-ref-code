# U2F Demo Deployment

There are two deployments possible:

1. **NoExt** -- The appspot server will call into the expension already bundled into Chrome.  This deploys to https://noext-dot-u2fdemo.appspot.com (which is the same as https://u2fdemo.appspot.com).
2. **CrxJs** -- The appspot server will call into the unbundled extension that you have.  This deploys to  https://crxjs-dot-u2fdemo.appspot.com
 
## NoExt
In order to configure the appspot server to the extension that is built into Chrome, set the extension id in [u2f-api.js](https://github.com/google/u2f-ref-code/blob/master/u2f-gae-demo/war/js/u2f-api.js) to ```kmendfapggjehodndflmmgagdbamhnfd```:
```
  u2f.EXTENSION_ID = 'kmendfapggjehodndflmmgagdbamhnfd';
```

In order to deploy to https://noext-dot-u2fdemo.appspot.com, set the version value to ```noext``` in [appengine-web.xml](https://github.com/google/u2f-ref-code/blob/master/u2f-gae-demo/war/WEB-INF/appengine-web.xml):
```
  <version>noext</version>
```

## CrxJs
In order to configure the appspot server to call the U2F extension you have, set the extension id in [u2f-api.js](https://github.com/google/u2f-ref-code/blob/master/u2f-gae-demo/war/js/u2f-api.js) to ```pfboblefjcgdjicmnffhdgionmgcdmne```:
```
  u2f.EXTENSION_ID = 'pfboblefjcgdjicmnffhdgionmgcdmne';
```
 
In order to deploy to https://crxjs-dot-u2fdemo.appspot.com, set the version value to ```crxjs``` in [appengine-web.xml](https://github.com/google/u2f-ref-code/blob/master/u2f-gae-demo/war/WEB-INF/appengine-web.xml):
```
  <version>crxjs</version>
```
