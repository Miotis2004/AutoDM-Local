package com.example.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * A player-controlled character that belongs to a single {@link Campaign}.
 *
 * <p>A player character carries everything needed to represent a character at the
 * table: identity ({@link #name}, {@link #ancestry}, {@link #characterClass},
 * {@link #level}, {@link #background}, {@link #alignment}), core combat statistics
 * ({@link #hitPoints}, {@link #maxHitPoints}, {@link #armorClass},
 * {@link #movement}), and the finer-grained character progression data
 * ({@link #abilityScores}, {@link #savingThrows}, {@link #skills}, and
 * {@link #proficiencyBonus}).</p>
 *
 * <p>Each player character is owned by exactly one campaign. A campaign may own
 * many player characters, so the relationship is a one-to-many from the campaign's
 * point of view and a many-to-one from the character's point of view.</p>
 */
@Entity
@Table(name = "player_characters")
public class PlayerCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The campaign that owns this character. Every player character lives inside
     * exactly one campaign; the campaign owns the relationship.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String name;

    @Column(name = "ancestry", nullable = false)
    private String ancestry;

    /**
     * The character's class. Stored under the {@code character_class} column to
     * avoid colliding with {@link java.lang.Class}.
     */
    @Column(name = "character_class", nullable = false)
    private String characterClass;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private String background;

    @Column(nullable = false)
    private String alignment;

    @Column(name = "hit_points", nullable = false)
    private int hitPoints;

    @Column(name = "max_hit_points", nullable = false)
    private int maxHitPoints;

    @Column(name = "armor_class", nullable = false)
    private int armorClass;

    @Column(nullable = false)
    private int movement;

    /**
     * The character's proficiency bonus, applied to proficient skills and saving
     * throws.
     */
    @Column(name = "proficiency_bonus", nullable = false)
    private int proficiencyBonus;

    /**
     * The six core ability scores and their raw values.
     */
    @Embedded
    private AbilityScores abilityScores = new AbilityScores();

    /**
     * The saving throws the character can make, keyed by the ability they draw on.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "player_character_saving_throws",
            joinColumns = @JoinColumn(name = "player_character_id"))
    private List<SavingThrowEntry> savingThrows = new ArrayList<>();

    /**
     * The skills the character can attempt, together with each skill's bonus and
     * proficiency status.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "player_character_skills",
            joinColumns = @JoinColumn(name = "player_character_id"))
    private List<SkillEntry> skills = new ArrayList<>();

    public PlayerCharacter() {
        /* Required by JPA. */
    }

    public PlayerCharacter(Campaign campaign, String name, String ancestry,
                           String characterClass, int level, String background,
                           String alignment, int hitPoints, int maxHitPoints,
                           int armorClass, int movement) {
        this.campaign = campaign;
        this.name = name;
        this.ancestry = ancestry;
        this.characterClass = characterClass;
        this.level = level;
        this.background = background;
        this.alignment = alignment;
        this.hitPoints = hitPoints;
        this.maxHitPoints = maxHitPoints;
        this.armorClass = armorClass;
        this.movement = movement;
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

    public String getAncestry() {
        return ancestry;
    }

    public void setAncestry(String ancestry) {
        this.ancestry = ancestry;
    }

    public String getCharacterClass() {
        return characterClass;
    }

    public void setCharacterClass(String characterClass) {
        this.characterClass = characterClass;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
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

    public int getArmorClass() {
        return armorClass;
    }

    public void setArmorClass(int armorClass) {
        this.armorClass = armorClass;
    }

    public int getMovement() {
        return movement;
    }

    public void setMovement(int movement) {
        this.movement = movement;
    }

    public int getProficiencyBonus() {
        return proficiencyBonus;
    }

    public void setProficiencyBonus(int proficiencyBonus) {
        this.proficiencyBonus = proficiencyBonus;
    }

    public AbilityScores getAbilityScores() {
        return abilityScores;
    }

    public void setAbilityScores(AbilityScores abilityScores) {
        this.abilityScores = abilityScores;
    }

    public List<SavingThrowEntry> getSavingThrows() {
        return savingThrows;
    }

    public void setSavingThrows(List<SavingThrowEntry> savingThrows) {
        this.savingThrows = savingThrows;
    }

    public List<SkillEntry> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillEntry> skills) {
        this.skills = skills;
    }
}
