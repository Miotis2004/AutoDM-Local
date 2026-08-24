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
 * A scene is a single, contiguous slice of in-game time within a {@link Campaign}:
 * a room explored, a conversation held, or a fight fought. Encounters and combat
 * participants are anchored to a scene so that the shape of a fight can be reloaded
 * exactly as the players left it.
 *
 * <p>The entity stores the scene's {@link #title} and a free-form {@link #narrative},
 * the {@link #location} the scene takes place in, the {@link #encounter} it references
 * when a combat or other encounter is in progress, and its {@link #status}. At most one
 * scene is active per campaign at a time. Every scene belongs to exactly one campaign, so
 * a scene exists only inside the game that created it and never leaks into another
 * campaign.</p>
 */
@Entity
@Table(name = "scenes")
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** The scene's title - its short, identifying name. */
    @Column(nullable = false)
    private String title;

    /** The scene's free-form narrative description. */
    @Column(columnDefinition = "TEXT")
    private String narrative;

    /** The location the scene takes place in, if one has been recorded. */
    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    /** The encounter this scene references when a combat or other encounter is in progress. */
    @ManyToOne
    @JoinColumn(name = "encounter_id")
    private Encounter encounter;

    /** The lifecycle state of the scene. Defaults to {@link SceneStatus#READY}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SceneStatus status = SceneStatus.READY;

    /** When the scene was recorded, used to order scenes by creation. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Scene() {
        /* Required by JPA. */
    }

    public Scene(Campaign campaign, String title) {
        this.campaign = campaign;
        this.title = title;
        this.createdAt = LocalDateTime.now();
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    public SceneStatus getStatus() {
        return status;
    }

    public void setStatus(SceneStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Marks this scene as active (in focus) and ready for play.
     *
     * @return this scene, for chaining
     */
    public Scene activate() {
        this.status = SceneStatus.ACTIVE;
        return this;
    }

    /**
     * Marks this scene as completed.
     *
     * @return this scene, for chaining
     */
    public Scene complete() {
        this.status = SceneStatus.COMPLETED;
        return this;
    }

    /**
     * Marks this scene as not yet in focus.
     *
     * @return this scene, for chaining
     */
    public Scene reset() {
        this.status = SceneStatus.READY;
        return this;
    }
}
