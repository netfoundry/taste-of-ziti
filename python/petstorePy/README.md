# Petstore
This example shows the use of Ziti monkey patching a standard socket, via the requests module, to intercept network 
connections using a Ziti overlay.

## Setup :wrench:
This example uses the Taste-of-Ziti demo network and its aperitivo service to obtain a temporary identity. Refer to 
the [Taste-of-Ziti README](../../README.md) for additional details on the Taste-of-Ziti demo network.

### Install Python Requirements
If you haven't already installed them, you'll need the dependent libraries used in the examples.
  ```bash
  pip install -r ../requirements
  ```

## Running the Example :arrow_forward:
Usage for this example shows the _optional_ arguments

```shell
$ python petstore.py -h
petstore.py -i <identityFile> -q <petstore query> -a <aperitivoUrl>
```
By default, the example obtains a new identity from the NetFoundry aperitivo service at `https://aperitivo.production.netfoundry.io` 
and stores it locally in a `taste_of_ziti.json` file.  This example can also utilize an identity referenced in the `ZITI_IDENTITIES` 
environment variable.
```shell
export ZITI_IDENTITIES="/path/to/id.json"
```

## Testing the Example :clipboard:
```shell
$ python petstore.py -q /api/v3/pet/findByStatus?status=sold 
Connecting to aperitivo to generate a new temporary identity
Saved an openziti identity in taste_of_ziti.json. This is a temporary identity that is valid until 2023-11-18T16:48:26.614768Z
Loading identity from taste_of_ziti.json
Querying petstore over openziti with query: /api/v3/pet/findByStatus?status=sold
[{"id":5,"category":{"id":1,"name":"Dogs"},"name":"Dog 2","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag2"},{"id":2,"name":"tag3"}],"status":"sold"}]
```
