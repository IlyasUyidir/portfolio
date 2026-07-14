package com.gc2026.portfolio.domain.exception;

import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionThrowerController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    @RequestMapping("/test")
    static class ExceptionThrowerController {
        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Resource not found");
        }

        @GetMapping("/validation")
        void validation() {
            throw new ValidationException("Invalid input");
        }

        @GetMapping("/bad-creds")
        void badCreds() {
            throw new BadCredentialsException("Bad credentials");
        }

        @GetMapping("/generic")
        void generic() {
            throw new RuntimeException("Unexpected error");
        }

        @PostMapping("/bean-validation")
        void beanValidation(@RequestBody @Valid SomeDto dto) {
        }

        @GetMapping("/number-format")
        void numberFormat() {
            throw new NumberFormatException("For input string: \"abc\"");
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    static class SomeDto {
        @jakarta.validation.constraints.NotBlank(message = "Name is required")
        private String name;
    }

    @Test
    void handleNotFound_shouldReturn404WithErrorJson() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource not found"));
    }

    @Test
    void handleValidation_shouldReturn400WithErrorJson() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid input"));
    }

    @Test
    void handleBadCredentials_shouldReturn401WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/bad-creds"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void handleGeneric_shouldReturn500WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("An unexpected error occurred"));
    }

    @Test
    void handleBeanValidation_shouldReturn400WithFieldErrors() throws Exception {
        String invalidBody = "{\"name\": \"\"}";

        mockMvc.perform(post("/test/bean-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Name is required")));
    }

    @Test
    void handleNumberFormatException_shouldReturn400WithErrorJson() throws Exception {
        mockMvc.perform(get("/test/number-format"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Invalid numeric format: For input string: \"abc\"")));
    }
}
