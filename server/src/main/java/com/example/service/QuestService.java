package com.example.service;

import com.example.domain.Campaign;
import com.example.domain.Location;
import com.example.domain.Objective;
import com.example.domain.ObjectiveStatus;
import com.example.domain.Quest;
import com.example.domain.QuestLocationRef;
import com.example.domain.QuestStatus;
import com.example.db.CampaignRepository;
import com.example.db.LocationRepository;
import com.example.db.ObjectiveRepository;
import com.example.db.QuestRepository;

import com.example.service.CampaignEventService;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Business logic for persistent quests owned by a campaign.
 *
 * <p>This service is the single place where quests are created, updated, consulted,
 * and progressed. Every mutation resolves its owning campaign, applies the change to a
 * managed entity, and relies on the repository to persist it, so quests and their
 * objectives reload across sessions within a campaign.</p>
 */
@Service
public class QuestService {

    private final CampaignRepository campaigns;
    private final LocationRepository locations;
    private final QuestRepository quests;
    private final ObjectiveRepository objectives;
    private final CampaignEventService events;

    public QuestService(CampaignRepository campaigns,
                        LocationRepository locations,
                        QuestRepository quests,
                        ObjectiveRepository objectives,
                        CampaignEventService events) {
        this.campaigns = campaigns;
        this.locations = locations;
        this.quests = quests;
        this.objectives = objectives;
        this.events = events;
    }

    // ------------------------------------------------------------------
    // Campaign / location / quest / objective lookup
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

