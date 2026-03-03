package com.example.untitled.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.untitled.common.constant.ApiSecurityConstants;
import com.example.untitled.common.interceptor.ApiKeyInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String ALL_PATHS = "/**";
    private static final String ORIGIN_SEPARATOR_REGEX = "\\s*,\\s*";
    private static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

    private final ApiKeyInterceptor apiKeyInterceptor;
    private final String allowedOrigins;

    public WebConfig(
            ApiKeyInterceptor apiKeyInterceptor,
            @Value("${app.allowed-origins}") String allowedOrigins
    ) {
        this.apiKeyInterceptor = apiKeyInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(ORIGIN_SEPARATOR_REGEX);
        registry.addMapping(ALL_PATHS)
                .allowedOrigins(origins)
            .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor)
                .excludePathPatterns(ApiSecurityConstants.HEALTH_PATH);
    }
}
