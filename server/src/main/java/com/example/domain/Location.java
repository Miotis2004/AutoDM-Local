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
 * A location is a single, discoverable place in the world: a crossroads, a dungeon
 * entrance, a mountain pass, a settlement, or a point of interest.
 *
 * <p>The {@link Location} is the canonical "place" node of the world model. It is the
 * unit that the party may be located at and the endpoint of {@link TravelRoute}s, so
 * settlements and points of interest each reference one of these rows. It stores its
 * own {@link #description} and {@link #discovered} state, so a place can be explored
 * (marking it discovered) and later reloaded exactly as the players left it.</p>
 */
@Entity
@Table(name = "world_locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** The region this location belongs to, if it is part of a named region. */
    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Whether the party has already explored this place. Undiscovered places are hidden. */
    @Column(nullable = false)
    private boolean discovered;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    public Location() {
        /* Required by JPA. */
    }

    public Location(Campaign campaign, String name, String description, boolean discovered) {
        this.campaign = campaign;
        this.name = name;
        this.description = description;
        this.discovered = discovered;
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

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
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

    public boolean isDiscovered() {
        return discovered;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * Marks this place as discovered.
     *
     * @return this location, for chaining
     */
    public void discover() {
        this.discovered = true;
    }
}
