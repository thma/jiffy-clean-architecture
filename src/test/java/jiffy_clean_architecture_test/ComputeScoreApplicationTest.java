package jiffy_clean_architecture_test;

import jiffy_clean_architecture.application.CustomerScoreApplication;
import jiffy_clean_architecture.application.CustomerScoreController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = CustomerScoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class ComputeScoreApplicationTest {

  @LocalServerPort
  private int port;

  @Test
  void contextLoads() {
  }

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void testComputeScore() {
    var response = this.restTemplate.getForObject(
        "http://localhost:" + port + "/customers/1/score",
        CustomerScoreController.ScoreResponse.class);
    assertEquals(100, response.getScore());
  }


}
