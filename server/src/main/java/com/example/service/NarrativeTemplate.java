package com.example.service;

import com.example.domain.NarrativeCategory;
import com.example.domain.NarrativeContext;
import com.example.domain.NarrativeEntry;

/**
 * Turns one piece of structured game state into a single {@link NarrativeEntry}.
 *
 * <p>A template is the data- or code-driven heart of the narrative system. It receives a
 * {@link NarrativeContext} holding the structured game state for one moment and returns the
 * {@link NarrativeEntry} the game log should show. The {@link NarrativeTemplates} service keeps a
 * registry of one template per {@link NarrativeCategory} and delegates to the registered template
 * for a category, so adding a new category is a matter of introducing a constant on
 * {@link NarrativeCategory} and registering a template for it - the service and its REST surface
 * already know how to pass the category through.</p>
 *
 * <p>Templates are plain functions of the context they are given, mirroring every other pure game
 * service on the back-end. A template never mutates game state; it only describes a moment.</p>
 */
@FunctionalInterface
public interface NarrativeTemplate {

    /**
     * Renders the structured game state in a context into a game-log entry.
     *
     * @param context the structured game state (never {@code null})
     * @return the entry to show (never {@code null}); the owning service stamps the category and
     *         timestamp when the entry is returned through the registry
     */
    NarrativeEntry render(NarrativeContext context);
}
