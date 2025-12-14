package com.example.todo.component;

import com.example.todo.model.SwimLane;
import com.example.todo.model.User;
import com.example.todo.repository.SwimLaneRepository;
import com.example.todo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SwimLaneRepository swimLaneRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments args;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(userRepository, swimLaneRepository, passwordEncoder);
    }

    @Test
    void run_ShouldCreateNewUser_WhenUserNotExists() {
        when(userRepository.findByEmail("shah.rohan@microsoft.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(swimLaneRepository.findAll()).thenReturn(Collections.emptyList());

        dataInitializer.run(args);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void run_ShouldSetPassword_WhenUserExistsWithNullPassword() {
        User existingUser = User.builder()
                .id(1L)
                .name("rohan")
                .email("shah.rohan@microsoft.com")
                .passwordHash(null) // No password set
                .build();

        when(userRepository.findByEmail("shah.rohan@microsoft.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(swimLaneRepository.findAll()).thenReturn(Collections.emptyList());

        dataInitializer.run(args);

        verify(userRepository).save(existingUser);
        verify(passwordEncoder).encode(anyString());
    }

    @Test
    void run_ShouldSetPassword_WhenUserExistsWithEmptyPassword() {
        User existingUser = User.builder()
                .id(1L)
                .name("rohan")
                .email("shah.rohan@microsoft.com")
                .passwordHash("") // Empty password
                .build();

        when(userRepository.findByEmail("shah.rohan@microsoft.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(swimLaneRepository.findAll()).thenReturn(Collections.emptyList());

        dataInitializer.run(args);

        verify(userRepository).save(existingUser);
    }

    @Test
    void run_ShouldNotSetPassword_WhenUserExistsWithPassword() {
        User existingUser = User.builder()
                .id(1L)
                .name("rohan")
                .email("shah.rohan@microsoft.com")
                .passwordHash("existingHashedPassword") // Password already set
                .build();

        when(userRepository.findByEmail("shah.rohan@microsoft.com")).thenReturn(Optional.of(existingUser));
        when(swimLaneRepository.findAll()).thenReturn(Collections.emptyList());

        dataInitializer.run(args);

        // Should not call passwordEncoder since password already exists
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_ShouldAssociateOrphanSwimlanes() {
        User existingUser = User.builder()
                .id(1L)
                .name("rohan")
                .email("shah.rohan@microsoft.com")
                .passwordHash("hashedPassword")
                .build();

        SwimLane orphanLane = SwimLane.builder()
                .id(1L)
                .name("Orphan Lane")
                .user(null) // No user assigned
                .build();

        when(userRepository.findByEmail("shah.rohan@microsoft.com")).thenReturn(Optional.of(existingUser));
        when(swimLaneRepository.findAll()).thenReturn(Arrays.asList(orphanLane));
        when(swimLaneRepository.save(any(SwimLane.class))).thenReturn(orphanLane);

        dataInitializer.run(args);

        verify(swimLaneRepository).save(orphanLane);
    }

    @Test
    void run_ShouldNotAssociateSwimlanes_WhenAlreadyAssigned() {
        User existingUser = User.builder()
                .id(1L)
                .name("rohan")
                .email("shah.rohan@microsoft.com")
                .passwordHash("hashedPassword")
                .build();

        SwimLane assignedLane = SwimLane.builder()
                .id(1L)
                .name("Assigned Lane")
                .user(existingUser) // Already assigned
                .build();

        when(userRepository.findByEmail("shah.rohan@microsoft.com")).thenReturn(Optional.of(existingUser));
        when(swimLaneRepository.findAll()).thenReturn(Arrays.asList(assignedLane));

        dataInitializer.run(args);

        // Should not save already assigned lanes
        verify(swimLaneRepository, never()).save(any(SwimLane.class));
    }
}
