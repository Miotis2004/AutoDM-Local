package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A travel route is a directed connection between two {@link Location}s, carrying the
 * distance and estimated travel time between them.
 *
 * <p>Routes are campaign-scoped and may connect any two locations in the same campaign,
 * including settlements and points of interest (through their backing
 * {@link Location}). A route is directional: the reverse journey is a separate route
 * unless one is created. Routes use {@link #bidirectional} to optionally create the
 * return leg when one is added.</p>
 */
@Entity
@Table(name = "world_travel_routes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "from_location_id", "to_location_id"}))
public class TravelRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(optional = false)
    @JoinColumn(name = "from_location_id", nullable = false)
    private Location from;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_location_id", nullable = false)
    private Location to;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "travel_minutes")
    private Integer travelMinutes;

    public TravelRoute() {
        /* Required by JPA. */
    }

    public TravelRoute(Campaign campaign, Location from, Location to,
                       Double distanceKm, Integer travelMinutes) {
        this.campaign = campaign;
        this.from = from;
        this.to = to;
        this.distanceKm = distanceKm;
        this.travelMinutes = travelMinutes;
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

    public Location getFrom() {
        return from;
    }

    public void setFrom(Location from) {
        this.from = from;
    }

    public Location getTo() {
        return to;
    }

    public void setTo(Location to) {
        this.to = to;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getTravelMinutes() {
        return travelMinutes;
    }

    public void setTravelMinutes(Integer travelMinutes) {
        this.travelMinutes = travelMinutes;
    }
}
