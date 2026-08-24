package com.example.domain;

/**
 * Lifecycle states that a {@link Quest} can be in.
 *
 * <p>A quest starts {@link #ACTIVE} and moves to {@link #COMPLETED} when every one
 * of its objectives is satisfied, or to {@link #FAILED} when the game marks it as
 * failed. Both terminal states are durable so a quest can be queried in exactly one
 * of the three states.</p>
 */
public enum QuestStatus {
    ACTIVE,
    COMPLETED,
    FAILED
}
