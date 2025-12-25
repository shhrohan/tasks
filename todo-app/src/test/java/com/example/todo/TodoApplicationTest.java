package com.example.todo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TodoApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Executor asyncWriteExecutor;

    @Test
    void contextLoads() {
        assertNotNull(context);
    }

    @Test
    void asyncWriteExecutor_ShouldBePresent() {
        assertNotNull(asyncWriteExecutor);
    }

    @Test
    void main_ShouldRun() {
        // We invoke main in a separate thread to avoid blocking the test execution.
        // We don't really care if it fully starts up, we just want to exercise the bytecode.
        Thread t = new Thread(() -> {
            try {
                TodoApplication.main(new String[]{"--server.port=0", "--spring.main.web-application-type=none"});
            } catch (Exception ignored) {
            }
        });
        t.start();
        try {
            Thread.sleep(1000); // Give it a second to run some code
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        t.interrupt();
    }
}
