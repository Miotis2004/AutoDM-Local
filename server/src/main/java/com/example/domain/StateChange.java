package com.example.domain;

/**
 * A pending state change that the Dungeon Master engine has resolved for a player action but has
 * not yet applied.
 *
 * <p>The engine resolves the mechanic and, when the action calls for a mechanical change to the
 * world, records the intended change here as an intent. The owning {@link
 * com.example.service.DungeonMasterService} is the single source of truth for applying that change
 * (through {@link com.example.service.CombatantService}) and for persisting it; the engine itself
 * never mutates state. This keeps the engine a pure function of its inputs, exactly like the
 * {@link com.example.service.EnemyBehaviorEngine} it mirrors.</p>
 *
 * <p>A state change {@link #applies()} when it targets a specific combatant. A change that targets
 * no one (for example a purely social check, or an escape) is expressed with a {@code null}
 * {@link #combatantId()} and must not be applied.</p>
 */
public record StateChange(Kind kind, Long combatantId, int amount) {

    /** The kinds of hit-point state change the engine can request. */
    public enum Kind {
        /** Reduce a combatant's hit points (damage). */
        DAMAGE,
        /** Restore a combatant's hit points (healing). */
        HEAL
    }

    /**
     * An intent to deal the given amount of damage to a combatant.
     *
     * @param combatantId the combatant to damage (never {@code null})
     * @param amount      the damage to apply (must be positive)
     * @return the state change (never {@code null})
     */
    public static StateChange damage(Long combatantId, int amount) {
        return new StateChange(Kind.DAMAGE, combatantId, amount);
    }

    /**
     * An intent to heal the given amount to a combatant.
     *
     * @param combatantId the combatant to heal (never {@code null})
     * @param amount      the health to restore (must be non-negative)
     * @return the state change (never {@code null})
     */
    public static StateChange heal(Long combatantId, int amount) {
        return new StateChange(Kind.HEAL, combatantId, amount);
    }

    /**
     * An intent that performs no state change (a check with no lasting world effect).
     *
     * @return the empty state change (never {@code null})
     */
    public static StateChange none() {
        return new StateChange(null, null, 0);
    }

    /**
     * @return {@code true} when this change targets a specific combatant and must be applied
     */
    public boolean applies() {
        return combatantId != null && amount > 0;
    }
}
