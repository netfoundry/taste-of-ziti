# OpenZiti Browzer Access

Browzer is a way to access dark applications using nothing more than a web browser.
TODO: add more marketing speak about browzer here

Access to a dark application over Browzer utilizes an OpenZiti Identity configured with an
external [OIDC](https://auth0.com/docs/authenticate/protocols/openid-connect-protocol) compliant authentication provider.   For the Taste-of-Ziti demo, browzer applications
can be accessed using the provided Auth0 login.


## Requirements
* Google Chrome web browser, version 120 or later

## Usage

### Petstore Browzer Access

Visit the https://petstore.tasteofziti.browzer.cloudziti.io page using a Google Chrome browzer

Log into the Auth0 based OIDC provider using the Taste-of-Ziti demo credentials:
* Email address: `demouser@netfoundry.io`
* Password: `demo_Guest`

Once authenticated, the web browser is automatically redirected to the associated dark web application.

### Fireworks Browzer Access

Visit the https://fireworks.tasteofziti.browzer.cloudziti.io page using a Google Chrome browzer

Log into the Auth0 based OIDC provider using the Taste-of-Ziti demo credentials:
* Email address: `demouser@netfoundry.io`
* Password: `demo_Guest`

Once authenticated, the web browser is automatically redirected to the associated dark web application 
that displays some simple celebratory fireworks to show your success!

### ScadaLTS Browzer Access

The [ScadaLTS](https://github.com/SCADA-LTS/Scada-LTS/wiki) server is used by the ModbusServer examples.

Visit the https://scadalts.tasteofziti.browzer.cloudziti.io page using a Google Chrome browzer

Log into the Auth0 based OIDC provider using the Taste-of-Ziti demo credentials:
* Email address: `demouser@netfoundry.io`
* Password: `demo_Guest`

Once authenticated, the web browser is automatically redirected to the dark [ScadaLTS](https://github.com/SCADA-LTS/Scada-LTS/wiki) 
server.  This server requires additional login credentials.  For the Taste-of-Ziti demo environment use the following credentials:

* Username: `demoUser`
* Password: `demo_Guest`
