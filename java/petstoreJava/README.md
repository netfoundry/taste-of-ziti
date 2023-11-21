# OpenZiti PetstoreJava

This example is a simple Java client that connects to a Petstore server using OpenZiti.

## Setup :wrench:
This example uses the Taste-of-Ziti demo network and its aperitivo service to obtain a temporary identity. Refer to
the [Taste-of-Ziti README](../../README.md) for additional details on the Taste-of-Ziti demo network.

## Requirements
* JDK 17 or later
* maven or gradle to build

## Build
Execute either of the following to build with the desired build framework

1. Run the following to build using maven

       ./mvnw clean install

1. Run the following to build using gradle

       ./gradlew clean build

## Usage

By default, the PetstoreJava client connects to a hosted OpenZiti network and receives a temporary identity.  The
identity is saved and reused for subsequent calls.  An alternate identity file can be used by calling the client with
the `-i <identityFile>` parameter.


Gradle run command:

       ./gradlew run --args="-h"

```shell
> Task :runWithJavaExec
usage: PetstoreJava
-a,--aperitivoUrl <arg>   URL for the aperitivo service. Defaults to 'https://aperitivo.production.netfoundry.io'
-h,--help                 Show this help text
-i,--identityFile <arg>   Identity file, json or pkcs12. Defaults to 'taste_of_ziti.pkcs12'
-q,--query <arg>          Petstore query. Defaults to '/api/v3/pet/findByStatus?status=available'
```

Maven run command:

       ./mvnw exec:java -Dexec.args="-h"

```shell
usage: PetstoreJava
 -a,--aperitivoUrl <arg>   URL for the aperitivo service. Defaults to 'https://aperitivo.production.netfoundry.io'
 -h,--help                 Show this help text
 -i,--identityFile <arg>   Identity file, json or pkcs12. Defaults to 'taste_of_ziti.pkcs12'
 -q,--query <arg>          Petstore query. Defaults to '/'
```

### Example Output
```shell
$  ./mvnw exec:java -Dexec.args="-q /api/v3/pet/findByStatus?status=sold"
[com.example.restservice.PetstoreJava.main()] INFO com.example.restservice.PetstoreJava - Connecting to aperitivo at https://aperitivo.production.netfoundry.io to generate a new identity
[com.example.restservice.PetstoreJava.main()] INFO com.example.restservice.PetstoreJava - Building a keystore to contain the new identity
[com.example.restservice.PetstoreJava.main()] INFO com.example.restservice.PetstoreJava - A new identity is stored at taste_of_ziti.pkcs12. This is a temporary identity that is valid until 11/18/2023 15:18:36
[com.example.restservice.PetstoreJava.main()] INFO com.example.restservice.PetstoreJava - Attempting to connect to ziti using identity stored in taste_of_ziti.pkcs12
[com.example.restservice.PetstoreJava.main()] INFO org.openziti.impl.ZitiImpl - ZitiSDK version 0.25.1 @344b49b()
[DefaultDispatcher-worker-3] INFO org.openziti.api.Controller - controller[https://db2f5d29-d062-47e1-82dc-a12738f0768e.production.netfoundry.io/] version(v0.29.0/3ca2dd2f4e7b)
[DefaultDispatcher-worker-3] INFO org.openziti.net.dns.ZitiDNSManager - assigned petstore.ziti => petstore.ziti/100.64.1.3 []
[DefaultDispatcher-worker-3] INFO org.openziti.net.dns.ZitiDNSManager - registered: petstore.ziti => petstore.ziti/100.64.1.3
[com.example.restservice.PetstoreJava.main()] INFO com.example.restservice.PetstoreJava - Connected to ziti using identity u72hknkhe-
[com.example.restservice.PetstoreJava.main()] INFO com.example.restservice.PetstoreJava - Querying petstore over openziti with query: /api/v3/pet/findByStatus?status=sold
[com.example.restservice.PetstoreJava.main()] INFO com.example.restservice.PetstoreJava - Reading response
[com.example.restservice.PetstoreJava.main()] INFO com.example.restservice.PetstoreJava - --- [{"id":5,"category":{"id":1,"name":"Dogs"},"name":"Dog 2","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag2"},{"id":2,"name":"tag3"}],"status":"sold"}]
```
