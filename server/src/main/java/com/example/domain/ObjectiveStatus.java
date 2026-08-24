package com.example.domain;

/**
 * Completion state of a single {@link Objective} within a {@link Quest}.
 *
 * <p>Each objective tracks its own completion independently of the quest and of its
 * sibling objectives. An objective is {@link #INCOMPLETE} until its progress reaches
 * its target, at which point it becomes {@link #COMPLETE}.</p>
 */
public enum ObjectiveStatus {
    INCOMPLETE,
    COMPLETE
}
