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
 * An encounter is a discrete beat of play anchored to a {@link Scene} and a
 * {@link Location}. It is the container that turns combat into something trackable:
 * it owns the {@link Combatant}s that take part, the {@link EncounterStatus} that
 * tracks whether the encounter is scheduled, running, or finished, and the turn
 * bookkeeping that names whose turn it is and in what order the participants act.
 *
 * <p>Every encounter belongs to exactly one campaign (a foreign key on the
 * {@code encounters} table), references one scene and one location, and stores its
 * status plus the current turn position in the {@code encounters} table, so an
 * encounter reloads across sessions within a campaign.</p>
 */
@Entity
@Table(name = "encounters")
public class Encounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** The scene this encounter takes place within, if one has been recorded. */
    @ManyToOne
    @JoinColumn(name = "scene_id")
    private Scene scene;

    /** The location this encounter takes place in, if one has been recorded. */
    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EncounterStatus status = EncounterStatus.SCHEDULED;

    /**
     * The turn-order position (1-based) of the participant whose turn it is now, or
     * {@code null} before the first turn has been taken. The order of participants is
     * given by each {@link Combatant}'s {@link Combatant#getOrder()}.
     */
    @Column(name = "current_turn")
    private Integer currentTurn;

    /** When the encounter was recorded, used to order encounters by creation. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Encounter() {
        /* Required by JPA. */
    }

    public Encounter(Campaign campaign, Scene scene, Location location) {
        this.campaign = campaign;
        this.scene = scene;
        this.location = location;
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

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EncounterStatus getStatus() {
        return status;
    }

    public void setStatus(EncounterStatus status) {
        this.status = status;
    }

    public Integer getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(Integer currentTurn) {
        this.currentTurn = currentTurn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Marks this encounter as active.
     *
     * @return this encounter, for chaining
     */
    public Encounter begin() {
        this.status = EncounterStatus.ACTIVE;
        return this;
    }

    /**
     * Marks this encounter as finished.
     *
     * @return this encounter, for chaining
     */
    public Encounter finish() {
        this.status = EncounterStatus.FINISHED;
        return this;
    }
}
