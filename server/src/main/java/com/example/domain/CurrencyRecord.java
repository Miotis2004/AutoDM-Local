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
 * A persistent currency record owned by a {@link Campaign}: a stack of a single
 * coin denomination held in the campaign's treasury.
 *
 * <p>Each record ties an integer amount to exactly one {@link CurrencyUnit}, so a
 * campaign can track several denominations independently. Rows are campaign-scoped
 * and can be increased, decreased, and reloaded across sessions.</p>
 */
@Entity
@Table(name = "currency")
public class CurrencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_unit", nullable = false)
    private CurrencyUnit currencyUnit;

    @Column(nullable = false)
    private int amount;

    public CurrencyRecord() {
        /* Required by JPA. */
    }

    public CurrencyRecord(Campaign campaign, CurrencyUnit currencyUnit, int amount) {
        this.campaign = campaign;
        this.currencyUnit = currencyUnit;
        this.amount = amount;
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

    public CurrencyUnit getCurrencyUnit() {
        return currencyUnit;
    }

    public void setCurrencyUnit(CurrencyUnit currencyUnit) {
        this.currencyUnit = currencyUnit;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    /**
     * Adds a number of coins to this stack. Negative amounts reduce the stack.
     *
     * @param delta the amount to add (may be negative)
     */
    public void adjust(int delta) {
        this.amount += delta;
    }
}
