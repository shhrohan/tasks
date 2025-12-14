package com.example.todo.service;

import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

import java.util.Collections;

@Service
@Log4j2
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Attempting to load user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found: " + email);
                });

        log.info("User found: {} (id={}), passwordHash exists: {}", 
                 user.getEmail(), user.getId(), user.getPasswordHash() != null);

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash() != null ? user.getPasswordHash() : "",
                Collections.emptyList()
        );
    }
}

