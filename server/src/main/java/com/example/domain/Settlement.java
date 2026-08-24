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
 * A settlement is a location where people live or gather — a hamlet, village, town,
 * city, fortress, or the ruins of one.
 *
 * <p>A settlement is modelled as a {@link Location} plus settlement-specific
 * attributes: its {@link #type} and reported {@link #population}. The backing
 * location carries the settlement's description and discovered state, so discovery of
 * a settlement is tracked exactly like discovery of any other place.</p>
 */
@Entity
@Table(name = "world_settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** The underlying place that makes this settlement a location in the world. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private SettlementType type;

    @Column(nullable = false)
    private int population;

    public Settlement() {
        /* Required by JPA. */
    }

    public Settlement(Campaign campaign, Location location, SettlementType type, int population) {
        this.campaign = campaign;
        this.location = location;
        this.type = type;
        this.population = population;
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

    public SettlementType getType() {
        return type;
    }

    public void setType(SettlementType type) {
        this.type = type;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }
}
