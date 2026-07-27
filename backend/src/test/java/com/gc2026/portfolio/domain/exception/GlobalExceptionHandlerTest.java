package com.gc2026.portfolio.domain.exception;

import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MissingServletRequestParameterException;
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

        // C-4 regression targets
        @GetMapping("/not-readable")
        void notReadable() {
            throw new HttpMessageNotReadableException(
                    "JSON parse error",
                    new MockHttpInputMessage(new byte[0]));
        }

        @GetMapping("/missing-param")
        void missingParam() throws MissingServletRequestParameterException {
            throw new MissingServletRequestParameterException("amount", "Long");
        }

        // C-3 / DataIntegrityViolation regression target
        @GetMapping("/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException("Unique constraint violation");
        }

        // N-6 regression target — jakarta ConstraintViolationException
        @GetMapping("/constraint-violation")
        void constraintViolation() {
            // Manually construct a ConstraintViolationException with a known violation
            // using the Hibernate Validator factory so we can control the path prefix.
            jakarta.validation.Validation.buildDefaultValidatorFactory()
                    .getValidator()
                    .validate(new NegativeDto(-1));
            // If no violations are thrown by the above, simulate it directly:
            throw buildConstraintViolation();
        }

        private jakarta.validation.ConstraintViolationException buildConstraintViolation() {
            // Build a single violation whose path looks like "methodName.arg0.amount"
            // so we can assert that only "amount: must be positive" is returned.
            jakarta.validation.ConstraintViolation<NegativeDto> violation =
                    jakarta.validation.Validation.buildDefaultValidatorFactory()
                            .getValidator()
                            .validate(new NegativeDto(-1))
                            .iterator()
                            .next();
            return new jakarta.validation.ConstraintViolationException(
                    "constraintViolation.arg0.amount: must be greater than 0",
                    java.util.Set.of(violation));
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    static class SomeDto {
        @jakarta.validation.constraints.NotBlank(message = "Name is required")
        private String name;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    static class NegativeDto {
        @jakarta.validation.constraints.Positive(message = "must be greater than 0")
        private int amount;
    }

    // ─── Existing tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("handleNotFound → 404 with error + type=NOT_FOUND")
    void handleNotFound_shouldReturn404WithErrorJson() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.type").value("NOT_FOUND"));
    }

    /**
     * I-5 regression: ValidationException (business-rule violation) must return
     * 409 Conflict, NOT 400. Old code returned 400; this test would have FAILED
     * on the old handler and passes after the fix.
     */
    @Test
    @DisplayName("handleValidation (I-5) → 409 Conflict with BUSINESS_RULE_VIOLATION type")
    void handleValidation_shouldReturn409WithBusinessRuleViolationType() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(status().isConflict())                      // 409, not 400
                .andExpect(jsonPath("$.error").value("Invalid input"))
                .andExpect(jsonPath("$.type").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("handleBadCredentials → 401 with generic message")
    void handleBadCredentials_shouldReturn401WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/bad-creds"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    @DisplayName("handleGeneric → 500 with generic message (no internals leaked)")
    void handleGeneric_shouldReturn500WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("An unexpected error occurred"));
    }

    @Test
    @DisplayName("handleBeanValidation → 400 with VALIDATION_ERROR type")
    void handleBeanValidation_shouldReturn400WithFieldErrors() throws Exception {
        String invalidBody = "{\"name\": \"\"}";

        mockMvc.perform(post("/test/bean-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Name is required")))
                .andExpect(jsonPath("$.type").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("handleNumberFormatException → 400 with VALIDATION_ERROR type")
    void handleNumberFormatException_shouldReturn400WithErrorJson() throws Exception {
        mockMvc.perform(get("/test/number-format"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Invalid numeric format: For input string: \"abc\"")))
                .andExpect(jsonPath("$.type").value("VALIDATION_ERROR"));
    }

    // ─── New regression tests for C-4 ────────────────────────────────────────────

    /**
     * C-4 regression: HttpMessageNotReadableException (malformed JSON / invalid enum)
     * previously fell through to the 500 catch-all. Must now return 400 + VALIDATION_ERROR.
     * This test would FAIL on old code (got 500) and PASS after the fix.
     */
    @Test
    @DisplayName("handleNotReadable (C-4) → 400 with VALIDATION_ERROR when body is malformed")
    void handleNotReadable_shouldReturn400WhenBodyMalformed() throws Exception {
        mockMvc.perform(get("/test/not-readable"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("malformed or contains an invalid value")))
                .andExpect(jsonPath("$.type").value("VALIDATION_ERROR"));
    }

    /**
     * C-4 regression: MissingServletRequestParameterException previously fell through
     * to 500. Must now return 400 + VALIDATION_ERROR with the missing param name.
     * This test would FAIL on old code (got 500) and PASS after the fix.
     */
    @Test
    @DisplayName("handleMissingParam (C-4) → 400 with VALIDATION_ERROR naming the missing param")
    void handleMissingParam_shouldReturn400NamingMissingParam() throws Exception {
        mockMvc.perform(get("/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("amount")))
                .andExpect(jsonPath("$.type").value("VALIDATION_ERROR"));
    }

    // ─── New regression test for C-3 ─────────────────────────────────────────────

    /**
     * C-3 regression: DataIntegrityViolationException (budget upsert race) previously
     * fell through to 500. Must now return 409 Conflict with CONFLICT type.
     * This test would FAIL on old code (got 500) and PASS after the fix.
     */
    @Test
    @DisplayName("handleDataIntegrityViolation (C-3) → 409 Conflict with CONFLICT type")
    void handleDataIntegrityViolation_shouldReturn409Conflict() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("CONFLICT"));
    }

    // ─── New regression test for N-6 ─────────────────────────────────────────────

    /**
     * N-6 regression: ConstraintViolationException previously returned raw
     * "methodName.arg0.fieldName: message" leaking internal method paths.
     * Must now return only the leaf field name and message, e.g. "amount: must be greater than 0".
     * This test would FAIL on old code (contained the full path) and PASS after the fix.
     */
    @Test
    @DisplayName("handleConstraintViolation (N-6) → 400 with VALIDATION_ERROR, no internal path prefix leaked")
    void handleConstraintViolation_shouldStripInternalPathPrefix() throws Exception {
        mockMvc.perform(get("/test/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("VALIDATION_ERROR"))
                // Must NOT contain the full method-path prefix
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.not(
                        containsString("arg0"))))
                // Must contain the human-readable message
                .andExpect(jsonPath("$.error").value(containsString("must be greater than 0")));
    }
}
