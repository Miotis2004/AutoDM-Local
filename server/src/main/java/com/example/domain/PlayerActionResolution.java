package com.example.domain;

import java.util.List;

/**
 * The engine's mechanical verdict on a single player action: what it understood, how it validated,
 * the governing ability check that was resolved, and any pending state change.
 *
 * <p>This is the engine's mechanical output. It is deliberately free of narrative text - the
 * {@link com.example.service.DungeonMasterEngine#generateResponse} seam owns the narrative, so the
 * mechanical verdict and the story told about it stay separate. The {@link #check()} is the
 * resolved ability check backing the action (or {@code null} when the action could not be
 * resolved), and {@link #stateChange()} is the pending world effect the owning service must
 * apply.</p>
 *
 * <p>This is a plain, immutable value holder.</p>
 */
public record PlayerActionResolution(
        /** Whether the action was a recognized, resolvable action. */
        boolean recognized,
        /** Human-readable validation problems; empty when the action was recognized. */
        List<String> validationErrors,
        /** The normalized verb the action expressed (for example {@code ATTACK}), or empty. */
        String parsedVerb,
        /** The resolved ability check backing the action, or {@code null} when none applies. */
        AbilityCheckResult check,
        /** The pending state change to apply, or an empty (non-applying) change. */
        StateChange stateChange
) {

    /**
     * Builds a resolution for an unrecognized or invalid action.
     *
     * @param action        the original action string, used in the message
     * @param validationErrors the validation problems to report
     * @return the resolution (never {@code null})
     */
    public static PlayerActionResolution unrecognized(String action, List<String> validationErrors) {
        return new PlayerActionResolution(
                false,
                validationErrors,
                "",
                null,
                StateChange.none());
    }
}
