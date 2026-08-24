package com.example.service;

import com.example.db.CampaignRepository;
import com.example.db.EncounterRepository;
import com.example.db.LocationRepository;
import com.example.db.NpcRepository;
import com.example.db.PlayerCharacterRepository;
import com.example.db.SceneInvolvedCharacterRepository;
import com.example.db.SceneRepository;
import com.example.domain.Campaign;
import com.example.domain.Encounter;
import com.example.domain.Location;
import com.example.domain.Npc;
import com.example.domain.PlayerCharacter;
import com.example.domain.Scene;
import com.example.domain.SceneInvolvedCharacter;
import com.example.domain.SceneStatus;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Business logic for {@link Scene}s owned by a campaign.
 *
 * <p>This service is the single place scenes are created, wired to a location and
 * encounter, consulted for the characters involved, and advanced between. Every mutation
 * resolves its owning campaign, applies the change to a managed entity, and relies on the
 * repositories to persist it, so scenes (and their involved characters) reload across
 * application restarts within a campaign.</p>
 */
@Service
public class SceneService {

    private final CampaignRepository campaigns;
    private final SceneRepository scenes;
    private final SceneInvolvedCharacters sceneInvolvedCharacters;
    private final LocationRepository locations;
    private final EncounterRepository encounters;
    private final NpcRepository npcs;
    private final PlayerCharacterRepository playerCharacters;

