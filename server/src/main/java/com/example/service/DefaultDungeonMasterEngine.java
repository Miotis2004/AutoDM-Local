package com.example.service;

import com.example.domain.AbilityCheckResult;
import com.example.domain.ActionValidationResult;
import com.example.domain.Combatant;
import com.example.domain.PlayerAction;
import com.example.domain.PlayerActionInput;
import com.example.domain.PlayerActionResolution;
import com.example.domain.PlayerActionType;
import com.example.domain.Scene;
import com.example.domain.SceneBrief;
import com.example.domain.StateChange;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The default {@link DungeonMasterEngine}: a small, predictable, deterministic DM.
 *
 * <p>It understands a handful of plain-language action verbs - attacking, socialising, healing,
 * and escaping - and resolves each by rolling a d20 ability check through the shared {@link
 * DiceService} (never randomness from the browser), exactly like every other check on the
 * back-end. It targets a combatant by matching a name in the action, and it requests any hit-point
 * change as a {@link StateChange} intent that the owning {@link DungeonMasterService} applies and
 * persists.</p>
 *
 * <p>Narration is short and fixed, so outcomes are easy to reason about. Because everything lives
 * behind the {@link DungeonMasterEngine} interface, a richer or LLM-backed strategy can replace
 * this one without touching the {@link DungeonMasterService} or the REST surface above it.</p>
 */
@Component
public class DefaultDungeonMasterEngine implements DungeonMasterEngine {

    /** A d20 backs every resolved check, matching every other check on the back-end. */
    private static final int CHECK_DIE = 20;

    /** The damage an attack that lands deals, kept fixed so attacks are predictable. */
    private static final int ATTACK_DAMAGE = 4;

    /** The default statistic an attack resolves against. */
    private static final String ATTACK_STATISTIC = "Athletics";

    /** The default statistic a social attempt resolves against. */
    private static final String SOCIAL_STATISTIC = "Persuasion";

    private final DiceService dice;
    private final AbilityCheckService abilityChecks;
    private final ActionParser actionParser;
    private final ActionValidator actionValidator;

    /**
     * Creates the engine over the shared dice and ability-check services, so every check rolls
     * server-side just like every other check on the back-end.
     *
     * @param dice           the shared dice service (never {@code null})
     * @param abilityChecks the shared ability-check resolution service (never {@code null})
     * @param actionParser  the parser that reads structured meaning out of free-form text
     * @param actionValidator the validator that rejects impossible actions against game state
     */
    public DefaultDungeonMasterEngine(
            DiceService dice,
            AbilityCheckService abilityChecks,
            ActionParser actionParser,
            ActionValidator actionValidator) {
        this.dice = dice;
        this.abilityChecks = abilityChecks;
        this.actionParser = actionParser;
        this.actionValidator = actionValidator;
    }

    @Override
    public SceneBrief presentScene(
            Scene scene, Set<String> involvedNames, List<Combatant> sceneCombatants) {
        return SceneBrief.of(scene, involvedNames, sceneCombatants);
    }

    @Override
    public PlayerActionResolution resolvePlayerAction(
            Scene scene, List<Combatant> sceneCombatants, PlayerActionInput input) {
        if (input == null || input.action() == null || input.action().trim().isEmpty()) {
            return PlayerActionResolution.unrecognized("",
                    List.of("A player action must not be empty."));
        }

        PlayerAction structured = actionParser.parse(input.action());
        if (structured == null) {
            return PlayerActionResolution.unrecognized(
                    input.action(),
                    List.of("I do not understand that action: \"" + input.action() + "\"."));
        }

        ActionValidationResult validation = actionValidator.validate(structured, sceneCombatants);
        if (!validation.valid()) {
            return PlayerActionResolution.unrecognized(input.action(), validation.errors());
        }

        String statistic = statisticFor(structured, input);
        int difficulty = input.difficulty() > 0 ? input.difficulty()
                : PlayerActionInput.DEFAULT_DIFFICULTY;
        AbilityCheckResult check = abilityChecks.resolveWithDie(
                abilityScoreFor(statistic), input.modifier(), CHECK_DIE, difficulty);

        StateChange change = resolveStateChange(structured.type(), sceneCombatants, input.action());

        return new PlayerActionResolution(
                true,
                List.of(),
                structured.type().name(),
                check,
                change);
    }

    /**
     * Chooses the statistic that governs a validated action: the caller's explicit statistic wins,
     * otherwise the action type's own default statistic is used, a named skill governs a skill
     * action, and anything else falls back to the combat or social default.
     *
     * @param structured the structured action being resolved
     * @param input      the raw input, which may carry an explicit statistic
     * @return the governing statistic (never {@code null})
     */
    private String statisticFor(PlayerAction structured, PlayerActionInput input) {
        if (input.statistic() != null && !input.statistic().isBlank()) {
            return input.statistic();
        }
        PlayerActionType type = structured.type();
        if (type.defaultStatistic() != null) {
            return type.defaultStatistic();
        }
        if (type == PlayerActionType.SKILL && structured.hasSkill()) {
            return structured.skillName();
        }
        return type == PlayerActionType.ATTACK ? ATTACK_STATISTIC : SOCIAL_STATISTIC;
    }

