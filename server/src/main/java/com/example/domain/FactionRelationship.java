package com.example.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

/**
 * The directed relationship one {@link Faction} holds toward another {@link Faction}
 * within a campaign. A relationship is always between two factions of the same
 * campaign and is captured by the faction it points at
 * ({@link #relatedFactionId}) and the {@link #relationship} describing it.
 *
 * <p>This is an embeddable value object rather than its own entity so that faction
 * relationships are stored as part of the owning campaign's {@link Faction} rows in
 * the {@code faction_relationships} table instead of as separate rows in their own
 * table. The pair is value-equal, so it is safe to keep inside a {@link java.util.Set}.</p>
 */
@Embeddable
public class FactionRelationship {

    /**
     * The id of the faction this relationship points at. Within a campaign this is the
     * other faction the relationship describes; it mirrors the foreign key on the
     * {@code faction_relationships.related_faction_id} column.
     */
    private Long relatedFactionId;

    /**
     * How the acting {@link Faction} regards the related faction. Broader than a
     * momentary {@link Disposition}, this captures the durable standing (allied,
     * neutral, foe, and so on) between the two factions.
     */
    @Enumerated(EnumType.STRING)
    private NpcRelationship relationship;

    public FactionRelationship() {
        /* Required by JPA. */
    }

    public FactionRelationship(Long relatedFactionId, NpcRelationship relationship) {
        this.relatedFactionId = relatedFactionId;
        this.relationship = relationship;
    }

    public Long getRelatedFactionId() {
        return relatedFactionId;
    }

    public void setRelatedFactionId(Long relatedFactionId) {
        this.relatedFactionId = relatedFactionId;
    }

    public NpcRelationship getRelationship() {
        return relationship;
    }

    public void setRelationship(NpcRelationship relationship) {
        this.relationship = relationship;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FactionRelationship that)) {
            return false;
        }
        return Objects.equals(relatedFactionId, that.relatedFactionId)
                && relationship == that.relationship;
    }

    @Override
    public int hashCode() {
        return Objects.hash(relatedFactionId, relationship);
    }

    @Override
    public String toString() {
        return "FactionRelationship{relatedFactionId=" + relatedFactionId
                + ", relationship=" + relationship + '}';
    }
}
