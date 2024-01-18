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

package com.example.restservice;

import static com.example.demoutils.AperitivoUtils.DEFAULT_ZITI_IDENTITY_FILE;
import static java.lang.System.exit;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openziti.Ziti;
import org.openziti.ZitiContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demoutils.AperitivoUtils;

/**
 * This example is a simple Java client that connects to a dark Petstore server using OpenZiti.
 */
 public class PetstoreClient {
  private static final Logger log = LoggerFactory.getLogger(PetstoreClient.class);
  private static final String APERITIVO_URL_OPTION = "aperitivoUrl";
  private static final String DEFAULT_APERITIVO_URL = "https://aperitivo.production.netfoundry.io";
  private static final String DEFAULT_QUERY = "/api/v3/pet/findByStatus?status=available";
  private static final String IDENTITY_OPTION = "identityFile";
  private static final String HELP_OPTION = "help";
  private static final String QUERY_OPTION = "query";

  public static void main(final String[] args) {
    final CommandLine cmdLine = parseCommandLineOptions(args);
    final ZitiContext zitiContext = checkCreateIdentity(cmdLine);
    // Simple demo that uses the identity and service to perform a http request to that service
    connectZitiService(zitiContext, cmdLine.getOptionValue(QUERY_OPTION, DEFAULT_QUERY));
    exit(0);
  }

  private static CommandLine parseCommandLineOptions(final String[] args) {
    final Options options = new Options();
    options.addOption(Option.builder().option("a").longOpt(APERITIVO_URL_OPTION).hasArg(true)
        .desc(String.format("URL for the aperitivo service. Defaults to '%s'", DEFAULT_APERITIVO_URL)).build());
    options.addOption(Option.builder().option("i").longOpt(IDENTITY_OPTION).hasArg(true)
        .desc(String.format("Identity file, json or pkcs12. Defaults to '%s'", DEFAULT_ZITI_IDENTITY_FILE)).build());
    options.addOption(Option.builder().option("q").longOpt(QUERY_OPTION).hasArg(true)
        .desc(String.format("Petstore query. Defaults to '%s'", DEFAULT_QUERY)).build());
    options.addOption(Option.builder().option("h").longOpt(HELP_OPTION).desc("Show this help text").build());
    CommandLine commandLine = null;
    try {
      final CommandLineParser parser = new DefaultParser();
      commandLine = parser.parse(options, args);
    } catch (ParseException exception) {
      log.error("Parsing options failed. Reason: {}", exception.getMessage());
      exit(1);
    }
    if (commandLine.hasOption(HELP_OPTION)) {
      final HelpFormatter formatter = new HelpFormatter();
      formatter.setWidth(120);
      formatter.printHelp("PetstoreJava", options);
      exit(0);
    }
    return commandLine;
  }

  private static ZitiContext loadIdentity(final String identityFile) {
    log.info("Attempting to connect to ziti using identity stored in {}", identityFile);
    Ziti.init(identityFile, "".toCharArray(), false);
    final ZitiContext zitiContext = Ziti.getContexts().stream().findFirst().orElseThrow(() -> {
      log.error("Could not establish a Ziti context using the identity {}", identityFile);
      return new IllegalArgumentException("Could not create a ZitiContext");
    });
    if (!zitiContext.getStatus().toString().equals("Active")) {
      log.warn("Failed to establish a ziti context, status: {}.", zitiContext.getStatus());
      if (zitiContext.getStatus().toString().equals("NotAuthorized")) {
        log.error("Cannot authenticate with the configured identity. If using '{}', try deleting this saved identity file and trying again",
            DEFAULT_ZITI_IDENTITY_FILE);
      }
      throw new IllegalArgumentException("Could not authenticate the ZitiContext from: " + identityFile);
    }
    return zitiContext;
  }

  private static ZitiContext checkCreateIdentity(final CommandLine cmdLine) {
    if (cmdLine.hasOption(IDENTITY_OPTION)) {
      return loadIdentity(cmdLine.getOptionValue(IDENTITY_OPTION));
    } else if (new File(DEFAULT_ZITI_IDENTITY_FILE).exists()) {
      return loadIdentity(DEFAULT_ZITI_IDENTITY_FILE);
    } else {
      return loadIdentity(AperitivoUtils.createIdentityKeystore(
          cmdLine.getOptionValue(APERITIVO_URL_OPTION, DEFAULT_APERITIVO_URL)));
    }
  }

  private static void connectZitiService(final ZitiContext zitiContext, final String petstoreQuery) {

    if (zitiContext == null) {
      throw new IllegalArgumentException("No Ziti context ");
    }

    try {
      log.info("Connected to ziti using identity {}", zitiContext.name());

      // throws an exception if the service cannot be found within the specified time
      zitiContext.getService("PetstoreDemo", 10000);

      // we could use the service.getConfig("intercept.v1", JsonNode.class) to load the address and port range to hit for the
      // service but for this demo, just use what we know about the petstore service
      log.info("Querying petstore over openziti with query: {}", petstoreQuery);
      final Request httpRequest = new Builder()
          .url(String.format("http://%s:%d%s", "petstore.ziti", 80, petstoreQuery))
          .header("Accept", "*/*")
          .get()
          .build();
      // This demonstrates using a third-party HTTP client, like OkHttp, with OpenZiti.  To do so,
      // replace the socketFactory and DNSResolver of the client with those provided by OpenZiti.
      // By doing this, the OpenZiti service's intercept address "petstore.ziti" becomes addressable
      // just like any other address
      final OkHttpClient client = new OkHttpClient.Builder()
          .followRedirects(true)
          .socketFactory(Ziti.getSocketFactory())
          .dns(hostname -> {
            InetAddress address = Ziti.getDNSResolver().resolve(hostname);
            if (address == null) {
              address = InetAddress.getByName(hostname);
            }
            return Optional.ofNullable(address).map(Collections::singletonList).orElse(Collections.emptyList());
          })
          .build();
      try (final Response response = client.newCall(httpRequest).execute()) {
        log.info("Reading response");
        if (response.code() == 200) {
          log.info("--- {}", response.body().string());
        } else {
          log.error("Response code {} received", response.code());
        }
      } catch (final IOException exception) {
        log.error("IOException on http call received: ", exception);
      }
    }
    finally {
      zitiContext.destroy();
      Ziti.removeContext(zitiContext);
    }
  }
}
