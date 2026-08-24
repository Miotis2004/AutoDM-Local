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
 * A persistent, limited-use ability owned by a {@link Campaign}: for example a
 * class's "Second Wind", a monk's "Ki", or a custom once-per-day power.
 *
 * <p>Each record tracks how many uses remain and how many the ability can hold at
 * peak, together with which rests reset its uses. Because the row is owned by the
 * campaign and persisted, a character's remaining uses can be spent, saved, and
 * reloaded across sessions rather than being lost when the server restarts.</p>
 */
@Entity
@Table(name = "limited_abilities")
public class LimitedUseAbility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String name;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "uses_remaining", nullable = false)
    private int usesRemaining;

    @Column(name = "recovers_on_long_rest", nullable = false)
    private boolean recoversOnLongRest;

    @Column(name = "recovers_on_short_rest", nullable = false)
    private boolean recoversOnShortRest;

    public LimitedUseAbility() {
        /* Required by JPA. */
    }

    public LimitedUseAbility(Campaign campaign, String name, int maxUses,
                             int usesRemaining, boolean recoversOnLongRest,
                             boolean recoversOnShortRest) {
        this.campaign = campaign;
        this.name = name;
        this.maxUses = maxUses;
        this.usesRemaining = usesRemaining;
        this.recoversOnLongRest = recoversOnLongRest;
        this.recoversOnShortRest = recoversOnShortRest;
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

    public int getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(int maxUses) {
        this.maxUses = maxUses;
    }

    public int getUsesRemaining() {
        return usesRemaining;
    }

    public void setUsesRemaining(int usesRemaining) {
        this.usesRemaining = usesRemaining;
    }

    public boolean isRecoversOnLongRest() {
        return recoversOnLongRest;
    }

    public void setRecoversOnLongRest(boolean recoversOnLongRest) {
        this.recoversOnLongRest = recoversOnLongRest;
    }

    public boolean isRecoversOnShortRest() {
        return recoversOnShortRest;
    }

    public void setRecoversOnShortRest(boolean recoversOnShortRest) {
        this.recoversOnShortRest = recoversOnShortRest;
    }

    /**
     * Records a single spent use, clamping the remaining count at zero.
     *
     * @return the number of uses actually spent (never negative)
     */
    public int useOnce() {
        int spent = Math.min(1, usesRemaining);
        usesRemaining -= spent;
        return spent;
    }

    /**
     * Restores a number of uses, clamped to the ability's maximum.
     *
     * @param amount the number of uses to restore
     * @return the number of uses actually restored
     */
    public int recharge(int amount) {
        int room = maxUses - usesRemaining;
        int restored = Math.min(amount, room);
        usesRemaining += restored;
        return restored;
    }
}
