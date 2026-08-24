package com.example.domain;

import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * A reference from a {@link Quest} to a related {@link Location} within the same
 * campaign. A quest can name any number of places its objectives point at, and each
 * reference is captured here by the {@link #locationId} of a location in the owning
 * campaign.
 *
 * <p>This is an embeddable value object rather than its own entity so that related
 * locations are stored as part of the owning campaign's {@link Quest} rows in the
 * {@code quest_related_locations} table instead of in a separate table. The pair is
 * value-equal, so it is safe to keep inside a {@link java.util.Set}.</p>
 */
@Embeddable
public class QuestLocationRef {

    /**
     * The id of the related location. Within a campaign this is the location the quest
     * points at; it mirrors the foreign key on the
     * {@code quest_related_locations.location_id} column.
     */
    private Long locationId;

    public QuestLocationRef() {
        /* Required by JPA. */
    }

    public QuestLocationRef(Long locationId) {
        this.locationId = locationId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QuestLocationRef that)) {
            return false;
        }
        return Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationId);
    }

    @Override
    public String toString() {
        return "QuestLocationRef{locationId=" + locationId + '}';
    }
}
