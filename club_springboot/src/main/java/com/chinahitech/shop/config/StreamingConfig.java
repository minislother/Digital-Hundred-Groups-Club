package com.chinahitech.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 为长连接设置有界线程池，避免模型响应缓慢时无限创建线程。 */
@Configuration
public class StreamingConfig implements WebMvcConfigurer {
    @Bean
    public ThreadPoolTaskExecutor knowledgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("knowledge-stream-");
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(knowledgeExecutor()).setDefaultTimeout(190000);
    }
}
