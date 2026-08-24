package com.example.service;

import com.example.domain.AbilityCheckResult;
import com.example.domain.AbilityCheckOutcome;
import com.example.domain.Encounter;
import com.example.domain.Objective;
import com.example.domain.ObjectiveStatus;
import com.example.domain.PlayerActionType;
import com.example.domain.Npc;
import com.example.domain.NpcRelationship;
import com.example.domain.Quest;
import com.example.domain.QuestLocationRef;
import com.example.domain.QuestStatus;
import com.example.domain.Scene;
import com.example.domain.StateChange;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Applies the world effects a recognised player action may trigger.
 *
 * <p>The {@link DungeonMasterEngine} resolves the mechanics of an action but never mutates the
 * world: this service is where an action's broader consequences are realised. For a recognised
 * action it derives, from the resolved {@link PlayerActionType} and the mechanical verdict, which
 * of the four persistent world effects are appropriate and applies them, recording each as a
 * {@code campaign event} and returning a short, human-readable description of every effect that
 * fired.</p>
 *
 * <p>The four effects map onto the existing services, so they reload with the rest of the
 * campaign:</p>
 * <ul>
 *   <li><strong>discover locations</strong> - a {@code SEARCH} or {@code INVESTIGATE} that finds
 *       the scene's location reveals it to the campaign;</li>
 *   <li><strong>complete objectives</strong> - discovering a location a quest cares about advances
 *       and, when it is enough, completes that quest's objective;</li>
 *   <li><strong>update relationships</strong> - a {@code TALK} with an involved non-player character
 *       nudges that character's relationship toward the party;</li>
 *   <li><strong>trigger encounters</strong> - a landed {@code ATTACK} that leaves no encounter
 *       already in progress begins one for the scene.</li>
 * </ul>
 *
 * <p>Every effect is optional and defensive: a missing scene location, no involved NPCs, no active
 * quests, or an existing encounter simply means that effect does not fire. Nothing here throws for
 * absent game state, so the action flow always completes.</p>
 */
@Service
public class ActionEffectsService {

    private final WorldService world;
    private final NpcService npcs;
    private final QuestService quests;
    private final EncounterService encounters;
    private final CampaignEventService events;
    private final SceneService scenes;

    /**
     * Creates the effects service over the persistent world services.
     *
     * @param world      the world service that owns location discovery
     * @param npcs       the NPC service that owns relationships
     * @param quests     the quest service that owns objectives and completion
     * @param encounters the encounter service that owns encounters
     * @param events     the campaign-event recorder
     */
    public ActionEffectsService(
            WorldService world, NpcService npcs, QuestService quests,
            EncounterService encounters, CampaignEventService events, SceneService scenes) {
        this.world = world;
        this.npcs = npcs;
        this.quests = quests;
        this.encounters = encounters;
        this.events = events;
        this.scenes = scenes;
    }

    /**
     * Applies the world effects appropriate to a recognised action and returns a short,
     * human-readable description of each effect that fired.
     *
     * @param campaignId the owning campaign
     * @param scene      the active scene the action takes place in
     * @param type       the structured type the action expressed (may be {@code null})
     * @param check      the resolved ability check backing the action (may be {@code null})
     * @param stateChange the pending hit-point change the action resolved (may be non-applying)
     * @return the ordered descriptions of every effect that fired, empty when none applied
     */
    public List<String> resolveEffects(
            Long campaignId, Scene scene, PlayerActionType type,
            AbilityCheckResult check, StateChange stateChange) {
        List<String> effects = new ArrayList<>();
        if (type == null || scene == null) {
            return effects;
        }

        if (type == PlayerActionType.SEARCH || type == PlayerActionType.INVESTIGATE) {
            discoverLocation(campaignId, scene, effects);
            progressObjectives(campaignId, scene, effects);
        } else if (type == PlayerActionType.TALK) {
            improveRelationship(campaignId, scene, effects);
        } else if (type == PlayerActionType.ATTACK && landed(check, stateChange)) {
            beginEncounter(campaignId, scene, effects);
        }

        return effects;
    }

    /**
     * Reveals the scene's location when the action investigated or searched and the location has
     * not already been discovered.
     *
     * @param campaignId the owning campaign
     * @param scene      the active scene
     * @param effects    the accumulating descriptions of effects that fired
     */
    private void discoverLocation(Long campaignId, Scene scene, List<String> effects) {
        if (scene.getLocation() == null) {
            return;
        }
        Long locationId = scene.getLocation().getId();
        if (locationId == null || scene.getLocation().isDiscovered()) {
            return;
        }
        world.discoverLocation(campaignId, locationId);
        effects.add("Discovered location: " + scene.getLocation().getName());
    }

