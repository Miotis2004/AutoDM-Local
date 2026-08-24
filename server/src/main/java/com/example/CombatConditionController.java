package com.example;

import com.example.domain.CombatCondition;
import com.example.service.CombatConditionService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST surface for persistent combat conditions (status effects) owned by a campaign.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link CombatConditionService} call. All condition creation, toggling, round
 * advancement, and lookup logic lives in the service.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class CombatConditionController {

    private final CombatConditionService conditions;

    public CombatConditionController(CombatConditionService conditions) {
        this.conditions = conditions;
    }

    @PostMapping("/{campaignId}/combatants/{combatantId}/conditions")
    public CombatCondition addCondition(@PathVariable Long campaignId,
                                        @PathVariable Long combatantId,
                                        @RequestParam String name,
                                        @RequestParam(required = false) String description,
                                        @RequestParam(required = false) Integer duration,
                                        @RequestParam(required = false) String source) {
        return conditions.addCondition(campaignId, combatantId, name, description, duration,
                source);
    }

    @PostMapping("/{campaignId}/conditions/{conditionId}/toggle")
    public CombatCondition toggleCondition(@PathVariable Long campaignId,
                                           @PathVariable Long conditionId,
                                           @RequestParam boolean active) {
        return conditions.toggle(campaignId, conditionId, active);
    }

    @PostMapping("/{campaignId}/combat/conditions/advance")
    public List<CombatCondition> advanceConditions(@PathVariable Long campaignId) {
        return conditions.advanceRounds(campaignId);
    }

    @GetMapping("/{campaignId}/conditions/{conditionId}")
    public CombatCondition getCondition(@PathVariable Long campaignId,
                                        @PathVariable Long conditionId) {
        return conditions.getCondition(campaignId, conditionId);
    }

    @GetMapping("/{campaignId}/conditions")
    public List<CombatCondition> listConditions(@PathVariable Long campaignId) {
        return conditions.listConditions(campaignId);
    }

    @GetMapping("/{campaignId}/combatants/{combatantId}/conditions")
    public List<CombatCondition> listConditionsOnCombatant(@PathVariable Long campaignId,
                                                           @PathVariable Long combatantId) {
        return conditions.listConditionsOnCombatant(campaignId, combatantId);
    }
}
