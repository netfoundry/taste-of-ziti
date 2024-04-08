# Demo Services OpenZiti and Docker Container Setup

The services hosted by this docker setup fall into the category of non-ziti server applications that are accessed
through a OpenZiti tunneler using an OpenZiti connected client application. 

# Pre-Setup

## Create Basics - Controller and Router
1. Create an OpenZiti controller.  Use any of the [OpenZiti Quickstarts](https://openziti.io/docs/learn/quickstarts/network/hosted) or a CloudZiti Network.
1. Create an edge router or update an existing edge router so that it has the roleAttributes of `DemoServiceRouters` and `DemoClientRouters`

# Demo Services

## Petstore Service
The `PetstoreDemo` service is hosted by a tunneller that has the role `#DemoTunnellers` and can be accessed via the `petstore.ziti` address.
The host.v1 address of `petstore.demo` matches the container name in the demo docker configuration of the `pet_store` service.

    ziti edge create config PetstoreDemo.hostv1 host.v1 '{"protocol":"tcp", "address":"petstore.demo", "port":8080}'
    ziti edge create config PetstoreDemo.clientv1 intercept.v1 '{"protocols":["tcp"], "addresses":["petstore.ziti"],"portRanges":[{"low":80,"high":80}]}'
    ziti edge create service PetstoreDemo -a 'DemoTunnelledServices' --configs "PetstoreDemo.hostv1,PetstoreDemo.clientv1"
    ziti edge create service-edge-router-policy PetstoreDemo.serp --edge-router-roles '#all' --service-roles '@PetstoreDemo'
    ziti edge create service-policy PetstoreDemo.bind Bind --service-roles "@PetstoreDemo" --identity-roles "#DemoTunnellers"

## Postgres DB Service
The `PostgresDemo` service is hosted by a tunneller that has the role `#DemoTunnellers` and can be accessed via the `postgres.ziti` address.
The host.v1 address of `postgres.demo` matches the container name in the demo docker configuration of the `postgres_db` service.

    ziti edge create config PostgresDemo.hostv1 host.v1 '{"protocol":"tcp", "address":"postgres.demo", "port":5432}'
    ziti edge create config PostgresDemo.clientv1 intercept.v1 '{"protocols":["tcp"], "addresses":["postgres.ziti"],"portRanges":[{"low":5432,"high":5432}]}'
    ziti edge create service PostgresDemo -a 'DemoTunnelledServices' --configs "PostgresDemo.hostv1,PostgresDemo.clientv1"
    ziti edge create service-edge-router-policy PostgresDemo.serp --edge-router-roles '#all' --service-roles '@PostgresDemo'
    ziti edge create service-policy PostgresDemo.bind Bind --service-roles "@PostgresDemo" --identity-roles "#DemoTunnellers"

## Policies for Demo Services
    ziti edge create service-policy 'Demo Services' Dial --service-roles "#DemoTunnelledServices" --identity-roles "#DemoClients"

## Router Policies
    ziti edge create edge-router-policy "Demo Client Edge Router Policy" --semantic AnyOf --edge-router-roles '#DemoClientRouters' --identity-roles '#DemoClients'
    ziti edge create edge-router-policy "Demo Service Edge Router Policy" --semantic AnyOf --edge-router-roles '#DemoServiceRouters' --identity-roles '#DemoTunnellers'

## Service Edge Router Policies
    ziti edge create service-edge-router-policy "Demo Service Policy" --semantic AllOf --edge-router-roles '#DemoServiceRouters' --service-roles '#DemoTunnelledServices'

## Tunneller Identity
    ziti edge create identity device demoTunneller -a "DemoTunnellers" -o demoTunneller.jwt
    ziti edge enroll -j demoTunneller.jwt
Copy the contents of the generated `demoTunneller.json` to the demo docker `.env` file's `ZITI_IDENTITY_JSON` variable

## Start the demo docker network
Running from the demo docker directory:

    docker compose up -d

Once started, the Taste-of-Ziti petstore and dbclient examples can be used to access these containers.  Note, since these clients are not
using the Taste-of-Ziti hosted controller, a client identity must be generated manually.  Execute the following to generate an identity for the 
client applications:

    ziti edge create identity device clientIdentity -a "DemoClients" -o taste_of_ziti.jwt
    ziti edge enroll -j taste_of_ziti.jwt

This generates a `taste_of_ziti.json` file and can be used like `go run . -i taste_of_ziti.json` (from the golang/petstoreClient directory).