    /**
     * Advances the objective of any active quest that cares about the scene's location, completing
     * the quest when the objective reaches its target. Discovery is the trigger that turns progress
     * into completion.
     *
     * @param campaignId the owning campaign
     * @param scene      the active scene
     * @param effects    the accumulating descriptions of effects that fired
     */
    private void progressObjectives(Long campaignId, Scene scene, List<String> effects) {
        if (scene.getLocation() == null || scene.getLocation().getId() == null) {
            return;
        }
        Long locationId = scene.getLocation().getId();
        for (Quest quest : quests.listQuests(campaignId)) {
            if (quest.getStatus() == null || quest.getStatus() == QuestStatus.COMPLETED) {
                continue;
            }
            boolean caresAboutLocation = false;
            for (QuestLocationRef ref : quest.getRelatedLocations()) {
                if (ref != null && locationId.equals(ref.getLocationId())) {
                    caresAboutLocation = true;
                    break;
                }
            }
            if (!caresAboutLocation) {
                continue;
            }
            Objective target = unfinishedObjective(campaignId, quest);
            if (target == null) {
                continue;
            }
            int next = Math.min(target.getTargetCount(), target.getCurrentCount() + 1);
            quests.setObjectiveProgress(campaignId, quest.getId(), target.getId(), next);
            if (next >= target.getTargetCount()) {
                quests.completeQuest(campaignId, quest.getId());
                effects.add("Completed objective and quest: " + quest.getTitle());
            } else {
                effects.add("Progressed objective for quest: " + quest.getTitle());
            }
        }
    }

    /**
     * Nudges the relationship of the first involved non-player character toward the party as a
     * result of a social action, recording the change.
     *
     * @param campaignId the owning campaign
     * @param scene      the active scene
     * @param effects    the accumulating descriptions of effects that fired
     */
    private void improveRelationship(Long campaignId, Scene scene, List<String> effects) {
        List<Npc> involved = scenes.involvedNpcs(campaignId, scene.getId());
        if (involved == null || involved.isEmpty()) {
            return;
        }
        Npc npc = null;
        for (Npc candidate : involved) {
            if (candidate != null && candidate.getId() != null) {
                npc = candidate;
                break;
            }
        }
        if (npc == null) {
            return;
        }
        NpcRelationship current = npc.getRelationship();
        NpcRelationship improved = friendlier(current);
        if (improved == null || improved.equals(current)) {
            return;
        }
        npcs.setRelationship(campaignId, npc.getId(), improved);
        events.recordRelationshipChange(campaignId, npc.getId(), null, improved.name());
        effects.add("Relationship with " + npc.getName() + " updated to " + improved.name());
    }

    /**
     * Begins a combat encounter for the scene when a landed attack leaves no encounter already in
     * progress and the scene has a location to anchor it.
     *
     * @param campaignId the owning campaign
     * @param scene      the active scene
     * @param effects    the accumulating descriptions of effects that fired
     */
    private void beginEncounter(Long campaignId, Scene scene, List<String> effects) {
        if (scene.getEncounter() != null || scene.getLocation() == null || scene.getLocation().getId() == null) {
            return;
        }
        Encounter created = encounters.createEncounter(campaignId, scene.getId(),
                scene.getLocation().getId());
        encounters.beginEncounter(campaignId, created.getId());
        effects.add("Triggered combat encounter for the scene");
    }

    /**
     * Returns the least-progressed objective of the quest that has not reached its target, or
     * {@code null} when the quest has no unfinished objective.
     *
     * @param quest the quest to inspect
     * @return the unfinished objective, or {@code null} when none is unfinished
     */
    private Objective unfinishedObjective(Long campaignId, Quest quest) {
        Objective best = null;
        for (Objective objective : quests.listObjectives(campaignId, quest.getId())) {
            if (objective == null || objective.getStatus() == ObjectiveStatus.COMPLETE) {
                continue;
            }
            if (best == null || objective.getCurrentCount() < best.getCurrentCount()) {
                best = objective;
            }
        }
        return best;
    }

    /**
     * The friendlier relationship to move toward, or {@code null} when the current relationship is
     * already the friendliest or is a fixed enmity that should not be softened by a single
     * conversation.
     *
     * @param current the relationship that currently holds
     * @return a friendlier relationship, or {@code null} when none applies
     */
    private NpcRelationship friendlier(NpcRelationship current) {
        if (current == null || current == NpcRelationship.KNOWN || current == NpcRelationship.NEUTRAL) {
            return NpcRelationship.FRIENDLY;
        }
        if (current == NpcRelationship.FRIENDLY) {
            return NpcRelationship.ALLIED;
        }
        return null;
    }

    /**
     * Whether an attack landed: a successful check that resolved a damaging state change.
     *
     * @param check      the resolved ability check (may be {@code null})
     * @param stateChange the pending state change (may be non-applying)
     * @return {@code true} when the attack's check succeeded and it dealt damage
     */
    private boolean landed(AbilityCheckResult check, StateChange stateChange) {
        return stateChange != null && stateChange.applies()
                && stateChange.kind() == StateChange.Kind.DAMAGE
                && check != null && check.outcome() == AbilityCheckOutcome.SUCCESS;
    }
}
