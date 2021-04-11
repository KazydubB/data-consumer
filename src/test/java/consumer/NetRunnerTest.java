package consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import consumer.config.RabbitMQConfiguration;
import consumer.model.Comment;
import consumer.model.Data;
import consumer.service.Client;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NetRunnerTest.TestConfig.class)
@PropertySource("classpath:application-test.properties")
public class NetRunnerTest {

    @Autowired
    private NetRunner netRunner;
    @Autowired
    private Client client;
    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private Environment env;

    @Test
    public void testRun() {
        Data commentData = new Data();
        commentData.setType(Data.Type.COMMENT);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        when(client.getData(anyLong())).thenReturn(commentData);
        when(rabbitAdmin.getRabbitTemplate()).thenReturn(rabbitTemplate);
        doNothing().when(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfiguration.TOPIC_EXCHANGE_NAME), anyString(), any(Comment.class));

        netRunner.run();

        String countValue = env.getProperty("consumer.netrunner.retrieveCount", "1000");
        int expectedCountOfInvocations = Integer.parseInt(countValue);
        verify(client, times(expectedCountOfInvocations)).getData(anyLong());
        verify(rabbitTemplate, times(expectedCountOfInvocations)).convertAndSend(
                eq(RabbitMQConfiguration.TOPIC_EXCHANGE_NAME), eq(RabbitMQConfiguration.ROUTING_KEY_COMMENT), any(Comment.class));
        verify(rabbitTemplate, never()).convertAndSend(
                eq(RabbitMQConfiguration.TOPIC_EXCHANGE_NAME), eq(RabbitMQConfiguration.ROUTING_KEY_STORY), any(Comment.class));
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public ThreadPoolTaskExecutor taskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(4);
            executor.setMaxPoolSize(8);
            executor.setWaitForTasksToCompleteOnShutdown(true);
            // Wait for all threads to complete on shutdown
            executor.setAwaitTerminationSeconds(Integer.MAX_VALUE);
            executor.setThreadNamePrefix("ConsumerTest-");
            executor.initialize();
            return executor;
        }

        @Bean
        public Client client() {
            return Mockito.mock(Client.class);
        }

        @Bean
        public RabbitAdmin rabbitAdmin() {
            return Mockito.mock(RabbitAdmin.class);
        }

        @Bean
        public NetRunner netRunner(Client client, ThreadPoolTaskExecutor taskExecutor,
                                   RabbitAdmin rabbitAdmin, Environment env) {
            return new NetRunner(client, taskExecutor, rabbitAdmin, env);
        }
    }
}
