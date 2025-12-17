package com.example.todo.component;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
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

    // ==================== getCoverageGrade Branch Tests ====================

    @Test
    void getCoverageGrade_ShouldReturnAPlusFor90AndAbove() throws Exception {
        StartupLogger logger = createLogger();
        Method method = StartupLogger.class.getDeclaredMethod("getCoverageGrade", double.class);
        method.setAccessible(true);

        assertEquals("A+ (Excellent)", method.invoke(logger, 90.0));
        assertEquals("A+ (Excellent)", method.invoke(logger, 95.0));
        assertEquals("A+ (Excellent)", method.invoke(logger, 100.0));
    }

    @Test
    void getCoverageGrade_ShouldReturnAFor80To89() throws Exception {
        StartupLogger logger = createLogger();
        Method method = StartupLogger.class.getDeclaredMethod("getCoverageGrade", double.class);
        method.setAccessible(true);

        assertEquals("A  (Very Good)", method.invoke(logger, 80.0));
        assertEquals("A  (Very Good)", method.invoke(logger, 85.0));
        assertEquals("A  (Very Good)", method.invoke(logger, 89.9));
    }

    @Test
    void getCoverageGrade_ShouldReturnBFor70To79() throws Exception {
        StartupLogger logger = createLogger();
        Method method = StartupLogger.class.getDeclaredMethod("getCoverageGrade", double.class);
        method.setAccessible(true);

        assertEquals("B  (Good)", method.invoke(logger, 70.0));
        assertEquals("B  (Good)", method.invoke(logger, 75.0));
        assertEquals("B  (Good)", method.invoke(logger, 79.9));
    }

    @Test
    void getCoverageGrade_ShouldReturnCFor60To69() throws Exception {
        StartupLogger logger = createLogger();
        Method method = StartupLogger.class.getDeclaredMethod("getCoverageGrade", double.class);
        method.setAccessible(true);

        assertEquals("C  (Acceptable)", method.invoke(logger, 60.0));
        assertEquals("C  (Acceptable)", method.invoke(logger, 65.0));
        assertEquals("C  (Acceptable)", method.invoke(logger, 69.9));
    }

    @Test
    void getCoverageGrade_ShouldReturnDFor50To59() throws Exception {
        StartupLogger logger = createLogger();
        Method method = StartupLogger.class.getDeclaredMethod("getCoverageGrade", double.class);
        method.setAccessible(true);

        assertEquals("D  (Needs Improvement)", method.invoke(logger, 50.0));
        assertEquals("D  (Needs Improvement)", method.invoke(logger, 55.0));
        assertEquals("D  (Needs Improvement)", method.invoke(logger, 59.9));
    }

    @Test
    void getCoverageGrade_ShouldReturnFForBelow50() throws Exception {
        StartupLogger logger = createLogger();
        Method method = StartupLogger.class.getDeclaredMethod("getCoverageGrade", double.class);
        method.setAccessible(true);

        assertEquals("F  (Poor - Increase Test Coverage!)", method.invoke(logger, 0.0));
        assertEquals("F  (Poor - Increase Test Coverage!)", method.invoke(logger, 25.0));
        assertEquals("F  (Poor - Increase Test Coverage!)", method.invoke(logger, 49.9));
    }

    // Helper method to create a StartupLogger with mocked dependencies
    private StartupLogger createLogger() throws SQLException {
        Environment env = mock(Environment.class);
        DataSource dataSource = mock(DataSource.class);
        when(env.getActiveProfiles()).thenReturn(new String[] { "test" });
        return new StartupLogger(env, dataSource);
    }
}
