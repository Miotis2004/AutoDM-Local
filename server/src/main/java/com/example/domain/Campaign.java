package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A campaign represents a single, self-contained game session. Each campaign owns
 * its own isolated state so that games run within it never leak into or depend on
 * the state of any other campaign.
 *
 * <p>The entity is a plain domain model holding the metadata that describes a
 * campaign (title, description, status, dates, notes) together with the isolated
 * game {@link #state}.</p>
 */
@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "last_played_at")
    private LocalDate lastPlayedAt;

    @Column
    private String notes;

    /**
     * The isolated per-campaign game state. Stored as text (typically JSON) so that
     * each campaign keeps its game state entirely separate from every other campaign.
     */
    @Column(columnDefinition = "TEXT")
    private String state;

    public Campaign() {
        /* Required by JPA. */
    }

    public Campaign(String title, CampaignStatus status) {
        this.title = title;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getLastPlayedAt() {
        return lastPlayedAt;
    }

    public void setLastPlayedAt(LocalDate lastPlayedAt) {
        this.lastPlayedAt = lastPlayedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
