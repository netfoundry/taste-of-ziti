/*
	Copyright NetFoundry Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	https://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package main

import (
  "context"
  "flag"
  "fmt"
  "github.com/openziti/sdk-golang/ziti"
  "io"
  "log"
  "net"
  "net/http"
  "os"
)

const defaultAperitivoUrl = "https://aperitivo.production.netfoundry.io"

func parseOpts() map[string]string {
  aperitivoPtr := flag.String("a", defaultAperitivoUrl, "optional Aperitivo url for acquiring an identity")
  identityPtr := flag.String("i", "", "optional identity file")
  queryPtr := flag.String("q", "/api/v3/pet/findByStatus?status=available", "petstore query to execute")
  var showHelp bool
  flag.BoolVar(&showHelp, "h", false, "Display usage")
  flag.Parse()
  if showHelp {
    flag.PrintDefaults()
    os.Exit(0)
  }
  parsedOpts := make(map[string]string)
  if *identityPtr != "" {
    parsedOpts["identity"] = *identityPtr
  }
  // Options with default values so just add
  parsedOpts["query"] = *queryPtr
  parsedOpts["aperitivo"] = *aperitivoPtr
  return parsedOpts
}

// Reads the identity JSON file and adds the parsed ziti.Context into a ziti.CtxCollection
func loadIdentity(identityFile string) *ziti.CtxCollection {
  log.Printf("Loading identity from %s", identityFile)
  ctx, err := ziti.NewContextFromFile(identityFile)
  if err != nil {
    log.Fatalf("Unable to load context from %s", identityFile)
  }
  collection := ziti.NewSdkCollection()
  collection.Add(ctx)

  return collection
}

// load or generate and then load the ziti Identity
func checkCreateIdentity(opts map[string]string) *ziti.CtxCollection {
  // First check to see if an identity file was specified on the command line.  If so, use it
  if cmdLineIdentity, present := opts["identity"]; present {
    return loadIdentity(cmdLineIdentity)
  }
  // Next check to see if the default identity file is available. If not, generate an identity into it
  if _, err := os.Stat(DefaultIdentityFile); err != nil {
    GenerateIdentity(opts["aperitivo"])
  }
  // Lastly, load the identity from the generated or pre-existing default identity file
  return loadIdentity(DefaultIdentityFile)
}

func buildZitiClient(collection *ziti.CtxCollection) *http.Client {
  zitiTransport := http.DefaultTransport.(*http.Transport).Clone() // copy default transport
  zitiTransport.DialContext = func(_ context.Context, _, addr string) (net.Conn, error) {
    // Need to search for a context within the collection that provides access to the service name in the address parameter
    service, _, err := net.SplitHostPort(addr)
    if err != nil {
      return nil, err
    }

    var ztx ziti.Context
    var matchFound = false
    collection.ForAll(func(ctx ziti.Context) {
      if matchFound {
        return
      }

      if _, found := ctx.GetService(service); found {
        ztx = ctx
        matchFound = true
      }
    })

    if matchFound {
      return ztx.Dial(service)
    }
    return nil, fmt.Errorf("service [%s] is not available by any ziti context", service)
  }
  zitiTransport.TLSClientConfig.InsecureSkipVerify = true
  return &http.Client{Transport: zitiTransport}
}

func connectZitiService(ctxCollection *ziti.CtxCollection, opts map[string]string) {
  if !CheckIdentityIsStillValid(ctxCollection, "PetstoreDemo") {
    log.Fatal("PetstoreDemo service not accessible by this Identity")
  }
  log.Printf("Calling PetstoreDemo with query string from command line: %s", opts["query"])
  response, err := buildZitiClient(ctxCollection).Get("http://PetstoreDemo" + opts["query"])
  if err != nil {
    log.Fatal(err)
  }
  defer func(Body io.ReadCloser) {
    err := Body.Close()
    if err != nil {
      log.Fatal(err)
    }
  }(response.Body)
  responseData, err := io.ReadAll(response.Body)
  if err != nil {
    log.Fatal(err)
  }
  if response.StatusCode != 200 {
    log.Printf("ERROR: Non-success response (%d) received from the petstore service on the query for %s.  Body received is: %s",
      response.StatusCode, opts["query"], responseData)
    return
  }

  log.Printf("Received: %s", responseData)

}

func main() {
  userOpts := parseOpts()

  zitiCtxCollection := checkCreateIdentity(userOpts)

  connectZitiService(zitiCtxCollection, userOpts)
}
