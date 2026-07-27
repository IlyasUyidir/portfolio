package com.gc2026.portfolio.domain.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised error-response shaping for the entire API.
 *
 * Error-type vocabulary:
 *  VALIDATION_ERROR         – DTO / schema failures caught by Bean Validation
 *                             (MethodArgumentNotValidException, ConstraintViolationException,
 *                              HttpMessageNotReadableException, MissingServletRequestParameterException,
 *                              MethodArgumentTypeMismatchException)
 *  BUSINESS_RULE_VIOLATION  – Service-layer logic rejections (ValidationException)
 *                             These return 409 Conflict so clients can distinguish
 *                             "your request is malformed" from "your request violates a rule".
 *  NOT_FOUND                – Resource not found (404)
 *  CONFLICT                 – Duplicate / integrity constraint (409)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 404 ───────────────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "type", "NOT_FOUND"
                ));
    }

    // ─── 409 Conflict — business-rule violations ───────────────────────────────

    /**
     * ValidationException represents a service-layer business-rule rejection
     * (e.g. "already achieved", "limit exceeded"). It is semantically distinct
     * from a malformed request (400) and is returned as 409 Conflict so that
     * clients can branch on the status code without parsing free-text.
     *
     * NOTE: This changes the previous 400 response to 409. Any client code or
     * test that was asserting 400 for business-rule violations must be updated.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "type", "BUSINESS_RULE_VIOLATION"
                ));
    }

    /**
     * DataIntegrityViolationException → 409 Conflict.
     * Covers the race window in budget upsert (C-3) where two concurrent threads
     * both try to insert the same budget row and the DB unique constraint fires.
     * Returning 409 (not 500) makes the situation recoverable from the client side.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolationException caught: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "The resource already exists or conflicts with an existing record",
                        "type", "CONFLICT"
                ));
    }

    // ─── 400 Bad Request — schema / binding failures ───────────────────────────

    /**
     * MethodArgumentNotValidException — Bean Validation on @RequestBody DTOs.
     * Returns VALIDATION_ERROR so clients know this is a structural request problem.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleBeanValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", errors,
                        "type", "VALIDATION_ERROR"
                ));
    }

    /**
     * ConstraintViolationException — Bean Validation on @Validated method parameters
     * (path variables, query params).
     *
     * N-6 fix: raw ex.getMessage() leaks "methodName.arg0.fieldName: message" prefixes.
     * We extract only the constraint message text from each violation.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations().stream()
                .map(cv -> {
                    // cv.getPropertyPath() returns "methodName.paramName.fieldName"
                    // We only want the leaf field name, not the full path.
                    String path = cv.getPropertyPath().toString();
                    String leafName = path.contains(".")
                            ? path.substring(path.lastIndexOf('.') + 1)
                            : path;
                    return leafName + ": " + cv.getMessage();
                })
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", errors,
                        "type", "VALIDATION_ERROR"
                ));
    }

    /**
     * C-4 fix: HttpMessageNotReadableException was falling through to the 500 catch-all.
     * Covers malformed JSON bodies and invalid enum values in @RequestBody.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.debug("HttpMessageNotReadableException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Request body is malformed or contains an invalid value",
                        "type", "VALIDATION_ERROR"
                ));
    }

    /**
     * C-4 fix: MissingServletRequestParameterException was falling through to 500.
     * Covers required @RequestParam fields that are absent from the request.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Required request parameter '" + ex.getParameterName() + "' is missing",
                        "type", "VALIDATION_ERROR"
                ));
    }

    /**
     * C-4 / already present: MethodArgumentTypeMismatchException — handles invalid
     * enum values in path variables and type-mismatched parameters.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Invalid parameter format: " + ex.getName(),
                        "type", "VALIDATION_ERROR"
                ));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, String>> handleNumberFormatException(NumberFormatException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Invalid numeric format: " + ex.getMessage(),
                        "type", "VALIDATION_ERROR"
                ));
    }

    // ─── 401 ───────────────────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid email or password"));
    }

    // ─── 500 catch-all (never expose internals) ─────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unhandled exception occurred:", ex);
        // Never expose stack traces (security rule)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred"));
    }
}
