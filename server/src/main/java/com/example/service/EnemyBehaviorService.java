package com.example.service;

import com.example.domain.Combatant;
import com.example.domain.CombatantKind;
import com.example.domain.Campaign;
import com.example.domain.CampaignEvent;
import com.example.domain.CampaignEventType;
import com.example.domain.DamageType;
import com.example.domain.EnemyActionOutcome;
import com.example.db.CampaignEventRepository;
import com.example.db.CampaignRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for enemy turns.
 *
 * <p>This service is the single place an enemy's action is driven end to end: it gathers the
 * valid living targets an enemy may act on, delegates the target choice and attack resolution to
 * a pluggable {@link EnemyBehaviorEngine}, and applies the resulting damage through
 * {@link CombatantService}. Because the behaviour lives behind the {@link EnemyBehaviorEngine}
 * interface, a richer AI can replace the default strategy without changing how this service, or
 * the REST surface above it, works — simply provide a different engine implementation.</p>
 *
 * <p>The engine is swappable at runtime via {@link #setEnemyBehaviorEngine}, so a campaign (or a
 * test) can install a bespoke strategy while the application is running.</p>
 */
@Service
public class EnemyBehaviorService {

    /** The default roll total an enemy attack must meet to land, mirroring a base armour class. */
    public static final int DEFAULT_ATTACK_DIFFICULTY = 10;

    private EnemyBehaviorEngine engine;

    private final CombatantService combatants;
    private final CampaignEventRepository events;
    private final CampaignRepository campaigns;

    /**
     * Creates the service over the default enemy AI.
     *
     * @param combatants the combatant service that owns hit-point bookkeeping and persistence
     * @param events     the repository used to record attack outcomes as campaign events
     */
    public EnemyBehaviorService(CombatantService combatants, DefaultEnemyBehaviorEngine engine,
                                CampaignEventRepository events, CampaignRepository campaigns) {
        this.combatants = combatants;
        this.engine = engine;
        this.events = events;
        this.campaigns = campaigns;
    }

    /**
     * Installs a behaviour engine. Because the engine is an interface, any implementation — for
     * example a future, more sophisticated AI — can be plugged in here, replacing the default.
     *
     * @param engine the engine to use for selecting targets and resolving attacks
     */
    public void setEnemyBehaviorEngine(EnemyBehaviorEngine engine) {
        this.engine = engine;
    }

    /**
     * @return the behaviour engine currently driving enemy actions
     */
    public EnemyBehaviorEngine getEnemyBehaviorEngine() {
        return engine;
    }

    /**
     * Selects the valid living target an enemy would act on, of the given kind.
     *
     * <p>Defeated combatants and combatants not of the requested kind are skipped, as is the enemy
     * itself. This is the enemy-targeting rule exposed on its own so callers can ask "who would an
     * enemy attack?" without resolving an attack.</p>
     *
     * @param campaignId the owning campaign
     * @param enemyId    the enemy choosing a target
     * @param targetKind the kind of combatant the enemy may target (for example {@link CombatantKind#PLAYER})
     * @return the chosen living target, or empty when none is available
     */
    public Optional<Combatant> selectLivingTarget(Long campaignId, Long enemyId, CombatantKind targetKind) {
        Combatant enemy = combatants.getCombatant(campaignId, enemyId);
        Optional<Long> encounterId = encounterIdFor(enemy);
        if (encounterId.isEmpty()) {
            return Optional.empty();
        }
        return engine.selectLivingTarget(livingTargets(campaignId, encounterId.get(), enemy, targetKind));
    }

    /**
     * Drives an enemy's attack against the living targets of its opposition, using the default
     * attack difficulty.
     *
     * @param campaignId  the owning campaign
     * @param enemyId     the enemy taking the attack
     * @param attackBonus the enemy's attack bonus added to its d20 roll (may be negative)
     * @param damage      the damage applied when the attack lands (may be zero)
     * @return an {@link EnemyActionOutcome} describing what happened
     */
    public EnemyActionOutcome performEnemyAttack(Long campaignId, Long enemyId,
                                                 int attackBonus, int damage) {
        return performEnemyAttack(campaignId, enemyId, attackBonus, damage, DEFAULT_ATTACK_DIFFICULTY,
                DamageType.PHYSICAL);
    }

    /**
     * Drives an enemy's attack against the living targets of its opposition.
     *
     * <p>The enemy selects a valid living target and its attack is resolved by the
     * {@link EnemyBehaviorEngine}. When the attack lands, the target's hit points are reduced and
     * it is marked defeated at zero through {@link CombatantService}, so the result is persisted.
     * When the enemy has no valid living target, the outcome reports {@link
     * EnemyActionOutcome#actionTaken()} as {@code false} and nothing is damaged.</p>
     *
     * @param campaignId  the owning campaign
     * @param enemyId     the enemy taking the attack
     * @param attackBonus the enemy's attack bonus added to its d20 roll (may be negative)
     * @param damage      the damage applied when the attack lands (may be zero)
     * @param difficulty  the roll total required to land the attack (typically a target's armour class)
     * @return an {@link EnemyActionOutcome} describing what happened
     */
    public EnemyActionOutcome performEnemyAttack(Long campaignId, Long enemyId,
                                                 int attackBonus, int damage, int difficulty) {
        return performEnemyAttack(campaignId, enemyId, attackBonus, damage, difficulty, DamageType.PHYSICAL);
    }

