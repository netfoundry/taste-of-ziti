#  Copyright NetFoundry Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import getopt
import os
import os.path
import sys
import openziti
import requests

DEFAULT_IDENTITY_FILE = 'taste_of_ziti.json'
APERITIVO_OPT = 'aperitivo'
IDENTITY_OPT = 'identity'
QUERY_OPT = 'query'


def create_aperitivo_identity(opts):
    print('Connecting to aperitivo to generate a new temporary identity')
    if APERITIVO_OPT in opts:
        aperitivo_url = opts[APERITIVO_OPT]
    else:
        aperitivo_url = 'https://aperitivo.production.netfoundry.io'

    aperitivo_response = requests.post(aperitivo_url + '/aperitivo/v1/identities')
    if aperitivo_response.status_code != 200:
        print('Failure to connect to aperitivo to generate a new identity.  Status code = ' +
              str(aperitivo_response.status_code))
        sys.exit(2)

    # Aperitivo returns a json structure like this:
    # {
    #   "name": String identity name,
    #   "jwt": String identity jwt to enroll,
    #   "validUntil": Date string indicating when the demo identity will be automatically removed
    # }
    token_file = open(DEFAULT_IDENTITY_FILE, 'w')
    token_file.write(openziti.enroll(aperitivo_response.json()['jwt']))
    token_file.close()
    print('Saved an openziti identity in ' + DEFAULT_IDENTITY_FILE +
          '. This is a temporary identity that is valid until ' + aperitivo_response.json()['validUntil'])


def check_create_identity(opts):
    # check user input and if specified, return it
    if opts.get(IDENTITY_OPT, '') != '':
        print('Loading identity from ' + opts[IDENTITY_OPT])
        openziti.load(opts[IDENTITY_OPT])

    # If the identity is not specified on the command line, try to pull it from the environment
    elif os.environ.get('ZITI_IDENTITIES', '') != '':
        print('Loading identity from ' + os.environ['ZITI_IDENTITIES'])
        # do not need to call openziti.load here, will be loaded automatically

    # If still empty, create or use the identity from aperitivo
    else:
        if not os.path.exists(DEFAULT_IDENTITY_FILE):
            create_aperitivo_identity(opts)
        print('Loading identity from ' + DEFAULT_IDENTITY_FILE)
        try:
            openziti.load(DEFAULT_IDENTITY_FILE)
        except:
            print('Cannot authenticate with the existing aperitivo identity. Try deleting the previously saved ' +
                  'identity file at \'' + DEFAULT_IDENTITY_FILE + '\' and trying again.')
            sys.exit(1)


def parse_opts(argv):
    parsed_opts = {}
    opts, args = getopt.getopt(argv, "a:hi:q:", ["aperitivo=", "identity=", "query="])
    for opt, arg in opts:
        if opt == '-h':
            print('petstore.py -i <identityFile> -q <petstore query> -a <aperitivoUrl>')
            sys.exit()
        elif opt in ("-a", "--aperitivoUrl"):
            parsed_opts[APERITIVO_OPT] = arg
        elif opt in ("-i", "--identity"):
            parsed_opts[IDENTITY_OPT] = arg
        elif opt in ("-q", "--query"):
            parsed_opts[QUERY_OPT] = arg
    return parsed_opts


def hit_ziti_service(opts):
    # Use openziti monkeypatch to query over ziti
    if QUERY_OPT in opts:
        query_str = opts[QUERY_OPT]
    else:
        query_str = '/'

    with openziti.monkeypatch():
        print('Querying petstore over openziti with query: ' + query_str)
        r = requests.get("http://petstore.ziti" + query_str)
        print(r.text)


if __name__ == "__main__":
    user_opts = parse_opts(sys.argv[1:])

    check_create_identity(user_opts)
    hit_ziti_service(user_opts)
