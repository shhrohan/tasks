package com.example.todo.controller;

import com.example.todo.model.User;
import com.example.todo.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Log4j2
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody String newName) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Request to update name for user: {} to {}", email, newName);

        String cleanName = newName;
        if (newName != null && newName.startsWith("\"") && newName.endsWith("\"")) {
            cleanName = newName.substring(1, newName.length() - 1);
        }

        try {
            User updated = userService.updateUser(email, cleanName);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update user name for {}", email, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
