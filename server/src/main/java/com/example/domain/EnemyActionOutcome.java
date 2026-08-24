package com.example.domain;

/**
 * The result of a single enemy action resolved by the {@link com.example.service.EnemyBehaviorEngine}.
 *
 * <p>This is a plain, immutable value holder describing what an enemy did (or failed to do)
 * during its turn: whether it took any action at all, which combatant attacked, which valid
 * living target it chose, whether the attack landed, the die roll that decided it, the damage
 * applied, and whether the target fell.</p>
 *
 * <p>{@link #actionTaken()} is {@code false} when the enemy had no valid living target to act
 * on — every candidate was defeated, invalid, or absent. In that case the remaining fields
 * describe a no-op: there is no target and no damage was dealt.</p>
 */
public record EnemyActionOutcome(
        boolean actionTaken,
        Combatant attacker,
        Combatant target,
        boolean hit,
        int attackRollTotal,
        int difficulty,
        int damageApplied,
        boolean targetDefeated,
        DamageType damageType) {

    /**
     * Builds a no-op outcome, used when an enemy has no valid living target.
     *
     * @param attacker the enemy that took the turn (never {@code null})
     * @return an outcome with {@link #actionTaken()} {@code false}
     */
    public static EnemyActionOutcome none(Combatant attacker) {
        return new EnemyActionOutcome(false, attacker, null, false, 0, 0, 0, false, DamageType.PHYSICAL);
    }

    /**
     * @return {@code true} when a valid living target existed and the enemy acted on it
     */
    public boolean acted() {
        return actionTaken;
    }

    /**
     * @return {@code true} when the enemy acted and its attack landed on the target
     */
    public boolean hit() {
        return actionTaken && hit;
    }
}
