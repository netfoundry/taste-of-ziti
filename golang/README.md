![Ziggy using the sdk-golang](https://raw.githubusercontent.com/openziti/branding/main/images/banners/Go.jpg)
The [OpenZiti](https://github.com/openziti) SDK for GoLang allows developers to create their own custom OpenZiti network endpoint clients and
management tools. OpenZiti is a modern, programmable network overlay with associated edge components, for
application-embedded, zero trust network connectivity, written by developers for developers. The SDK harnesses that
power via APIs that allow developers to imagine and develop solutions beyond what OpenZiti handles by default.


## Using the SDK
Using the GoLang SDK is as simple as adding the ziti import:
```golang
import (
  "github.com/openziti/sdk-golang/ziti"
  ...
)
```

## Examples
Here are the Taste-of-Ziti examples for GoLang.  Also, see the general OpenZiti GoLang SDK [examples](https://github.com/openziti/sdk-golang/tree/main/example)
* [Petstore Client](petstoreClient)
* [Database Client](dbClient)
* [ModbusTCP Server](modbusServer)


## License
[Apache 2.0](../LICENSE)

