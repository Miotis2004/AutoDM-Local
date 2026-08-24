package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Persistent vitals for a {@link Campaign}: current and maximum hit points,
 * temporary health, and the character's life-state flags.
 *
 * <p>Unlike a transient field that would be lost when the server restarts, a
 * {@link CharacterVitals} row is owned by a campaign and persisted for the life of
 * the campaign, so health and life-state survive across sessions. Temporary health
 * is modelled separately from {@link #hitPoints} because it absorbs damage before
 * current hit points and does not represent lasting injury. Conditions affecting
 * the campaign are tracked as standalone {@link ConditionRecord} rows.</p>
 */
@Entity
@Table(name = "character_vitals")
public class CharacterVitals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private int hitPoints;

    @Column(name = "max_hit_points", nullable = false)
    private int maxHitPoints;

    /**
     * Temporary hit points. These absorb damage before current hit points and are
     * distinct from {@link #hitPoints} because they do not represent lasting injury.
     */
    @Column(name = "temporary_health", nullable = false)
    private int temporaryHealth;

    /**
     * Whether the character is currently unconscious.
     */
    @Column(nullable = false)
    private boolean unconscious;

    /**
     * Whether the character is currently dead.
     */
    @Column(nullable = false)
    private boolean dead;

    public CharacterVitals() {
        /* Required by JPA. */
    }

    public CharacterVitals(Campaign campaign, int hitPoints, int maxHitPoints,
                           int temporaryHealth, boolean unconscious, boolean dead) {
        this.campaign = campaign;
        this.hitPoints = hitPoints;
        this.maxHitPoints = maxHitPoints;
        this.temporaryHealth = temporaryHealth;
        this.unconscious = unconscious;
        this.dead = dead;
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

    public int getHitPoints() {
        return hitPoints;
    }

    public void setHitPoints(int hitPoints) {
        this.hitPoints = hitPoints;
    }

    public int getMaxHitPoints() {
        return maxHitPoints;
    }

    public void setMaxHitPoints(int maxHitPoints) {
        this.maxHitPoints = maxHitPoints;
    }

    public int getTemporaryHealth() {
        return temporaryHealth;
    }

    public void setTemporaryHealth(int temporaryHealth) {
        this.temporaryHealth = temporaryHealth;
    }

    public boolean isUnconscious() {
        return unconscious;
    }

    public void setUnconscious(boolean unconscious) {
        this.unconscious = unconscious;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }
}
