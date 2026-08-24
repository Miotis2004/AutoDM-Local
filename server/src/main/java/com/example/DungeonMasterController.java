package com.example;

import com.example.domain.EngineResponse;
import com.example.domain.PlayerActionInput;
import com.example.domain.SceneBrief;
import com.example.service.DungeonMasterService;
import com.example.service.ValidationException;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for the Dungeon Master engine.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single {@link
 * DungeonMasterService} call. Scene presentation, action validation, mechanic resolution, state
 * changes, and event recording all live in the service and the pluggable {@link
 * com.example.service.DungeonMasterEngine} it owns.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class DungeonMasterController {

    private final DungeonMasterService master;

    public DungeonMasterController(DungeonMasterService master) {
        this.master = master;
    }

    /**
     * Runs a player's action against the current scene end to end.
     *
     * <p>The action is validated and its mechanic resolved, any resulting state change is applied
     * and recorded as a campaign event, and a narrative response is generated.</p>
     *
     * @param campaignId the owning campaign
     * @param sceneId    the active scene the action takes place in
     * @param action     the free-form player action (required)
     * @param statistic  the governing ability/skill statistic (optional; engine defaults it)
     * @param modifier   the modifier applied to the statistic (optional; defaults to {@code 0})
     * @param difficulty the difficulty class the check must meet (optional; defaults to a
     *                   sensible value)
     * @return a brief for the scene now in focus, or {@code null} when there is no next scene
     */
    @PostMapping("/{campaignId}/scenes/advance")
    public SceneBrief advanceScene(@PathVariable Long campaignId) {
        return master.advanceScene(campaignId);
    }

    /**
     * Runs a player's action against the current scene end to end.
     *
     * <p>The action is validated and its mechanic resolved, any resulting state change is applied
     * and recorded as a campaign event, any appropriate world effect (an encounter begun, an
     * objective completed, a location discovered, or a relationship updated) is triggered, and a
     * narrative response is generated.</p>
     *
     * @param campaignId the owning campaign
     * @param sceneId    the active scene the action takes place in
     * @param action     the free-form player action (required)
     * @param statistic  the governing ability/skill statistic (optional; engine defaults it)
     * @param modifier   the modifier applied to the statistic (optional; defaults to {@code 0})
     * @param difficulty the difficulty class the check must meet (optional; defaults to a
     *                   sensible value)
     * @return an {@link EngineResponse} describing the scene, the verdict, the response, the
     *         resolved state change, and the world effects the action triggered
     */
    @PostMapping("/{campaignId}/scenes/{sceneId}/action")
    public EngineResponse act(
            @PathVariable Long campaignId,
            @PathVariable Long sceneId,
            @RequestParam String action,
            @RequestParam(required = false) String statistic,
            @RequestParam(required = false, defaultValue = "0") int modifier,
            @RequestParam(required = false, defaultValue = "0") int difficulty) {
        PlayerActionInput input = new PlayerActionInput(
                action, statistic, modifier, difficulty > 0 ? difficulty : PlayerActionInput.DEFAULT_DIFFICULTY);
        EngineResponse response = master.act(campaignId, sceneId, input);

        // A recognized action is a normal 200 response. An action the engine could not resolve
        // (empty, unrecognised, or impossible in the current scene) is a client error, so surface
        // it with a clear 400 carrying the first validation problem rather than a silent 200.
        if (!response.recognized() && response.validationErrors() != null && !response.validationErrors().isEmpty()) {
            throw new ValidationException(response.validationErrors().get(0));
        }
        return response;
    }
}
