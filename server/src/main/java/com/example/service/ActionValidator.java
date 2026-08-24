package com.example.service;

import com.example.domain.Combatant;
import com.example.domain.PlayerAction;
import com.example.domain.PlayerActionType;
import com.example.domain.ActionValidationResult;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Validates a structured {@link PlayerAction} against the current game state.
 *
 * <p>The {@link ActionParser} decides what a player meant; this service decides whether that action
 * is actually possible in the scene it is being taken in. It is where "invalid or impossible
 * actions are rejected with clear errors": an attack with no living target, a rest while an enemy
 * still stands, or a skill action that never names its skill are each reported with a specific,
 * human-readable problem rather than silently resolving to nothing.</p>
 *
 * <p>Validation is stateful by design: it is handed the scene's {@link Combatant}s (the live game
 * state) so it can reason about who is present, who is fighting, and whose side they are on. Every
 * check produces either an empty (valid) result or a list of the reasons the action was refused.
 * The service holds no persistence of its own: it is a pure function of the action and the scene,
 * exactly like the {@link com.example.service.AbilityCheckService} it sits beside.</p>
 */
@Service
public class ActionValidator {

    /**
     * Validates a structured action against the combatants present in the scene.
     *
     * @param action        the structured action to validate (never {@code null})
     * @param sceneCombatants the combatants present in the scene (may be {@code null} or empty)
     * @return an {@link ActionValidationResult} describing whether the action is allowed
     */
    public ActionValidationResult validate(PlayerAction action, List<Combatant> sceneCombatants) {
        if (action == null || action.type() == null) {
            return ActionValidationResult.invalid("A player action must specify a type.");
        }

        List<Combatant> combatants = sceneCombatants == null ? List.of() : sceneCombatants;
        List<String> errors = new ArrayList<>();

        switch (action.type()) {
            case ATTACK -> validateAttack(action, combatants, errors);
            case REST -> validateRest(combatants, errors);
            case SKILL -> validateSkill(action, errors);
            case USE_ITEM -> validateUseItem(action, errors);
            case TRAVEL -> validateTravel(action, errors);
            case TALK, INTERACT, SEARCH, INVESTIGATE -> {
                /* These are always possible in principle; no state to check. */
            }
        }

        return errors.isEmpty() ? ActionValidationResult.accepted() : ActionValidationResult.invalid(errors);
    }

    /**
     * An attack must name a target that is present in the scene and still fighting. A named target
     * that no combatant matches, or one that is already down, is impossible and is rejected.
     *
     * <p>When no target is named the action is still structurally valid: the engine simply resolves
     * it without a specific target. When combatants are present the named target is checked against
     * them so an attack on an absent foe is caught.</p>
     */
    private void validateAttack(PlayerAction action, List<Combatant> combatants, List<String> errors) {
        if (!action.hasTarget()) {
            return;
        }
        Combatant target = findByName(combatants, action.targetName());
        if (target == null) {
            errors.add("You attack \"" + action.targetName() + "\", but no such combatant is present in the scene.");
            return;
        }
        if (!target.isFighting()) {
            errors.add("You cannot attack \"" + action.targetName() + "\", who is already down.");
        }
    }

    /**
     * Resting is only possible when the scene holds no active threat. A living enemy among the
     * scene's combatants makes rest impossible.
     */
    private void validateRest(List<Combatant> combatants, List<String> errors) {
        Combatant threat = combatants.stream()
                .filter(combatant -> combatant != null && combatant.isFighting())
                .filter(combatant -> combatant.getKind() == com.example.domain.CombatantKind.ENEMY)
                .findFirst()
                .orElse(null);
        if (threat != null) {
            errors.add("You cannot rest while \"" + threat.getName() + "\" is still standing.");
        }
    }

    /**
     * A skill action must name the skill it uses; a bare "make a skill check" carries no mechanic
     * and so cannot be resolved.
     */
    private void validateSkill(PlayerAction action, List<String> errors) {
        if (!action.hasSkill()) {
            errors.add("A skill action must name the skill it uses.");
        }
    }

    /**
     * Using an item must say which item is used: the free-form description is how the action names
     * its item, so a use-item action without a description is impossible to resolve.
     */
    private void validateUseItem(PlayerAction action, List<String> errors) {
        if (!action.hasDescription()) {
            errors.add("Using an item requires describing the item you use.");
        }
    }

    /**
     * Traveling requires a destination: either an explicit target or a description naming where the
     * party is heading.
     */
    private void validateTravel(PlayerAction action, List<String> errors) {
        if (!action.hasTarget() && !action.hasDescription()) {
            errors.add("Traveling requires a destination.");
        }
    }

    /**
     * Finds the combatant whose name matches the given target name, ignoring case.
     *
     * @param combatants the combatants to search
     * @param name       the target name to match
     * @return the matching combatant, or {@code null} when none matches
     */
    private Combatant findByName(List<Combatant> combatants, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (Combatant combatant : combatants) {
            if (combatant != null && combatant.getName() != null
                    && combatant.getName().toLowerCase(Locale.ROOT).equals(lower)) {
                return combatant;
            }
        }
        return null;
    }
}
