package io.paulbaker.mitm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.littleshoot.proxy.HttpProxyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringMitmApplication.class)
public class SpringMitmApplicationTests {

  @Autowired
  private HttpProxyServer httpProxyServer;

  @Test
  public void contextLoads() {
    while (true) {

    }
  }

}
