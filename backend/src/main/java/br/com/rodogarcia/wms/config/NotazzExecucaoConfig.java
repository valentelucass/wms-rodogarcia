package br.com.rodogarcia.wms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAsync
public class NotazzExecucaoConfig {
    @Bean("notazzExecutor")
    org.springframework.core.task.AsyncTaskExecutor notazzExecutor(
            @org.springframework.beans.factory.annotation.Qualifier("notazzPool")
                    ThreadPoolTaskExecutor executor) {
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }

    @Bean("notazzPool")
    ThreadPoolTaskExecutor notazzPool() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("wms-notazz-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }
}
