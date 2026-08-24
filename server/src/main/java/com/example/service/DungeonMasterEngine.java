package com.example.service;

import com.example.domain.Combatant;
import com.example.domain.PlayerActionInput;
import com.example.domain.PlayerActionResolution;
import com.example.domain.Scene;
import com.example.domain.SceneBrief;

import java.util.List;
import java.util.Set;

/**
 * The pluggable Dungeon Master engine.
 *
 * <p>This interface is the cognitive core of the DM application: it turns a free-form player
 * action into a mechanical verdict and a narrative response. It is deliberately modelled as an
 * interface so the deterministic {@link DefaultDungeonMasterEngine} that drives the initial
 * application can be swapped for a richer, data-driven, or LLM-backed strategy without rewriting
 * the rest of the app. The {@link DungeonMasterService} that owns a single
 * {@code DungeonMasterEngine} can be pointed at any implementation.</p>
 *
 * <p>The interface abstracts the two things an LLM provider would specialise: <em>action
 * handling</em> (understanding an action, validating it, and resolving the mechanic that governs
 * it) and <em>response generation</em> (narrating the outcome). A local or remote LLM provider can
 * implement either or both seams while the surrounding service, persistence, and REST surface stay
 * unchanged.</p>
 *
 * <p>The engine never mutates game state: it resolves the governing ability check and, when an
 * action calls for a mechanical change, records the intended change as a {@link
 * com.example.domain.StateChange}. The owning service is the single source of truth for applying
 * that change (through {@link CombatantService}) and for persisting it. Every method is a pure
 * function of its inputs plus freshly generated randomness, exactly like the
 * {@link EnemyBehaviorEngine} it mirrors.</p>
 */
public interface DungeonMasterEngine {

    /**
     * Presents the current scene to the players as a compact {@link SceneBrief}.
     *
     * <p>The engine uses this to "present scene info": a readable snapshot of the active scene and
     * the combatants present in it. The brief is rebuilt every time the engine acts, so it always
     * reflects the latest state.</p>
     *
     * @param scene          the active scene (never {@code null})
     * @param involvedNames the names of the characters and NPCs involved in the scene (nullable)
     * @param sceneCombatants the combatants present in the scene (may be {@code null} or empty)
     * @return the scene brief (never {@code null})
     */
    SceneBrief presentScene(
            Scene scene, Set<String> involvedNames, List<Combatant> sceneCombatants);

    /**
     * Validates a player action and resolves the mechanic that governs it.
     *
     * <p>This is the engine's "action handling": it understands the free-form {@code action},
     * validates it against the current scene, and - when it is a recognized action - rolls and
     * resolves the governing ability check via the shared {@link DiceService}, recording any pending
     * state change as a {@link com.example.domain.StateChange}. The engine resolves the check but
     * does not apply state changes itself.</p>
     *
     * @param scene          the active scene the action takes place in (never {@code null})
     * @param sceneCombatants the combatants present in the scene, used to resolve action targets
     *                        (may be {@code null} or empty)
     * @param input           the free-form player action (never {@code null})
     * @return a {@link PlayerActionResolution} describing what the engine understood and resolved
     */
    PlayerActionResolution resolvePlayerAction(
            Scene scene, List<Combatant> sceneCombatants, PlayerActionInput input);

    /**
     * Generates the narrative response for a resolved action.
     *
     * <p>This is the engine's "response generation" seam: the part an LLM provider is most likely
     * to specialise. Given the scene brief and the mechanical verdict, it returns the story the
     * players see. The deterministic implementation produces short, fixed text; a future provider
     * can replace this alone to change the narration without touching resolution.</p>
     *
     * @param scene      the scene brief presented around the action (never {@code null})
     * @param resolution the mechanical verdict to narrate (never {@code null})
     * @return the generated response text (never {@code null})
     */
    String generateResponse(SceneBrief scene, PlayerActionResolution resolution);
}
