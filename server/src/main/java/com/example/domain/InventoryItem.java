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
 * A concrete inventory holding: a stack of a single item owned by one owner.
 *
 * <p>An inventory holding is the point at which an item's catalogue attributes
 * ({@link #name}, {@link #category}, {@link #value}, {@link #description}) meet an
 * owner ({@link #ownerKind} / {@link #ownerId}). The owner is either the owning
 * {@link Campaign} (a shared, campaign-wide stash) or one {@code
 * player_characters} row (a hero's personal goods). That single holding row is what
 * is transferred between owners and what survives across sessions.</p>
 *
 * <p>Item catalogue attributes are stored inline on the holding rather than on a
 * separate template table so that a transferred stack carries its own description,
 * value, and category with it.</p>
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String name;

    /**
     * The broad category of the held item (weapon, armor, consumable, quest item,
     * or miscellaneous).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemCategory category;

    /**
     * The number of identical items in this stack. Never negative; clamped at zero
     * when consumed or transferred out.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * The item's value in the campaign's lowest coin denomination. Kept as a
     * non-negative whole number so values compare and sum cleanly.
     */
    @Column(nullable = false)
    private int value;

    /**
     * Whether the item is currently equipped. Meaningful only when the item is owned
     * by a player character (for example armor or a weapon); a campaign-owned stack
     * is never equipped.
     */
    @Column(nullable = false)
    private boolean equipped;

    @Column
    private String description;

    /**
     * Whether this holding is owned by the {@link InventoryOwnerKind#CAMPAIGN} or by
     * a single {@code player_characters} row.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_kind", nullable = false)
    private InventoryOwnerKind ownerKind;

    /**
     * The id of the owning campaign or player character, depending on
     * {@link #ownerKind}.
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    public InventoryItem() {
        /* Required by JPA. */
    }

    public InventoryItem(Campaign campaign, String name, ItemCategory category, int quantity,
                         int value, String description, InventoryOwnerKind ownerKind, Long ownerId) {
        this.campaign = campaign;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.value = value;
        this.description = description;
        this.ownerKind = ownerKind;
        this.ownerId = ownerId;
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

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean isEquipped() {
        return equipped;
    }

    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InventoryOwnerKind getOwnerKind() {
        return ownerKind;
    }

    public void setOwnerKind(InventoryOwnerKind ownerKind) {
        this.ownerKind = ownerKind;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Whether this holding is owned by the given owner.
     *
     * @param kind   the owner kind to check
     * @param ownerId the id of the owning campaign or player character
     * @return {@code true} if this holding belongs to the given owner
     */
    public boolean isOwnedBy(InventoryOwnerKind kind, Long ownerId) {
        return kind == this.ownerKind
                && ownerId != null
                && ownerId.equals(this.ownerId);
    }

    /**
     * Adds a number of items to this stack, or removes them if the amount is
     * negative. The resulting quantity is clamped at zero.
     *
     * @param delta the number of items to add (or subtract when negative)
     * @return the number of items actually added to the stack (never negative)
     */
    public int adjust(int delta) {
        this.quantity = Math.max(0, this.quantity + delta);
        return Math.max(0, delta);
    }

    /**
     * Removes the requested number of items, returning the number that were removed.
     *
     * @param amount the number of items to remove
     * @return the number removed, clamped to the current stack size
     */
    public int remove(int amount) {
        int removed = Math.min(amount, quantity);
        quantity -= removed;
        return removed;
    }
}
