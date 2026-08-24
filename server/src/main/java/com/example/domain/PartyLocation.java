package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * The party's current location for a campaign.
 *
 * <p>Each campaign has at most one current party location, identified by its
 * {@code campaign_id} primary key. It references a {@link Location} so the party can be
 * placed at any place in the world — a settlement, a point of interest, or wilderness.
 * This is what lets the game know where the party is at any given moment.</p>
 */
@Entity
@Table(name = "world_party_locations")
public class PartyLocation {

    @Id
    @Column(name = "campaign_id")
    private Long campaignId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    public PartyLocation() {
        /* Required by JPA. */
    }

    public PartyLocation(Long campaignId, Location location) {
        this.campaignId = campaignId;
        this.location = location;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
