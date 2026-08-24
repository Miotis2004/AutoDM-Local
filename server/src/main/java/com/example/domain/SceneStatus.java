package com.example.domain;

/**
 * The lifecycle state of a {@link Scene}.
 *
 * <p>A scene moves through three states. It starts {@link #READY} when created, becomes
 * {@link #ACTIVE} when it is the scene currently in focus, and reaches {@link #COMPLETED}
 * when the play within it has finished and the Dungeon Master engine has moved on to the
 * next scene.</p>
 */
public enum SceneStatus {
    /** The scene has been created but is not yet in focus. */
    READY,
    /** The scene is the one currently in focus. */
    ACTIVE,
    /** The scene's play has finished. */
    COMPLETED
}
