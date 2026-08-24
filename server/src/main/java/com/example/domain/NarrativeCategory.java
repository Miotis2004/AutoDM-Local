package com.example.domain;

/**
 * The categories a {@link NarrativeEntry} can belong to.
 *
 * <p>These are the five kinds of moment a table needs in its game log:
 * {@link #DM_NARRATION} for the Dungeon Master's descriptive narration,
 * {@link #PLAYER_ACTION} for a player action and its mechanical verdict,
 * {@link #DICE_RESULT} for a dice roll and its outcome,
 * {@link #COMBAT_EVENT} for a combat beat (an attack landing or missing), and
 * {@link #SYSTEM_EVENT} for any other campaign or system moment.</p>
 *
 * <p>The list is intentionally open: a bespoke or future template can add a new
 * constant here to introduce a category without touching any consumer, so the
 * game log can grow to cover new kinds of moments as the game does.</p>
 */
public enum NarrativeCategory {
    /** The Dungeon Master's descriptive narration of a scene or moment. */
    DM_NARRATION,
    /** A player action and the mechanical verdict the engine returned for it. */
    PLAYER_ACTION,
    /** A dice roll and its total and outcome. */
    DICE_RESULT,
    /** A combat beat: an attack, damage, healing, or similar combat moment. */
    COMBAT_EVENT,
    /** Any other campaign or system moment recorded through the event system. */
    SYSTEM_EVENT
}
