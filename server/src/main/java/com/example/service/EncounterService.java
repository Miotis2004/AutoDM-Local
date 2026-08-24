package com.example.service;

import com.example.db.CampaignRepository;
import com.example.db.EncounterRepository;
import com.example.db.LocationRepository;
import com.example.db.SceneRepository;
import com.example.domain.Campaign;
import com.example.domain.Encounter;
import com.example.domain.EncounterStatus;
import com.example.domain.Location;
import com.example.domain.Scene;

import com.example.service.CampaignEventService;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for {@link Encounter}s owned by a campaign.
 *
 * <p>This service is the single place where encounters are created, started,
 * finished, and consulted. Every mutation resolves its owning campaign, applies the
 * change to a managed entity, and relies on the repository to persist it, so
 * encounters reload across application restarts within a campaign. Turn bookkeeping
 * (whose turn it is and in what order participants act) is delegated to
 * {@link CombatantService}.</p>
 */
@Service
public class EncounterService {

    private final CampaignRepository campaigns;
    private final EncounterRepository encounters;
    private final SceneRepository scenes;
    private final LocationRepository locations;
    private final CampaignEventService events;

    public EncounterService(CampaignRepository campaigns, EncounterRepository encounters,
                            SceneRepository scenes, LocationRepository locations,
                            CampaignEventService events) {
        this.campaigns = campaigns;
        this.encounters = encounters;
        this.scenes = scenes;
        this.locations = locations;
        this.events = events;
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private Scene requireScene(Long campaignId, Long sceneId) {
        requireCampaign(campaignId);
        return scenes.findById(sceneId)
                .orElseThrow(() -> new IllegalArgumentException("No scene with id " + sceneId));
    }

    private Location requireLocation(Long campaignId, Long locationId) {
        requireCampaign(campaignId);
        return locations.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No location with id " + locationId));
    }

    public Encounter createEncounter(Long campaignId, Long sceneId, Long locationId) {
        Campaign campaign = requireCampaign(campaignId);
        Scene scene = requireScene(campaignId, sceneId);
        Location location = requireLocation(campaignId, locationId);
        Encounter encounter = new Encounter(campaign, scene, location);
        return encounters.save(encounter);
    }

    /**
     * Starts the given encounter, moving it from {@link EncounterStatus#SCHEDULED} to
     * {@link EncounterStatus#ACTIVE}.
     *
     * @param campaignId the owning campaign
     * @param encounterId the encounter to start
     * @return the now-active encounter (never {@code null})
     */
    public Encounter beginEncounter(Long campaignId, Long encounterId) {
        Encounter encounter = requireOwnedEncounter(campaignId, encounterId);
        if (encounter.getStatus() != EncounterStatus.SCHEDULED) {
            throw new ValidationException(
                    "Cannot begin encounter " + encounterId
                            + "; it is already " + encounter.getStatus() + ".");
        }
        encounter.begin();
        encounters.save(encounter);
        events.recordCombat(campaignId, encounterId, null);
        return encounter;
    }

    /**
     * Finishes the given encounter, moving it to {@link EncounterStatus#FINISHED}.
     *
     * @param campaignId the owning campaign
     * @param encounterId the encounter to finish
     * @return the finished encounter (never {@code null})
     */
    public Encounter finishEncounter(Long campaignId, Long encounterId) {
        Encounter encounter = requireOwnedEncounter(campaignId, encounterId);
        if (encounter.getStatus() != EncounterStatus.ACTIVE) {
            throw new ValidationException(
                    "Cannot finish encounter " + encounterId
                            + "; it is " + encounter.getStatus() + ", not active.");
        }
        encounter.finish();
        return encounters.save(encounter);
    }

    public Encounter getEncounter(Long campaignId, Long encounterId) {
        return requireOwnedEncounter(campaignId, encounterId);
    }

    public List<Encounter> listEncounters(Long campaignId) {
        requireCampaign(campaignId);
        return encounters.findByCampaignOrderByCreatedAtAsc(requireCampaign(campaignId));
    }

    private Encounter requireOwnedEncounter(Long campaignId, Long encounterId) {
        return encounters.findById(encounterId)
                .filter(e -> e.getCampaign() != null && e.getCampaign().getId() != null
                        && e.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No encounter with id " + encounterId));
    }
}
