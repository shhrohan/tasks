package com.example.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfig - Registers custom interceptors for Spring MVC.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CacheLoggingInterceptor cacheLoggingInterceptor;

    public WebMvcConfig(CacheLoggingInterceptor cacheLoggingInterceptor) {
        this.cacheLoggingInterceptor = cacheLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cacheLoggingInterceptor)
                .addPathPatterns("/", "/api/**");
    }
}
