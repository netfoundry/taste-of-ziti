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
	"errors"
	"flag"
	"fmt"
	"github.com/aldas/go-modbus-client/packet"
	"github.com/aldas/go-modbus-client/server"
	"github.com/openziti/sdk-golang/ziti"
	"log"
	"math/rand"
	"os"
	"os/signal"
	"time"
)

const defaultAperitivoUrl = "https://aperitivo.production.netfoundry.io"

type modbusPeripheral struct {
}

// Handle function to process and reply to received ModbusTCP requests
func (s *modbusPeripheral) Handle(ctx context.Context, received packet.Request) (packet.Response, error) {
	switch req := received.(type) {
	case *packet.ReadHoldingRegistersRequestTCP:
		n := rand.Intn(100)
		log.Printf("Received a ReadHoldingRegistersRequestTCP request for id=%d", req.UnitID)
		p := packet.ReadHoldingRegistersResponseTCP{
			MBAPHeader: req.MBAPHeader,
			ReadHoldingRegistersResponse: packet.ReadHoldingRegistersResponse{
				UnitID:          req.UnitID,
				RegisterByteLen: 2,
				Data:            []byte{byte(n / 256), byte(n % 256)}, // big endian
			},
		}
		return p, nil
	case *packet.ReadCoilsRequestTCP:
		log.Printf("Received a ReadCoilsRequestTCP request for id=%d", req.UnitID)
		n := rand.Intn(2)
		p := packet.ReadCoilsResponseTCP{
			MBAPHeader: req.MBAPHeader,
			ReadCoilsResponse: packet.ReadCoilsResponse{
				UnitID:          req.UnitID,
				CoilsByteLength: 1,
				Data:            []byte{byte(n % 2)}, // 0 / 1
			},
		}
		return p, nil
	}
	return nil, packet.NewErrorParseTCP(packet.ErrIllegalFunction, "unsupported request")
}

// ZitiModbusListen starts the Modbus server listening on the specified Ziti service.
// Received requests are handled by the above Handle function.
func ZitiModbusListen(ztx ziti.Context, service string) {
	// the Listen function returns a net.listener for the specified Ziti service
	listener, err := ztx.Listen(service)
	if err != nil {
		fmt.Printf("Error binding Ziti service %+v\n", err)
		log.Fatal(err)
	}
	ctx := context.Background()
	ctx, cancel := context.WithCancel(ctx)
	ctx, cancel = signal.NotifyContext(ctx, os.Kill, os.Interrupt)
	defer cancel()

	// for this simple example, create a single handler for all received ModbusTCP requests
	mbPeripheral := new(modbusPeripheral)
	mbServer := new(server.Server)
	mbServer.ReadTimeout = 10 * time.Second
	mbServer.WriteTimeout = 5 * time.Second

	// Start the server on a different Goroutine so the application doesn't stop here.
	log.Printf("listening to Ziti service: %v\n", service)
	go mbServer.Serve(ctx, listener, mbPeripheral)
	if err != nil && !errors.Is(err, server.ErrServerClosed) {
		log.Printf("ZitiModbusListen end: %v", err)
	}

	<-ctx.Done()
	log.Printf("ZitiModbusListen shutting down")
	cancel()
}

func main() {
	// First, load the ziti identity
	userOpts := parseOpts()
	zitiContext := checkCreateIdentity(userOpts)

	// Second, authenticate the identity
	err := zitiContext.Authenticate()
	if err != nil {
		log.Printf("ERROR: Cannot authenticate with the configured identity. If using '%s', try deleting this saved identity file and trying again",
			DefaultIdentityFile)
		log.Fatal(err)
	}

	// Third, start the Modbus Server that listens for incoming ModbusTCP requests.
	id, _ := zitiContext.GetCurrentIdentity()
	// aperitivo created a service with the name + "-modbus".  Use it as the name of the service for the listen (bind) request
	ZitiModbusListen(zitiContext, *id.Name+"-modbus")

}

// Below are the command line and Ziti identity loading functions
func parseOpts() map[string]string {
	aperitivoPtr := flag.String("a", defaultAperitivoUrl, "optional Aperitivo url for acquiring an identity")
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
func loadIdentity(identityFile string) ziti.Context {
	log.Printf("Loading identity from %s", identityFile)
	ctx, err := ziti.NewContextFromFile(identityFile)
	if err != nil {
		log.Fatalf("Unable to load context from %s", identityFile)
	}
	return ctx
}

// load or generate and then load the ziti Identity
func checkCreateIdentity(opts map[string]string) ziti.Context {
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
