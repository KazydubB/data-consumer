package consumer.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

// General application configuration
@Configuration
public class ConsumerConfiguration {

    private final Environment env;

    public ConsumerConfiguration(Environment env) {
        this.env = env;
    }

    // Task executor, used to query the endpoint
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        String corePoolSizeValue = env.getProperty("consumer.executor.corePoolSize", "1"); // not the best default value
        executor.setCorePoolSize(Integer.parseInt(corePoolSizeValue));
        String maxPoolSizeValue = env.getProperty("consumer.executor.maxPoolSize", "1");
        executor.setMaxPoolSize(Integer.parseInt(maxPoolSizeValue));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // Wait for all threads to complete on shutdown
        executor.setAwaitTerminationSeconds(Integer.MAX_VALUE);
        executor.setThreadNamePrefix("Consumer-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
