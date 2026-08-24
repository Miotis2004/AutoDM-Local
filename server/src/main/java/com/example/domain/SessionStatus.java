package com.example.domain;

/**
 * Lifecycle states that a {@link Session} can be in.
 *
 * <p>A session starts {@link #ACTIVE} when the game is started or resumed and moves
 * to {@link #ENDED} when the game is ended. The terminal {@link #ENDED} state is
 * durable so a session can be queried in exactly one of the two states and so the
 * history of a campaign keeps a complete, ordered record of every game played.</p>
 */
public enum SessionStatus {
    ACTIVE,
    ENDED
}
