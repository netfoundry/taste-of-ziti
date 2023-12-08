# OpenZiti JdbcJava

This example is a simple Java client that connects to a JDBC database using OpenZiti

## Setup :wrench:
This example uses the Taste-of-Ziti demo network and its aperitivo service to obtain a temporary identity. Refer to
the [Taste-of-Ziti README](../../README.md) for additional details on the Taste-of-Ziti demo network.

## Requirements
* JDK 20 or later
* maven or gradle to build

## Build
Execute either of the following to build with the desired build framework

1. Run the following to build using maven

       ./mvnw clean install

1. Run the following to build using gradle

       ./gradlew clean build

## Usage

By default, the JdbcJava client connects to a hosted OpenZiti network and receives a temporary identity.  The
identity is saved and reused for subsequent calls.  An alternate identity file can be used by calling the client with
the `-i <identityFile>` parameter.


Gradle run command:

       ./gradlew run --args="-h"

```shell
> Task :runWithJavaExec
usage: JdbcJava
-a,--aperitivoUrl <arg>   URL for the aperitivo service. Defaults to 'https://aperitivo.production.netfoundry.io'
-h,--help                 Show this help text
-i,--identityFile <arg>   Identity file, json or pkcs12. Defaults to 'taste_of_ziti.pkcs12'
```

Maven run command:

       ./mvnw exec:java -Dexec.args="-h"

```shell
usage: JdbcJava
 -a,--aperitivoUrl <arg>   URL for the aperitivo service. Defaults to 'https://aperitivo.production.netfoundry.io'
 -h,--help                 Show this help text
 -i,--identityFile <arg>   Identity file, json or pkcs12. Defaults to 'taste_of_ziti.pkcs12'
```

### Example Output
```shell
$  ./mvnw exec:java
[com.example.jdbcservice.JdbcJava.main()] INFO com.example.jdbcservice.JdbcJava - Connecting to aperitivo at https://aperitivo.production.netfoundry.io to generate a new identity
[com.example.jdbcservice.JdbcJava.main()] INFO com.example.jdbcservice.JdbcJava - Building a keystore to contain the new identity
[com.example.jdbcservice.JdbcJava.main()] INFO com.example.jdbcservice.JdbcJava - A new identity is stored at taste_of_ziti.pkcs12. This is a temporary identity that is valid until 12/08/2023 15:20:38
[com.example.jdbcservice.JdbcJava.main()] INFO com.example.jdbcservice.JdbcJava - Attempting to connect to ziti using identity stored in taste_of_ziti.pkcs12
[com.example.jdbcservice.JdbcJava.main()] INFO org.openziti.impl.ZitiImpl - ZitiSDK version 0.25.1 @344b49b()
[DefaultDispatcher-worker-3] INFO org.openziti.api.Controller - controller[https://4c17a450-52c0-4c6b-b3db-5a0477a7e5f7.production.netfoundry.io/] version(v0.31.0/5237e2b4794a)
[DefaultDispatcher-worker-2] INFO org.openziti.net.dns.ZitiDNSManager - assigned petstore.ziti => petstore.ziti/100.64.1.2 []
[DefaultDispatcher-worker-2] INFO org.openziti.net.dns.ZitiDNSManager - registered: petstore.ziti => petstore.ziti/100.64.1.2
[DefaultDispatcher-worker-2] INFO org.openziti.net.dns.ZitiDNSManager - assigned fireworks.ziti => fireworks.ziti/100.64.1.3 []
[DefaultDispatcher-worker-2] INFO org.openziti.net.dns.ZitiDNSManager - registered: fireworks.ziti => fireworks.ziti/100.64.1.3
[DefaultDispatcher-worker-2] INFO org.openziti.net.dns.ZitiDNSManager - assigned whatismyip.ziti => whatismyip.ziti/100.64.1.4 []
[DefaultDispatcher-worker-2] INFO org.openziti.net.dns.ZitiDNSManager - registered: whatismyip.ziti => whatismyip.ziti/100.64.1.4
[DefaultDispatcher-worker-2] INFO org.openziti.net.dns.ZitiDNSManager - assigned postgres.ziti => postgres.ziti/100.64.1.5 []
[DefaultDispatcher-worker-2] INFO org.openziti.net.dns.ZitiDNSManager - registered: postgres.ziti => postgres.ziti/100.64.1.5
[com.example.jdbcservice.JdbcJava.main()] INFO com.example.jdbcservice.JdbcJava - Connected to ziti using identity 8blb64jub
[com.example.jdbcservice.JdbcJava.main()] INFO com.example.jdbcservice.JdbcJava - Querying simpletable in the postgres database over openziti
Result from database is: a:0
Result from database is: b:1
Result from database is: c:2
Result from database is: d:3
Result from database is: e:4
Result from database is: f:5
Result from database is: g:6
Result from database is: h:7
Result from database is: i:8
Result from database is: j:9
```
