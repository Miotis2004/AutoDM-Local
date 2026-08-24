package com.example.domain;

/**
 * The structured categories a player action can express.
 *
 * <p>Where {@link PlayerActionInput} is free-form natural language, a {@code PlayerActionType} is
 * the machine-readable meaning the engine tries to recover from that text. The set below is the
 * structured vocabulary the game understands: investigative, social, movement, combat, item, rest,
 * and general-skill actions.</p>
 *
 * <p>The list is intentionally open. Adding a new constant - for example {@code CAST_SPELL} or
 * {@code DODGE} - is a safe, backward-compatible extension: anything that does not match an existing
 * verb is simply treated as an unrecognised action today and becomes resolvable tomorrow once a
 * parser learns the new keyword. A future, richer natural-language interpreter (an LLM provider,
 * for instance) is expected to map prose onto these same constants, which is why the type carries
 * only a stable name, a human label, and an optional governing statistic - never any parsing
 * rules - so interpretation can evolve without changing the model itself.</p>
 */
public enum PlayerActionType {

    /** Investigate the surroundings or a situation, typically with an insight or perception check. */
    INVESTIGATE("Investigate", "Insight"),

    /** A social attempt directed at someone - persuade, deceive, intimidate, and so on. */
    TALK("Talk", "Persuasion"),

    /** Move from one place to another, which may require an athletic or navigation effort. */
    TRAVEL("Travel", "Athletics"),

    /** An attack against a target in the scene. */
    ATTACK("Attack", "Athletics"),

    /** Use an item - a consumable, tool, or ability - described in the action's text. */
    USE_ITEM("Use item", null),

    /** Rest to recover, which is only possible when the scene holds no active threat. */
    REST("Rest", null),

    /** Search a location or creature for something, typically with a perception check. */
    SEARCH("Search", "Perception"),

    /** Interact with the environment or a character in a general, non-social way. */
    INTERACT("Interact", "Persuasion"),

    /** A generic ability or skill check named explicitly by its skill. */
    SKILL("Skill", null);

    private final String label;
    private final String defaultStatistic;

    PlayerActionType(String label, String defaultStatistic) {
        this.label = label;
        this.defaultStatistic = defaultStatistic;
    }

    /**
     * @return the human-readable name shown to players and logs
     */
    public String label() {
        return label;
    }

    /**
     * @return the statistic that governs this action by default, or {@code null} when the action
     *         carries its own statistic (for example {@link #USE_ITEM} or {@link #REST})
     */
    public String defaultStatistic() {
        return defaultStatistic;
    }

    /**
     * Resolves a type from free-form text. Matching is case-insensitive and ignores surrounding
     * whitespace, so callers can pass either a stored {@link #name()} or a word pulled from a
     * player's sentence.
     *
     * @param text the raw text to interpret (may be {@code null})
     * @return the matching type, or {@code null} when the text matches no structured type
     */
    public static PlayerActionType fromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String key = text.trim().toUpperCase(java.util.Locale.ROOT);
        for (PlayerActionType type : values()) {
            if (type.name().equals(key) || type.label().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
