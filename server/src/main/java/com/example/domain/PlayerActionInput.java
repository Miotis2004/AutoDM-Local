package com.example.domain;

/**
 * A raw player action fed into the Dungeon Master engine for interpretation.
 *
 * <p>This is the engine's input contract. A {@code DungeonMasterService} receives it from a thin
 * REST controller and passes it to a pluggable {@link com.example.service.DungeonMasterEngine} to
 * validate and resolve. The {@link #action()} is free-form natural language (for example "I try to
 * persuade the guard"); the optional {@link #statistic()}, {@link #modifier()}, and
 * {@link #difficulty()} narrow the governing mechanic. When the statistic is absent the engine
 * chooses a sensible default from the action's verb (an attack uses an athletic statistic, a social
 * attempt uses a Charisma-style statistic).</p>
 *
 * <p>This is a plain, immutable value holder used only to carry inputs into the resolution path.</p>
 */
public record PlayerActionInput(
        /** The free-form action the player wants to take (required, non-blank). */
        String action,
        /** The ability/skill statistic that governs the action (optional; engine defaults it). */
        String statistic,
        /** The modifier applied to the statistic (optional; defaults to {@code 0}). */
        int modifier,
        /** The difficulty class the check must meet (optional; defaults to a sensible default). */
        int difficulty
) {

    /** The default difficulty class the engine uses when the caller does not supply one. */
    public static final int DEFAULT_DIFFICULTY = 12;

    /**
     * Creates a player action with the engine's default modifier and difficulty.
     *
     * @param action the free-form action (required, non-blank)
     * @return the player action input (never {@code null})
     */
    public static PlayerActionInput of(String action) {
        return new PlayerActionInput(action, null, 0, DEFAULT_DIFFICULTY);
    }

    /**
     * Creates a player action with the given statistic, modifier, and difficulty.
     *
     * @param action     the free-form action (required, non-blank)
     * @param statistic  the governing statistic (may be {@code null} so the engine defaults it)
     * @param modifier   the modifier applied to the statistic (may be negative)
     * @param difficulty the difficulty class the check must meet (must be positive)
     * @return the player action input (never {@code null})
     */
    public static PlayerActionInput of(String action, String statistic, int modifier, int difficulty) {
        return new PlayerActionInput(action, statistic, modifier, difficulty);
    }
}
