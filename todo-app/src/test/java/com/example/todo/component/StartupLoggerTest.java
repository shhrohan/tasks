package com.example.todo.component;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class StartupLoggerTest {

    @Test
    void run_ShouldLogStartupInfo() throws SQLException {
        Environment env = mock(Environment.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(env.getActiveProfiles()).thenReturn(new String[] { "test" });
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn("jdbc:h2:mem:test");
        when(metaData.getUserName()).thenReturn("sa");

        StartupLogger logger = new StartupLogger(env, dataSource);

        assertDoesNotThrow(() -> logger.run());
    }

    @Test
    void run_ShouldHandleDatabaseConnectionError() throws SQLException {
        Environment env = mock(Environment.class);
        DataSource dataSource = mock(DataSource.class);

        when(env.getActiveProfiles()).thenReturn(new String[] { "test" });
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        StartupLogger logger = new StartupLogger(env, dataSource);

        // Should not throw, should log error
        assertDoesNotThrow(() -> logger.run());
    }

    @Test
    void run_ShouldHandleEmptyProfiles() throws SQLException {
        Environment env = mock(Environment.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(env.getActiveProfiles()).thenReturn(new String[] {});
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn("jdbc:h2:mem:test");
        when(metaData.getUserName()).thenReturn("sa");

        StartupLogger logger = new StartupLogger(env, dataSource);

        assertDoesNotThrow(() -> logger.run());
    }
}
