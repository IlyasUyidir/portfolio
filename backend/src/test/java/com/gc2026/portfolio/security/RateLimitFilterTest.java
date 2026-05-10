package com.gc2026.portfolio.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RateLimitFilterTest {

    private MockMvc mockMvc;
    private RateLimitFilter rateLimitFilter;

    @RestController
    static class TestController {
        @PostMapping("/api/v1/auth/login")
        public void login() {}

        @PostMapping("/api/v1/other")
        public void other() {}
    }

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(rateLimitFilter)
                .build();
    }

    @Test
    void rateLimit_shouldReturn429After10Requests() throws Exception {
        String loginJson = "{\"email\":\"test@example.com\",\"password\":\"password123\"}";

        // 10 requests should pass
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson))
                    .andExpect(status().isOk());
        }

        // 11th request should be rate limited
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rateLimit_shouldNotAffectOtherEndpoints() throws Exception {
        // This request doesn't start with /api/v1/auth/
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(post("/api/v1/other")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk());
        }
    }
}
