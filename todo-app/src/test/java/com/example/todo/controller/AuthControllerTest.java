package com.example.todo.controller;

import com.example.todo.model.User;
import com.example.todo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userService, passwordEncoder);
    }

    @Test
    void login_ShouldAddError_WhenErrorParamPresent() {
        String view = authController.login("true", null, model);
        
        verify(model).addAttribute("error", "Invalid email or password");
        assertEquals("login", view);
    }

    @Test
    void login_ShouldAddLogoutMessage_WhenLogoutParamPresent() {
        String view = authController.login(null, "true", model);
        
        verify(model).addAttribute("message", "You have been logged out successfully");
        assertEquals("login", view);
    }

    @Test
    void login_ShouldDoNothing_WhenParamsNull() {
        String view = authController.login(null, null, model);
        
        verifyNoInteractions(model); // No attributes added
        assertEquals("login", view);
    }

    @Test
    void register_ShouldAddError_WhenErrorParamPresent() {
        String view = authController.register("Some Error", model);
        
        verify(model).addAttribute("error", "Some Error");
        assertEquals("register", view);
    }

    @Test
    void registerUser_ShouldFail_WhenNameIsEmpty() {
        String view = authController.registerUser("", "test@example.com", "password", "password", redirectAttributes);
        
        verify(redirectAttributes).addAttribute("error", "Name is required");
        assertEquals("redirect:/register", view);
    }

    @Test
    void registerUser_ShouldFail_WhenEmailIsInvalid() {
        String view = authController.registerUser("Test", "invalid-email", "password", "password", redirectAttributes);
        
        verify(redirectAttributes).addAttribute("error", "Valid email is required");
        assertEquals("redirect:/register", view);
    }

    @Test
    void registerUser_ShouldFail_WhenPasswordIsShort() {
        String view = authController.registerUser("Test", "test@example.com", "123", "123", redirectAttributes);
        
        verify(redirectAttributes).addAttribute("error", "Password must be at least 6 characters");
        assertEquals("redirect:/register", view);
    }

    @Test
    void registerUser_ShouldFail_WhenPasswordsDoNotMatch() {
        String view = authController.registerUser("Test", "test@example.com", "password123", "password456", redirectAttributes);
        
        verify(redirectAttributes).addAttribute("error", "Passwords do not match");
        assertEquals("redirect:/register", view);
    }

    @Test
    void registerUser_ShouldFail_WhenEmailExists() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(new User()));

        String view = authController.registerUser("Test", "test@example.com", "password123", "password123", redirectAttributes);
        
        verify(redirectAttributes).addAttribute("error", "Email already registered");
        assertEquals("redirect:/register", view);
    }

    @Test
    void registerUser_ShouldSucceed_WhenValid() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        String view = authController.registerUser("Test", "test@example.com", "password123", "password123", redirectAttributes);
        
        verify(userService).createUser(any(User.class));
        verify(redirectAttributes).addFlashAttribute(eq("message"), anyString());
        assertEquals("redirect:/login", view);
    }
    @Test
    void register_ShouldDoNothing_WhenErrorIsNull() {
        String view = authController.register(null, model);
        
        verifyNoInteractions(model);
        assertEquals("register", view);
    }

    @Test
    void registerUser_ShouldFail_WhenNameIsNull() {
        String view = authController.registerUser(null, "test@example.com", "password", "password", redirectAttributes);
        
        verify(redirectAttributes).addAttribute("error", "Name is required");
        assertEquals("redirect:/register", view);
    }

    @Test
    void registerUser_ShouldFail_WhenEmailIsNull() {
        String view = authController.registerUser("Test", null, "password", "password", redirectAttributes);
        
        verify(redirectAttributes).addAttribute("error", "Valid email is required");
        assertEquals("redirect:/register", view);
    }
    
    @Test
    void registerUser_ShouldFail_WhenPasswordIsNull() {
        String view = authController.registerUser("Test", "test@example.com", null, "password", redirectAttributes);
        
        verify(redirectAttributes).addAttribute("error", "Password must be at least 6 characters");
        assertEquals("redirect:/register", view);
    }
}
