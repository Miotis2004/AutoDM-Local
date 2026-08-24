package com.example.service;

import com.example.domain.AbilityCheckResult;
import com.example.domain.Campaign;
import com.example.domain.CampaignEvent;
import com.example.domain.CampaignEventType;
import com.example.domain.Combatant;
import com.example.domain.EngineResponse;
import com.example.domain.PlayerActionInput;
import com.example.domain.PlayerActionResolution;
import com.example.domain.Scene;
import com.example.domain.SceneBrief;
import com.example.domain.SceneStatus;
import com.example.domain.StateChange;
import com.example.domain.PlayerActionType;
import com.example.db.CampaignEventRepository;
import com.example.db.CampaignRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Business logic for the Dungeon Master engine.
 *
 * <p>This service is the single place a player's action is driven end to end. It presents the
 * current scene, delegates the mechanical {@code action handling} and {@code response generation}
 * to a pluggable {@link DungeonMasterEngine}, applies any resulting state change through
 * {@link CombatantService}, and records the outcome on the campaign through the {@link
 * CampaignEvent} system. Because the cognitive core lives behind the {@link
 * DungeonMasterEngine} interface, a richer or LLM-backed strategy can replace the deterministic
 * default without changing how this service, or the REST surface above it, works.</p>
 *
 * <p>The engine is swappable at runtime via {@link #setEngine}, so a campaign (or a verification)
 * can install a bespoke strategy while the application is running.</p>
 */
@Service
public class DungeonMasterService {

    private DungeonMasterEngine engine;

    private final SceneService scenes;
    private final CombatantService combatants;
    private final CampaignEventRepository events;
    private final CampaignRepository campaigns;
    private final ActionEffectsService effects;

    /**
     * Creates the service over the default deterministic {@link DungeonMasterEngine}.
     *
     * @param scenes      the scene service used to resolve the active scene
     * @param combatants  the combatant service that owns hit-point bookkeeping and persistence
     * @param events      the repository used to record actions as campaign events
     * @param campaigns   the repository used to resolve campaigns
     * @param defaultEngine the deterministic engine to drive the initial application
     */
    public DungeonMasterService(
            SceneService scenes,
            CombatantService combatants,
            CampaignEventRepository events,
            CampaignRepository campaigns,
            DefaultDungeonMasterEngine defaultEngine,
            ActionEffectsService effects) {
        this.scenes = scenes;
        this.combatants = combatants;
        this.events = events;
        this.campaigns = campaigns;
        this.engine = defaultEngine;
        this.effects = effects;
    }

    /**
     * Installs a master engine. Because the engine is an interface, any implementation - for
     * example a future local or remote LLM provider - can be plugged in here, replacing the
     * deterministic default.
     *
     * @param engine the engine to use for action handling and response generation
     */
    public void setEngine(DungeonMasterEngine engine) {
        this.engine = engine;
    }

    /**
     * @return the master engine currently driving player actions
     */
    public DungeonMasterEngine getEngine() {
        return engine;
    }

    /**
     * Runs a player's action against the current scene end to end.
     *
     * <p>The scene is presented, the action is validated and its mechanic resolved by the
     * {@link DungeonMasterEngine}, any resulting state change is applied through
     * {@link CombatantService} and recorded as a campaign event, and a narrative response is
     * generated. The full {@link EngineResponse} - the scene brief, the recognition and validation
     * result, the resolved ability check, and the response text - is returned.</p>
     *
     * @param campaignId the owning campaign
     * @param sceneId    the active scene the action takes place in
     * @param input      the free-form player action
     * @return the complete engine response (never {@code null})
     */
    public EngineResponse act(Long campaignId, Long sceneId, PlayerActionInput input) {
        Scene scene = scenes.getScene(campaignId, sceneId);
        List<Combatant> sceneCombatants = sceneCombatants(campaignId, sceneId);
        Set<String> involvedNames = scenes.involvedNames(campaignId, sceneId);
        SceneBrief brief = engine.presentScene(scene, involvedNames, sceneCombatants);

        PlayerActionResolution resolution = engine.resolvePlayerAction(scene, sceneCombatants, input);

        applyStateChange(campaignId, scene, resolution);
        if (resolution.recognized()) {
            recordActionEvent(campaignId, resolution);
        }

        String response = engine.generateResponse(brief, resolution);

        List<String> resolvedEffects = List.of();
        if (resolution.recognized()) {
            PlayerActionType type = PlayerActionType.valueOf(resolution.parsedVerb());
            resolvedEffects = effects.resolveEffects(
                    campaignId, scene, type, resolution.check(), resolution.stateChange());
        }

        return new EngineResponse(
                brief,
                resolution.recognized(),
                resolution.validationErrors(),
                resolution.check(),
                response,
                resolution.stateChange(),
                resolvedEffects);
    }

    /**
     * Convenience over {@link #act} that takes a bare action string with the engine's default
     * statistic, modifier, and difficulty.
     *
     * @param campaignId the owning campaign
     * @param sceneId    the active scene the action takes place in
     * @param action     the free-form player action
     * @return the complete engine response (never {@code null})
     */
    public EngineResponse act(Long campaignId, Long sceneId, String action) {
        return act(campaignId, sceneId, PlayerActionInput.of(action));
    }

    /**
     * Advances the campaign from its current active scene to the next scene, if any, and presents
     * the resulting scene brief so the engine can narrate the move. When there is no next scene
     * the current scene is left unchanged and {@code null} is returned.
     *
     * <p>This is how the DM engine moves play forward between scenes: the scene left behind is
     * marked {@link SceneStatus#COMPLETED} and the next scene is activated and marked
     * {@link SceneStatus#ACTIVE}.</p>
     *
     * @param campaignId the owning campaign
     * @return the scene brief for the scene now in focus, or {@code null} when there is no next scene
     */
    public SceneBrief advanceScene(Long campaignId) {
        Scene next = scenes.advanceScene(campaignId);
        if (next == null) {
            return null;
        }
        List<Combatant> sceneCombatants = sceneCombatants(campaignId, next.getId());
        Set<String> involvedNames = scenes.involvedNames(campaignId, next.getId());
        return engine.presentScene(next, involvedNames, sceneCombatants);
    }

    /**
     * Applies the pending state change resolved for an action, if any, and records a campaign event.
     *
     * <p>The engine resolves the change but never applies it: here, as the single source of truth
     * for hit-point changes, the owning service applies damage or healing through {@link
     * CombatantService} and persists it. It then records the outcome as a campaign event so the
     * action is available to the campaign's event history.</p>
     *
     * @param campaignId the owning campaign
     * @param scene      the active scene
     * @param resolution the mechanical verdict (may be unrecognized, in which case nothing is applied)
     */
    private void applyStateChange(Long campaignId, Scene scene, PlayerActionResolution resolution) {
        if (!resolution.recognized() || !resolution.stateChange().applies()) {
            return;
        }
        StateChange change = resolution.stateChange();
        if (change.kind() == StateChange.Kind.DAMAGE) {
            Combatant damaged = combatants.applyDamage(campaignId, change.combatantId(), change.amount());
            recordDamageEvent(campaignId, damaged, change.amount(), resolution.check());
        } else {
            combatants.heal(campaignId, change.combatantId(), change.amount());
        }
    }

    /**
     * Records a landed attack as a {@link CampaignEventType#DAMAGE} campaign event so the damage it
     * dealt is available through the campaign's event history.
     *
     * @param campaignId the owning campaign
     * @param damaged    the combatant that was damaged
     * @param amount     the damage that was dealt
     * @param check      the resolved check backing the attack
     */
    private void recordDamageEvent(Long campaignId, Combatant damaged, int amount, AbilityCheckResult check) {
        String description = "A landed attack dealt " + amount
                + " damage to " + damaged.getName();
        events.save(new CampaignEvent(requireCampaign(campaignId), CampaignEventType.DAMAGE,
                LocalDateTime.now())
                .withDescription(description)
                .withDetails("{\"target\":" + damaged.getId()
                        + ",\"hitPoints\":" + damaged.getHitPoints()
                        + ",\"check\":\"" + (check != null ? check.outcome() : "NONE") + "\"}"));
    }

    /**
     * Records the resolution of any recognized action as a {@link CampaignEventType#GAME_ACTION}
     * campaign event, so the action, its verb, and its check outcome are part of the campaign's
     * event history even when no damage was dealt.
     *
     * @param campaignId the owning campaign
     * @param resolution the mechanical verdict (recognized actions only)
     */
    private void recordActionEvent(Long campaignId, PlayerActionResolution resolution) {
        AbilityCheckResult check = resolution.check();
        String description = "The engine resolved: " + resolution.parsedVerb()
                + (check != null ? " -> " + check.outcome() : "");
        events.save(new CampaignEvent(requireCampaign(campaignId), CampaignEventType.GAME_ACTION,
                LocalDateTime.now())
                .withDescription(description)
                .withDetails("{\"verb\":\"" + resolution.parsedVerb()
                        + "\",\"check\":\"" + (check != null ? check.outcome() : "NONE")
                        + ",\"difficulty\":" + (check != null ? check.difficulty() : 0) + "}"));
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private List<Combatant> sceneCombatants(Long campaignId, Long sceneId) {
        requireCampaign(campaignId);
        return combatants.listCombatants(campaignId).stream()
                .filter(combatant -> combatant != null
                        && combatant.getScene() != null
                        && combatant.getScene().getId() != null
                        && combatant.getScene().getId().equals(sceneId))
                .toList();
    }
}
