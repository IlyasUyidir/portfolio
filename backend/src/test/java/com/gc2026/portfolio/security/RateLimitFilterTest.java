package com.gc2026.portfolio.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimitFilterTest {

    private MockMvc mockMvc;

    @RestController
    static class StubController {
        @PostMapping("/api/v1/auth/login")
        void login() {}

        @PostMapping("/api/v1/other")
        void other() {}

        @GetMapping("/api/v1/auth/register")
        void register() {}
    }

    @BeforeEach
    void setUp() {
        RateLimitFilter filter = new RateLimitFilter();
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .addFilters(filter)
                .build();
    }

    @Test
    void rateLimit_whenFirst10RequestsOnAuthPath_shouldReturn200() throws Exception {
        // Arrange
        String loginJson = "{\"email\":\"test@folio.io\",\"password\":\"password123\"}";

        // Act & Assert
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void rateLimit_whenEleventhRequestOnAuthPath_shouldReturn429() throws Exception {
        // Arrange
        String loginJson = "{\"email\":\"test@folio.io\",\"password\":\"password123\"}";
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson));
        }

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rateLimit_whenRateLimited_shouldReturnJsonErrorBody() throws Exception {
        // Arrange
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"));
        }

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("Too many requests")))
                .andExpect(content().string(containsString("Try again in a minute")));
    }

    @Test
    void rateLimit_whenPathIsNotAuth_shouldNotBeRateLimited() throws Exception {
        // Act & Assert
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(post("/api/v1/other")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void rateLimit_whenAuthPathIsGetRequest_shouldStillApplyRateLimit() throws Exception {
        // Arrange
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/auth/register"));
        }

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/register"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rateLimit_whenDifferentIPs_shouldHaveSeparateBuckets() throws Exception {
        // Arrange: Exhaust bucket for IP 1.1.1.1
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .with(request -> { request.setRemoteAddr("1.1.1.1"); return request; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"));
        }

        // 1.1.1.1 is now rate limited
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> { request.setRemoteAddr("1.1.1.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests());

        // Act & Assert: IP 2.2.2.2 should still be allowed
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> { request.setRemoteAddr("2.2.2.2"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}

