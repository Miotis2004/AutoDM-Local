package com.example;

import com.example.domain.Objective;
import com.example.domain.Quest;
import com.example.domain.QuestStatus;
import com.example.service.QuestService;

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
 * REST surface for persistent quests owned by a campaign.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link QuestService} call. All quest construction, objective tracking, reward and
 * giver handling, and state-transition logic lives in the service, and persistence is
 * what lets quests and their objectives reload across sessions within a campaign.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class QuestController {

    private final QuestService quests;

    public QuestController(QuestService quests) {
        this.quests = quests;
    }

    // ------------------------------------------------------------------
    // Creation and listing
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/quests")
    public Quest addQuest(@PathVariable Long campaignId,
                          @RequestParam String title,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) String giver,
                          @RequestParam(required = false) String rewards,
                          @RequestParam(required = false) QuestStatus status,
                          @RequestParam(required = false) List<Long> relatedLocationIds) {
        return quests.addQuest(campaignId, title, description, giver, rewards, status, relatedLocationIds);
    }

    @GetMapping("/{campaignId}/quests")
    public List<Quest> listQuests(@PathVariable Long campaignId) {
        return quests.listQuests(campaignId);
    }

    @GetMapping("/{campaignId}/quests/status")
    public List<Quest> listQuestsByStatus(@PathVariable Long campaignId,
                                          @RequestParam QuestStatus status) {
        return quests.listQuestsByStatus(campaignId, status);
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    @GetMapping("/{campaignId}/quests/{questId}")
    public Quest getQuest(@PathVariable Long campaignId, @PathVariable Long questId) {
        return quests.getQuest(campaignId, questId);
    }

    @GetMapping("/{campaignId}/quests/{questId}/objectives")
    public List<Objective> listObjectives(@PathVariable Long campaignId, @PathVariable Long questId) {
        return quests.listObjectives(campaignId, questId);
    }

    @GetMapping("/{campaignId}/objectives/{objectiveId}")
    public Objective getObjective(@PathVariable Long campaignId, @PathVariable Long objectiveId) {
        return quests.getObjective(campaignId, objectiveId);
    }

    // ------------------------------------------------------------------
    // Quest identity and state fields
    // ------------------------------------------------------------------

    @PutMapping("/{campaignId}/quests/{questId}")
    public Quest updateQuest(@PathVariable Long campaignId, @PathVariable Long questId,
                             @RequestParam(required = false) String title,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String giver,
                             @RequestParam(required = false) String rewards,
                             @RequestParam(required = false) QuestStatus status) {
        return quests.updateQuest(campaignId, questId, title, description, giver, rewards, status);
    }

    @PutMapping("/{campaignId}/quests/{questId}/notes")
    public Quest setNotes(@PathVariable Long campaignId, @PathVariable Long questId,
                          @RequestParam(required = false) String notes) {
        return quests.setNotes(campaignId, questId, notes);
    }

    // ------------------------------------------------------------------
    // Related locations
    // ------------------------------------------------------------------

    @PutMapping("/{campaignId}/quests/{questId}/locations")
    public Quest setRelatedLocations(@PathVariable Long campaignId, @PathVariable Long questId,
                                     @RequestParam(required = false) List<Long> relatedLocationIds) {
        return quests.setRelatedLocations(campaignId, questId, relatedLocationIds);
    }

    @DeleteMapping("/{campaignId}/quests/{questId}/locations/{locationId}")
    public Quest removeRelatedLocation(@PathVariable Long campaignId, @PathVariable Long questId,
                                       @PathVariable Long locationId) {
        return quests.removeRelatedLocation(campaignId, questId, locationId);
    }

    // ------------------------------------------------------------------
    // Objectives and per-objective completion tracking
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/quests/{questId}/objectives")
    public Objective addObjective(@PathVariable Long campaignId, @PathVariable Long questId,
                                  @RequestParam String description,
                                  @RequestParam(defaultValue = "1") int targetCount) {
        return quests.addObjective(campaignId, questId, description, targetCount);
    }

    @PutMapping("/{campaignId}/quests/{questId}/objectives/{objectiveId}/progress")
    public Objective setObjectiveProgress(@PathVariable Long campaignId, @PathVariable Long questId,
                                          @PathVariable Long objectiveId,
                                          @RequestParam int currentCount) {
        return quests.setObjectiveProgress(campaignId, questId, objectiveId, currentCount);
    }

    @PutMapping("/{campaignId}/quests/{questId}/objectives/{objectiveId}/complete")
    public Objective markObjectiveComplete(@PathVariable Long campaignId, @PathVariable Long questId,
                                           @PathVariable Long objectiveId) {
        return quests.markObjectiveComplete(campaignId, questId, objectiveId);
    }

    // ------------------------------------------------------------------
    // Quest completion and failure
    // ------------------------------------------------------------------

    @PutMapping("/{campaignId}/quests/{questId}/complete")
    public Quest completeQuest(@PathVariable Long campaignId, @PathVariable Long questId) {
        return quests.completeQuest(campaignId, questId);
    }

    @PutMapping("/{campaignId}/quests/{questId}/fail")
    public Quest failQuest(@PathVariable Long campaignId, @PathVariable Long questId) {
        return quests.failQuest(campaignId, questId);
    }

    // ------------------------------------------------------------------
    // Removal
    // ------------------------------------------------------------------

    @DeleteMapping("/{campaignId}/quests/{questId}")
    public void removeQuest(@PathVariable Long campaignId, @PathVariable Long questId) {
        quests.removeQuest(campaignId, questId);
    }
}
