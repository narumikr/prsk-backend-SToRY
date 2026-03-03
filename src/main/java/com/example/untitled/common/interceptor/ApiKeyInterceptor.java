package com.example.untitled.common.interceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.untitled.common.constant.ApiSecurityConstants;
import com.example.untitled.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final String apiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyInterceptor(
            @Value("${app.api-key}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        if (request.getServletPath().startsWith(ApiSecurityConstants.HEALTH_PATH)) {
            return true;
        }

        // Allow CORS preflight requests without API key
        if (ApiSecurityConstants.OPTIONS_METHOD.equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String providedKey = request.getHeader(ApiSecurityConstants.API_KEY_HEADER);
        if (providedKey == null || !MessageDigest.isEqual(
                providedKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ErrorResponse error = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.name(),
                    "Invalid or missing API key."
            );
            objectMapper.writeValue(response.getWriter(), error);
            return false;
        }

        return true;
    }
}
