package com.example.domain;

/**
 * The kinds of conditions that can be applied to a campaign's resources.
 *
 * <p>Each constant names a discrete condition that {@link ConditionRecord}
 * instances describe. The set is intentionally a common subset of the conditions
 * a tabletop game is likely to apply; new entries can be added without touching
 * the persistence layer because the value is stored as its string name.</p>
 */
public enum ConditionKind {
    BLINDNESS,
    CHARMED,
    DEAFENED,
    FRIGHTENED,
    PARALYZED,
    PETRIFIED,
    PRONE,
    RESTRAINED,
    STUNNED,
    UNCONSCIOUS,
    POISONED,
    VULNERABLE,
    EXHAUSTION
}
