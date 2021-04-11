package consumer.service;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import consumer.model.Data;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ClientTest.TestConfig.class)
public class ClientTest {

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @Autowired
    private Client client;

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testGetData() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Data data = new Data();
        data.setBy("By");
        data.setText("Text");
        data.setId(1L);

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("https://hacker-news.firebaseio.com/v0/item/1.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(data))
                );

        Data actual = client.getData(1L);
        mockServer.verify();
        Assertions.assertEquals(data, actual);
    }

    @TestConfiguration
    public static class TestConfig {

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        public Client client(RestTemplate restTemplate) {
            return new Client(restTemplate);
        }
    }
}
