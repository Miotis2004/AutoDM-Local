package com.example.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

/**
 * A faction is a group, organization, or power that shares a common purpose within a
 * single {@link Campaign}. Each {@link Faction} belongs to exactly one campaign, so a
 * faction exists only inside the game that created it and never leaks into another
 * game's state.
 *
 * <p>The entity stores the identity and nature of a group: its {@link #name},
 * {@link #description}, the {@link #disposition} it generally shows, its
 * {@link #reputation} (the durable standing the wider world holds toward it), and
 * free-form {@link #notes}. Factions can also be linked to one another with directed
 * {@link #relationships}, capturing alliances, rivalries, and so on. Every faction is
 * owned by a campaign through a foreign key on the {@code factions} table.</p>
 */
@Entity
@Table(name = "factions")
public class Faction {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Disposition disposition;

    /**
     * The durable standing the wider world holds toward this faction. Reuses
     * {@link NpcRelationship}, the same closed set used for an NPC's standing with the
     * party, so reputation is comparable and filterable rather than free-form text.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NpcRelationship reputation;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * The directed relationships this faction holds toward other factions in the same
     * campaign. Each entry names the related faction and the {@link NpcRelationship}
     * that describes the bond. Present only when at least one relationship has been
     * recorded; a faction with no links has an empty set here.
     */
    @Embedded
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "faction_relationships",
            joinColumns = @JoinColumn(name = "faction_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"faction_id", "related_faction_id", "relationship"}))
    private Set<FactionRelationship> relationships = new HashSet<>();

    public Faction() {
        /* Required by JPA. */
    }

    public Faction(Campaign campaign, String name, Disposition disposition, NpcRelationship reputation) {
        this.campaign = campaign;
        this.name = name;
        this.disposition = disposition;
        this.reputation = reputation;
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

    public Disposition getDisposition() {
        return disposition;
    }

    public void setDisposition(Disposition disposition) {
        this.disposition = disposition;
    }

    public NpcRelationship getReputation() {
        return reputation;
    }

    public void setReputation(NpcRelationship reputation) {
        this.reputation = reputation;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Set<FactionRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(Set<FactionRelationship> relationships) {
        this.relationships = relationships == null ? new HashSet<>() : relationships;
    }

    /**
     * Records (or overwrites, if it already exists) this faction's relationship toward
     * the given faction. Idempotent for the same pair and relationship.
     */
    public void setRelationship(Long relatedFactionId, NpcRelationship relationship) {
        this.relationships.remove(new FactionRelationship(relatedFactionId, relationship));
        this.relationships.add(new FactionRelationship(relatedFactionId, relationship));
    }

    /**
     * Removes this faction's relationship toward the given faction, if present.
     */
    public boolean removeRelationship(Long relatedFactionId, NpcRelationship relationship) {
        return this.relationships.remove(new FactionRelationship(relatedFactionId, relationship));
    }
}
