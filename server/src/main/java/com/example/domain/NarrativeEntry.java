package com.example.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A single structured line for the game log: one moment of play rendered by a
 * {@link com.example.service.NarrativeTemplate}.
 *
 * <p>This is the unit the frontend game log consumes. Every entry carries the
 * {@link #category()} it belongs to (one of the {@link NarrativeCategory}s), a short
 * human-readable {@link #message()} the log shows as a line, an optional {@link #title()}
 * for entries that want a header, the {@link #timestamp()} the moment occurred, and the
 * structured {@link #data()} the moment was rendered from. The data is an immutable map so a
 * client can render whatever it likes - a dice result's individual dice, an attack's damage
 * type, a campaign event's JSON detail - while still having the ready-made {@link #message()}
 * to display.</p>
 *
 * <p>This is a plain, immutable value holder. {@link #message()} and {@link #title()} are never
 * {@code null} and {@link #data()} is never {@code null}.</p>
 */
public record NarrativeEntry(
        /** The category this entry belongs to. */
        NarrativeCategory category,
        /** The header line for the entry; empty when the entry carries no title. */
        String title,
        /** The readable line the game log shows as this entry's content. */
        String message,
        /** When the moment occurred. */
        LocalDateTime timestamp,
        /** The structured game state the entry was rendered from; never {@code null}. */
        Map<String, Object> data) {

    /**
     * Builds an entry.
     *
     * @param category  the category this entry belongs to (never {@code null})
     * @param title     the optional header (nullable; stored as an empty string when {@code null})
     * @param message   the readable line to show (nullable; stored as an empty string when {@code null})
     * @param timestamp when the moment occurred (nullable; defaults to {@link LocalDateTime#now()})
     * @param data      the structured game state (nullable; stored as an empty map when {@code null})
     * @return a completed entry (never {@code null})
     */
    public NarrativeEntry(NarrativeCategory category, String title, String message,
                          LocalDateTime timestamp, Map<String, Object> data) {
        this.category = category;
        this.title = title == null ? "" : title;
        this.message = message == null ? "" : message;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.data = copy(data);
    }

    /**
     * Copies a data map into an immutable view, dropping any {@code null} values so a structured
     * entry always holds a clean, serialisable map.
     *
     * @param data the source map (nullable)
     * @return an immutable map with no {@code null} values
     */
    private static Map<String, Object> copy(Map<String, Object> data) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (data != null) {
            data.forEach((key, value) -> {
                if (value != null) {
                    copy.put(key, value);
                }
            });
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Builds a minimal entry carrying only a category and message (useful for the simplest
     * log lines), stamped with the current time and no structured data.
     *
     * @param category the category this entry belongs to (never {@code null})
     * @param message  the readable line to show (nullable; stored as an empty string when {@code null})
     * @return a completed entry (never {@code null})
     */
    public static NarrativeEntry of(NarrativeCategory category, String message) {
        return new NarrativeEntry(category, "", message, null, Map.of());
    }

    /**
     * Builds an entry carrying a category, message, and structured data.
     *
     * @param category the category this entry belongs to (never {@code null})
     * @param message  the readable line to show (nullable; stored as an empty string when {@code null})
     * @param data     the structured game state (nullable; stored as an empty map when {@code null})
     * @return a completed entry (never {@code null})
     */
    public static NarrativeEntry of(NarrativeCategory category, String message,
                                    Map<String, Object> data) {
        return new NarrativeEntry(category, "", message, null, data);
    }

    /**
     * @return the title, or an empty string when the entry carries no title
     */
    public String safeTitle() {
        return title == null ? "" : title;
    }

    /**
     * @return the message, or an empty string when the entry carries no message
     */
    public String safeMessage() {
        return message == null ? "" : message;
    }
}
