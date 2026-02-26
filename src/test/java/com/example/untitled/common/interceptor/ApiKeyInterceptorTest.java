package com.example.untitled.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyInterceptorTest {

    private static final String VALID_KEY = "test-api-key";
    private ApiKeyInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ApiKeyInterceptor(VALID_KEY, new ObjectMapper());
    }

    /**
     * 正しい API key を指定した場合は通過する
     */
    @Test
    void shouldPassWhenValidKeyProvided() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-api-key", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    /**
     * x-api-key ヘッダーがない場合は 401 を返す
     */
    @Test
    void shouldReturn401WhenNoKeyProvided() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
    }

    /**
     * x-api-key が不一致の場合は 401 を返す
     */
    @Test
    void shouldReturn401WhenInvalidKeyProvided() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-api-key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
    }

    /**
     * /health エンドポイントは認証をスキップする
     */
    @Test
    void shouldSkipAuthForHealthEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
