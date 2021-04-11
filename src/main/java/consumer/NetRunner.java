package consumer;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import consumer.config.RabbitMQConfiguration;
import consumer.model.Data;
import consumer.service.Client;

// Runner responsible for `consume` command. This runner consumes data at most 1 time to keep things simple (otherwise
// some other mechanism should be introduced to keep track of duplicated values in case of multiple consumes).
// (Do note though, that with the current implementation if the check is removed, duplicate values, and those that are
// in batch with such, will not be persisted to the DB because of the externalId Unique constraint.)
@Component
public class NetRunner implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NetRunner.class);

    private final Client client;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RabbitAdmin rabbitAdmin;
    private final Environment env;

    // a simple variable that tracks whether the NetRunner has already consumed data; is not persisted.
    private boolean isDone = false;

    public NetRunner(Client client, ThreadPoolTaskExecutor taskExecutor, RabbitAdmin rabbitAdmin, Environment env) {
        this.client = client;
        this.taskExecutor = taskExecutor;
        this.rabbitAdmin = rabbitAdmin;
        this.env = env;
    }

    @Override
    public void run() {

        if (isDone) {
            logger.info("The NetRunner has already consumed the data.");
            return;
        }

        // Start the clock
        long start = System.currentTimeMillis();
        AtomicLong counter = new AtomicLong(0);

        final RabbitTemplate rabbitTemplate = rabbitAdmin.getRabbitTemplate();

        String startIDValue = env.getProperty("consumer.netrunner.startID", "0");
        final long startID = Long.parseLong(startIDValue);
        String retrieveCountValue = env.getProperty("consumer.netrunner.retrieveCount", "1000");
        final long count = Long.parseLong(retrieveCountValue);
        final long lastId = count + startID;
        // Retrieve the entities asynchronously, using ThreadPoolTaskExecutor configured in ConsumerConfiguration.
        // Async approach is adequate in the case because there is a need to fetch many entries each from its own endpoint.
        // Thread pool fits the task well, because there is a need to use multiple threads and this threads are going to
        // be re-used instead of creating new ones.
        // As an alternative, one may use Java's standard ThreadPoolExecutor (e.g., Executor#newFixedThreadPool(int))
        // with ExecutorService interface. Spring's ThreadPoolTaskExecutor was chosen for the task because of the ease
        // of bootstrapping it as a Spring @Bean.
        for (long i = startID; i < lastId; i++) {
            long id = i;
            taskExecutor.execute(() -> {

                Data result;
                try {
                    result = client.getData(id);
                } catch (RestClientException e) {
                    // End the run.
                    logger.warn("Entity {} is not retrieved, re-try...", id);
                    // But first, let's re-try one more time
                    // (This is a some-what naive re-try approach, but it is very likely to work.
                    // If it doesn't - the entity will not be retrieved.)
                    result = client.getData(id); // For brevity, in case of failure there is no exception handling
                }

                Serializable entity;
                String routingKey;
                Data.Type type = result.getType();
                // Convert the result to appropriate entity and set appropriate routing key
                if (type == Data.Type.COMMENT) {
                    entity = result.convertToComment();
                    routingKey = RabbitMQConfiguration.ROUTING_KEY_COMMENT;
                } else if (type == Data.Type.STORY) {
                    entity = result.convertToStory();
                    routingKey = RabbitMQConfiguration.ROUTING_KEY_STORY;
                } else {
                    throw new IllegalArgumentException("Unexpected type encountered: " + result.getType());
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Sending entity to queue (routingKey={})", routingKey);
                }
                // send the entity to dedicated RabbitMQ queue in order to free the current thread and let the
                // queue consumer handle the processing of entities.
                // Sent entities are persisted by queue consumer in batches, to avoid redundant DB calls.
                rabbitTemplate.convertAndSend(RabbitMQConfiguration.TOPIC_EXCHANGE_NAME, routingKey, entity);

                counter.incrementAndGet();
            });
        }

        // wait for all threads to finish
        taskExecutor.shutdown();

        isDone = true;

        logger.info("Elapsed time, ms: " + (System.currentTimeMillis() - start));
        logger.info("retrieved " + counter.get() + " rows");
    }

    public void clearQueues() {
        logger.info("Clearing queues");
        rabbitAdmin.purgeQueue(RabbitMQConfiguration.QUEUE_COMMENT, false);
        rabbitAdmin.purgeQueue(RabbitMQConfiguration.QUEUE_STORY, false);
    }
}
