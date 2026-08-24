package com.example;

import com.example.domain.Combatant;
import com.example.domain.CombatantKind;
import com.example.service.CombatantService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * REST surface for persistent combatants and the turn order they form within an
 * {@link com.example.domain.Encounter}.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link CombatantService} call. All combatant creation, encounter attachment,
 * damage/healing, and turn-ordering logic lives in the service.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class CombatantController {

    private final CombatantService combatants;

    public CombatantController(CombatantService combatants) {
        this.combatants = combatants;
    }

    @PostMapping("/{campaignId}/combatants")
    public Combatant createCombatant(@PathVariable Long campaignId,
                                     @RequestParam String name,
                                     @RequestParam CombatantKind kind,
                                     @RequestParam int hitPoints,
                                     @RequestParam int maxHitPoints) {
        return combatants.createCombatant(campaignId, name, kind, hitPoints, maxHitPoints);
    }

    @PostMapping("/{campaignId}/combatants/{combatantId}/encounters/{encounterId}/join")
    public Combatant joinEncounter(@PathVariable Long campaignId,
                                   @PathVariable Long combatantId,
                                   @PathVariable Long encounterId) {
        return combatants.addToEncounter(campaignId, combatantId, encounterId);
    }

    @PostMapping("/{campaignId}/encounters/{encounterId}/turn-order")
    public List<Combatant> buildTurnOrder(@PathVariable Long campaignId,
                                          @PathVariable Long encounterId) {
        return combatants.buildTurnOrder(campaignId, encounterId);
    }

    @PostMapping("/{campaignId}/encounters/{encounterId}/next-turn")
    public com.example.domain.Encounter nextTurn(@PathVariable Long campaignId,
                                                 @PathVariable Long encounterId) {
        return combatants.nextTurn(campaignId, encounterId);
    }

    @GetMapping("/{campaignId}/encounters/{encounterId}/current-combatant")
    public Optional<Combatant> currentCombatant(@PathVariable Long campaignId,
                                                @PathVariable Long encounterId) {
        return combatants.currentCombatant(campaignId, encounterId);
    }

    @GetMapping("/{campaignId}/encounters/{encounterId}/complete")
    public boolean isEncounterComplete(@PathVariable Long campaignId,
                                       @PathVariable Long encounterId) {
        return combatants.isEncounterComplete(campaignId, encounterId);
    }

    @GetMapping("/{campaignId}/encounters/{encounterId}/winner")
    public Optional<CombatantKind> encounterWinner(@PathVariable Long campaignId,
                                                   @PathVariable Long encounterId) {
        return combatants.winningSide(campaignId, encounterId);
    }

    @PostMapping("/{campaignId}/combatants/{combatantId}/damage")
    public Combatant applyDamage(@PathVariable Long campaignId,
                                 @PathVariable Long combatantId, @RequestParam int delta) {
        return combatants.applyDamage(campaignId, combatantId, delta);
    }

    @PostMapping("/{campaignId}/combatants/{combatantId}/heal")
    public Combatant heal(@PathVariable Long campaignId,
                          @PathVariable Long combatantId, @RequestParam int amount) {
        return combatants.heal(campaignId, combatantId, amount);
    }

    @GetMapping("/{campaignId}/combatants/{combatantId}")
    public Combatant getCombatant(@PathVariable Long campaignId, @PathVariable Long combatantId) {
        return combatants.getCombatant(campaignId, combatantId);
    }

    @GetMapping("/{campaignId}/combatants")
    public List<Combatant> listCombatants(@PathVariable Long campaignId) {
        return combatants.listCombatants(campaignId);
    }

    @GetMapping("/{campaignId}/encounters/{encounterId}/combatants")
    public List<Combatant> listCombatantsOfEncounter(@PathVariable Long campaignId,
                                                     @PathVariable Long encounterId) {
        return combatants.listCombatantsOfEncounter(campaignId, encounterId);
    }
}
