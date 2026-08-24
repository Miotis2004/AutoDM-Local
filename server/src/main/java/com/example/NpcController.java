package com.example;

import com.example.domain.Disposition;
import com.example.domain.Npc;
import com.example.domain.NpcRelationship;
import com.example.domain.SavingThrowEntry;
import com.example.service.NpcService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST surface for persistent non-player characters.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link NpcService} call. All NPC construction, relationship tracking, and combat
 * statistics logic lives in the service, and persistence is what lets NPCs reload
 * across sessions.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class NpcController {

    private final NpcService npc;

    public NpcController(NpcService npc) {
        this.npc = npc;
    }

    // ------------------------------------------------------------------
    // Creation and listing
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/npcs")
    public Npc addNpc(@PathVariable Long campaignId,
                      @RequestParam String name,
                      @RequestParam(required = false) String description,
                      @RequestParam(required = false) String role,
                      @RequestParam Disposition disposition,
                      @RequestParam(required = false) String faction,
                      @RequestParam(defaultValue = "KNOWN") NpcRelationship relationship) {
        return npc.addNpc(campaignId, name, description, role, disposition, faction, relationship);
    }

    @GetMapping("/{campaignId}/npcs")
    public List<Npc> listNpcs(@PathVariable Long campaignId) {
        return npc.listNpcs(campaignId);
    }

    @GetMapping("/{campaignId}/npcs/active")
    public List<Npc> listActiveNpcs(@PathVariable Long campaignId) {
        return npc.listActiveNpcs(campaignId);
    }

    @GetMapping("/{campaignId}/npcs/disposition")
    public List<Npc> listByDisposition(@PathVariable Long campaignId,
                                       @RequestParam Disposition disposition) {
        return npc.listByDisposition(campaignId, disposition);
    }

    @GetMapping("/{campaignId}/npcs/relationship")
    public List<Npc> listByRelationship(@PathVariable Long campaignId,
                                        @RequestParam NpcRelationship relationship) {
        return npc.listByRelationship(campaignId, relationship);
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    @GetMapping("/{campaignId}/npcs/{npcId}")
    public Npc getNpc(@PathVariable Long campaignId, @PathVariable Long npcId) {
        return npc.getNpc(campaignId, npcId);
    }

    // ------------------------------------------------------------------
    // Story fields
    // ------------------------------------------------------------------

    @PutMapping("/{campaignId}/npcs/{npcId}")
    public Npc updateNpc(@PathVariable Long campaignId, @PathVariable Long npcId,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String role,
                         @RequestParam(required = false) Disposition disposition,
                         @RequestParam(required = false) String faction,
                         @RequestParam(required = false) NpcRelationship relationship) {
        return npc.updateStoryFields(campaignId, npcId, description, role, disposition, faction, relationship);
    }

    @PutMapping("/{campaignId}/npcs/{npcId}/notes")
    public Npc setNotes(@PathVariable Long campaignId, @PathVariable Long npcId,
                        @RequestParam(required = false) String notes) {
        return npc.setNotes(campaignId, npcId, notes);
    }

    // ------------------------------------------------------------------
    // Active / relationship / location
    // ------------------------------------------------------------------

    @PutMapping("/{campaignId}/npcs/{npcId}/active")
    public Npc setActive(@PathVariable Long campaignId, @PathVariable Long npcId,
                         @RequestParam boolean active) {
        return npc.setActive(campaignId, npcId, active);
    }

    @PutMapping("/{campaignId}/npcs/{npcId}/relationship")
    public Npc setRelationship(@PathVariable Long campaignId, @PathVariable Long npcId,
                               @RequestParam NpcRelationship relationship) {
        return npc.setRelationship(campaignId, npcId, relationship);
    }

    @PutMapping("/{campaignId}/npcs/{npcId}/location")
    public Npc setLocation(@PathVariable Long campaignId, @PathVariable Long npcId,
                           @RequestParam Long locationId) {
        return npc.setLocation(campaignId, npcId, locationId);
    }

    // ------------------------------------------------------------------
    // Optional combat statistics
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/npcs/{npcId}/combat-stats")
    public Npc setCombatStats(@PathVariable Long campaignId, @PathVariable Long npcId,
                              @RequestParam(required = false) Integer hitPoints,
                              @RequestParam(required = false) Integer maxHitPoints,
                              @RequestParam(required = false) Integer armorClass,
                              @RequestParam(required = false) Integer movement,
                              @RequestParam(required = false) Integer proficiencyBonus,
                              @RequestParam(required = false) Integer abilityStrength,
                              @RequestParam(required = false) Integer abilityDexterity,
                              @RequestParam(required = false) Integer abilityConstitution,
                              @RequestParam(required = false) Integer abilityIntelligence,
                              @RequestParam(required = false) Integer abilityWisdom,
                              @RequestParam(required = false) Integer abilityCharisma) {
        return npc.setCombatStats(campaignId, npcId, hitPoints, maxHitPoints, armorClass, movement,
                proficiencyBonus, abilityStrength, abilityDexterity, abilityConstitution,
                abilityIntelligence, abilityWisdom, abilityCharisma);
    }

    @PostMapping("/{campaignId}/npcs/{npcId}/saving-throws")
    public Npc setSavingThrows(@PathVariable Long campaignId, @PathVariable Long npcId,
                               @RequestParam List<String> abilities,
                               @RequestParam List<Integer> bonuses,
                               @RequestParam List<Boolean> proficient) {
        java.util.LinkedHashSet<SavingThrowEntry> entries = new java.util.LinkedHashSet<>();
        for (int i = 0; i < abilities.size(); i++) {
            entries.add(new SavingThrowEntry(abilities.get(i), bonuses.get(i), proficient.get(i)));
        }
        return npc.setSavingThrows(campaignId, npcId, entries);
    }

    @GetMapping("/{campaignId}/npcs/{npcId}/combat-stats")
    public boolean npcHasCombatStats(@PathVariable Long campaignId, @PathVariable Long npcId) {
        return npc.npcHasCombatStats(campaignId, npcId);
    }

    // ------------------------------------------------------------------
    // Removal
    // ------------------------------------------------------------------

    @DeleteMapping("/{campaignId}/npcs/{npcId}")
    public void removeNpc(@PathVariable Long campaignId, @PathVariable Long npcId) {
        npc.removeNpc(campaignId, npcId);
    }
}
