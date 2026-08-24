package com.example.service;

import com.example.domain.Combatant;
import com.example.domain.DamageType;
import com.example.domain.EnemyActionOutcome;

import java.util.List;
import java.util.Optional;

/**
 * The pluggable behavior that drives an enemy's actions in combat.
 *
 * <p>The engine answers two questions for an enemy combatant: which valid living target it
 * should act on, and how its attack resolves. It is deliberately modelled as an interface so
 * that the simple default strategy can be swapped for a richer AI without touching the rest of
 * the combat system — {@link EnemyBehaviorService} holds a single {@code EnemyBehaviorEngine}
 * that can be replaced by any implementation.</p>
 *
 * <p>Implementations must skip defeated or otherwise invalid candidates: {@link #selectLivingTarget}
 * only ever returns living targets, and {@link #resolveAttack} reports {@link EnemyActionOutcome#actionTaken()}
 * as {@code false} when no valid living target exists.</p>
 */
public interface EnemyBehaviorEngine {

    /**
     * Selects a valid living target for an enemy's action from a set of candidates.
     *
     * <p>Defeated combatants, {@code null} entries, and any other invalid candidate are skipped.
     * The returned target is always a combatant that is still fighting.</p>
     *
     * @param candidates the combatants to choose from (may contain defeated or {@code null}
     *                   entries, which must be ignored)
     * @return the chosen living target, or empty when none is valid
     */
    Optional<Combatant> selectLivingTarget(List<Combatant> candidates);

    /**
     * Resolves an attack made by an enemy against its chosen living target.
     *
     * <p>The enemy rolls a d20 plus its attack bonus and lands the attack when the total meets
     * or exceeds the given difficulty (typically the target's armor class). When the attack
     * lands the target is damaged by the given amount. If the enemy has no valid living target,
     * the returned outcome reports {@link EnemyActionOutcome#actionTaken()} as {@code false} and
     * no damage is applied.</p>
     *
     * @param attacker   the enemy making the attack (never {@code null})
     * @param candidates the living candidates the enemy may target (defeated entries skipped)
     * @param attackBonus the attack bonus added to the d20 roll (may be negative)
     * @param damage     the damage applied when the attack lands (never negative)
     * @param difficulty the roll total required to land the attack (typically armor class)
     * @param damageType the kind of damage the attack deals (defaults to physical when unknown)
     * @return an {@link EnemyActionOutcome} describing what happened
     */
    EnemyActionOutcome resolveAttack(Combatant attacker, List<Combatant> candidates,
                                     int attackBonus, int damage, int difficulty, DamageType damageType);
}
