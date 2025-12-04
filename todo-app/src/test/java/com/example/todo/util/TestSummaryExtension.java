package com.example.todo.util;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class TestSummaryExtension implements TestWatcher {

    private static final AtomicLong totalTests = new AtomicLong(0);
    private static final AtomicLong successTests = new AtomicLong(0);
    private static final AtomicLong failedTests = new AtomicLong(0);
    private static final AtomicLong abortedTests = new AtomicLong(0);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            String summary = "\n=======================================================\n" +
                    "                 TEST EXECUTION SUMMARY\n" +
                    "=======================================================\n" +
                    "Total Tests Run : " + totalTests.get() + "\n" +
                    "Succeeded       : " + successTests.get() + "\n" +
                    "Failed          : " + failedTests.get() + "\n" +
                    "Aborted         : " + abortedTests.get() + "\n" +
                    "=======================================================\n";

            System.out.println(summary);
            System.err.println(summary);
        }));
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        // Disabled tests are not counted as run in this summary, or we can add a
        // counter.
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        totalTests.incrementAndGet();
        successTests.incrementAndGet();
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        totalTests.incrementAndGet();
        abortedTests.incrementAndGet();
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        totalTests.incrementAndGet();
        failedTests.incrementAndGet();
    }
}
