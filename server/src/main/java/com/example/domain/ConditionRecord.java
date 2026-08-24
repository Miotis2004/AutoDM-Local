package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A persistent condition owned by a {@link Campaign}: a temporary effect such as
 * {@code FRIGHTENED}, {@code RESTRAINED}, or {@code EXHAUSTION} applied to the
 * campaign's current focus.
 *
 * <p>Each record names the condition, its {@link #source}, and bookkeeping used to
 * track whether it is maintained by concentration, whether multiple stacks build up,
 * and how many rounds it lasts. Rows are campaign-scoped and reload across sessions.</p>
 */
@Entity
@Table(name = "conditions")
public class ConditionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_kind", nullable = false)
    private ConditionKind conditionKind;

    /**
     * The creature or effect that applied the condition, if known.
     */
    @Column
    private String source;

    @Column(nullable = false)
    private boolean concentration;

    @Column(nullable = false)
    private boolean stackable;

    /**
     * The number of rounds the condition is expected to last, or {@code null} when
     * it persists until it is explicitly removed.
     */
    @Column(name = "remaining_rounds")
    private Integer remainingRounds;

    public ConditionRecord() {
        /* Required by JPA. */
    }

    public ConditionRecord(Campaign campaign, String name, ConditionKind conditionKind,
                           String source, boolean concentration, boolean stackable,
                           Integer remainingRounds) {
        this.campaign = campaign;
        this.name = name;
        this.conditionKind = conditionKind;
        this.source = source;
        this.concentration = concentration;
        this.stackable = stackable;
        this.remainingRounds = remainingRounds;
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

    public ConditionKind getConditionKind() {
        return conditionKind;
    }

    public void setConditionKind(ConditionKind conditionKind) {
        this.conditionKind = conditionKind;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isConcentration() {
        return concentration;
    }

    public void setConcentration(boolean concentration) {
        this.concentration = concentration;
    }

    public boolean isStackable() {
        return stackable;
    }

    public void setStackable(boolean stackable) {
        this.stackable = stackable;
    }

    public Integer getRemainingRounds() {
        return remainingRounds;
    }

    public void setRemainingRounds(Integer remainingRounds) {
        this.remainingRounds = remainingRounds;
    }

    /**
     * Advances the condition by one round. Conditions without a finite duration are
     * left untouched.
     *
     * @return the updated remaining rounds, or {@code null} if the condition has no
     *         finite duration
     */
    public Integer advanceOneRound() {
        if (remainingRounds == null) {
            return null;
        }
        int remaining = remainingRounds - 1;
        if (remaining <= 0) {
            remainingRounds = null;
            return null;
        }
        remainingRounds = remaining;
        return remaining;
    }
}
