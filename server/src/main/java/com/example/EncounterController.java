package com.example;

import com.example.domain.Encounter;
import com.example.domain.EncounterDifficulty;
import com.example.service.EncounterGenerator;
import com.example.service.EncounterService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST surface for persistent encounters owned by a campaign.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link EncounterService} call. All encounter creation, start/finish, and lookup
 * logic lives in the service.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class EncounterController {

    private final EncounterService encounters;
    private final EncounterGenerator generator;

    public EncounterController(EncounterService encounters, EncounterGenerator generator) {
        this.encounters = encounters;
        this.generator = generator;
    }

    @PostMapping("/{campaignId}/encounters")
    public Encounter createEncounter(@PathVariable Long campaignId,
                                     @RequestParam Long sceneId,
                                     @RequestParam Long locationId) {
        return encounters.createEncounter(campaignId, sceneId, locationId);
    }

    @PostMapping("/{campaignId}/encounters/{encounterId}/begin")
    public Encounter beginEncounter(@PathVariable Long campaignId,
                                    @PathVariable Long encounterId) {
        return encounters.beginEncounter(campaignId, encounterId);
    }

    @PostMapping("/{campaignId}/encounters/{encounterId}/finish")
    public Encounter finishEncounter(@PathVariable Long campaignId,
                                     @PathVariable Long encounterId) {
        return encounters.finishEncounter(campaignId, encounterId);
    }

    @GetMapping("/{campaignId}/encounters/{encounterId}")
    public Encounter getEncounter(@PathVariable Long campaignId, @PathVariable Long encounterId) {
        return encounters.getEncounter(campaignId, encounterId);
    }

    @GetMapping("/{campaignId}/encounters")
    public List<Encounter> listEncounters(@PathVariable Long campaignId) {
        return encounters.listEncounters(campaignId);
    }

    // ------------------------------------------------------------------
    // Automated generation
    // ------------------------------------------------------------------

    /**
     * Generates an encounter for a location, sized to the party and filled with enemies
     * drawn from the campaign's available templates. The party's size and average level
     * are derived from its player characters unless supplied; difficulty defaults to
     * {@link EncounterDifficulty#MEDIUM} when omitted.
     */
    @PostMapping("/{campaignId}/encounters/generate")
    public Encounter generateEncounter(@PathVariable Long campaignId,
                                       @RequestParam Long locationId,
                                       @RequestParam(required = false) EncounterDifficulty difficulty,
                                       @RequestParam(required = false) Integer partySize,
                                       @RequestParam(required = false) Integer averageLevel,
                                       @RequestParam(required = false) List<Long> templateIds) {
        return generator.generateEncounter(campaignId, locationId, difficulty, partySize, averageLevel, templateIds);
    }
}
