package com.example.domain;

/**
 * The specific standing an {@link Npc} has with the party. Broader than
 * {@link Disposition}, which captures a momentary attitude, {@link NpcRelationship}
 * captures the durable bond (allies, acquaintances, sworn enemies, and so on) that
 * the campaign can track and evolve over time.
 */
public enum NpcRelationship {
    KNOWN,
    ALLIED,
    FRIENDLY,
    NEUTRAL,
    HOSTILE,
    FOE
}
