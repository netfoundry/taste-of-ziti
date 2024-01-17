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
  "encoding/json"
  "github.com/openziti/sdk-golang/ziti/enroll"
  "io"
  "log"
  "net/http"
  "os"
  "path/filepath"
  "time"
)

// DefaultIdentityFile defines the name of the default identity file for storing the identity received from Aperitivo
const DefaultIdentityFile string = "taste_of_ziti.json"

type aperitivoIdentity struct {
  Name       string    `json:"name"`
  Jwt        string    `json:"jwt"`
  ValidUntil time.Time `json:"validUntil"`
}

// GenerateIdentity will connect to the specified Aperitivo URL to generate a new temporary Ziti Identity
// Then, enroll that Identity.  It stores the Identity in the current working directory.
// Errors obtaining, enrolling or storing the Identity are fatal and will terminate the application
func GenerateIdentity(aperitivoUrl string) string {
  log.Printf("Connecting to Aperitivo at %s to generate a new temporary Identity", aperitivoUrl)

  // simple REST based query that returns a JSON formatted response
  response, err := http.Post(aperitivoUrl+"/aperitivo/v1/identities", "application/json", nil)
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
    log.Fatalf("Non-success response (%d) received from the Aperitivo service when getting a new Identity.  Body received is: %s",
      response.StatusCode, responseData)
  }

  var responseObject aperitivoIdentity
  err = json.Unmarshal(responseData, &responseObject)
  if err != nil {
    log.Fatal(err)
  }

  zitiEnrollJwt(responseObject.Jwt, DefaultIdentityFile, responseObject.ValidUntil)
  return DefaultIdentityFile
}

// Use the ziti Enroll method to register the Identity with the OpenZiti controller (referenced in the JWT)
func zitiEnrollJwt(jwt string, outputFile string, validUntil time.Time) {

  claims, token, err := enroll.ParseToken(jwt)
  if err != nil {
    log.Fatal(err)
  }
  options := enroll.EnrollmentFlags{
    Token:    claims,
    JwtToken: token,
    KeyAlg:   "RSA",
    Verbose:  true}

  zitiCfg, err := enroll.Enroll(options)
  if err != nil {
    log.Fatal(err)
  }

  // store the enrolled Identity in the output file as JSON
  fos, err := os.Create(outputFile)
  if err != nil {
    log.Fatal(err)
  }
  outStr, err := json.Marshal(zitiCfg)
  _, err = fos.Write(outStr)
  if err != nil {
    log.Fatal(err)
  }
  err = fos.Close()
  if err != nil {
    log.Fatal(err)
  }
  if fullIdentityPath, err := filepath.Abs(outputFile); err == nil {
    log.Printf("A new Identity was enrolled and stored in %s. This is a temporary Identity that is valid until %s",
      fullIdentityPath, validUntil.Format(time.RFC1123))
  } else {
    log.Fatal("Failure retrieving full path of stored Identity file")
  }
}