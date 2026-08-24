package com.example;

import com.example.domain.Npc;
import com.example.domain.PlayerCharacter;
import com.example.domain.Scene;
import com.example.service.SceneService;
import com.example.service.SceneService.InvolvedReference;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST surface for persistent scenes owned by a campaign.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link SceneService} call. All scene creation, activation, advancing, involved-character
 * management, and lookup logic lives in the service.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class SceneController {

    private final SceneService scenes;

    public SceneController(SceneService scenes) {
        this.scenes = scenes;
    }

    /**
     * Creates a scene for a campaign.
     *
     * @param campaignId  the owning campaign
     * @param title       the scene's title (required)
     * @param narrative   the scene's free-form narrative (optional)
     * @param locationId  the id of the location the scene takes place in (optional)
     * @param encounterId the id of the encounter the scene references (optional)
     */
    @PostMapping("/{campaignId}/scenes")
    public Scene createScene(
            @PathVariable Long campaignId,
            @RequestParam String title,
            @RequestParam(required = false) String narrative,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long encounterId) {
        return scenes.createScene(campaignId, title, narrative, locationId, encounterId);
    }

    @PostMapping("/{campaignId}/scenes/{sceneId}/activate")
    public Scene activateScene(@PathVariable Long campaignId, @PathVariable Long sceneId) {
        return scenes.activateScene(campaignId, sceneId);
    }

    @GetMapping("/{campaignId}/scenes/{sceneId}")
    public Scene getScene(@PathVariable Long campaignId, @PathVariable Long sceneId) {
        return scenes.getScene(campaignId, sceneId);
    }

    @GetMapping("/{campaignId}/scenes")
    public List<Scene> listScenes(@PathVariable Long campaignId) {
        return scenes.listScenes(campaignId);
    }

    /**
     * Names player characters as involved in a scene.
     *
     * @param campaignId     the owning campaign
     * @param sceneId        the scene
     * @param characterIds   the ids of the player characters to involve (as a repeated param)
     */
    @PostMapping("/{campaignId}/scenes/{sceneId}/involve/players")
    public void involvePlayerCharacters(
            @PathVariable Long campaignId,
            @PathVariable Long sceneId,
            @RequestParam List<Long> characterIds) {
        scenes.involvePlayerCharacters(campaignId, sceneId, characterIds);
    }

    /**
     * Names NPCs as involved in a scene.
     *
     * @param campaignId the owning campaign
     * @param sceneId    the scene
     * @param npcIds     the ids of the NPCs to involve (as a repeated param)
     */
    @PostMapping("/{campaignId}/scenes/{sceneId}/involve/npcs")
    public void involveNpcs(
            @PathVariable Long campaignId,
            @PathVariable Long sceneId,
            @RequestParam List<Long> npcIds) {
        scenes.involveNpcs(campaignId, sceneId, npcIds);
    }

    @GetMapping("/{campaignId}/scenes/{sceneId}/involved")
    public List<InvolvedReference> involvedReferences(
            @PathVariable Long campaignId, @PathVariable Long sceneId) {
        return scenes.involvedReferences(campaignId, sceneId);
    }

    @GetMapping("/{campaignId}/scenes/{sceneId}/involved/players")
    public List<PlayerCharacter> involvedPlayerCharacters(
            @PathVariable Long campaignId, @PathVariable Long sceneId) {
        return scenes.involvedPlayerCharacters(campaignId, sceneId);
    }

    @GetMapping("/{campaignId}/scenes/{sceneId}/involved/npcs")
    public List<Npc> involvedNpcs(
            @PathVariable Long campaignId, @PathVariable Long sceneId) {
        return scenes.involvedNpcs(campaignId, sceneId);
    }
}
