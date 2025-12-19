package com.example.todo.service;

import com.example.todo.dao.UserDAO;
import com.example.todo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();
    }

    @Test
    void getUserByEmail_ShouldReturnUser() {
        when(userDAO.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUserByEmail("test@example.com");

        assertTrue(result.isPresent());
    }

    @Test
    void createUser_ShouldSaveAndReturnUser() {
        when(userDAO.save(testUser)).thenReturn(testUser);

        User result = userService.createUser(testUser);

        assertEquals(testUser, result);
        verify(userDAO).save(testUser);
    }

    @Test
    void getOrCreateDefaultUser_ShouldReturnExisting_WhenUserExists() {
        when(userDAO.findByEmail("shah.rohan@microsoft.com")).thenReturn(Optional.of(testUser));

        User result = userService.getOrCreateDefaultUser();

        assertEquals(testUser, result);
        verify(userDAO, never()).save(any());
    }

    @Test
    void getOrCreateDefaultUser_ShouldCreateNew_WhenUserNotExists() {
        when(userDAO.findByEmail("shah.rohan@microsoft.com")).thenReturn(Optional.empty());
        when(userDAO.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.getOrCreateDefaultUser();

        assertEquals("rohan", result.getName());
        assertEquals("shah.rohan@microsoft.com", result.getEmail());
        verify(userDAO).save(any(User.class));
    }
}

