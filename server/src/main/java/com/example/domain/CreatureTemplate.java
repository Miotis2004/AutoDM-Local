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
 * A reusable creature or enemy definition. A template captures everything the game
 * needs to describe an adversary — its {@link #name} and {@link #description}, its
 * combat profile ({@link #health}, {@link #defense}, {@link #attack},
 * {@link #damage}, and {@link #initiativeModifier}), and free-form
 * {@link #behaviorNotes} describing how it fights or acts.
 *
 * <p>A template is a reusable blueprint rather than a living enemy in a game: the same
 * template can be instantiated into many distinct enemies over the life of a campaign
 * and across campaigns. Each {@link CreatureTemplate} belongs to exactly one
 * {@link Campaign} (a foreign key on the {@code creature_templates} table), so a
 * template exists only inside the game that created it and never leaks into another
 * campaign's catalogue of foes.</p>
 *
 * <p>All combat values are nullable, so a template may carry only partial statistics;
 * an empty or fully populated profile is stored identically. {@link #health},
 * {@link #defense}, {@link #attack}, and {@link #damage} are the creature's hit
 * points, armor class, attack bonus, and damage output respectively.
 * {@link #initiativeModifier} is the modifier applied when the creature rolls for
 * initiative, and {@link #behaviorNotes} holds tactical or roleplay guidance.</p>
 */
@Entity
@Table(name = "creature_templates")
public class CreatureTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Hit points the creature starts each fight with. Nullable. */
    @Column
    private Integer health;

    /** Armor class or equivalent defence. Nullable. */
    @Column
    private Integer defense;

    /** Attack bonus. Nullable. */
    @Column
    private Integer attack;

    /** Damage output of the creature's primary attack. Nullable. */
    @Column
    private Integer damage;

    /** Modifiers applied when the creature rolls initiative. Nullable. */
    @Column(name = "initiative_modifier")
    private Integer initiativeModifier;

    /** Free-form tactical or roleplay guidance for running the creature. */
    @Column(columnDefinition = "TEXT")
    private String behaviorNotes;

    public CreatureTemplate() {
        /* Required by JPA. */
    }

    public CreatureTemplate(Campaign campaign, String name) {
        this.campaign = campaign;
        this.name = name;
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

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }

    public Integer getDefense() {
        return defense;
    }

    public void setDefense(Integer defense) {
        this.defense = defense;
    }

    public Integer getAttack() {
        return attack;
    }

    public void setAttack(Integer attack) {
        this.attack = attack;
    }

    public Integer getDamage() {
        return damage;
    }

    public void setDamage(Integer damage) {
        this.damage = damage;
    }

    public Integer getInitiativeModifier() {
        return initiativeModifier;
    }

    public void setInitiativeModifier(Integer initiativeModifier) {
        this.initiativeModifier = initiativeModifier;
    }

    public String getBehaviorNotes() {
        return behaviorNotes;
    }

    public void setBehaviorNotes(String behaviorNotes) {
        this.behaviorNotes = behaviorNotes;
    }
}
