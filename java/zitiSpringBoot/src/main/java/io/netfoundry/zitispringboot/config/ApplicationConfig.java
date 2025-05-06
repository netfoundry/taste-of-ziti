package io.netfoundry.zitispringboot.config;

import static io.netfoundry.zitispringboot.demoutils.AperitivoUtils.DEFAULT_ZITI_IDENTITY_FILE;
import java.io.File;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.openziti.Ziti;
import org.openziti.ZitiContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.netfoundry.zitispringboot.demoutils.AperitivoUtils;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ApplicationConfig {
  private final String apertivoUrl;

  @Autowired
  public ApplicationConfig(@Value("${io.openziti.taste-of-ziti.apertivoUrl:}") String apertivoUrl) {
    this.apertivoUrl = apertivoUrl;
  }

  @Bean
  public ZitiContext zitiContext() {
    if (new File(DEFAULT_ZITI_IDENTITY_FILE).exists()) {
      return loadIdentity(DEFAULT_ZITI_IDENTITY_FILE);
    }
    return loadIdentity(AperitivoUtils.createIdentityKeystore(apertivoUrl));
  }

  @Bean("zitiTlsSocketStrategy")
  public TlsSocketStrategy zitiTlsSocketStrategy() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    return new DefaultClientTlsStrategy(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build(),
        HostnameVerificationPolicy.CLIENT, NoopHostnameVerifier.INSTANCE);
  }

  private ZitiContext loadIdentity(final String identityFile) {
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

}
