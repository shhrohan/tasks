package com.example.todo.controller;

import com.example.todo.base.BaseIntegrationTest;
import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @WithAnonymousUser
    void login_ShouldRenderLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @WithAnonymousUser
    void login_WithError_ShouldShowErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithAnonymousUser
    void login_WithLogout_ShouldShowLogoutMessage() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    @WithAnonymousUser
    void register_ShouldRenderRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @WithAnonymousUser
    void register_WithError_ShouldShowErrorMessage() throws Exception {
        mockMvc.perform(get("/register").param("error", "Some error"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithAnonymousUser
    void registerUser_ShouldCreateUserAndRedirect() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Test User")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithAnonymousUser
    void registerUser_WithEmptyName_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/register?error=*"));
    }

    @Test
    @WithAnonymousUser
    void registerUser_WithInvalidEmail_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Test")
                        .param("email", "invalid-email")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/register?error=*"));
    }

    @Test
    @WithAnonymousUser
    void registerUser_WithShortPassword_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Test")
                        .param("email", "test@example.com")
                        .param("password", "12345")
                        .param("confirmPassword", "12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/register?error=*"));
    }

    @Test
    @WithAnonymousUser
    void registerUser_WithMismatchedPasswords_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Test")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "different123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/register?error=*"));
    }

    @Test
    @WithAnonymousUser
    void registerUser_WithExistingEmail_ShouldRedirectWithError() throws Exception {
        // Create existing user
        User existingUser = User.builder()
                .name("Existing")
                .email("existing@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .build();
        userRepository.save(existingUser);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "New User")
                        .param("email", "existing@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/register?error=*"));
    }
}
