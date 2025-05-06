package io.netfoundry.zitispringboot;

import org.openziti.springboot.client.web.config.EnableZitiHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableZitiHttpClient
public class ZitiSpringBootApplication {

  public static void main(String[] args) {
    SpringApplication.run(ZitiSpringBootApplication.class, args);
  }

}