    @Override
    public String generateResponse(SceneBrief scene, PlayerActionResolution resolution) {
        String place = (scene == null || scene.sceneTitle() == null || scene.sceneTitle().isEmpty())
                ? "the current scene"
                : "\"" + scene.sceneTitle() + "\"";

        if (!resolution.recognized()) {
            String reason = resolution.validationErrors().isEmpty()
                    ? "Nothing happens."
                    : resolution.validationErrors().get(0);
            return "In " + place + ", " + reason;
        }

        StringBuilder response = new StringBuilder();
        if (resolution.stateChange().applies()) {
            StateChange change = resolution.stateChange();
            String target = "combatant #" + change.combatantId();
            if (change.kind() == StateChange.Kind.DAMAGE) {
                if (resolution.check().outcome() == com.example.domain.AbilityCheckOutcome.SUCCESS) {
                    response.append("Your attack lands, dealing ")
                            .append(change.amount())
                            .append(" damage to ")
                            .append(target)
                            .append(".");
                } else {
                    response.append("Your attack on ")
                            .append(target)
                            .append(" misses in ")
                            .append(place)
                            .append(".");
                }
            } else {
                response.append("You restore ")
                        .append(change.amount())
                        .append(" hit points to ")
                        .append(target)
                        .append(" in ")
                        .append(place)
                        .append(".");
            }
            return response.toString();
        }

        // A check with no lasting world effect (a social attempt, an escape, and so on).
        String summary = abilityChecks.summarize(resolution.check());
        String verbLower = resolution.parsedVerb().toLowerCase(Locale.ROOT);
        response.append("In ")
                .append(place)
                .append(", you ")
                .append(verbLower)
                .append(". ");
        response.append(summary);
        return response.toString();
    }

    /**
     * Maps a statistic name to a raw ability score for the deterministic roll, so the default
     * engine can resolve a check even when no real character sheet is supplied.
     *
     * @param statistic the statistic name (never {@code null})
     * @return a fixed ability score backing the roll (defaults to a middling 10)
     */
    private int abilityScoreFor(String statistic) {
        return switch (statistic.toLowerCase(Locale.ROOT)) {
            case "athletics" -> 3;
            case "persuasion", "charisma" -> 2;
            case "stealth", "perception" -> 1;
            default -> 0;
        };
    }

    /**
     * Resolves any pending state change for a recognized action. An attack or heal names a specific
     * combatant to affect; a social attempt or an escape changes nothing persistent.
     *
     * @param verb          the recognised verb group
     * @param sceneCombatants the combatants present in the scene (may be {@code null} or empty)
     * @param action        the original free-form action, used to find a named target
     * @return the intended state change (never {@code null})
     */
    private StateChange resolveStateChange(
            PlayerActionType type, List<Combatant> sceneCombatants, String action) {
        if (type != PlayerActionType.ATTACK && !isHealing(action)) {
            return StateChange.none();
        }
        Long target = findNamedCombatant(sceneCombatants, action);
        if (target == null) {
            return StateChange.none();
        }
        return isHealing(action)
                ? StateChange.heal(target, 4)
                : StateChange.damage(target, ATTACK_DAMAGE);
    }

    /**
     * Detects whether an action is a healing one, so a {@link PlayerActionType#USE_ITEM} action such
     * as "heal the goblin" still resolves a healing change the way an attack resolves damage.
     *
     * @param action the free-form action text
     * @return {@code true} when the action names a healing verb
     */
    private boolean isHealing(String action) {
        if (action == null) {
            return false;
        }
        String lower = action.toLowerCase(Locale.ROOT);
        for (String healVerb : HEAL_VERBS) {
            if (lower.contains(healVerb)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the id of a combatant whose name appears as a whole word in the action. Names are
     * matched case-insensitively.
     *
     * @param sceneCombatants the combatants to search (may be {@code null} or empty)
     * @param action        the free-form action containing a possible name
     * @return the matching combatant id, or {@code null} when none is named
     */
    private Long findNamedCombatant(List<Combatant> sceneCombatants, String action) {
        if (sceneCombatants == null || action == null) {
            return null;
        }
        String[] words = action.toLowerCase(Locale.ROOT).split("\\s+");
        for (Combatant combatant : sceneCombatants) {
            if (combatant == null || combatant.getName() == null || combatant.getName().isEmpty()) {
                continue;
            }
            String name = combatant.getName().toLowerCase(Locale.ROOT);
            if (name.isEmpty()) {
                continue;
            }
            for (String word : words) {
                if (word.equals(name)) {
                    return combatant.getId();
                }
            }
        }
        return null;
    }

    /** The healing verbs the default engine recognises for a healing action. */
    private static final List<String> HEAL_VERBS = List.of(
            "heal", "cure", "mend", "restore");
}
