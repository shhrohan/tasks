package com.example.todo.dao;

import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDAOTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDAO userDAO;

    @Test
    void findAll_ShouldCallRepository() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        userDAO.findAll();
        verify(userRepository).findAll();
    }

    @Test
    void findById_ShouldCallRepository() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        userDAO.findById(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    void findByEmail_ShouldCallRepository() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        userDAO.findByEmail("test@test.com");
        verify(userRepository).findByEmail("test@test.com");
    }

    @Test
    void save_ShouldCallRepository() {
        User user = new User();
        when(userRepository.save(user)).thenReturn(user);
        userDAO.save(user);
        verify(userRepository).save(user);
    }

    @Test
    void deleteById_ShouldCallRepository() {
        userDAO.deleteById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void existsByEmail_ShouldCallRepository() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);
        assertTrue(userDAO.existsByEmail("test@test.com"));
        verify(userRepository).existsByEmail("test@test.com");
    }
}
