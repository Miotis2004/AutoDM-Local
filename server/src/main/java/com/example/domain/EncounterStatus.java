package com.example.domain;

/**
 * Lifecycle states that an {@link Encounter} can be in.
 *
 * <p>An encounter begins scheduled, becomes active once play on it has started, and
 * finishes when its participants are resolved.</p>
 */
public enum EncounterStatus {
    SCHEDULED,
    ACTIVE,
    FINISHED
}
