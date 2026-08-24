package com.example.domain;

/**
 * The kinds of significant campaign event that a {@link CampaignEvent} can record.
 *
 * <p>Campaign events capture the memorable moments of a game so that a campaign keeps
 * a durable, inspectable history across sessions. The list is deliberately open:
 * callers choose the value that best describes the moment. The canonical moments are
 * the start and end of a play session, the party entering a location, a place being
 * discovered, a combat encounter, damage being dealt, an item being acquired, a quest
 * changing state, a relationship changing, and a faction's standing changing.</p>
 */
public enum CampaignEventType {
    /** The play session began. */
    SESSION_START,
    /** The party entered a location. */
    LOCATION_ENTRY,
    /** A location, point of interest, or settlement was discovered. */
    DISCOVERY,
    /** A combat encounter took place. */
    COMBAT,
    /** Damage was dealt to a character, creature, or other target. */
    DAMAGE,
    /** An item was acquired. */
    ITEM_ACQUISITION,
    /** A quest changed state (started, progressed, completed, or failed). */
    QUEST_CHANGE,
    /** A relationship between two parties changed. */
    RELATIONSHIP_CHANGE,
    /** A faction's standing (reputation) with the wider world changed. */
    STANDING_CHANGE,
    /** The play session ended. */
    SESSION_END,
    /** A short or long rest was taken. */
    REST,
    /** The Dungeon Master engine resolved a player action into a mechanical outcome. */
    GAME_ACTION
}
