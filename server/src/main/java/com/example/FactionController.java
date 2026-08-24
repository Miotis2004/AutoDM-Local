package com.example;

import com.example.domain.Disposition;
import com.example.domain.Faction;
import com.example.domain.FactionRelationship;
import com.example.domain.NpcRelationship;
import com.example.service.FactionService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * REST surface for persistent factions owned by a campaign.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link FactionService} call. All faction construction, relationship tracking, and
 * update logic lives in the service, and persistence is what lets factions reload
 * across sessions.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class FactionController {

    private final FactionService factions;

    public FactionController(FactionService factions) {
        this.factions = factions;
    }

    // ------------------------------------------------------------------
    // Creation and listing
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/factions")
    public Faction addFaction(@PathVariable Long campaignId,
                              @RequestParam String name,
                              @RequestParam(required = false) String description,
                              @RequestParam Disposition disposition,
                              @RequestParam(defaultValue = "NEUTRAL") NpcRelationship reputation) {
        return factions.addFaction(campaignId, name, description, disposition, reputation);
    }

    @GetMapping("/{campaignId}/factions")
    public List<Faction> listFactions(@PathVariable Long campaignId) {
        return factions.listFactions(campaignId);
    }

    @GetMapping("/{campaignId}/factions/disposition")
    public List<Faction> listByDisposition(@PathVariable Long campaignId,
                                           @RequestParam Disposition disposition) {
        return factions.listByDisposition(campaignId, disposition);
    }

    @GetMapping("/{campaignId}/factions/reputation")
    public List<Faction> listByReputation(@PathVariable Long campaignId,
                                          @RequestParam NpcRelationship reputation) {
        return factions.listByReputation(campaignId, reputation);
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    @GetMapping("/{campaignId}/factions/{factionId}")
    public Faction getFaction(@PathVariable Long campaignId, @PathVariable Long factionId) {
        return factions.getFaction(campaignId, factionId);
    }

    // ------------------------------------------------------------------
    // Update and persistence
    // ------------------------------------------------------------------

    @PutMapping("/{campaignId}/factions/{factionId}")
    public Faction updateFaction(@PathVariable Long campaignId, @PathVariable Long factionId,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) Disposition disposition,
                                 @RequestParam(required = false) NpcRelationship reputation) {
        return factions.updateFaction(campaignId, factionId, description, disposition, reputation);
    }

    @PutMapping("/{campaignId}/factions/{factionId}/notes")
    public Faction setNotes(@PathVariable Long campaignId, @PathVariable Long factionId,
                            @RequestParam(required = false) String notes) {
        return factions.setNotes(campaignId, factionId, notes);
    }

    // ------------------------------------------------------------------
    // Faction-to-faction relationships
    // ------------------------------------------------------------------

    @PutMapping("/{campaignId}/factions/{factionId}/relationships")
    public Faction setRelationship(@PathVariable Long campaignId, @PathVariable Long factionId,
                                   @RequestParam Long relatedFactionId,
                                   @RequestParam NpcRelationship relationship) {
        return factions.setRelationship(campaignId, factionId, relatedFactionId, relationship);
    }

    @DeleteMapping("/{campaignId}/factions/{factionId}/relationships")
    public void removeRelationship(@PathVariable Long campaignId, @PathVariable Long factionId,
                                   @RequestParam Long relatedFactionId,
                                   @RequestParam NpcRelationship relationship) {
        factions.removeRelationship(campaignId, factionId, relatedFactionId, relationship);
    }

    @GetMapping("/{campaignId}/factions/{factionId}/relationships")
    public Set<FactionRelationship> listRelationships(@PathVariable Long campaignId,
                                                      @PathVariable Long factionId) {
        return factions.getRelationships(campaignId, factionId);
    }

    // ------------------------------------------------------------------
    // Standing (reputation)
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/factions/{factionId}/standing")
    public Faction adjustStanding(@PathVariable Long campaignId, @PathVariable Long factionId,
                                  @RequestParam NpcRelationship reputation) {
        return factions.adjustStanding(campaignId, factionId, reputation);
    }

    // ------------------------------------------------------------------
    // Removal
    // ------------------------------------------------------------------

    @DeleteMapping("/{campaignId}/factions/{factionId}")
    public void removeFaction(@PathVariable Long campaignId, @PathVariable Long factionId) {
        factions.removeFaction(campaignId, factionId);
    }
}
