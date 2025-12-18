package com.example.todo.service;

import com.example.todo.dao.UserDAO;
import com.example.todo.model.User;
import org.springframework.stereotype.Service;


import java.util.Optional;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
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
        return userDAO.findByEmail(defaultEmail)
                .orElseGet(() -> {
                    log.info("Creating default user: {}", defaultEmail);
                    User defaultUser = User.builder()
                            .name("rohan")
                            .email(defaultEmail)
                            .build();
                    return userDAO.save(defaultUser);
                });
    }
}
