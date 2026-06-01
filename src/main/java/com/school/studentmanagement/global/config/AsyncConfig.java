package com.school.studentmanagement.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @Async 비동기 실행기 설정. 알림 팬아웃 등 백그라운드 작업에 사용된다.
 * 기본 SimpleAsyncTaskExecutor(요청마다 새 스레드) 대신 풀을 사용해 자원 사용을 통제한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    // @Async가 별도 한정자 없이 사용할 기본 실행기 (빈 이름 "taskExecutor")
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
