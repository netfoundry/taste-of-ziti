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
  "encoding/json"
  "errors"
  "flag"
  "fmt"
  "github.com/michaelquigley/pfxlog"
  "github.com/openziti/sdk-golang/ziti"
  "github.com/openziti/sdk-golang/ziti/enroll"
  "github.com/sirupsen/logrus"
  "io"
  "net"
  "net/http"
  "os"
  "time"
)

var log = pfxlog.Logger()
var defaultAperitivoUrl = "https://aperitivo.production.netfoundry.io"
var defaultIdentityFile = "taste_of_ziti.json"

func init() {
  pfxlog.GlobalInit(logrus.InfoLevel, pfxlog.DefaultOptions().SetTrimPrefix("github.com/openziti/"))
  logrus.SetFormatter(&logrus.TextFormatter{
    ForceColors:      true,
    DisableTimestamp: true,
    TimestampFormat:  "",
    PadLevelText:     true,
  })
  logrus.SetReportCaller(false)
}

func parseOpts() map[string]string {
  aperitivoPtr := flag.String("a", defaultAperitivoUrl, "optional aperitivo url for acquiring an identity")
  identityPtr := flag.String("i", "", "optional identity file")
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
  parsedOpts["aperitivo"] = *aperitivoPtr
  return parsedOpts
}

type Identity struct {
  Name       string    `json:"name"`
  Jwt        string    `json:"jwt"`
  ValidUntil time.Time `json:"validUntil"`
}

func generateIdentity(aperitivoUrl string) (identityFile string, err error) {
  log.Infof("Connecting to aperitivo at %s to generate a new temporary identity", aperitivoUrl)

  response, err := http.Post(aperitivoUrl+"/aperitivo/v1/identities", "application/json", nil)
  if err != nil {
    log.Fatal(err)
    return "", err
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
    return "", err
  }

  if response.StatusCode != 200 {
    log.Errorf("Non-success response (%d) received from the aperitivo service when getting a new identity.  Body received is: %s",
      response.StatusCode, responseData)
    return "", errors.New("unable to contact aperitivo to get a new identity")
  }

  var responseObject Identity
  err = json.Unmarshal(responseData, &responseObject)
  if err != nil {
    log.Fatal(err)
    return "", err
  }

  err = zitiEnrollJwt(responseObject.Jwt, defaultIdentityFile, responseObject.ValidUntil)
  if err != nil {
    log.Fatal(err)
    return "", err
  }
  return defaultIdentityFile, nil
}

func zitiEnrollJwt(jwt string, outputFile string, validUntil time.Time) error {

  claims, token, err := enroll.ParseToken(jwt)
  if err != nil {
    log.Fatal(err)
    return err
  }
  options := enroll.EnrollmentFlags{
    Token:    claims,
    JwtToken: token,
    KeyAlg:   "RSA",
    Verbose:  true}

  zitiCfg, err := enroll.Enroll(options)
  if err != nil {
    log.Fatal(err)
    return err
  }

  log.Infof("A new identity is being enrolled and stored in %s. This is a temporary identity that is valid until %s",
    outputFile, validUntil.Format(time.RFC1123))
  fos, err := os.Create(outputFile)
  if err != nil {
    log.Fatal(err)
    return err
  }
  outStr, err := json.Marshal(zitiCfg)
  _, err = fos.Write(outStr)
  if err != nil {
    log.Fatal(err)
    return err
  }
  err = fos.Close()
  if err != nil {
    log.Fatal(err)
    return err
  }
  log.Info("Identity write completed")
  return nil
}

func checkCreateIdentity(opts map[string]string) (*ziti.CtxCollection, error) {
  if cmdLineIdentity, present := opts["identity"]; present {
    log.Infof("Loading identity from %s", cmdLineIdentity)
    collection := ziti.NewSdkCollection()
    if ctx, err := ziti.NewContextFromFile(cmdLineIdentity); err == nil {
      collection.Add(ctx)
      return collection, nil
    } else {
      log.Errorf("Unable to load context from %s", cmdLineIdentity)
      return nil, err
    }
  }
  if _, present := os.LookupEnv("ZITI_IDENTITIES"); present {
    log.Info("Loading identity from ZITI_IDENTITIES environment variable")
    return ziti.NewSdkCollectionFromEnv("ZITI_IDENTITIES"), nil
  }
  collection := ziti.NewSdkCollection()
  if _, err := os.Stat(defaultIdentityFile); err != nil {
    if _, err := generateIdentity(opts["aperitivo"]); err != nil {
      return nil, err
    }
  }
  log.Infof("Loading identity from %s", defaultIdentityFile)
  if ctx, err := ziti.NewContextFromFile(defaultIdentityFile); err == nil {
    collection.Add(ctx)
    return collection, nil
  } else {
    return nil, err
  }
}

func newZitiClient(collection *ziti.CtxCollection) *http.Client {
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

func hitZitiService(ctxCollection *ziti.CtxCollection, opts map[string]string) {
  found := false
  ctxCollection.ForAll(func(ctxItem ziti.Context) {
    err := ctxItem.Authenticate()
    if err != nil {
      log.Errorf("Cannot authenticate with the configured identity. If using '%s', try deleting this saved identity file and trying again",
        defaultIdentityFile)
      log.Fatal(err)
      return
    }
    svcs, err := ctxItem.GetServices()
    if err != nil {
      log.Fatal(err)
    }
    for _, svc := range svcs {
      log.Infof("This identity provides access to the service: %s", *svc.Name)
      if *svc.Name == "PetstoreDemo" {
        found = true
        break
      }
    }
  })

  if !found {
    log.Fatal("PetstoreDemo service not accessible by this identity")
  }
  log.Infof("Dialing PetstoreDemo with query string from command line: %s", opts["query"])
  response, err := newZitiClient(ctxCollection).Get("http://PetstoreDemo" + opts["query"])
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
    log.Errorf("Non-success response (%d) received from the petstore service on the query for %s.  Body received is: %s",
      response.StatusCode, opts["query"], responseData)
    return
  }

  log.Infof("Received: %s", responseData)

}

func main() {
  userOpts := parseOpts()

  zitiCtxCollection, err := checkCreateIdentity(userOpts)
  if err != nil {
    log.Fatal(err)
  }

  hitZitiService(zitiCtxCollection, userOpts)
}
