package com.example.service;

import com.example.domain.Combatant;
import com.example.domain.DamageType;
import com.example.domain.EnemyActionOutcome;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The default {@link EnemyBehaviorEngine}: a simple, predictable enemy AI.
 *
 * <p>It focuses fire on the weakest living target — the living candidate with the fewest hit
 * points, ties broken by id — and resolves attacks by rolling a d20 plus the attack bonus and
 * comparing against the target's defence. Attacks use the shared {@link DiceService} so every
 * roll originates on the back-end. This keeps enemy turns easy to reason about while the
 * {@link EnemyBehaviorService} that owns it can be pointed at a richer strategy later.</p>
 */
@Component
public class DefaultEnemyBehaviorEngine implements EnemyBehaviorEngine {

    /** A d20 backs every attack roll, so attacks are decided by the same die as every other roll. */
    private static final int ATTACK_DIE = 20;

    private final DiceService dice;

    /**
     * Creates the engine over the shared dice service, so every attack roll originates on the
     * back-end just like every other roll.
     *
     * @param dice the dice service used to roll every attack
     */
    public DefaultEnemyBehaviorEngine(DiceService dice) {
        this.dice = dice;
    }

    @Override
    public Optional<Combatant> selectLivingTarget(List<Combatant> candidates) {
        if (candidates == null) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(combatant -> combatant != null)
                .filter(Combatant::isFighting)
                .min(Comparator
                        .comparingInt((Combatant combatant) -> combatant.getHitPoints())
                        .thenComparing(Combatant::getId));
    }

    @Override
    public EnemyActionOutcome resolveAttack(Combatant attacker, List<Combatant> candidates,
                                             int attackBonus, int damage, int difficulty,
                                             DamageType damageType) {
        Optional<Combatant> chosen = selectLivingTarget(candidates);
        if (chosen.isEmpty()) {
            return EnemyActionOutcome.none(attacker);
        }
        Combatant target = chosen.get();

        int roll = dice.rollSingle(ATTACK_DIE).total() + attackBonus;
        boolean hit = roll >= difficulty;
        // The engine resolves the attack but does not apply damage itself: the owning
        // {@link EnemyBehaviorService} is the single source of truth for hit-point changes
        // and persistence. Here we only project whether a landed blow would defeat the
        // target, without mutating it.
        boolean projectedDefeated = hit && (target.getHitPoints() - damage) <= 0;
        return new EnemyActionOutcome(
                true,
                attacker,
                target,
                hit,
                roll,
                difficulty,
                hit ? damage : 0,
                projectedDefeated,
                damageType);
    }
}
