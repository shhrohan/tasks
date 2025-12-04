package com.example.todo.util;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class TestSummaryExtension implements TestWatcher, BeforeAllCallback {

    private static final AtomicLong totalTests = new AtomicLong(0);
    private static final AtomicLong successTests = new AtomicLong(0);
    private static final AtomicLong failedTests = new AtomicLong(0);
    private static final AtomicLong abortedTests = new AtomicLong(0);
    private static final long startTime = System.currentTimeMillis();

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot().getStore(Namespace.create(TestSummaryExtension.class))
                .getOrComputeIfAbsent("summary-printer", key -> new SummaryPrinter());
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
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

    private static class SummaryPrinter implements CloseableResource {
        @Override
        public void close() {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("\n=======================================================");
            System.out.println("                 TEST EXECUTION SUMMARY");
            System.out.println("=======================================================");
            System.out.println("Total Tests Run : " + totalTests.get());
            System.out.println("Succeeded       : " + successTests.get());
            System.out.println("Failed          : " + failedTests.get());
            System.out.println("Aborted         : " + abortedTests.get());
            System.out.println("Total Time      : " + duration + " ms");
            System.out.println("=======================================================\n");
        }
    }
}
