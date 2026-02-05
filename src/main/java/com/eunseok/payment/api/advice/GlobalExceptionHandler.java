package com.eunseok.payment.api.advice;

import com.eunseok.payment.api.dto.ApiErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1) Handles ResponseStatusException explicitly thrown
     *    from controllers or services.
     *
     * Typical usage:
     *  - throw new ResponseStatusException(HttpStatus.CONFLICT, "message")
     *
     * Responsibility:
     *  - Extract HTTP status
     *  - Convert status to API error code
     *  - Choose the best human-readable message
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handle(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String code = toCode(status);
        String msg = firstNonBlank(e.getReason(), e.getMessage(), "Request failed");

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(code, msg));
    }

    /**
     * 2) Handles Spring's internal ErrorResponseException.
     *
     * This exception is sometimes thrown by Spring itself
     * (e.g. unsupported HTTP method, missing parameters, etc.).
     *
     * We normalize it to our API error response format.
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiErrorResponse> handle(ErrorResponseException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String msg = firstNonBlank(e.getMessage(), "Request failed");

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(toCode(status), msg));
    }

    /**
     * 3) Handles @Valid validation failures.
     *
     * Triggered when request body validation fails
     * (e.g. @NotNull, @Size, @Min, etc.).
     *
     * Always returns HTTP 400 with a field-level error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handle(MethodArgumentNotValidException e) {
        var fieldError = e.getBindingResult().getFieldError();

        String msg = (fieldError == null)
                ? "Validation failed"
                : fieldError.getField() + ": " + fieldError.getDefaultMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("VALIDATION_FAILED", msg));
    }

    /**
     * 4) Handles domain-level illegal state transitions.
     *
     * Example:
     *  - Invalid payment status transition
     *  - Business rule violations
     *
     * We standardize these as HTTP 409 (Conflict).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handle(IllegalStateException e) {
        String msg = firstNonBlank(e.getMessage(), "Invalid state");

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("INVALID_STATE", msg));
    }

    /**
     * 5) Handles database integrity violations.
     *
     * Examples:
     *  - Unique constraint violation
     *  - Foreign key violation
     *
     * IMPORTANT:
     *  - We intentionally do NOT expose raw DB error messages
     *    for security and stability reasons.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handle(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        "DATA_INTEGRITY_VIOLATION",
                        "Request conflicts with existing data"
                ));
    }

    /**
     * 6) Final safety net for any uncaught exceptions.
     *
     * This prevents stack traces or internal details
     * from leaking to API consumers.
     *
     * Always returns HTTP 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handle(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", "Unexpected error"));
    }

    /**
     * Converts HTTP status into a stable API error code.
     *
     * Purpose:
     *  - Decouple HTTP semantics from client-facing error codes
     *  - Allow future extension without breaking clients
     */
    private String toCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            default -> "REQUEST_FAILED";
        };
    }

    /**
     * Returns the first non-null and non-blank string
     * from the given arguments.
     *
     * Used to select the most meaningful error message
     * without excessive null/blank checks.
     */
    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
