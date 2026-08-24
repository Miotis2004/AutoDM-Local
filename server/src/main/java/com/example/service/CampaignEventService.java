package com.example.service;

import com.example.domain.Campaign;
import com.example.domain.CampaignEvent;
import com.example.domain.CampaignEventType;
import com.example.db.CampaignEventRepository;
import com.example.db.CampaignRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Business logic for persistent campaign events.
 *
 * <p>This service is the single place where significant campaign events are recorded
 * and consulted. Every mutation resolves its owning campaign and applies the change to
 * a managed entity, and persistence is what lets the campaign's event history reload
 * across application restarts and survive across sessions.</p>
 *
 * <p>Recording is centralized here so the DM engine and the other services can record
 * events through a small, consistent set of helpers. Each helper builds a
 * {@link CampaignEvent} of the right {@link CampaignEventType}, stamps it with a
 * human-readable description and (where useful) structured JSON detail, and persists it,
 * so every event in a campaign's history has the same shape.</p>
 */
@Service
public class CampaignEventService {

    private final CampaignRepository campaigns;
    private final CampaignEventRepository events;

    public CampaignEventService(CampaignRepository campaigns, CampaignEventRepository events) {
        this.campaigns = campaigns;
        this.events = events;
    }

    // ------------------------------------------------------------------
    // Campaign lookup
    // ------------------------------------------------------------------

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private CampaignEvent requireOwnedEvent(Long campaignId, Long eventId) {
        return events.findById(eventId)
                .filter(e -> e.getCampaign() != null && e.getCampaign().getId() != null
                        && e.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No event with id " + eventId));
    }

    // ------------------------------------------------------------------
    // Recording
    // ------------------------------------------------------------------

    /**
     * Records a new event on the given campaign at the current time.
     *
     * @param campaignId the campaign to record the event on
     * @param type       the kind of event being recorded
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordEvent(Long campaignId, CampaignEventType type) {
        return recordEvent(campaignId, type, LocalDateTime.now());
    }

    /**
     * Records a new event on the given campaign at the given time.
     *
     * @param campaignId the campaign to record the event on
     * @param type       the kind of event being recorded
     * @param at         the time the event occurred
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordEvent(Long campaignId, CampaignEventType type, LocalDateTime at) {
        Campaign campaign = requireCampaign(campaignId);
        return events.save(new CampaignEvent(campaign, type, at));
    }

    /**
     * Records a new event on the given campaign at the current time, with an optional
     * human-readable description.
     *
     * @param campaignId the campaign to record the event on
     * @param type       the kind of event being recorded
     * @param description a human-readable description, or {@code null} to leave unset
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordEvent(Long campaignId, CampaignEventType type, String description) {
        CampaignEvent event = recordEvent(campaignId, type, LocalDateTime.now());
        if (description != null) {
            event.setDescription(description);
            return events.save(event);
        }
        return event;
    }

    /**
     * Records a new event on the given campaign at the current time, with a human-readable
     * description and optional structured (typically JSON) detail. This is the shared entry
     * point the DM engine and the other services use to record significant moments, so the
     * shape of every event is consistent across the campaign's history.
     *
     * @param campaignId the campaign to record the event on
     * @param type       the kind of event being recorded
     * @param description a human-readable description; falls back to the type name when null
     * @param details    optional structured detail, or {@code null} to leave unset
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordEvent(
            Long campaignId, CampaignEventType type, String description, String details) {
        Campaign campaign = requireCampaign(campaignId);
        CampaignEvent event = new CampaignEvent(campaign, type, LocalDateTime.now())
                .withDescription(description != null && !description.isBlank() ? description : type.name());
        if (details != null) {
            event.withDetails(details);
        }
        return events.save(event);
    }

    // ------------------------------------------------------------------
    // Session events
    // ------------------------------------------------------------------

    /**
     * Records that the play session began.
     *
     * @param campaignId the owning campaign
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordSessionStart(Long campaignId) {
        return recordEvent(campaignId, CampaignEventType.SESSION_START,
                "The play session began", null);
    }

    /**
     * Records that the play session ended.
     *
     * @param campaignId the owning campaign
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordSessionEnd(Long campaignId) {
        return recordEvent(campaignId, CampaignEventType.SESSION_END,
                "The play session ended", null);
    }

    // ------------------------------------------------------------------
    // Location and discovery events
    // ------------------------------------------------------------------

    /**
     * Records that the party entered a location.
     *
     * @param campaignId    the owning campaign
     * @param locationId    the id of the location entered (recorded in the structured detail)
     * @param locationName  the location's name, used in the human-readable description
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordLocationEntry(Long campaignId, Long locationId, String locationName) {
        String name = (locationName != null && !locationName.isBlank()) ? locationName
                : String.valueOf(locationId);
        return recordEvent(campaignId, CampaignEventType.LOCATION_ENTRY,
                "The party entered " + name,
                "{\"location\":" + locationId + "}");
    }

    /**
     * Records the discovery of a location, point of interest, or settlement.
     *
     * @param campaignId    the owning campaign
     * @param discoveredName the name of what was discovered, used in the description
     * @param discoveredId   the id of what was discovered (recorded in the structured detail)
     * @param kind           a short label for what was discovered (for example "location")
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordDiscovery(
            Long campaignId, String discoveredName, Long discoveredId, String kind) {
        String label = (discoveredName != null && !discoveredName.isBlank()) ? discoveredName
                : String.valueOf(discoveredId);
        String field = (kind != null && !kind.isBlank()) ? kind.toLowerCase(Locale.ROOT) : "discovery";
        return recordEvent(campaignId, CampaignEventType.DISCOVERY,
                "A new discovery: " + label,
                "{\"" + field + "\":" + discoveredId + "}");
    }

    // ------------------------------------------------------------------
    // Combat events
    // ------------------------------------------------------------------

    /**
     * Records that a combat encounter has begun.
     *
     * @param campaignId    the owning campaign
     * @param encounterId   the id of the encounter that began (recorded in the structured detail)
     * @param encounterLabel a human-readable label for the encounter (may be {@code null})
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordCombat(Long campaignId, Long encounterId, String encounterLabel) {
        String label = (encounterLabel != null && !encounterLabel.isBlank())
                ? encounterLabel : "A combat encounter began";
        return recordEvent(campaignId, CampaignEventType.COMBAT,
                label, "{\"encounter\":" + encounterId + "}");
    }

    // ------------------------------------------------------------------
    // Item, quest, and relationship events
    // ------------------------------------------------------------------

    /**
     * Records that an item was acquired by an owner.
     *
     * @param campaignId the owning campaign
     * @param itemName   the name of the acquired item, used in the description
     * @param ownerKind  the kind of owner that acquired the item (for example "party")
     * @param ownerId    the id of the owner that acquired the item (recorded in the detail)
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordItemAcquisition(
            Long campaignId, String itemName, String ownerKind, Long ownerId) {
        return recordEvent(campaignId, CampaignEventType.ITEM_ACQUISITION,
                "Acquired: " + itemName,
                "{\"item\":" + ownerId
                        + ",\"ownerKind\":\"" + ownerKind + "\",\"name\":\"" + itemName + "\"}");
    }

    /**
     * Records that a quest changed state (started, progressed, completed, or failed).
     *
     * @param campaignId the owning campaign
     * @param questId    the id of the quest that changed (recorded in the structured detail)
     * @param questName  the quest's name, used in the description
     * @param status     the quest's new status, used in the description and structured detail
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordQuestChange(Long campaignId, Long questId, String questName, String status) {
        return recordEvent(campaignId, CampaignEventType.QUEST_CHANGE,
                "Quest: " + (questName != null ? questName : questId) + " -> " + status,
                "{\"quest\":" + questId + "\",\"status\":\"" + status + "\"}");
    }

    /**
     * Records that a relationship between two parties changed.
     *
     * @param campaignId     the owning campaign
     * @param factionId      the id of the faction whose relationship changed (recorded in the detail)
     * @param relatedFactionId the id of the faction it now regards (recorded in the detail)
     * @param relationship   the relationship that now holds (for example "ally"), used in the detail
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordRelationshipChange(
            Long campaignId, Long factionId, Long relatedFactionId, String relationship) {
        return recordEvent(campaignId, CampaignEventType.RELATIONSHIP_CHANGE,
                "Relationship change for faction " + factionId
                        + " toward " + relatedFactionId
                        + (relationship != null ? " (" + relationship + ")" : ""),
                "{\"faction\":" + factionId
                        + ",\"relatedFaction\":" + relatedFactionId
                        + ",\"relationship\":\"" + (relationship != null ? relationship : "") + "\"}");
    }

    /**
     * Records that a faction's standing (reputation) with the wider world changed.
     *
     * @param campaignId   the owning campaign
     * @param factionId    the id of the faction whose standing changed (recorded in the detail)
     * @param reputation   the standing that now holds (for example \"favoured\"), used in the detail
     * @return the recorded event (never {@code null})
     */
    public CampaignEvent recordStandingChange(Long campaignId, Long factionId, String reputation) {
        return recordEvent(campaignId, CampaignEventType.STANDING_CHANGE,
                "Standing change for faction " + factionId
                        + (reputation != null ? " (" + reputation + ")" : ""),
                "\"faction\":" + factionId
                        + ",\"reputation\":" + (reputation != null ? reputation : "") + "}");
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    /**
     * Returns every event recorded on the given campaign, most recent first.
     *
     * @param campaignId the owning campaign
     * @return the campaign's events, most recent first (never {@code null})
     */
    public List<CampaignEvent> listEvents(Long campaignId) {
        return events.findByCampaignOrderByIdDesc(requireCampaign(campaignId));
    }

    /**
     * Returns the events of a single type recorded on the given campaign, most recent
     * first.
     *
     * @param campaignId the owning campaign
     * @param type       the event type to filter by
     * @return the campaign's events of the given type, most recent first (never
     *     {@code null})
     */
    public List<CampaignEvent> listEventsByType(Long campaignId, CampaignEventType type) {
        return events.findByCampaignAndEventTypeOrderByIdDesc(requireCampaign(campaignId), type);
    }

    /**
     * Inspects a single event, confirming it belongs to the given campaign.
     *
     * @param campaignId the owning campaign
     * @param eventId    the event to inspect
     * @return the event (never {@code null})
     */
    public CampaignEvent getEvent(Long campaignId, Long eventId) {
        return requireOwnedEvent(campaignId, eventId);
    }
}
