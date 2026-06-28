package come.back.gotoday.payment.subscription.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SubscriptionSchedulerConfig {

    @Value("${thread.batch.core:4}")
    private int corePoolSize;

    @Value("${thread.batch.max:10}")
    private int maxPoolSize;

    @Bean(name = "schedulerTaskExecutor")
    public TaskExecutor schedulerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("SubSchedulerThread-");
        executor.initialize();
        return executor;
    }
}