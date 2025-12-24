package com.example.todo.service;

import com.example.todo.dao.UserDAO;
import com.example.todo.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class UserService {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> getUserByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    public User createUser(User user) {
        log.info("Creating user: {}", user.getEmail());
        return userDAO.save(user);
    }

    /**
     * Gets or creates the default user for the application.
     * This is used during initial setup and for associating orphan swimlanes.
     */
    public User getOrCreateDefaultUser() {
        String defaultEmail = "shah.rohan@microsoft.com";
        User user = userDAO.findByEmail(defaultEmail)
                .orElseGet(() -> {
                    log.info("Creating default user: {}", defaultEmail);
                    User defaultUser = User.builder()
                            .name("rohan")
                            .email(defaultEmail)
                            .build();
                    return userDAO.save(defaultUser);
                });
        
        // Ensure password is reset to 123123 for the user
        log.info("Resetting password for default user: {}", defaultEmail);
        user.setPasswordHash(passwordEncoder.encode("123123"));
        return userDAO.save(user);
    }

    public User updateUser(String email, String newName) {
        log.info("Updating user name for email: {} to {}", email, newName);
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        user.setName(newName);
        return userDAO.save(user);
    }
}
