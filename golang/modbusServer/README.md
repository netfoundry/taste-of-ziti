# OpenZiti ModbusTCP Server Demo

This example is a simple golang server that listens for ModbusTCP device messages using OpenZiti. 

## Setup :wrench:
This example uses the Taste-of-Ziti demo network and its Aperitivo service to obtain a temporary identity. Refer to
the [Taste-of-Ziti README](../../README.md) for additional details on the Taste-of-Ziti demo network.

## Requirements
* go 1.21.x or later
* gcc compiler

## Usage

By default, the modbusServer connects to a hosted OpenZiti network and receives a temporary identity.  The 
identity is saved and reused for subsequent calls.  An alternate identity file can be used by calling the application with
the `-i <identityFile>` parameter.

### Example Output
```shell
$ go run .
```

Results:
```
2024/03/20 16:04:37 Connecting to Aperitivo at https://aperitivo.production.netfoundry.io to generate a new temporary identity 
INFO[0004] generating 4096 bit RSA key                  
2024/03/20 16:04:42 A new Identity was enrolled and stored in taste_of_ziti.json. This is a temporary Identity that is valid until Wed, 20 Mar 2024 22:04:37 UTC
2024/03/20 16:04:42 Loading identity from taste_of_ziti.json
2024/03/20 16:04:43 listening to ziti service: Colatura-di-Alici-con-Coquillettes-modbus
INFO[0005] new service session                           session token=99190df4-1216-4c4e-8b5b-81fea2375faa
2024/03/20 16:14:42 Received a ReadHoldingRegistersRequestTCP request for id=2
2024/03/20 16:14:42 Received a ReadCoilsRequestTCP request for id=1
2024/03/20 16:14:42 Received a ReadCoilsRequestTCP request for id=2
2024/03/20 16:14:42 Received a ReadHoldingRegistersRequestTCP request for id=1
```
### Testing your server from ScadaLTS

The Taste-of-Ziti demo environment also has an instance of [ScadaLTS](https://github.com/SCADA-LTS/Scada-LTS/wiki) that can be accessed
using [BrowZer](https://scadalts.tasteofziti.browzer.cloudziti.io). As described in the [Taste-of-Ziti BrowZer Configuration](../../browzer/README.md),
first authenticate using the demo credentials specified there, then login to the ScadaLTS application using the `demoUser` / `demo_Guest` credentials.

The sample data points created for your server should already be available and can be added to a new watch list. The ScadaLTS application queries those data points
over OpenZiti to your listening server.
