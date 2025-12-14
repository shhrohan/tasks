package com.example.todo.component;

import com.example.todo.model.User;
import com.example.todo.repository.SwimLaneRepository;
import com.example.todo.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
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

    public DataInitializer(UserRepository userRepository, SwimLaneRepository swimLaneRepository) {
        this.userRepository = userRepository;
        this.swimLaneRepository = swimLaneRepository;
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
                            .build();
                    return userRepository.save(user);
                });

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
