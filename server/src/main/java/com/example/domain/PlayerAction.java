package com.example.domain;

/**
 * A structured player action: the machine-readable meaning a player wants to express, with an
 * optional free-form description.
 *
 * <p>This is the game's structured action model. It pairs a required {@link #type()} (one of the
 * {@link PlayerActionType} categories) with an optional {@link #description()} that carries the
 * player's own words - the "I want to search the room for traps" that the structured type alone
 * cannot hold. The optional {@link #targetName()} and {@link #skillName()} capture the two pieces
 * of structured detail that most actions carry: what (or whom) the action is aimed at and which
 * skill governs it.</p>
 *
 * <p>The model is deliberately small and open-ended. It holds no parsing rules: a free-form
 * {@link PlayerActionInput} is turned into a {@code PlayerAction} by a pluggable interpreter (see
 * the {@code ActionParser}), so a richer natural-language interpreter can be added later without
 * changing what an action <em>is</em>. This is a plain, immutable value holder.</p>
 */
public record PlayerAction(
        /** The structured category the action expresses (required, never {@code null}). */
        PlayerActionType type,
        /** The player's own words for the action (optional; may be {@code null} or blank). */
        String description,
        /** What or whom the action targets, when named (optional). */
        String targetName,
        /** The skill that governs the action, when named explicitly (optional). */
        String skillName
) {

    /**
     * Creates a structured action of the given type with only a description.
     *
     * @param type       the structured category (never {@code null})
     * @param description the player's own words (may be {@code null})
     * @return the structured action (never {@code null})
     */
    public static PlayerAction of(PlayerActionType type, String description) {
        return new PlayerAction(type, description, null, null);
    }

    /**
     * Creates a structured action with a target and, optionally, a governing skill.
     *
     * @param type       the structured category (never {@code null})
     * @param description the player's own words (may be {@code null})
     * @param targetName  what or whom the action targets (may be {@code null})
     * @param skillName   the governing skill (may be {@code null})
     * @return the structured action (never {@code null})
     */
    public static PlayerAction of(
            PlayerActionType type, String description, String targetName, String skillName) {
        return new PlayerAction(type, description, targetName, skillName);
    }

    /**
     * @return {@code true} when the action carries a non-blank free-form description
     */
    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    /**
     * @return {@code true} when the action names a specific target
     */
    public boolean hasTarget() {
        return targetName != null && !targetName.isBlank();
    }

    /**
     * @return {@code true} when the action names a governing skill
     */
    public boolean hasSkill() {
        return skillName != null && !skillName.isBlank();
    }
}
