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

package com.example.jdbcservice;

import static java.lang.System.exit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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
import org.openziti.identity.Enroller;
import org.openziti.jdbc.ZitiDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JdbcJava {
  private static final Logger log = LoggerFactory.getLogger(JdbcJava.class);
  private static final String APERITIVO_URL_OPTION = "aperitivoUrl";
  private static final String DEFAULT_APERITIVO_URL = "https://aperitivo.production.netfoundry.io";
  private static final String DEFAULT_ZITI_IDENTITY_FILE = "taste_of_ziti.pkcs12";
  private static final String IDENTITY_OPTION = "identityFile";
  private static final String HELP_OPTION = "help";

  public static void main(final String[] args) {
    final CommandLine cmdLine = parseCommandLineOptions(args);
    final String identityFile = checkCreateIdentity(cmdLine);
    // Simple demo that uses the identity and service to perform a jdbc request to that service
    hitZitiService(identityFile);
    exit(0);
  }

  private static CommandLine parseCommandLineOptions(final String[] args) {
    final Options options = new Options();
    options.addOption(Option.builder().option("a").longOpt(APERITIVO_URL_OPTION).hasArg(true)
        .desc(String.format("URL for the aperitivo service. Defaults to '%s'", DEFAULT_APERITIVO_URL)).build());
    options.addOption(Option.builder().option("i").longOpt(IDENTITY_OPTION).hasArg(true)
        .desc(String.format("Identity file, json or pkcs12. Defaults to '%s'", DEFAULT_ZITI_IDENTITY_FILE)).build());
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
      formatter.printHelp("JdbcJava", options);
      exit(0);
    }
    return commandLine;
  }

  private static String checkCreateIdentity(final CommandLine cmdLine) {
    if (cmdLine.hasOption(IDENTITY_OPTION)) {
      return cmdLine.getOptionValue(IDENTITY_OPTION);
    } else if (new File(DEFAULT_ZITI_IDENTITY_FILE).exists()) {
      return DEFAULT_ZITI_IDENTITY_FILE;
    } else {
      return createIdentityKeystore(
          cmdLine.getOptionValue(APERITIVO_URL_OPTION, DEFAULT_APERITIVO_URL));
    }
  }

  private static String createIdentityKeystore(final String aperitivoUri) {
    try {
      log.info("Connecting to aperitivo at {} to generate a new identity", aperitivoUri);
      final Request request = new Request.Builder()
            .url(URI.create(aperitivoUri).resolve("/aperitivo/v1/identities").toURL())
          .header("Accept", "application/json")
          .post(RequestBody.create(new byte[0], null))
          .build();

      // get an identity from the aperitivo service
      final OkHttpClient client = new OkHttpClient.Builder().followRedirects(true).build();
      final JsonMapper jsonMapper = new JsonMapper();
      try (final Response response = client.newCall(request).execute()) {
        if (response.code() == 200) {
          // create a keystore to contain the enrolled identity
          log.info("Building a keystore to contain the new identity");
          final KeyStore ks = KeyStore.getInstance("PKCS12");
          // load with first parameter null creates an empty keystore
          ks.load(null, "".toCharArray());

          /* Aperitivo returns a json structure like this:
          {
            "name": String identityName,
            "jwt": String identity jwt to enroll,
            "validUntil": Date string indicating when the demo identity will be automatically removed
          }
          */
          final JsonNode identity = jsonMapper.readTree(response.body().string());
          if (identity == null || identity.get("jwt").isNull()) {
            log.warn("Unable to read the identity response, or the received identity does not have a jwt field.");
            return null;
          }
          Enroller.fromJWT(identity.get("jwt").asText()).enroll(null, ks, ""); // alias name (3rd param) not used during enroll
          try (final FileOutputStream fos = new FileOutputStream(DEFAULT_ZITI_IDENTITY_FILE)) {
            ks.store(fos, "".toCharArray());
          }
          final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
          final Instant validUntil = Instant.parse(identity.get("validUntil").asText());
          log.info("A new identity is stored at {}. This is a temporary identity that is valid until {}",
              DEFAULT_ZITI_IDENTITY_FILE, formatter.format(validUntil));
          return DEFAULT_ZITI_IDENTITY_FILE;
        } else {
          log.error("Non-success response ({}) received from the aperitivo service when getting a new identity.  Body received is: {}",
              response.code(), response.body().string());
          System.exit(2);
        }
      }
    }
    catch (final IOException ioException) {
      log.error("Failure contacting the Aperitivo service to get an identity: {}", ioException.getMessage());
      System.exit(2);
    }
    catch (final KeyStoreException | NoSuchAlgorithmException | CertificateException keystoreExceptions) {
      log.error("Failure generating the keystore for the received identity: {}", keystoreExceptions.getMessage());
      System.exit(2);
    }
    return null;
  }

  private static void hitZitiService(final String identityFile) {

    // The demo environment has a 'postgres.ziti' service that connects to a 'simpledb` database with a simpletable in it
    log.info("Querying simpletable in the postgres database over openziti");
    final String url = "zdbc:postgresql://postgres.ziti/simpledb";

    final Properties props = new Properties();
    // the ziti-jdbc database driver directly supports ziti identities by setting either the keystore and keystore password
    // when using a keystore based identity or the 'zitiIdentityFile' property when using json based identities
    log.info("Attempting to connect to ziti using identity stored in {}", identityFile);
    props.setProperty("zitiKeystore", identityFile);
    props.setProperty("zitiKeystorePassword", "");

    props.setProperty("user", "viewuser");     //  the database read-only username
    props.setProperty("password", "viewpass"); //  the database read-only password
    props.setProperty("connectTimeout", "60");

    // Tell the ZDBC driver to wait for up to one minute for a service named "postgres" to be available
    // in the local SDK before connecting.  This is the service configured with the 'postgres.ziti' intercept
    // in the database url above
    props.setProperty(ZitiDriver.ZITI_WAIT_FOR_SERVICE_NAME, "PostgresDemo");
    props.setProperty(ZitiDriver.ZITI_WAIT_FOR_SERVICE_TIMEOUT, "PT60S");

    try (Connection conn = DriverManager.getConnection(url, props)) {
      try (Statement stmt = conn.createStatement()) {
        try (ResultSet rs = stmt.executeQuery("select * from vets")) {
          while (rs.next()) {
            System.out.println(String.format("Result from database is: %d: %s %s",
                rs.getInt(1), rs.getString(2), rs.getString(3)));
          }
        }
      }
    } catch (final SQLException exception) {
      log.error("IOException on http call received: ", exception);
    }
  }
}
