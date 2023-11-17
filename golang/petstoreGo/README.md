# OpenZiti PetstoreGo

This example is a simple golang client that connects to a Petstore server using OpenZiti. 

## Setup :wrench:
This example uses the Taste-of-Ziti demo network and its aperitivo service to obtain a temporary identity. Refer to
the [Taste-of-Ziti README](../../README.md) for additional details on the Taste-of-Ziti demo network.

## Requirements
* go 1.19 or later
* gcc compiler

## Build
Execute the following to build and place the executable in a directory labeled `build`

1. Run the following to create the build directory and build the project

       mkdir build
       go mod tidy
       go build -o build ./...

## Usage

By default, the petstoreGo client connects to a hosted OpenZiti network and receives a temporary identity.  The 
identity is saved and reused for subsequent calls.  An alternate identity file can be used by calling the client with
the `-i <identityFile>` parameter or from the `ZITI_IDENTITIES` environment variable.

### Example Output
```shell
$ ./petstoreGo -h
  -a string
    	optional aperitivo url for acquiring an identity (default "https://aperitivo.netfoundry.io")
  -h	Display usage
  -i string
    	optional identity file
  -q string
    	petstore query to execute (default "/")

$ ./petstoreGo -q /api/v3/pet/findByStatus?status=sold
INFO    Connecting to aperitivo at https://aperitivo.netfoundry.io to generate a new temporary identity 
INFO    generating 4096 bit RSA key                  
INFO    A new identity is being enrolled and stored in taste_of_ziti.json. This is a temporary identity that is valid until Fri, 17 Nov 2023 18:48:11 UTC 
INFO    Identity write completed                     
INFO    Loading identity from taste_of_ziti.json     
INFO    This identity provides access to the service: FireworksDemo 
INFO    This identity provides access to the service: WhatIsMyIpDemo 
INFO    This identity provides access to the service: PetstoreDemo 
INFO    Dialing PetstoreDemo with query string from command line: /api/v3/pet/findByStatus?status=sold 
INFO    Received: [{"id":5,"category":{"id":1,"name":"Dogs"},"name":"Dog 2","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag2"},{"id":2,"name":"tag3"}],"status":"sold"}] 
```
