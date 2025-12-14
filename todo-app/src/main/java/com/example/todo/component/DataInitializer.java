package com.example.todo.component;

import com.example.todo.model.User;
import com.example.todo.repository.SwimLaneRepository;
import com.example.todo.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.log4j.Log4j2;

/**
 * Initializes default data on application startup.
 * Creates default user and associates orphan swimlanes.
 */
@Component
@Order(1) // Run before other runners
@Log4j2
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final SwimLaneRepository swimLaneRepository;
    private final PasswordEncoder passwordEncoder;

    // Default password for initial setup - users should change this
    private static final String DEFAULT_PASSWORD = "123123";

    public DataInitializer(UserRepository userRepository, SwimLaneRepository swimLaneRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.swimLaneRepository = swimLaneRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Running data initialization...");
        
        // Create or find default user
        User defaultUser = userRepository.findByEmail("shah.rohan@microsoft.com")
                .orElseGet(() -> {
                    log.info("Creating default user: rohan (shah.rohan@microsoft.com)");
                    User user = User.builder()
                            .name("rohan")
                            .email("shah.rohan@microsoft.com")
                            .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                            .build();
                    return userRepository.save(user);
                });

        // Ensure user has a password set (only if null or empty)
        if (defaultUser.getPasswordHash() == null || defaultUser.getPasswordHash().isEmpty()) {
            log.info("Setting default password for user: {}", defaultUser.getEmail());
            defaultUser.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            userRepository.save(defaultUser);
        }

        log.info("Default user ID: {}", defaultUser.getId());

        // Associate orphan swimlanes with default user
        long updated = swimLaneRepository.findAll().stream()
                .filter(lane -> lane.getUser() == null)
                .peek(lane -> {
                    lane.setUser(defaultUser);
                    swimLaneRepository.save(lane);
                })
                .count();

        if (updated > 0) {
            log.info("Associated {} orphan swimlanes with default user", updated);
        }
        
        log.info("Data initialization complete.");
    }
}

