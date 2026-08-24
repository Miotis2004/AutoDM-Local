package com.example.domain;

/**
 * The general attitude an {@link Npc} shows toward the party. Kept as a small
 * closed set so the disposition can be compared and filtered rather than stored
 * as free-form text.
 */
public enum Disposition {
    FRIENDLY,
    FRIENDLY_NEUTRAL,
    NEUTRAL,
    NEUTRAL_HOSTILE,
    HOSTILE
}
