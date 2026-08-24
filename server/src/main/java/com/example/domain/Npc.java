package com.example.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
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

import java.util.HashSet;
import java.util.Set;

/**
 * A non-player character controlled by the game. Each {@link Npc} belongs to exactly
 * one {@link Campaign}, so a character exists only inside the campaign that created it
 * and never leaks into another game's state.
 *
 * <p>An NPC carries the identity and story of a character — {@link #name},
 * {@link #description}, {@link #role}, {@link #disposition}, {@link #faction} — plus
 * where it currently is ({@link #location}), whether it is currently active in the
 * story ({@link #active}), and how it stands with the party ({@link #relationship})
 * with free-form {@link #notes}.</p>
 *
 * <p>Combat statistics are entirely optional and are only populated for NPCs that
 * fight. Every combat column is nullable, so a purely social NPC stores no combat
 * data; an NPC that fights fills in hit points, armor class, movement, proficiency
 * bonus, ability scores, and optionally its saving throws.</p>
 */
@Entity
@Table(name = "npcs")
public class Npc {

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

    @Column
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Disposition disposition;

    @Column
    private String faction;

    /**
     * Where this NPC currently is. Nullable because an NPC may not yet have a known
     * location in the campaign world.
     */
    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    /**
     * Whether the NPC is currently active in the story. Inactive NPCs are offstage
     * (dead, travelling, or otherwise not available) but their records are kept.
     */
    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NpcRelationship relationship;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // --- Optional combat statistics -------------------------------------

    @Column
    private Integer hitPoints;

    @Column
    private Integer maxHitPoints;

    @Column
    private Integer armorClass;

    @Column
    private Integer movement;

    @Column
    private Integer proficiencyBonus;

    @Column
    private Integer abilityStrength;

    @Column
    private Integer abilityDexterity;

    @Column
    private Integer abilityConstitution;

    @Column
    private Integer abilityIntelligence;

    @Column
    private Integer abilityWisdom;

    @Column
    private Integer abilityCharisma;

    /**
     * Attack bonus copied from a {@link CreatureTemplate} when an NPC is instantiated
     * as an enemy. Nullable; an NPC created without a template source keeps no value.
     */
    @Column
    private Integer attack;

    /**
     * Damage output copied from a {@link CreatureTemplate}. Nullable; an NPC created
     * without a template source keeps no value.
     */
    @Column
    private Integer damage;

    /**
     * Initiative modifier copied from a {@link CreatureTemplate}. Nullable; an NPC
     * created without a template source keeps no value.
     */
    @Column(name = "initiative_bonus")
    private Integer initiativeBonus;

    /**
     * Optional saving throws for this NPC, one {@link SavingThrowEntry} per ability it
     * can be thrown on. Present only when the NPC has combat statistics that include
     * saving throws; an NPC that does not fight has an empty set here.
     */
    @Embedded
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "npc_saving_throws", joinColumns = @JoinColumn(name = "npc_id"))
    private Set<SavingThrowEntry> savingThrows = new HashSet<>();

    public Npc() {
        /* Required by JPA. */
    }

    public Npc(Campaign campaign, String name, Disposition disposition, NpcRelationship relationship) {
        this.campaign = campaign;
        this.name = name;
        this.disposition = disposition;
        this.relationship = relationship;
        this.active = true;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Disposition getDisposition() {
        return disposition;
    }

    public void setDisposition(Disposition disposition) {
        this.disposition = disposition;
    }

    public String getFaction() {
        return faction;
    }

    public void setFaction(String faction) {
        this.faction = faction;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public NpcRelationship getRelationship() {
        return relationship;
    }

    public void setRelationship(NpcRelationship relationship) {
        this.relationship = relationship;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // --- Optional combat statistics -------------------------------------

    public Integer getHitPoints() {
        return hitPoints;
    }

    public void setHitPoints(Integer hitPoints) {
        this.hitPoints = hitPoints;
    }

    public Integer getMaxHitPoints() {
        return maxHitPoints;
    }

    public void setMaxHitPoints(Integer maxHitPoints) {
        this.maxHitPoints = maxHitPoints;
    }

    public Integer getArmorClass() {
        return armorClass;
    }

    public void setArmorClass(Integer armorClass) {
        this.armorClass = armorClass;
    }

    public Integer getMovement() {
        return movement;
    }

    public void setMovement(Integer movement) {
        this.movement = movement;
    }

    public Integer getProficiencyBonus() {
        return proficiencyBonus;
    }

    public void setProficiencyBonus(Integer proficiencyBonus) {
        this.proficiencyBonus = proficiencyBonus;
    }

    public Integer getAbilityStrength() {
        return abilityStrength;
    }

    public void setAbilityStrength(Integer abilityStrength) {
        this.abilityStrength = abilityStrength;
    }

    public Integer getAbilityDexterity() {
        return abilityDexterity;
    }

    public void setAbilityDexterity(Integer abilityDexterity) {
        this.abilityDexterity = abilityDexterity;
    }

    public Integer getAbilityConstitution() {
        return abilityConstitution;
    }

    public void setAbilityConstitution(Integer abilityConstitution) {
        this.abilityConstitution = abilityConstitution;
    }

    public Integer getAbilityIntelligence() {
        return abilityIntelligence;
    }

    public void setAbilityIntelligence(Integer abilityIntelligence) {
        this.abilityIntelligence = abilityIntelligence;
    }

    public Integer getAbilityWisdom() {
        return abilityWisdom;
    }

    public void setAbilityWisdom(Integer abilityWisdom) {
        this.abilityWisdom = abilityWisdom;
    }

    public Integer getAbilityCharisma() {
        return abilityCharisma;
    }

    public void setAbilityCharisma(Integer abilityCharisma) {
        this.abilityCharisma = abilityCharisma;
    }

    // --- Template-sourced combat profile --------------------------------

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

    public Integer getInitiativeBonus() {
        return initiativeBonus;
    }

    public void setInitiativeBonus(Integer initiativeBonus) {
        this.initiativeBonus = initiativeBonus;
    }

    /**
     * @return {@code true} when this NPC carries a creature-template combat profile
     * (attack, damage, and initiative bonus all present)
     */
    public boolean hasTemplateProfile() {
        return attack != null && damage != null && initiativeBonus != null;
    }

    /**
     * @return {@code true} if this NPC has been given any combat statistics at all
     */
    public Set<SavingThrowEntry> getSavingThrows() {
        return savingThrows;
    }

    public void setSavingThrows(Set<SavingThrowEntry> savingThrows) {
        this.savingThrows = savingThrows;
    }

    public boolean hasCombatStats() {
        return maxHitPoints != null
                && armorClass != null
                && abilityStrength != null
                && abilityDexterity != null
                && abilityConstitution != null
                && abilityIntelligence != null
                && abilityWisdom != null
                && abilityCharisma != null;
    }
}
