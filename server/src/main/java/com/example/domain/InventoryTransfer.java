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

import java.time.LocalDateTime;

/**
 * An immutable record of a single hand-off of inventory between two owners.
 *
 * <p>A transfer moves {@link #quantity} of a specific held {@link InventoryItem}
 * from one owner ({@link #fromOwnerKind} / {@link #fromOwnerId}) to another
 * ({@link #toOwnerKind} / {@link #toOwnerId}), both within the same
 * {@link Campaign}. The record itself never mutates: applying a transfer changes the
 * two holdings' quantities, but this row only records that something happened and
 * when.</p>
 */
@Entity
@Table(name = "inventory_transfers")
public class InventoryTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /**
     * The holding that was moved. Referenced by id so the transfer remains a valid
     * historical record even if the holding's quantity later changes.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_owner_kind", nullable = false)
    private InventoryOwnerKind fromOwnerKind;

    @Column(name = "from_owner_id", nullable = false)
    private Long fromOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_owner_kind", nullable = false)
    private InventoryOwnerKind toOwnerKind;

    @Column(name = "to_owner_id", nullable = false)
    private Long toOwnerId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "transferred_at", nullable = false)
    private LocalDateTime transferredAt;

    public InventoryTransfer() {
        /* Required by JPA. */
    }

    public InventoryTransfer(Campaign campaign, InventoryItem item, int quantity,
                             InventoryOwnerKind fromOwnerKind, Long fromOwnerId,
                             InventoryOwnerKind toOwnerKind, Long toOwnerId) {
        this.campaign = campaign;
        this.item = item;
        this.quantity = quantity;
        this.fromOwnerKind = fromOwnerKind;
        this.fromOwnerId = fromOwnerId;
        this.toOwnerKind = toOwnerKind;
        this.toOwnerId = toOwnerId;
        this.transferredAt = LocalDateTime.now();
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

    public InventoryItem getItem() {
        return item;
    }

    public void setItem(InventoryItem item) {
        this.item = item;
    }

    public InventoryOwnerKind getFromOwnerKind() {
        return fromOwnerKind;
    }

    public void setFromOwnerKind(InventoryOwnerKind fromOwnerKind) {
        this.fromOwnerKind = fromOwnerKind;
    }

    public Long getFromOwnerId() {
        return fromOwnerId;
    }

    public void setFromOwnerId(Long fromOwnerId) {
        this.fromOwnerId = fromOwnerId;
    }

    public InventoryOwnerKind getToOwnerKind() {
        return toOwnerKind;
    }

    public void setToOwnerKind(InventoryOwnerKind toOwnerKind) {
        this.toOwnerKind = toOwnerKind;
    }

    public Long getToOwnerId() {
        return toOwnerId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getTransferredAt() {
        return transferredAt;
    }

    public void setTransferredAt(LocalDateTime transferredAt) {
        this.transferredAt = transferredAt;
    }
}
