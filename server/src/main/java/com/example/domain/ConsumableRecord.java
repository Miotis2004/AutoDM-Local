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
 * A persistent consumable record owned by a {@link Campaign}: potions, oils,
 * wands, scrolls, rations, and other usable items that come in stacks.
 *
 * <p>Each record tracks the item's name, an optional category, and the number of
 * charges remaining. As a campaign-scoped row it can be refilled and consumed and
 * reloaded across sessions.</p>
 */
@Entity
@Table(name = "consumables")
public class ConsumableRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String name;

    /**
     * An optional category used to group consumables (for example {@code potion},
     * {@code oil}, or {@code ration}).
     */
    @Column
    private String category;

    @Column(nullable = false)
    private int count;

    public ConsumableRecord() {
        /* Required by JPA. */
    }

    public ConsumableRecord(Campaign campaign, String name, String category, int count) {
        this.campaign = campaign;
        this.name = name;
        this.category = category;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Removes a number of charges from this stack, clamping at zero.
     *
     * @param amount the number of charges to consume
     * @return the number of charges actually consumed (never negative)
     */
    public int consume(int amount) {
        int spent = Math.min(amount, count);
        count -= spent;
        return spent;
    }
}
