package com.example.todo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * This configuration overrides the asyncWriteExecutor with a SyncTaskExecutor during tests.
 * This is crucial because integration tests run in a transaction that is normally not committed 
 * when the async thread attempts to read/write data, leading to JpaObjectRetrievalFailureException.
 * By making it synchronous, the "async" work happens in the same thread and transaction.
 */
@TestConfiguration
public class TestAsyncConfig {

    @Bean(name = "asyncWriteExecutor")
    @Primary
    public TaskExecutor asyncWriteExecutor() {
        return new SyncTaskExecutor();
    }
}
