package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A single significant event within a {@link Campaign}, such as the start or end of a
 * play session, the party entering a location, a discovery, a combat encounter, damage
 * being dealt, an item being acquired, a quest or relationship changing, and so on.
 *
 * <p>An event records what happened ({@link #eventType}), when it happened
 * ({@link #timestamp}), which campaign it belongs to ({@link #campaign}), and a
 * description of the moment. The description is captured in two complementary forms so
 * that both people and programs can consume it: a human-readable {@link #description}
 * and an optional structured {@link #details} (typically JSON) carrying machine-readable
 * detail. Every event is owned by exactly one campaign, so it exists only inside the
 * game that recorded it and never leaks into another campaign. The row is persisted in
 * the {@code campaign_events} table (server/src/main/resources/schema.sql, loaded by
 * Hibernate at bootstrap) so the campaign's event history reloads across application
 * restarts and survives across sessions.</p>
 */
@Entity
@Table(name = "campaign_events")
public class CampaignEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private CampaignEventType eventType;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * A human-readable description of the event. Present whenever a description is
     * known; may be empty for events captured only as structured {@link #details}.
     */
    @Column
    private String description;

    /**
     * Optional structured detail for the event, typically a JSON object describing the
     * event in a machine-readable form (targets, quantities, coordinates, and so on).
     * Stored as text so any structure can be recorded without a fixed schema.
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    public CampaignEvent() {
        /* Required by JPA. */
    }

    public CampaignEvent(Campaign campaign, CampaignEventType eventType, LocalDateTime timestamp) {
        this.campaign = campaign;
        this.eventType = eventType;
        this.timestamp = timestamp;
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

    public CampaignEventType getEventType() {
        return eventType;
    }

    public void setEventType(CampaignEventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Records the human-readable description of this event.
     *
     * @param description the description to store
     * @return this event, to allow chaining
     */
    public CampaignEvent withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Records structured (typically JSON) detail for this event.
     *
     * @param details the structured detail to store
     * @return this event, to allow chaining
     */
    public CampaignEvent withDetails(String details) {
        this.details = details;
        return this;
    }
}
