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
 * A persistent ammunition record owned by a {@link Campaign}: a stack of arrows,
 * bolts, darts, bullets, and so on, together with how many remain.
 *
 * <p>Ammunition is a shared, consumable resource, so it lives as a campaign-scoped
 * row that can be refilled and spent and then reloaded across sessions.</p>
 */
@Entity
@Table(name = "ammunition")
public class AmmunitionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "ammo_type", nullable = false)
    private String ammoType;

    @Column(nullable = false)
    private int count;

    public AmmunitionRecord() {
        /* Required by JPA. */
    }

    public AmmunitionRecord(Campaign campaign, String ammoType, int count) {
        this.campaign = campaign;
        this.ammoType = ammoType;
        this.count = count;
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

    public String getAmmoType() {
        return ammoType;
    }

    public void setAmmoType(String ammoType) {
        this.ammoType = ammoType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Removes a number of rounds from this stack, clamping at zero.
     *
     * @param amount the number of rounds to spend
     * @return the number of rounds actually spent (never negative)
     */
    public int spend(int amount) {
        int spent = Math.min(amount, count);
        count -= spent;
        return spent;
    }
}
