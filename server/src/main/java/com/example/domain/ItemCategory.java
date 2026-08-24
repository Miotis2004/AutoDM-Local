package com.example.domain;

/**
 * The broad category an {@link InventoryItem} belongs to.
 *
 * <p>The set mirrors the way items are grouped at the table: combat gear
 * ({@link #WEAPON} and {@link #ARMOR}), things that are used up ({@link #CONSUMABLE}),
 * things that are tracked as story markers rather than stock ({@link #QUEST_ITEM}), and
 * everything else ({@link #MISCELLANEOUS}).</p>
 */
public enum ItemCategory {

    WEAPON,
    ARMOR,
    CONSUMABLE,
    QUEST_ITEM,
    MISCELLANEOUS
}
