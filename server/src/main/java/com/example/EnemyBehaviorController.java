package com.example;

import com.example.domain.DamageType;
import com.example.domain.EnemyActionOutcome;
import com.example.service.EnemyBehaviorService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for enemy turns.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link EnemyBehaviorService} call. Target selection, attack resolution, and damage logic all
 * live in the service and the pluggable {@link com.example.service.EnemyBehaviorEngine} it owns.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class EnemyBehaviorController {

    private final EnemyBehaviorService behavior;

    public EnemyBehaviorController(EnemyBehaviorService behavior) {
        this.behavior = behavior;
    }

    /**
     * Drives an enemy's attack against the living targets of its opposition.
     *
     * <p>The enemy selects a valid living target and its attack is resolved (a d20 plus the
     * {@code attackBonus} must meet or exceed {@code difficulty}). When it lands, the target takes
     * {@code damage}. If the enemy has no valid living target the outcome reports that no action
     * was taken.</p>
     *
     * @param campaignId  the owning campaign
     * @param combatantId the enemy taking the attack
     * @param attackBonus the enemy's attack bonus added to its d20 roll (defaults to {@code 0})
     * @param damage      the damage applied when the attack lands (defaults to {@code 0})
     * @param difficulty  the roll total required to land (defaults to {@code 10})
     * @param damageType  the kind of damage the attack deals (defaults to {@code PHYSICAL})
     * @return an {@link EnemyActionOutcome} describing what happened
     */
    @PostMapping("/{campaignId}/combatants/{combatantId}/attack")
    public EnemyActionOutcome enemyAttack(@PathVariable Long campaignId,
                                          @PathVariable Long combatantId,
                                          @RequestParam(defaultValue = "0") int attackBonus,
                                          @RequestParam(defaultValue = "0") int damage,
                                          @RequestParam(defaultValue = "10") int difficulty,
                                          @RequestParam(defaultValue = "PHYSICAL") DamageType damageType) {
        return behavior.performEnemyAttack(campaignId, combatantId, attackBonus, damage, difficulty, damageType);
    }
}
