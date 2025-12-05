package com.example.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@org.springframework.scheduling.annotation.EnableAsync
@SpringBootApplication
@EnableScheduling
public class TodoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }

    @org.springframework.context.annotation.Bean(name = "asyncWriteExecutor")
    public java.util.concurrent.Executor asyncWriteExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("AsyncDB-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void logMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TodoApplication.class);
        log.info("==========================================================================================");
        log.info("Memory Stats: Max={}MB, Total={}MB, Free={}MB, Used={}MB",
                maxMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                freeMemory / (1024 * 1024),
                usedMemory / (1024 * 1024));
        log.info("Available Processors: {}", runtime.availableProcessors());
        log.info("==========================================================================================");
    }
}
