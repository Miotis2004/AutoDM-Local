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
 * A point of interest is a notable building or spot within a settlement — a tavern, a
 * temple, a market, a castle, and so on — that players may want to visit.
 *
 * <p>Like a settlement, a point of interest is a {@link Location} plus a
 * {@link #category}. The backing location stores the point of interest's description
 * and discovered state.</p>
 */
@Entity
@Table(name = "world_points_of_interest")
public class PointOfInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** The underlying place that makes this a location in the world. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /** The settlement this point of interest belongs to, if any. */
    @ManyToOne
    @JoinColumn(name = "settlement_id")
    private Settlement settlement;

    @Column(nullable = false)
    private PointOfInterestCategory category;

    public PointOfInterest() {
        /* Required by JPA. */
    }

    public PointOfInterest(Campaign campaign, Location location,
                           PointOfInterestCategory category, Settlement settlement) {
        this.campaign = campaign;
        this.location = location;
        this.category = category;
        this.settlement = settlement;
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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Settlement getSettlement() {
        return settlement;
    }

    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
    }

    public PointOfInterestCategory getCategory() {
        return category;
    }

    public void setCategory(PointOfInterestCategory category) {
        this.category = category;
    }
}
