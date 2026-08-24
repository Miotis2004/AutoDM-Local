package com.example.domain;

/**
 * The kind of thing that can own an {@link InventoryItem}.
 *
 * <p>An inventory holding may belong either to the whole campaign (for example a
 * party treasury or a shared stash) or to a single player character (for example a
 * hero's backpack). Transfers move holdings between two of these owners.</p>
 */
public enum InventoryOwnerKind {

    CAMPAIGN,
    PLAYER_CHARACTER
}
