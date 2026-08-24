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
 * A persistent spell or power resource owned by a {@link Campaign}: the number of
 * spell slots or power points a character can spend, optionally tied to a specific
 * slot level.
 *
 * <p>The resource is stored as a campaign-scoped record so remaining points can be
 * spent and reloaded across sessions. {@link #slotLevel} is nullable so the same
 * table can model both a pool of free-form power points and per-level spell slots.</p>
 */
@Entity
@Table(name = "spell_power_resources")
public class SpellPowerResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String name;

    @Column(name = "max_points", nullable = false)
    private int maxPoints;

    @Column(name = "points_remaining", nullable = false)
    private int pointsRemaining;

    /**
     * The slot level this resource belongs to, or {@code null} for a free-form pool
     * of power points that is not tied to a particular slot level.
     */
    @Column(name = "slot_level")
    private Integer slotLevel;

    @Column(nullable = false)
    private boolean concentration;

    public SpellPowerResource() {
        /* Required by JPA. */
    }

    public SpellPowerResource(Campaign campaign, String name, int maxPoints,
                              int pointsRemaining, Integer slotLevel,
                              boolean concentration) {
        this.campaign = campaign;
        this.name = name;
        this.maxPoints = maxPoints;
        this.pointsRemaining = pointsRemaining;
        this.slotLevel = slotLevel;
        this.concentration = concentration;
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

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public int getPointsRemaining() {
        return pointsRemaining;
    }

    public void setPointsRemaining(int pointsRemaining) {
        this.pointsRemaining = pointsRemaining;
    }

    public Integer getSlotLevel() {
        return slotLevel;
    }

    public void setSlotLevel(Integer slotLevel) {
        this.slotLevel = slotLevel;
    }

    public boolean isConcentration() {
        return concentration;
    }

    public void setConcentration(boolean concentration) {
        this.concentration = concentration;
    }

    /**
     * Attempts to spend a number of points, clamping at zero.
     *
     * @param amount the number of points to spend
     * @return the number of points actually spent (never negative)
     */
    public int spend(int amount) {
        int spent = Math.min(amount, pointsRemaining);
        pointsRemaining -= spent;
        return spent;
    }

    /**
     * Restores a number of points, clamped to the resource's maximum.
     *
     * @param amount the number of points to restore
     * @return the number of points actually restored
     */
    public int restore(int amount) {
        int room = maxPoints - pointsRemaining;
        int restored = Math.min(amount, room);
        pointsRemaining += restored;
        return restored;
    }
}
