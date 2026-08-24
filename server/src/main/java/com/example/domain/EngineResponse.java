package com.example.domain;

import java.util.List;

/**
 * The complete result of running one player action through the Dungeon Master engine.
 *
 * <p>This is the value the engine-driven service returns to a thin REST controller. It carries
 * back what the engine presented ({@link #scene()}), what it decided about the action
 * ({@link #recognized()}, {@link #validationErrors()}, {@link #check()}), the generated
 * narrative response ({@link #response()}) the players see, the pending {@link #stateChange()}
 * the world should apply, and the list of world {@link #effects()} the action triggered (any
 * encounters begun, objectives completed, locations discovered, or relationships updated).</p>
 *
 * <p>This is a plain, immutable value holder intended to be serialized to a client.</p>
 */
public record EngineResponse(
        /** The scene brief the engine presented around this action. */
        SceneBrief scene,
        /** Whether the action was recognized as a resolvable action. */
        boolean recognized,
        /** Human-readable validation problems; empty when the action was recognized. */
        List<String> validationErrors,
        /** The resolved ability check backing the action, or {@code null} when none applies. */
        AbilityCheckResult check,
        /** The narrative response generated for the players. */
        String response,
        /** The pending hit-point change the action resolved, or a non-applying change. */
        StateChange stateChange,
        /** The world effects the action triggered (encounters, objectives, discoveries, and so on). */
        List<String> effects
) {
}
