package com.example;

import com.example.service.ValidationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every back-end failure into a single, consistent JSON error shape.
 *
 * <p>AutoDM's API answers "the caller was wrong" and "the server was wrong" with two
 * distinct, well-behaved responses rather than leaking stack traces or mixing shapes. Every
 * error the API returns is a small JSON object of the form:</p>
 *
 * <pre>
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "A campaign identifier is required.",
 *   "path": "/api/campaigns/1/action",
 *   "timestamp": "2026-01-02T03:04:05.678"
 * }
 * </pre>
 *
 * <p>The fields are stable across every error the API produces:</p>
 *
 * <ul>
 *   <li>{@code status} is the HTTP status code,</li>
 *   <li>{@code error} is the standard short reason phrase for that status,</li>
 *   <li>{@code message} is the human-readable, non-sensitive description of the problem,</li>
 *   <li>{@code path} is the request path that failed, and</li>
 *   <li>{@code timestamp} is the time the error was recorded.</li>
 * </ul>
 *
 * <p><strong>Validation failures</strong> (the request was structurally unsound) are surfaced as
 * {@code 400 Bad Request}. The back-end rejects unsound input by throwing {@link ValidationException}
 * (a structured DTO-validation failure) or {@link IllegalArgumentException} (the historical,
 * per-service rejection used throughout the services). Both mean "the request was bad," so both are
 * surfaced as {@code 400}s carrying a clear message.</p>
 *
 * <p><strong>Invalid game actions</strong> (a player action that cannot be resolved in the current
 * scene) are likewise rejected at {@code 400} with a clear, game-specific message rather than a
 * silent {@code recognized:false} body.</p>
 *
 * <p><strong>Unexpected backend errors</strong> fall through to the catch-all handler and become a
 * {@code 500 Internal Server Error} carrying the same shape, so a caller can always parse the body
 * the same way and never has to guess whether an error response is JSON. Server-side failures are
 * deliberately stripped of internal detail so stack traces and SQL are never returned to clients.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Handles DTO-validation rejections, invalid game actions, and the historical per-service
     * {@link IllegalArgumentException} rejections. All of these mean "the request was bad."
     *
     * @param exception the validation failure
     * @param request   the failing request, used for the {@code path} field
     * @return an HTTP 400 carrying the consistent error body
     */
    @ExceptionHandler({ValidationException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleValidation(
            Exception exception, jakarta.servlet.http.HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    /**
     * Handles every uncaught, unexpected back-end failure so it is returned as a consistent
     * {@code 500} JSON error rather than Spring's default HTML error page. The internal cause is
     * logged server-side but never sent to the client.
     *
     * @param exception the uncaught failure
     * @param request   the failing request, used for the {@code path} field
     * @return an HTTP 500 carrying the consistent error body
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            RuntimeException exception, jakarta.servlet.http.HttpServletRequest request) {
        // Log the full cause for the operator, without echoing it to the client.
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = exception.getClass().getSimpleName();
        }
        System.err.println("[autodm-server] Unexpected error on "
                + request.getRequestURI() + ": " + detail);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing the request.", request);
    }

    /**
     * Builds the shared error body for a failed request at the given status.
     *
     * @param status  the HTTP status to return
     * @param message the human-readable message, or {@code null} for a generic one
     * @param request the failing request, used for the {@code path} field
     * @return a {@link ResponseEntity} carrying the consistent, machine-readable error body
     */
    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status, String message, jakarta.servlet.http.HttpServletRequest request) {
        String bodyMessage = (message == null || message.isBlank())
                ? status.getReasonPhrase()
                : message;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", bodyMessage);
        body.put("path", requestPath(request));
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Resolves the request path used in the error body, degrading gracefully when the servlet
     * request is unavailable.
     *
     * @param request the failing request (may be {@code null})
     * @return the request path, or {@code "?"} when it cannot be determined
     */
    private String requestPath(jakarta.servlet.http.HttpServletRequest request) {
        if (request == null) {
            return "?";
        }
        String path = request.getRequestURI();
        return (path == null || path.isBlank()) ? "?" : path;
    }
}
