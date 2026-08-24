package com.example.service;

/**
 * Signals that a request failed DTO validation: an identifier was invalid, a
 * quantity was impossible, a referenced entity was absent, or a game action was
 * malformed.
 *
 * <p>This is a client-error signal: the request was structurally unsound rather
 * than the server failing. It is intentionally lightweight and carries only a
 * human-readable message so a controller advice can surface it directly as an
 * HTTP 400 (Bad Request) response. Prefer the shared {@link DtoValidator} over
 * throwing this directly; the validator builds consistent, specific messages.</p>
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