    public SceneService(
            CampaignRepository campaigns,
            SceneRepository scenes,
            SceneInvolvedCharacterRepository sceneInvolvedCharacters,
            LocationRepository locations,
            EncounterRepository encounters,
            NpcRepository npcs,
            PlayerCharacterRepository playerCharacters) {
        this.campaigns = campaigns;
        this.scenes = scenes;
        this.sceneInvolvedCharacters =
                new SceneInvolvedCharacters(sceneInvolvedCharacters);
        this.locations = locations;
        this.encounters = encounters;
        this.npcs = npcs;
        this.playerCharacters = playerCharacters;
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    /**
     * Creates a scene for a campaign. The new scene starts {@link SceneStatus#READY}.
     *
     * @param campaignId the owning campaign
     * @param title      the scene's title (never blank)
     * @param narrative  the scene's free-form narrative (may be {@code null})
     * @param locationId the id of the location the scene takes place in, or {@code null}
     * @param encounterId the id of the encounter the scene references, or {@code null}
     * @return the newly created scene (never {@code null})
     */
    public Scene createScene(
            Long campaignId,
            String title,
            String narrative,
            Long locationId,
            Long encounterId) {
        Campaign campaign = requireCampaign(campaignId);
        Scene scene = new Scene(campaign, title);
        scene.setNarrative(narrative);
        if (locationId != null) {
            scene.setLocation(requireLocation(campaignId, locationId));
        }
        if (encounterId != null) {
            scene.setEncounter(requireActiveEncounter(campaignId, encounterId));
        }
        return scenes.save(scene);
    }

    /**
     * Activates the named scene for a campaign, deactivating any previously active scene
     * of that campaign so at most one scene is active at a time. The activated scene is
     * marked {@link SceneStatus#ACTIVE}; the previously active scene is marked
     * {@link SceneStatus#COMPLETED}.
     *
     * @param campaignId the owning campaign
     * @param sceneId    the scene to activate
     * @return the now-active scene (never {@code null})
     */
    public Scene activateScene(Long campaignId, Long sceneId) {
        requireCampaign(campaignId);
        Scene target = scenes.findById(sceneId)
                .orElseThrow(() -> new IllegalArgumentException("No scene with id " + sceneId));
        scenes.findByCampaignAndActiveTrue(target.getCampaign())
                .ifPresent(other -> other.complete());
        target.activate();
        return scenes.save(target);
    }

    /**
     * Advances the campaign from its current active scene to the next scene, if any.
     *
     * <p>The next scene is the earliest-created scene that comes after the current active
     * scene. It is activated and marked {@link SceneStatus#ACTIVE}, while the scene left
     * behind is marked {@link SceneStatus#COMPLETED}. This is how the DM engine moves play
     * forward between scenes.</p>
     *
     * @param campaignId the owning campaign
     * @return the scene now active after advancing, or {@code null} when there is no next scene
     */
    public Scene advanceScene(Long campaignId) {
        Campaign campaign = requireCampaign(campaignId);
        List<Scene> ordered = scenes.findByCampaignOrderByCreatedAtAsc(campaign);
        if (ordered.isEmpty()) {
            return null;
        }
        Scene current = scenes.findByCampaignAndActiveTrue(campaign)
                .orElse(ordered.get(ordered.size() - 1));
        int nextIndex = indexOf(ordered, current) + 1;
        if (nextIndex >= ordered.size()) {
            return null;
        }
        Scene next = ordered.get(nextIndex);
        current.complete();
        next.activate();
        scenes.save(current);
        return scenes.save(next);
    }

    /**
     * @return the scene currently in focus for the campaign, if any.
     */
    public Scene activeScene(Long campaignId) {
        requireCampaign(campaignId);
        return scenes.findByCampaignAndActiveTrue(requireCampaign(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No active scene for campaign " + campaignId));
    }

    public Scene getScene(Long campaignId, Long sceneId) {
        requireCampaign(campaignId);
        return scenes.findById(sceneId)
                .orElseThrow(() -> new IllegalArgumentException("No scene with id " + sceneId));
    }

    public List<Scene> listScenes(Long campaignId) {
        requireCampaign(campaignId);
        return scenes.findByCampaignOrderByCreatedAtAsc(requireCampaign(campaignId));
    }

    /**
     * Names the player characters involved in a scene.
     *
     * @param campaignId the owning campaign
     * @param sceneId    the scene
     * @param characterIds the ids of the player characters to mark involved
     */
    public void involvePlayerCharacters(Long campaignId, Long sceneId, List<Long> characterIds) {
        involve(campaignId, sceneId, characterIds,
                SceneInvolvedCharacter.InvolvedKind.PLAYER_CHARACTER);
    }

    /**
     * Names the NPCs involved in a scene.
     *
     * @param campaignId the owning campaign
     * @param sceneId    the scene
     * @param npcIds     the ids of the NPCs to mark involved
     */
    public void involveNpcs(Long campaignId, Long sceneId, List<Long> npcIds) {
        involve(campaignId, sceneId, npcIds, SceneInvolvedCharacter.InvolvedKind.NPC);
    }

    /**
     * @return the player characters involved in a scene, ordered by their id
     */
    public List<PlayerCharacter> involvedPlayerCharacters(Long campaignId, Long sceneId) {
        requireCampaign(campaignId);
        Scene scene = requireScene(campaignId, sceneId);
        List<PlayerCharacter> result = new ArrayList<>();
        for (SceneInvolvedCharacter involvement :
                sceneInvolvedCharacters.findBySceneAndInvolvedKind(scene,
                        SceneInvolvedCharacter.InvolvedKind.PLAYER_CHARACTER)) {
            playerCharacters.findById(involvement.getInvolvedId())
                    .ifPresent(result::add);
        }
        return result;
    }

    /**
     * @return the NPCs involved in a scene, ordered by their id
     */
    public List<Npc> involvedNpcs(Long campaignId, Long sceneId) {
        requireCampaign(campaignId);
        Scene scene = requireScene(campaignId, sceneId);
        List<Npc> result = new ArrayList<>();
        for (SceneInvolvedCharacter involvement :
                sceneInvolvedCharacters.findBySceneAndInvolvedKind(scene,
                        SceneInvolvedCharacter.InvolvedKind.NPC)) {
            npcs.findById(involvement.getInvolvedId()).ifPresent(result::add);
        }
        return result;
    }

    /**
     * @return the names of all characters and NPCs involved in the scene, for use in a scene brief.
     */
    public Set<String> involvedNames(Long campaignId, Long sceneId) {
        requireCampaign(campaignId);
        Scene scene = requireScene(campaignId, sceneId);
        Set<String> names = new LinkedHashSet<>();
        for (PlayerCharacter character :
                involvedPlayerCharacters(campaignId, sceneId)) {
            if (character.getName() != null) {
                names.add(character.getName());
            }
        }
        for (Npc npc :
                involvedNpcs(campaignId, sceneId)) {
            if (npc.getName() != null) {
                names.add(npc.getName());
            }
        }
        return names;
    }

    /**
     * @return the ids of all characters and NPCs involved in the scene, together with their kind
     */
    public List<InvolvedReference> involvedReferences(Long campaignId, Long sceneId) {
        requireCampaign(campaignId);
        Scene scene = requireScene(campaignId, sceneId);
        List<InvolvedReference> result = new ArrayList<>();
        for (SceneInvolvedCharacter involvement : sceneInvolvedCharacters.findByScene(scene)) {
            result.add(new InvolvedReference(
                    involvement.getInvolvedKind(), involvement.getInvolvedId()));
        }
        return result;
    }

    private void involve(
            Long campaignId,
            Long sceneId,
            List<Long> ids,
            SceneInvolvedCharacter.InvolvedKind kind) {
        requireCampaign(campaignId);
        Scene scene = requireScene(campaignId, sceneId);
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            sceneInvolvedCharacters.save(new SceneInvolvedCharacter(scene, kind, id));
        }
    }

    private Scene requireScene(Long campaignId, Long sceneId) {
        requireCampaign(campaignId);
        return scenes.findById(sceneId)
                .orElseThrow(() -> new IllegalArgumentException("No scene with id " + sceneId));
    }

    private Location requireLocation(Long campaignId, Long locationId) {
        return locations.findById(locationId)
                .filter(location -> location.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No location with id " + locationId));
    }

    private Encounter requireActiveEncounter(Long campaignId, Long encounterId) {
        Encounter encounter = encounters.findById(encounterId)
                .filter(value -> value.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No encounter with id " + encounterId));
        return encounter;
    }

    private static int indexOf(List<Scene> scenes, Scene target) {
        int i = 0;
        for (Scene scene : scenes) {
            if (scene.getId() != null && scene.getId().equals(target.getId())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * A reference to an involved character or NPC: its kind and id.
     */
    public record InvolvedReference(
            SceneInvolvedCharacter.InvolvedKind kind,
            Long involvedId) {
    }

    /**
     * Thin wrapper over {@link SceneInvolvedCharacterRepository} so the rest of this service
     * depends on a small, focused seam rather than the Spring Data repository directly.
     */
    private static class SceneInvolvedCharacters {
        private final SceneInvolvedCharacterRepository repository;

        private SceneInvolvedCharacters(
                SceneInvolvedCharacterRepository repository) {
            this.repository = repository;
        }

        void save(SceneInvolvedCharacter involvement) {
            repository.save(involvement);
        }

        List<SceneInvolvedCharacter> findByScene(Scene scene) {
            return repository.findBySceneOrderByIdAsc(scene);
        }

        List<SceneInvolvedCharacter> findBySceneAndInvolvedKind(
                Scene scene, SceneInvolvedCharacter.InvolvedKind kind) {
            return repository.findBySceneAndInvolvedKindOrderByIdAsc(scene, kind);
        }
    }
}
