package come.back.gotoday.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CrowdRefreshExecutorConfig {

    @Bean(name = "crowdRefreshTaskExecutor")
    public TaskExecutor crowdRefreshTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("crowd-refresh-");
        executor.initialize();
        return executor;
    }
}