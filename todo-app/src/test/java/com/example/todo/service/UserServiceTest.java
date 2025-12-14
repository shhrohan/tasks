package com.example.todo.service;

import com.example.todo.dao.UserDAO;
import com.example.todo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
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
    void getAllUsers_ShouldReturnAllUsers() {
        when(userDAO.findAll()).thenReturn(Arrays.asList(testUser));

        List<User> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
    }

    @Test
    void getUser_ShouldReturnUser_WhenExists() {
        when(userDAO.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUser(1L);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
    }

    @Test
    void getUser_ShouldReturnEmpty_WhenNotExists() {
        when(userDAO.findById(999L)).thenReturn(Optional.empty());

        Optional<User> result = userService.getUser(999L);

        assertFalse(result.isPresent());
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
    void updateUser_ShouldUpdateExistingUser() {
        User updatedData = User.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        when(userDAO.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDAO.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateUser(1L, updatedData);

        assertEquals("Updated Name", testUser.getName());
        assertEquals("updated@example.com", testUser.getEmail());
        verify(userDAO).save(testUser);
    }

    @Test
    void updateUser_ShouldThrowException_WhenUserNotFound() {
        when(userDAO.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
                userService.updateUser(999L, testUser));
    }

    @Test
    void deleteUser_ShouldCallDeleteById() {
        userService.deleteUser(1L);

        verify(userDAO).deleteById(1L);
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