    private Quest requireOwnedQuest(Long campaignId, Long questId) {
        return quests.findById(questId)
                .filter(q -> q.getCampaign() != null && q.getCampaign().getId() != null
                        && q.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No quest with id " + questId));
    }

    private Objective requireOwnedObjective(Long campaignId, Long objectiveId) {
        return objectives.findById(objectiveId)
                .filter(o -> o.getCampaign() != null && o.getCampaign().getId() != null
                        && o.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No objective with id " + objectiveId));
    }

    // ------------------------------------------------------------------
    // Creation and listing
    // ------------------------------------------------------------------

    /**
     * Creates a new active quest in the given campaign with the given identity fields.
     * The quest starts {@link QuestStatus#ACTIVE} with no objectives; objectives are
     * attached separately via {@link #addObjective}.
     *
     * @param relatedLocationIds the ids of locations in the campaign this quest is
     *     related to (may be empty, never {@code null})
     */
    public Quest addQuest(Long campaignId, String title, String description, String giver,
                          String rewards, QuestStatus status, List<Long> relatedLocationIds) {
        Campaign campaign = requireCampaign(campaignId);
        Quest quest = new Quest(campaign, title);
        quest.setDescription(description);
        quest.setGiver(giver);
        quest.setRewards(rewards);
        if (status != null) {
            quest.setStatus(status);
        }
        for (Long locationId : relatedLocationIds == null ? new ArrayList<Long>() : relatedLocationIds) {
            requireLocation(campaignId, locationId);
            quest.addRelatedLocation(locationId);
        }
        Quest saved = quests.save(quest);
        events.recordQuestChange(campaignId, saved.getId(), saved.getTitle(), saved.getStatus().name());
        return saved;
    }

    public List<Quest> listQuests(Long campaignId) {
        return quests.findByCampaignOrderByTitle(requireCampaign(campaignId));
    }

    /**
     * Lists the quests in the given campaign that are in the given status, ordered by
     * title.
     */
    public List<Quest> listQuestsByStatus(Long campaignId, QuestStatus status) {
        return quests.findByCampaignAndStatusOrderByTitle(requireCampaign(campaignId), status);
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    public Quest getQuest(Long campaignId, Long questId) {
        return requireOwnedQuest(campaignId, questId);
    }

    public List<Objective> listObjectives(Long campaignId, Long questId) {
        Quest quest = requireOwnedQuest(campaignId, questId);
        return objectives.findByQuestOrderByDescription(quest);
    }

    public Objective getObjective(Long campaignId, Long objectiveId) {
        return requireOwnedObjective(campaignId, objectiveId);
    }

    // ------------------------------------------------------------------
    // Quest identity and state fields
    // ------------------------------------------------------------------

    /**
     * Updates any of the mutable identity and state fields of a quest. Only the fields
     * that are {@code non-null} are changed, so this doubles as a partial update.
     * Returns the saved quest.
     */
    public Quest updateQuest(Long campaignId, Long questId, String title, String description,
                             String giver, String rewards, QuestStatus status) {
        Quest quest = requireOwnedQuest(campaignId, questId);
        if (title != null) {
            quest.setTitle(title);
        }
        if (description != null) {
            quest.setDescription(description);
        }
        if (giver != null) {
            quest.setGiver(giver);
        }
        if (rewards != null) {
            quest.setRewards(rewards);
        }
        QuestStatus previous = quest.getStatus();
        if (status != null) {
            quest.setStatus(status);
        }
        Quest saved = quests.save(quest);
        if (!previous.equals(saved.getStatus())) {
            events.recordQuestChange(campaignId, saved.getId(), saved.getTitle(), saved.getStatus().name());
        }
        return saved;
    }

    public Quest setNotes(Long campaignId, Long questId, String notes) {
        Quest quest = requireOwnedQuest(campaignId, questId);
        quest.setNotes(notes);
        return quests.save(quest);
    }

    // ------------------------------------------------------------------
    // Related locations
    // ------------------------------------------------------------------

    /**
     * Replaces the set of related locations for a quest with the given location ids.
     * Every listed location must belong to the given campaign. Idempotent for a
     * location already present. Returns the saved quest.
     *
     * @param relatedLocationIds the new set of related location ids (may be empty,
     *     never {@code null})
     */
    public Quest setRelatedLocations(Long campaignId, Long questId, List<Long> relatedLocationIds) {
        Quest quest = requireOwnedQuest(campaignId, questId);
        Set<Long> current = new LinkedHashSet<>();
        for (QuestLocationRef ref : quest.getRelatedLocations()) {
            current.add(ref.getLocationId());
        }
        for (Long locationId : relatedLocationIds == null ? new ArrayList<Long>() : relatedLocationIds) {
            requireLocation(campaignId, locationId);
            current.add(locationId);
        }
        quest.getRelatedLocations().clear();
        for (Long locationId : current) {
            quest.addRelatedLocation(locationId);
        }
        return quests.save(quest);
    }

    public Quest removeRelatedLocation(Long campaignId, Long questId, Long locationId) {
        Quest quest = requireOwnedQuest(campaignId, questId);
        quest.removeRelatedLocation(locationId);
        return quests.save(quest);
    }

    // ------------------------------------------------------------------
    // Objectives and per-objective completion tracking
    // ------------------------------------------------------------------

    /**
     * Adds a new objective to the given quest in the given campaign.
     *
     * @param targetCount the progress the objective requires to be complete (must be
     *     at least one)
     */
    public Objective addObjective(Long campaignId, Long questId, String description, int targetCount) {
        if (targetCount < 1) {
            throw new IllegalArgumentException("Objective target count must be at least 1");
        }
        Quest quest = requireOwnedQuest(campaignId, questId);
        Objective objective = new Objective(quest.getCampaign(), quest, description, targetCount);
        return objectives.save(objective);
    }

    /**
     * Sets the raw progress of an objective to {@code currentCount}, clamped to the
     * range {@code [0, targetCount]}, and recomputes its {@link ObjectiveStatus}.
     * Returns the saved objective.
     */
    public Objective setObjectiveProgress(Long campaignId, Long questId, Long objectiveId,
                                          int currentCount) {
        Quest quest = requireOwnedQuest(campaignId, questId);
        Objective objective = requireOwnedObjective(campaignId, objectiveId);
        if (!objective.getQuest().getId().equals(quest.getId())) {
            throw new IllegalArgumentException(
                    "Objective " + objectiveId + " does not belong to quest " + questId);
        }
        objective.setCurrentCount(currentCount);
        return objectives.save(objective);
    }

    /**
     * Marks the given objective complete. Returns the saved objective.
     */
    public Objective markObjectiveComplete(Long campaignId, Long questId, Long objectiveId) {
        Quest quest = requireOwnedQuest(campaignId, questId);
        Objective objective = requireOwnedObjective(campaignId, objectiveId);
        if (!objective.getQuest().getId().equals(quest.getId())) {
            throw new IllegalArgumentException(
                    "Objective " + objectiveId + " does not belong to quest " + questId);
        }
        objective.setCurrentCount(objective.getTargetCount());
        return objectives.save(objective);
    }

    // ------------------------------------------------------------------
    // Quest completion and failure
    // ------------------------------------------------------------------

    /**
     * Marks the quest complete, transitioning it to {@link QuestStatus#COMPLETED}.
     * Returns the saved quest.
     */
    public Quest completeQuest(Long campaignId, Long questId) {
        Quest quest = requireOwnedQuest(campaignId, questId);
        QuestStatus previous = quest.getStatus();
        quest.setStatus(QuestStatus.COMPLETED);
        Quest saved = quests.save(quest);
        if (!previous.equals(QuestStatus.COMPLETED)) {
            events.recordQuestChange(campaignId, saved.getId(), saved.getTitle(), saved.getStatus().name());
        }
        return saved;
    }

    /**
     * Marks the quest failed, transitioning it to {@link QuestStatus#FAILED}. Returns
     * the saved quest.
     */
    public Quest failQuest(Long campaignId, Long questId) {
        Quest quest = requireOwnedQuest(campaignId, questId);
        QuestStatus previous = quest.getStatus();
        quest.setStatus(QuestStatus.FAILED);
        Quest saved = quests.save(quest);
        if (!previous.equals(QuestStatus.FAILED)) {
            events.recordQuestChange(campaignId, saved.getId(), saved.getTitle(), saved.getStatus().name());
        }
        return saved;
    }

    // ------------------------------------------------------------------
    // Removal
    // ------------------------------------------------------------------

    public void removeQuest(Long campaignId, Long questId) {
        Quest quest = requireOwnedQuest(campaignId, questId);
        for (Objective objective : objectives.findByQuestOrderByDescription(quest)) {
            objectives.delete(objective);
        }
        quests.delete(quest);
    }
}
