package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * A reference from a {@link Session} to a single in-game event within the same
 * campaign. A session can name any number of events it covered (a combat, a social
 * encounter, a skill challenge, and so on), and each reference is captured here by
 * the {@link #eventId} of the event and a human-readable {@link #eventName}.
 *
 * <p>This is an embeddable value object rather than its own entity so that event
 * references are stored as part of the owning session's rows in the
 * {@code session_events} table instead of in a separate table. The pair is
 * value-equal, so it is safe to keep inside a {@link java.util.Set}.</p>
 */
@Embeddable
public class SessionEventRef {

    /**
     * The id of the referenced event. Mirrors the {@code event_id} column of the
     * {@code session_events} table.
     */
    @Column(name = "event_id")
    private Long eventId;

    /**
     * A human-readable label for the referenced event, stored so the history view can
     * name an event even when only an id is available elsewhere.
     */
    @Column(name = "event_name")
    private String eventName;

    public SessionEventRef() {
        /* Required by JPA. */
    }

    public SessionEventRef(Long eventId, String eventName) {
        this.eventId = eventId;
        this.eventName = eventName;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SessionEventRef that)) {
            return false;
        }
        return Objects.equals(eventId, that.eventId)
                && Objects.equals(eventName, that.eventName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventName);
    }

    @Override
    public String toString() {
        return "SessionEventRef{eventId=" + eventId + ", eventName='" + eventName + "'}";
    }
}
