# OpenZiti ModbusTCP Server Demo

This example is a simple Java server that listens for ModbusTCP device messages using OpenZiti.

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

By default, the ModbusServer connects to a hosted OpenZiti network and receives a temporary identity.  The
identity is saved and reused for subsequent calls.  An alternate identity file can be used by starting the application with
the `-i <identityFile>` parameter.


### Example Output

```shell
$ ./gradlew run --args="-h"
usage: ModbusServer
 -a,--aperitivoUrl <arg>   URL for the aperitivo service. Defaults to 'https://aperitivo.production.netfoundry.io'
 -h,--help                 Show this help text
 -i,--identityFile <arg>   Identity file, json or pkcs12. Defaults to 'taste_of_ziti.pkcs12'

$ ./mvnw exec:java
[com.example.modbus.ModbusServer.main()] INFO com.example.demoutils.AperitivoUtils - Connecting to aperitivo at https://aperitivo.production.netfoundry.io to generate a new identity
[com.example.modbus.ModbusServer.main()] INFO com.example.demoutils.AperitivoUtils - Building a keystore to contain the new identity
[com.example.modbus.ModbusServer.main()] INFO com.example.demoutils.AperitivoUtils - A new identity is stored at taste_of_ziti.pkcs12. This is a temporary identity that is valid until 03/20/2024 16:51:07
[com.example.modbus.ModbusServer.main()] INFO com.example.modbus.ModbusServer - Attempting to connect to ziti using identity stored in taste_of_ziti.pkcs12
[com.example.modbus.ModbusServer.main()] INFO org.openziti.impl.ZitiImpl - ZitiSDK version 0.26.2 @b9421cb()
[DefaultDispatcher-worker-3] INFO org.openziti.api.Controller - controller[https://abbb6aa2-9bda-4c6e-9ad6-f315a7119d59.production.netfoundry.io/] version(v0.31.4/1c21434737ac)
[DefaultDispatcher-worker-3] INFO org.openziti.net.dns.ZitiDNSManager - assigned clam-con-ricciutelle.edge => clam-con-ricciutelle.edge/100.64.1.8 []
[DefaultDispatcher-worker-3] INFO org.openziti.net.dns.ZitiDNSManager - registered: clam-con-ricciutelle.edge => clam-con-ricciutelle.edge/100.64.1.8
[com.example.modbus.ModbusServer.main()] INFO com.example.modbus.ModbusServer - Starting bind to ziti service Clam-con-Ricciutelle-modbus
[modbus-netty-event-loop-1] INFO com.example.modbus.ModbusServer - Received request id=1 from :Clam-con-Ricciutelle-modbus for ReadHoldingRegistersRequest
[modbus-netty-event-loop-2] INFO com.example.modbus.ModbusServer - Received request id=1 from :Clam-con-Ricciutelle-modbus for ReadCoilsRequest
[modbus-netty-event-loop-3] INFO com.example.modbus.ModbusServer - Received request id=1 from :Clam-con-Ricciutelle-modbus for ReadCoilsRequest
[modbus-netty-event-loop-4] INFO com.example.modbus.ModbusServer - Received request id=1 from :Clam-con-Ricciutelle-modbus for ReadHoldingRegistersRequest
```

### Testing your server from ScadaLTS

The Taste-of-Ziti demo environment also has an instance of [ScadaLTS](https://github.com/SCADA-LTS/Scada-LTS/wiki) that can be accessed 
using [BrowZer](https://scadalts.tasteofziti.browzer.cloudziti.io). As described in the [Taste-of-Ziti BrowZer Configuration](../../browzer/README.md),
first authenticate using the demo credentials specified there, then login to the ScadaLTS application using the `demoUser` / `demo_Guest` credentials.

The sample data points created for your server should already be available and can be added to a new watch list. The ScadaLTS application queries those data points
over OpenZiti to your listening server.
