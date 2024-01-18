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

package com.example.demoutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.openziti.identity.Enroller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AperitivoUtils {
  public static final String DEFAULT_ZITI_IDENTITY_FILE = "taste_of_ziti.pkcs12";
  private static final Logger log = LoggerFactory.getLogger(AperitivoUtils.class);

  private AperitivoUtils() {
    // utility class.  No public constructor needed
  }

  /**
   * Aperitivo is a demo service hosted by NetFoundry that generates temporary Identities that have access to the services
   * on the demo CloudZiti network. Normally, Identity generation and configuration of access to services would be more tightly controlled
   * within a production network.  For the Taste-of-Ziti demo network, Aperitivo simplifies this aspect of an OpenZiti network and
   * allows the developer to just explore the connectivity aspects of OpenZiti.
   * @param aperitivoUri the address of the Aperitivo server
   * @return the file name of the generated keystore containing the temporary Identity
   */
  public static String createIdentityKeystore(final String aperitivoUri) {
    try {
      log.info("Connecting to aperitivo at {} to generate a new identity", aperitivoUri);
      final Request request = new Request.Builder()
          .url(String.format("%s/aperitivo/v1/identities", aperitivoUri))
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
          // calling load with first parameter set to null creates an empty keystore
          // Using an empty password here for the demo.  But in practice, the keystore should have secure password and not hard-coded
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
          // The Enroller writes the enrolled Identity into the provided keystore
          Enroller.fromJWT(identity.get("jwt").asText()).enroll(null, ks, ""); // alias name (3rd param) not used during enroll
          final File identityFile = new File(DEFAULT_ZITI_IDENTITY_FILE);
          try (final FileOutputStream fos = new FileOutputStream(identityFile)) {
            ks.store(fos, "".toCharArray());
          }
          final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
          final Instant validUntil = Instant.parse(identity.get("validUntil").asText());
          log.info("A new identity is stored at {}. This is a temporary identity that is valid until {}",
                 identityFile.getAbsolutePath(), formatter.format(validUntil));
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
}
