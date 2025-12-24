package com.example.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * WebMvcConfig - Configures Spring MVC for cache headers and interceptors.
 * 
 * PERFORMANCE OPTIMIZATION:
 * Adds Cache-Control headers to static resources for efficient browser caching.
 * This improves Lighthouse "Use efficient cache lifetimes" score.
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cache CSS files for 1 year (immutable with versioned filenames)
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());

        // Cache JS files for 1 year (immutable with versioned filenames)
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());

        // Cache webfonts for 1 year
        registry.addResourceHandler("/webfonts/**")
                .addResourceLocations("classpath:/static/webfonts/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());

        // Cache favicon for 1 week
        registry.addResourceHandler("/favicon.png")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
    }
}
