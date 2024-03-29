# OpenZiti PetstoreClient

This example is a simple golang client that connects to a Petstore server using OpenZiti. 

## Setup :wrench:
This example uses the Taste-of-Ziti demo network and its Aperitivo service to obtain a temporary identity. Refer to
the [Taste-of-Ziti README](../../README.md) for additional details on the Taste-of-Ziti demo network.

## Requirements
* go 1.21.x or later
* gcc compiler

## Usage

By default, the petstoreClient connects to a hosted OpenZiti network and receives a temporary identity.  The 
identity is saved and reused for subsequent calls.  An alternate identity file can be used by calling the client with
the `-i <identityFile>` parameter.

### Example Output
```shell
$ go run . -q /api/v3/pet/findByStatus?status=sold
```

Results:
```
2024/01/17 13:53:15 Connecting to Aperitivo at https://aperitivo.production.netfoundry.io to generate a new temporary identity 
INFO[0002] generating 4096 bit RSA key                  
2024/01/17 13:53:20 A new identity is being enrolled and stored in /home/myDemoUser/taste-of-ziti/golang/petstoreClient/taste_of_ziti.json. This is a temporary identity that is valid until Fri, 17 Nov 2023 18:48:11 UTC 
2024/01/17 13:53:20 Identity write completed                     
2024/01/17 13:53:20 Loading identity from taste_of_ziti.json     
2024/01/17 13:53:20 This identity provides access to the service: PetstoreDemo 
2024/01/17 13:53:20 Calling PetstoreDemo with url: http://PetstoreDemo/api/v3/pet/findByStatus?status=sold 
2024/01/17 13:53:20 Received: [{"id":5,"category":{"id":1,"name":"Dogs"},"name":"Dog 2","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag2"},{"id":2,"name":"tag3"}],"status":"sold"}] 
```