    /**
     * Drives an enemy's attack against the living targets of its opposition.
     *
     * <p>The enemy selects a valid living target and its attack is resolved by the
     * {@link EnemyBehaviorEngine}. When the attack lands, the target's hit points are reduced and
     * it is marked defeated at zero through {@link CombatantService}, so the result is persisted.
     * When the enemy has no valid living target, the outcome reports {@link
     * EnemyActionOutcome#actionTaken()} as {@code false} and nothing is damaged. Whenever an
     * attack lands and deals damage, the outcome is recorded on the campaign through the event
     * system (a {@link CampaignEventType#DAMAGE} event) so the attack and its damage are available
     * to the encounter engine and to anyone consulting the campaign's event history.</p>
     *
     * @param campaignId  the owning campaign
     * @param enemyId     the enemy taking the attack
     * @param attackBonus the enemy's attack bonus added to its d20 roll (may be negative)
     * @param damage      the damage applied when the attack lands (may be zero)
     * @param difficulty  the roll total required to land the attack (typically a target's armour class)
     * @param damageType  the kind of damage the attack deals (defaults to {@link DamageType#PHYSICAL})
     * @return an {@link EnemyActionOutcome} describing what happened
     */
    public EnemyActionOutcome performEnemyAttack(Long campaignId, Long enemyId,
                                                 int attackBonus, int damage, int difficulty,
                                                 DamageType damageType) {
        Combatant enemy = combatants.getCombatant(campaignId, enemyId);
        Optional<Long> encounterId = encounterIdFor(enemy);
        if (encounterId.isEmpty()) {
            return EnemyActionOutcome.none(enemy);
        }
        List<Combatant> targets = livingTargets(campaignId, encounterId.get(), enemy, CombatantKind.PLAYER);
        EnemyActionOutcome outcome = engine.resolveAttack(
                enemy, targets, attackBonus, damage, difficulty, damageType);
        if (outcome.hit()) {
            Combatant damaged = combatants.applyDamage(campaignId, outcome.target().getId(), outcome.damageApplied());
            EnemyActionOutcome resolved = new EnemyActionOutcome(
                    outcome.actionTaken(),
                    outcome.attacker(),
                    damaged,
                    outcome.hit(),
                    outcome.attackRollTotal(),
                    outcome.difficulty(),
                    outcome.damageApplied(),
                    damaged.isDefeated(),
                    outcome.damageType());
            recordAttackEvent(campaignId, enemy, outcome.target(), resolved);
            return resolved;
        }
        return outcome;
    }

    /**
     * Records a landed enemy attack as a {@link CampaignEventType#DAMAGE} campaign event so the
     * attack and the damage it dealt are available through the campaign's event history. Recorded
     * only when damage actually landed, so misses and no-ops leave the history untouched.
     *
     * @param campaignId the owning campaign
     * @param attacker   the enemy whose attack landed
     * @param target     the combatant that was hit
     * @param outcome    the resolved attack, describing what happened
     */
    private void recordAttackEvent(Long campaignId, Combatant attacker, Combatant target,
                                   EnemyActionOutcome outcome) {
        if (outcome.damageApplied() <= 0) {
            return;
        }
        String description = attacker.getName() + " hit " + target.getName() + " for "
                + outcome.damageApplied() + " " + outcome.damageType() + " damage";
        events.save(new CampaignEvent(
                requireCampaign(campaignId), CampaignEventType.DAMAGE,
                java.time.LocalDateTime.now())
                .withDescription(description)
                .withDetails("{\"attacker\":" + attacker.getId()
                        + ",\"target\":" + target.getId()
                        + ",\"damage\":" + outcome.damageApplied()
                        + ",\"damageType\":\"" + outcome.damageType()
                        + "\",\"rollTotal\":" + outcome.attackRollTotal()
                        + ",\"difficulty\":" + outcome.difficulty() + "}"));
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    /**
     * The combatants an enemy may target in its encounter: every combatant of the requested kind
     * except the enemy itself. Defeated combatants are included here so that the engine's
     * target-selection rule has defeated entries to skip.
     *
     * @param campaignId the owning campaign
     * @param encounterId the encounter the fighting takes place in
     * @param enemy      the enemy choosing a target (excluded from its own candidates)
     * @param targetKind the kind of combatant to collect
     * @return the candidate combatants, defeated ones included for the engine to skip
     */
    private List<Combatant> livingTargets(Long campaignId, Long encounterId, Combatant enemy, CombatantKind targetKind) {
        return combatants.listCombatantsOfEncounter(campaignId, encounterId).stream()
                .filter(combatant -> combatant != null)
                .filter(combatant -> combatant.getId() != null && !combatant.getId().equals(enemy.getId()))
                .filter(combatant -> targetKind == combatant.getKind())
                .toList();
    }

    private Optional<Long> encounterIdFor(Combatant enemy) {
        if (enemy.getEncounter() == null || enemy.getEncounter().getId() == null) {
            return Optional.empty();
        }
        return Optional.of(enemy.getEncounter().getId());
    }
}
