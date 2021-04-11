package consumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import consumer.model.Data;

@Service
public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private static final String URL_TEMPLATE = "https://hacker-news.firebaseio.com/v0/item/%d.json";

    private final RestTemplate restTemplate;

    public Client(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieve entity from {@code https://hacker-news.firebaseio.com/v0/item/{id}.json URL} identified by {@code id param}.
     * The entity is either 'comment' or a 'story' as indicated by its {@link Data#getType()} field.
     * @param id identifier of the entity
     * @return converted response
     */
    public Data getData(long id) {
        logger.info("Retrieving data with id {}", id);
        String url = String.format(URL_TEMPLATE, id);
        return restTemplate.getForObject(url, Data.class);
    }
}
