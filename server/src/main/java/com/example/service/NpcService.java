package com.example.service;

import com.example.domain.Campaign;
import com.example.domain.Disposition;
import com.example.domain.Location;
import com.example.domain.Npc;
import com.example.domain.NpcRelationship;
import com.example.domain.SavingThrowEntry;
import com.example.db.CampaignRepository;
import com.example.db.LocationRepository;
import com.example.db.NpcRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Business logic for persistent non-player characters owned by a campaign.
 *
 * <p>This service is the single place where NPCs are created, updated, and consulted.
 * Every mutation resolves its owning campaign, applies the change to a managed entity,
 * and relies on the repository to persist it, so NPCs reload across sessions.</p>
 */
@Service
public class NpcService {

    private final CampaignRepository campaigns;
    private final LocationRepository locations;
    private final NpcRepository npcs;

    public NpcService(CampaignRepository campaigns,
                      LocationRepository locations,
                      NpcRepository npcs) {
        this.campaigns = campaigns;
        this.locations = locations;
        this.npcs = npcs;
    }

    // ------------------------------------------------------------------
    // Campaign / location lookup
    // ------------------------------------------------------------------

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private Location requireLocation(Long campaignId, Long locationId) {
        return locations.findById(locationId)
                .filter(l -> l.getCampaign() != null && l.getCampaign().getId() != null
                        && l.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No location with id " + locationId));
    }

    // ------------------------------------------------------------------
    // Creation and listing
    // ------------------------------------------------------------------

    /**
     * Creates a new active NPC in the given campaign with the given identity and story
     * fields. Combat statistics start empty and can be attached later.
     */
    public Npc addNpc(Long campaignId, String name, String description, String role,
                      Disposition disposition, String faction, NpcRelationship relationship) {
        Campaign campaign = requireCampaign(campaignId);
        Npc npc = new Npc(campaign, name, disposition, relationship);
        npc.setDescription(description);
        npc.setRole(role);
        npc.setFaction(faction);
        return npcs.save(npc);
    }

    public List<Npc> listNpcs(Long campaignId) {
        return npcs.findByCampaignOrderByName(requireCampaign(campaignId));
    }

    public List<Npc> listActiveNpcs(Long campaignId) {
        return npcs.findByCampaignAndActiveTrueOrderByName(requireCampaign(campaignId));
    }

    public List<Npc> listByDisposition(Long campaignId, Disposition disposition) {
        return npcs.findByCampaignAndDispositionOrderByName(requireCampaign(campaignId), disposition);
    }

    public List<Npc> listByRelationship(Long campaignId, NpcRelationship relationship) {
        return npcs.findByCampaignAndRelationshipOrderByName(requireCampaign(campaignId), relationship);
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    public Npc getNpc(Long campaignId, Long npcId) {
        return requireOwnedNpc(campaignId, npcId);
    }

    // ------------------------------------------------------------------
    // Story fields
    // ------------------------------------------------------------------

    public Npc updateStoryFields(Long campaignId, Long npcId, String description, String role,
                                 Disposition disposition, String faction, NpcRelationship relationship) {
        Npc npc = requireOwnedNpc(campaignId, npcId);
        if (description != null) {
            npc.setDescription(description);
        }
        if (role != null) {
            npc.setRole(role);
        }
        if (disposition != null) {
            npc.setDisposition(disposition);
        }
        if (faction != null) {
            npc.setFaction(faction);
        }
        if (relationship != null) {
            npc.setRelationship(relationship);
        }
        return npcs.save(npc);
    }

    public Npc setNotes(Long campaignId, Long npcId, String notes) {
        Npc npc = requireOwnedNpc(campaignId, npcId);
        npc.setNotes(notes);
        return npcs.save(npc);
    }

    public Npc setActive(Long campaignId, Long npcId, boolean active) {
        Npc npc = requireOwnedNpc(campaignId, npcId);
        npc.setActive(active);
        return npcs.save(npc);
    }

    public Npc setRelationship(Long campaignId, Long npcId, NpcRelationship relationship) {
        Npc npc = requireOwnedNpc(campaignId, npcId);
        npc.setRelationship(relationship);
        return npcs.save(npc);
    }

    public Npc setLocation(Long campaignId, Long npcId, Long locationId) {
        Npc npc = requireOwnedNpc(campaignId, npcId);
        npc.setLocation(requireLocation(campaignId, locationId));
        return npcs.save(npc);
    }

    // ------------------------------------------------------------------
    // Optional combat statistics
    // ------------------------------------------------------------------

    /**
     * Attaches the core combat statistics to an NPC. Returns the saved NPC.
     */
    public Npc setCombatStats(Long campaignId, Long npcId, Integer hitPoints, Integer maxHitPoints,
                              Integer armorClass, Integer movement, Integer proficiencyBonus,
                              Integer abilityStrength, Integer abilityDexterity, Integer abilityConstitution,
                              Integer abilityIntelligence, Integer abilityWisdom, Integer abilityCharisma) {
        Npc npc = requireOwnedNpc(campaignId, npcId);
        npc.setHitPoints(hitPoints);
        npc.setMaxHitPoints(maxHitPoints);
        npc.setArmorClass(armorClass);
        npc.setMovement(movement);
        npc.setProficiencyBonus(proficiencyBonus);
        npc.setAbilityStrength(abilityStrength);
        npc.setAbilityDexterity(abilityDexterity);
        npc.setAbilityConstitution(abilityConstitution);
        npc.setAbilityIntelligence(abilityIntelligence);
        npc.setAbilityWisdom(abilityWisdom);
        npc.setAbilityCharisma(abilityCharisma);
        return npcs.save(npc);
    }

    public Npc setSavingThrows(Long campaignId, Long npcId, Set<SavingThrowEntry> savingThrows) {
        Npc npc = requireOwnedNpc(campaignId, npcId);
        npc.setSavingThrows(savingThrows);
        return npcs.save(npc);
    }

    public boolean npcHasCombatStats(Long campaignId, Long npcId) {
        return requireOwnedNpc(campaignId, npcId).hasCombatStats();
    }

    // ------------------------------------------------------------------
    // Removal
    // ------------------------------------------------------------------

    public void removeNpc(Long campaignId, Long npcId) {
        Npc npc = requireOwnedNpc(campaignId, npcId);
        npcs.delete(npc);
    }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    private Npc requireOwnedNpc(Long campaignId, Long npcId) {
        return npcs.findById(npcId)
                .filter(n -> n.getCampaign() != null && n.getCampaign().getId() != null
                        && n.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No npc with id " + npcId));
    }
}
