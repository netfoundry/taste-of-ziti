package io.netfoundry.zitispringboot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PetstoreService {

  private final CloseableHttpClient zitiHttpClient;

  private static String petstoreQuery = "/api/v3/pet/findByStatus?status=available";

  public PetstoreService(CloseableHttpClient zitiHttpClient) {
    this.zitiHttpClient = zitiHttpClient;
  }

  public void queryPetstore() throws URISyntaxException, IOException {
    HttpGet httpGet = new HttpGet(new URI(String.format("https://%s:%d%s", "petstore.demo", 443, petstoreQuery)));
    zitiHttpClient.execute(httpGet, response -> {
      log.info("Reading response");
      if (response.getCode() == 200) {
        log.info("--- {}", EntityUtils.toString(response.getEntity()));
      } else {
        log.error("Response code {} received", response.getCode());
      }
      return response;
    });

  }
}
