package com.example.domain;

/**
 * The binary result of resolving an ability or skill check.
 *
 * <p>An ability check succeeds when the check's total (the character statistic plus the
 * modifier plus the die roll) meets or exceeds the difficulty class, and fails otherwise.
 * This is a plain value used by Dungeon Master logic to decide what happens next.</p>
 */
public enum AbilityCheckOutcome {
    SUCCESS,
    FAILURE
}
