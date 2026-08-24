package com.example.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A single play session of a {@link Campaign}. Each session records when the game was
 * started ({@link #startTime}), when it was ended ({@link #endTime}, optional), its
 * {@link #status} ({@link SessionStatus#ACTIVE} or {@link SessionStatus#ENDED}), and
 * the in-game {@link #events} it covered.
 *
 * <p>Every session belongs to exactly one {@link Campaign}, so a session exists only
 * inside the game that created it and never leaks into another campaign. The session
 * row stores its timing and status in the {@code sessions} table; each event it
 * covered is stored as an embeddable {@link SessionEventRef} in the
 * {@code session_events} table. All of this is persisted in the SQLite database
 * (server/src/main/resources/schema.sql, loaded by Hibernate at bootstrap) so session
 * history reloads across application restarts within a campaign.</p>
 */
@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;

    /**
     * The in-game events this session covered, referenced by id and name. Present only
     * when at least one event has been referenced; a session with no referenced events
     * has an empty set here.
     */
    @Embedded
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "session_events",
            joinColumns = @JoinColumn(name = "session_id"))
    private Set<SessionEventRef> events = new HashSet<>();

    public Session() {
        /* Required by JPA. */
    }

    public Session(Campaign campaign, LocalDateTime startTime) {
        this.campaign = campaign;
        this.startTime = startTime;
    }

    public Long getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Set<SessionEventRef> getEvents() {
        return events;
    }

    public void setEvents(Set<SessionEventRef> events) {
        this.events = events == null ? new HashSet<>() : events;
    }

    /**
     * @return {@code true} when this session is still open and has not been ended
     */
    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    /**
     * Marks this session as ended at the given time, transitioning it from
     * {@link SessionStatus#ACTIVE} to {@link SessionStatus#ENDED}.
     *
     * @param endedAt the moment the game ended
     */
    public void endAt(LocalDateTime endedAt) {
        this.endTime = endedAt;
        this.status = SessionStatus.ENDED;
    }

    /**
     * Adds a referenced event to this session, if not already present.
     *
     * @return {@code true} if the event was newly added
     */
    public boolean addEvent(Long eventId, String eventName) {
        return this.events.add(new SessionEventRef(eventId, eventName));
    }

    /**
     * Removes a referenced event from this session, if present.
     *
     * @return {@code true} if the event was removed
     */
    public boolean removeEvent(Long eventId, String eventName) {
        return this.events.remove(new SessionEventRef(eventId, eventName));
    }
}
