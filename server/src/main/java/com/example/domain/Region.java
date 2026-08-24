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
 * A region is a large, named expanse of the world — a kingdom, province, wilderness
 * area, or continent — that groups together the {@link Location}s found within it.
 *
 * <p>Regions are the top level of the world's spatial hierarchy: locations nest
 * inside regions, settlements nest inside locations, and points of interest nest
 * inside settlements. Each region is owned by exactly one {@link Campaign}.</p>
 */
@Entity
@Table(name = "world_regions")
public class Region {

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

    public Region() {
        /* Required by JPA. */
    }

    public Region(Campaign campaign, String name, String description) {
        this.campaign = campaign;
        this.name = name;
        this.description = description;
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
}
