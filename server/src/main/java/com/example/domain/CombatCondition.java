package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A condition is a status effect applied to a {@link Combatant} during an
 * {@link Encounter}: things like {@code STUNNED}, {@code PRONE}, or {@code
 * RESTRAINED}. It records the effect's own name and description, how long it lasts
 * ({@link #duration}, in rounds), what produced it ({@link #source}), and whether it
 * is currently {@link #active}.
 *
 * <p>Each condition is scoped to exactly one campaign and points at the combatant it
 * is applied to, so status effects reload across sessions within a campaign and the
 * game always knows who each effect is on.</p>
 */
@Entity
@Table(name = "combat_conditions")
public class CombatCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** The combatant this condition is applied to, if the target has been recorded. */
    @ManyToOne
    @JoinColumn(name = "combatant_id")
    private Combatant combatant;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The number of rounds the condition is expected to last, or {@code null} when it
     * persists until explicitly removed.
     */
    @Column(name = "duration")
    private Integer duration;

    /** The creature or effect that applied the condition, if known. */
    @Column
    private String source;

    /** Whether the condition is currently in effect. */
    @Column(nullable = false)
    private boolean active = true;

    /** When the condition was recorded, used only to order conditions by creation. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public CombatCondition() {
        /* Required by JPA. */
    }

    public CombatCondition(Campaign campaign, Combatant combatant, String name,
                           String description, Integer duration, String source) {
        this.campaign = campaign;
        this.combatant = combatant;
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.source = source;
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

    public Combatant getCombatant() {
        return combatant;
    }

    public void setCombatant(Combatant combatant) {
        this.combatant = combatant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Advances the condition by one round. Conditions without a finite duration are
     * left untouched; a condition whose duration reaches zero is de-activated.
     *
     * @return the updated remaining duration, or {@code null} if the condition has no
     *         finite duration
     */
    public Integer advanceOneRound() {
        if (duration == null) {
            return null;
        }
        int remaining = duration - 1;
        if (remaining <= 0) {
            this.active = false;
            this.duration = null;
            return null;
        }
        this.duration = remaining;
        return remaining;
    }
}
