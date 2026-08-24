package com.example.service;

import com.example.domain.Campaign;
import com.example.domain.Disposition;
import com.example.domain.Faction;
import com.example.domain.FactionRelationship;
import com.example.domain.NpcRelationship;
import com.example.db.CampaignRepository;
import com.example.db.FactionRepository;

import com.example.service.CampaignEventService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Business logic for persistent factions owned by a campaign.
 *
 * <p>This service is the single place where factions are created, updated, and
 * consulted. Every mutation resolves its owning campaign, applies the change to a
 * managed entity, and relies on the repository to persist it, so factions reload
 * across sessions.</p>
 */
@Service
public class FactionService {

    private final CampaignRepository campaigns;
    private final FactionRepository factions;
    private final CampaignEventService events;

    public FactionService(
            CampaignRepository campaigns, FactionRepository factions, CampaignEventService events) {
        this.campaigns = campaigns;
        this.factions = factions;
        this.events = events;
    }

    // ------------------------------------------------------------------
    // Campaign / faction lookup
    // ------------------------------------------------------------------

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private Faction requireOwnedFaction(Long campaignId, Long factionId) {
        return factions.findById(factionId)
                .filter(f -> f.getCampaign() != null && f.getCampaign().getId() != null
                        && f.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No faction with id " + factionId));
    }

    // ------------------------------------------------------------------
    // Creation and listing
    // ------------------------------------------------------------------

    /**
     * Creates a new faction in the given campaign with the given identity and nature.
     */
    public Faction addFaction(Long campaignId, String name, String description,
                              Disposition disposition, NpcRelationship reputation) {
        Campaign campaign = requireCampaign(campaignId);
        Faction faction = new Faction(campaign, name, disposition, reputation);
        faction.setDescription(description);
        return factions.save(faction);
    }

    public List<Faction> listFactions(Long campaignId) {
        return factions.findByCampaignOrderByName(requireCampaign(campaignId));
    }

    public List<Faction> listByDisposition(Long campaignId, Disposition disposition) {
        return factions.findByCampaignAndDispositionOrderByName(requireCampaign(campaignId), disposition);
    }

    public List<Faction> listByReputation(Long campaignId, NpcRelationship reputation) {
        return factions.findByCampaignAndReputationOrderByName(requireCampaign(campaignId), reputation);
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    public Faction getFaction(Long campaignId, Long factionId) {
        return requireOwnedFaction(campaignId, factionId);
    }

    // ------------------------------------------------------------------
    // Update and persistence
    // ------------------------------------------------------------------

    /**
     * Updates any of the mutable identity and nature fields of a faction. Only the
     * fields that are {@code non-null} are changed, so this doubles as a partial
     * update. Returns the saved faction.
     */
    public Faction updateFaction(Long campaignId, Long factionId, String description,
                                 Disposition disposition, NpcRelationship reputation) {
        Faction faction = requireOwnedFaction(campaignId, factionId);
        if (description != null) {
            faction.setDescription(description);
        }
        if (disposition != null) {
            faction.setDisposition(disposition);
        }
        if (reputation != null) {
            faction.setReputation(reputation);
        }
        return factions.save(faction);
    }

    public Faction setNotes(Long campaignId, Long factionId, String notes) {
        Faction faction = requireOwnedFaction(campaignId, factionId);
        faction.setNotes(notes);
        return factions.save(faction);
    }

    // ------------------------------------------------------------------
    // Faction-to-faction relationships
    // ------------------------------------------------------------------

    /**
     * Records (or overwrites) the relationship this faction holds toward another
     * faction in the same campaign. Both factions must belong to the given campaign.
     * Idempotent for the same pair and relationship. Returns the saved faction.
     */
    public Faction setRelationship(Long campaignId, Long factionId, Long relatedFactionId,
                                   NpcRelationship relationship) {
        requireOwnedFaction(campaignId, relatedFactionId);
        Faction faction = requireOwnedFaction(campaignId, factionId);
        faction.setRelationship(relatedFactionId, relationship);
        Faction saved = factions.save(faction);
        events.recordRelationshipChange(campaignId, saved.getId(), relatedFactionId,
                relationship != null ? relationship.name() : null);
        return saved;
    }

    public Faction removeRelationship(Long campaignId, Long factionId, Long relatedFactionId,
                                      NpcRelationship relationship) {
        Faction faction = requireOwnedFaction(campaignId, factionId);
        faction.removeRelationship(relatedFactionId, relationship);
        return factions.save(faction);
    }

    public Set<FactionRelationship> getRelationships(Long campaignId, Long factionId) {
        return requireOwnedFaction(campaignId, factionId).getRelationships();
    }

    // ------------------------------------------------------------------
    // Standing (reputation)
    // ------------------------------------------------------------------

    /**
     * Applies a new standing (reputation) for a faction and persists it. This is the
     * durable, campaign-scoped record of how the wider world regards the faction. The
     * change is recorded as a {@code STANDING_CHANGE} campaign event and the saved
     * faction is returned.
     */
    public Faction adjustStanding(Long campaignId, Long factionId, NpcRelationship reputation) {
        Faction faction = requireOwnedFaction(campaignId, factionId);
        faction.setReputation(reputation);
        Faction saved = factions.save(faction);
        events.recordStandingChange(campaignId, saved.getId(),
                reputation != null ? reputation.name() : null);
        return saved;
    }

    // ------------------------------------------------------------------
    // Removal
    // ------------------------------------------------------------------

    public void removeFaction(Long campaignId, Long factionId) {
        factions.delete(requireOwnedFaction(campaignId, factionId));
    }
}
