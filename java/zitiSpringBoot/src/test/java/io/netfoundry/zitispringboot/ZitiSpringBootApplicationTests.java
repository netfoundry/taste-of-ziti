package io.netfoundry.zitispringboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ZitiSpringBootApplicationTests {

  @Autowired
  PetstoreService petstoreService;

  @Test
  void contextLoads() {
  }

  @Test
  void queryPetstore() throws Exception {
    petstoreService.queryPetstore();
  }
}
