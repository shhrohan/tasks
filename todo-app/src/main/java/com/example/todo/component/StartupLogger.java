package com.example.todo.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;

@Component
public class StartupLogger implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private final Environment env;
    private final DataSource dataSource;

    public StartupLogger(Environment env, DataSource dataSource) {
        this.env = env;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        log.info("==========================================================================================");
        log.info("Active Profile: {}", Arrays.toString(env.getActiveProfiles()));
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            log.info("Database URL:   {}", metaData.getURL());
            log.info("Database User:  {}", metaData.getUserName());
        } catch (SQLException e) {
            log.error("Failed to fetch database connection details", e);
        }

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        log.info("Memory Stats: Max={}MB, Total={}MB, Free={}MB, Used={}MB",
                maxMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                freeMemory / (1024 * 1024),
                usedMemory / (1024 * 1024));
        log.info("Available Processors: {}", runtime.availableProcessors());
        
        log.info("==========================================================================================");
    }
}
