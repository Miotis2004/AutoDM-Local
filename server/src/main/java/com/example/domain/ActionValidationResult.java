package com.example.domain;

import java.util.List;

/**
 * The outcome of validating a {@link PlayerAction} against the current game state.
 *
 * <p>This is a small result value that separates <em>whether</em> an action is allowed from the
 * <em>why</em>. A valid action carries no errors; an invalid or impossible action reports one or
 * more human-readable problems (for example "Attack must name a living target present in the
 * scene" or "You cannot rest while a threat remains"). Callers can inspect {@link #valid()} alone
 * or surface every {@link #errors()} at once.</p>
 *
 * <p>This is a plain, immutable value holder.</p>
 */
public record ActionValidationResult(boolean valid, List<String> errors) {

    /**
     * A result that accepts the action.
     *
     * @return a valid result with no errors
     */
    public static ActionValidationResult accepted() {
        return new ActionValidationResult(true, List.of());
    }

    /**
     * A result that rejects the action.
     *
     * @param errors the human-readable problems, in the order they were discovered
     * @return an invalid result carrying its errors
     */
    public static ActionValidationResult invalid(List<String> errors) {
        return new ActionValidationResult(false, List.copyOf(errors));
    }

    /**
     * A result that rejects the action with a single problem.
     *
     * @param error the single human-readable problem
     * @return an invalid result carrying that one error
     */
    public static ActionValidationResult invalid(String error) {
        return invalid(List.of(error));
    }
}
