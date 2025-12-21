package com.example.todo.base;

import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test@example.com", roles = "USER")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;
    
    @Autowired
    protected CacheManager cacheManager;

    protected User testUser;

    @BeforeEach
    void setUpTestUser() {
        // Clear all caches before each test to ensure clean state
        // This is important because write-through cache may contain stale data
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        
        // Create or find the test user that matches @WithMockUser
        testUser = userRepository.findByEmail("test@example.com")
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail("test@example.com");
                    user.setName("Test User");
                    user.setPasswordHash("$2a$10$test");
                    return userRepository.save(user);
                });
    }
}
