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

/**
 * A combatant is a single participant in an {@link Encounter}: a hero or an enemy.
 * It carries the identity of who is fighting, which side they are on
 * ({@link #kind}), their current and maximum {@link #hitPoints} hit points, and the
 * {@link #defeated} flag naming whether they have been knocked out.
 *
 * <p>Turn ordering lives on the combatant too: {@link #order} is the combatant's
 * position in the encounter's round and {@link #initiative} is the value used to
 * establish that order. Every combatant belongs to exactly one campaign, and each
 * one optionally points at its owning encounter and scene, so a fight reloads across
 * sessions within a campaign.</p>
 */
@Entity
@Table(name = "combatants")
public class Combatant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** The encounter this combatant takes part in, if one has been recorded. */
    @ManyToOne
    @JoinColumn(name = "encounter_id")
    private Encounter encounter;

    /** The scene this combatant is fighting within, if one has been recorded. */
    @ManyToOne
    @JoinColumn(name = "scene_id")
    private Scene scene;

    @Column(nullable = false)
    private String name;

    /** Whether the combatant is a player (a hero) or an enemy. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CombatantKind kind = CombatantKind.PLAYER;

    @Column(nullable = false)
    private int hitPoints;

    @Column(name = "max_hit_points", nullable = false)
    private int maxHitPoints;

    /**
     * The value rolled for initiative; used to establish turn order. May be
     * {@code null} when initiative has not yet been rolled.
     */
    @Column(name = "initiative")
    private Integer initiative;

    /**
     * The combatant's position in the encounter's round (1-based). Assigned when the
     * turn order is built; {@code null} before the order is established.
     */
    @Column
    private Integer order;

    /** Whether the combatant has been defeated. */
    @Column(nullable = false)
    private boolean defeated;

    public Combatant() {
        /* Required by JPA. */
    }

    public Combatant(Campaign campaign, String name, CombatantKind kind,
                     int hitPoints, int maxHitPoints) {
        this.campaign = campaign;
        this.name = name;
        this.kind = kind;
        this.hitPoints = hitPoints;
        this.maxHitPoints = maxHitPoints;
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

    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CombatantKind getKind() {
        return kind;
    }

    public void setKind(CombatantKind kind) {
        this.kind = kind;
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

    public Integer getInitiative() {
        return initiative;
    }

    public void setInitiative(Integer initiative) {
        this.initiative = initiative;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public boolean isDefeated() {
        return defeated;
    }

    public void setDefeated(boolean defeated) {
        this.defeated = defeated;
    }

    /**
     * Applies hit-point damage, clamping at zero and marking the combatant defeated
     * when the result reaches zero or below.
     *
     * @param delta the damage to apply (a positive number reduces hit points)
     * @return this combatant, for chaining
     */
    public Combatant takeDamage(int delta) {
        this.hitPoints = Math.max(0, this.hitPoints - delta);
        if (this.hitPoints <= 0) {
            this.defeated = true;
        }
        return this;
    }

    /**
     * Restores hit points, capped at the maximum.
     *
     * @param amount the health to restore
     * @return this combatant, for chaining
     */
    public Combatant heal(int amount) {
        this.hitPoints = Math.min(maxHitPoints, this.hitPoints + amount);
        return this;
    }

    /**
     * Marks the combatant defeated.
     *
     * @return this combatant, for chaining
     */
    public Combatant defeat() {
        this.defeated = true;
        return this;
    }

    /**
     * @return {@code true} while the combatant still has hit points left
     */
    public boolean isFighting() {
        return !defeated;
    }
}
