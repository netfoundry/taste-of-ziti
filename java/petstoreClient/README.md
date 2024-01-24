# OpenZiti PetstoreClient

This example is a simple Java client that connects to a dark Petstore server using OpenZiti.

## Setup :wrench:
This example uses the Taste-of-Ziti demo network and its aperitivo service to obtain a temporary identity. Refer to
the [Taste-of-Ziti README](../../README.md) for additional details on the Taste-of-Ziti demo network.

## Requirements
* JDK 21 or later
* maven or gradle to build

## Build
Execute either of the following to build with the desired build framework

1. Run the following to build using maven

       ./mvnw clean install

1. Run the following to build using gradle

       ./gradlew clean build

## Usage

By default, the PetstoreClient client connects to a hosted OpenZiti network and receives a temporary identity.  The
identity is saved and reused for subsequent calls.  An alternate identity file can be used by calling the client with
the `-i <identityFile>` parameter.


### Example Output

```shell
$ ./gradlew run --args="-h"
usage: PetstoreClient
 -a,--aperitivoUrl <arg>   URL for the aperitivo service. Defaults to 'https://aperitivo.production.netfoundry.io'
 -h,--help                 Show this help text
 -i,--identityFile <arg>   Identity file, json or pkcs12. Defaults to 'taste_of_ziti.pkcs12'
 -q,--query <arg>          Petstore query. Defaults to '/'

$ ./mvnw exec:java -Dexec.args="-q /api/v3/pet/findByStatus?status=sold"
[com.example.restservice.PetstoreClient.main()] INFO com.example.demoutils.AperitivoUtils - Connecting to aperitivo at https://aperitivo.production.netfoundry.io to generate a new identity
[com.example.restservice.PetstoreClient.main()] INFO com.example.demoutils.AperitivoUtils - Building a keystore to contain the new identity
[com.example.restservice.PetstoreClient.main()] INFO com.example.demoutils.AperitivoUtils - A new identity is stored at taste_of_ziti.pkcs12. This is a temporary identity that is valid until 11/18/2023 15:18:36
[com.example.restservice.PetstoreClient.main()] INFO com.example.restservice.PetstoreClient - Attempting to connect to ziti using identity stored in taste_of_ziti.pkcs12
[com.example.restservice.PetstoreClient.main()] INFO org.openziti.impl.ZitiImpl - ZitiSDK version 0.25.1 @344b49b()
[DefaultDispatcher-worker-3] INFO org.openziti.api.Controller - controller[https://db2f5d29-d062-47e1-82dc-a12738f0768e.production.netfoundry.io/] version(v0.29.0/3ca2dd2f4e7b)
[DefaultDispatcher-worker-3] INFO org.openziti.net.dns.ZitiDNSManager - assigned petstore.ziti => petstore.ziti/100.64.1.3 []
[DefaultDispatcher-worker-3] INFO org.openziti.net.dns.ZitiDNSManager - registered: petstore.ziti => petstore.ziti/100.64.1.3
[com.example.restservice.PetstoreClient.main()] INFO com.example.restservice.PetstoreClient - Connected to ziti using identity u72hknkhe-
[com.example.restservice.PetstoreClient.main()] INFO com.example.restservice.PetstoreClient - Calling PetstoreDemo with url: http://PetstoreDemo/api/v3/pet/findByStatus?status=sold
[com.example.restservice.PetstoreClient.main()] INFO com.example.restservice.PetstoreClient - Reading response
[com.example.restservice.PetstoreClient.main()] INFO com.example.restservice.PetstoreClient - --- [{"id":5,"category":{"id":1,"name":"Dogs"},"name":"Dog 2","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag2"},{"id":2,"name":"tag3"}],"status":"sold"}]
```
