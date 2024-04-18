![Ziggy using the ziti-sdk-jvm](https://raw.githubusercontent.com/openziti/branding/main/images/banners/Java.jpg)

The [OpenZiti](https://github.com/openziti) SDK for JVM enables Java and other developers to easily and securely connect their applications to backend services over
Ziti networks

OpenZiti is an open-source project that provides secure, zero-trust networking for applications running on any platform.

## Obtaining the SDK
The recommended way to use the OpenZiti SDK for Java in your project is to add the dependency
using your favorite build tool.
Our artifacts are hosted on Maven Central, so add repo(if needed) and dependency to your project.

Maven
___
```xml
  <project>
     ...
     <repositories>
        ...
        <repository>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <id>central</id>
            <name>Maven Central</name>
            <url>https://repo.maven.apache.org/maven2/</url>
         </repository>
     </repositories>
     ...
     <dependencies>
        ...
        <dependency>
           <groupId>org.openziti</groupId>
           <artifactId>ziti</artifactId>
           <version>[0,)</version>
        </dependency>
     </dependencies>
     ...
  </project>
```

Gradle
______
```gradle
repositories {
  mavenCentral()
}

dependencies {
  implementation 'org.openziti:ziti:+'
  ...
}
```

## Examples
Here are the Taste-of-Ziti examples for Java.  Also, see the general OpenZiti Java SDK [examples](https://github.com/openziti/ziti-sdk-jvm/tree/main/samples)
* [Petstore Client](petstoreClient)
* [Database Client](dbClient)
* [ModbusTCP Server](modbusServer)


## License
[Apache 2.0](../LICENSE)

