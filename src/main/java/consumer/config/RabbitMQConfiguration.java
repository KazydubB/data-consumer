package consumer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableRabbit
public class RabbitMQConfiguration {

    public static final String QUEUE_COMMENT = "queue.comment";
    public static final String QUEUE_STORY = "queue.story";
    public static final String TOPIC_EXCHANGE_NAME = "exchange";
    public static final String ROUTING_KEY_COMMENT = "comment";
    public static final String ROUTING_KEY_STORY = "story";

    private final Environment env;

    public RabbitMQConfiguration(Environment env) {
        this.env = env;
    }

    // Defines Queue for Comment type obtained from endpoint
    @Bean
    public Queue queueComment() {
        return new Queue(QUEUE_COMMENT, false);
    }

    // Defines Queue for Story type obtained from endpoint
    @Bean
    public Queue queueStory() {
        return new Queue(QUEUE_STORY, false);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }

    @Bean
    public Binding bindingComment(Queue queueComment, TopicExchange exchange) {
        return BindingBuilder.bind(queueComment).to(exchange).with(ROUTING_KEY_COMMENT);
    }

    @Bean
    public Binding bindingStory(Queue queueStory, TopicExchange exchange) {
        return BindingBuilder.bind(queueStory).to(exchange).with(ROUTING_KEY_STORY);
    }

    // Defines container factory which creates listener containers supporting batch messaging. This is useful because
    // it makes it easier to save entities in batches (after data is retrieved from endpoint, it is then passed to
    // the Queue as a MongoDB document; then the entities are consumed from the Queue and are persisted in batch rather
    // than individually).
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        // the actual parameter values are not validated for brevity
        String batchSizeValue = env.getProperty("consumer.rabbitmq.batchSize", "50");
        String concurrentConsumersValue = env.getProperty("consumer.rabbitmq.concurrentConsumers", "1");
        String receiveTimeoutValue = env.getProperty("consumer.rabbitmq.receiveTimeout", "1000");

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(Integer.parseInt(batchSizeValue));
        factory.setConcurrentConsumers(Integer.parseInt(concurrentConsumersValue));
        factory.setReceiveTimeout(Long.parseLong(receiveTimeoutValue));
        return factory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
