package com.example.untitled.e2e;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@Tag("e2e")
public abstract class E2ETestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    protected String getBaseUrl() {
        // Returns empty string because TestRestTemplate automatically prepends the context-path (/api/v1)
        return "";
    }
}
