package com.example.domain;

import java.util.Map;

/**
 * A request to render one narrative entry: the {@link NarrativeCategory} to render and the
 * free-form structured data to render it from.
 *
 * <p>This is the payload the frontend game log sends to {@code POST /api/narrative/render}. The
 * data map carries the structured game state for the moment - a dice roll's individual dice, an
 * attack's damage type, a campaign event's detail - so a bespoke or future template can render it
 * without the client needing to build a typed domain object.</p>
 */
public record NarrativeRenderRequest(
        /** The category to render (never {@code null}). */
        NarrativeCategory category,
        /** The structured data to render from (nullable; treated as an empty map). */
        Map<String, Object> data) {
}
