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
	"github.com/jackc/pgx/v5"
	"github.com/openziti/sdk-golang/ziti"
	"log"
	"net"
	"os"
)

const DefaultAperitivoUrl = "https://aperitivo.production.netfoundry.io"

func parseOpts() map[string]string {
	aperitivoPtr := flag.String("a", DefaultAperitivoUrl, "optional Aperitivo url for acquiring an identity")
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

func connectZitiPostgresClient(collection *ziti.CtxCollection) (conn *pgx.Conn, err error) {
	config, err := pgx.ParseConfig("postgresql://PostgresDemo/simpledb")
	if err != nil {
		return nil, err
	}
	config.User = "viewuser"
	config.Password = "viewpass"

	// the pgx connect will first use the LookupFunc to look up the host using the net.LookupHost function.
	// Since this is a ziti service name and the translation from service to ziti context is performed
	// in the DialFunc, just return the host string, as is, here.
	config.LookupFunc = func(_ context.Context, host string) (addrs []string, err error) {
		return []string{host}, nil
	}

	// The DialFunc returns a net.Conn to the database.  Search the ziti context collection
	// for a ziti context that provides access to the named service and then use the ziti.Dial
	// method to return a net.Conn connection to that service.
	config.DialFunc = func(_ context.Context, _, addr string) (net.Conn, error) {
		// Need to search for a context within the collection that provides access to the service name in the address parameter
		// the addr string will be in the format of ServiceName:port so split off the port portion since we are dialing
		// by service name.
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
	return pgx.ConnectConfig(context.Background(), config)
}

func queryDatabaseOverZiti(ctxCollection *ziti.CtxCollection) {
	if !CheckIdentityIsStillValid(ctxCollection, "PostgresDemo") {
		log.Fatal("PetstoreDemo service not accessible by this Identity")
	}

	log.Printf("Connecting to PostgresDemo and issuing a simple database query")

	var dbConn *pgx.Conn
	dbConn, err := connectZitiPostgresClient(ctxCollection)
	if err != nil {
		log.Fatal(err)
	}
	defer dbConn.Close(context.Background())
	rows, err := dbConn.Query(context.Background(), "select * from vets")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()
	// Loop through rows, using Scan to assign column data to struct fields.
	for rows.Next() {
		var firstName string
		var lastName string
		var intVal int32
		if err := rows.Scan(&intVal, &firstName, &lastName); err != nil {
			log.Fatal(err)
		}
		log.Printf("Result from database is: %d: %s %s", intVal, firstName, lastName)
	}
	if err := rows.Err(); err != nil {
		log.Fatal(err)
	}
}

func main() {
	userOpts := parseOpts()

	zitiCtxCollection := checkCreateIdentity(userOpts)

	queryDatabaseOverZiti(zitiCtxCollection)
}
