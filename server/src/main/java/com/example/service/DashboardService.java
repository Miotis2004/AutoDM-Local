package com.example.service;

import com.example.db.CampaignRepository;
import com.example.domain.Campaign;
import com.example.domain.CampaignEvent;
import com.example.domain.Encounter;
import com.example.domain.EncounterStatus;
import com.example.domain.PartyLocation;
import com.example.domain.PlayerCharacter;
import com.example.domain.Quest;
import com.example.dto.CampaignDto;
import com.example.dto.DashboardDto;
import com.example.dto.DashboardDto.NextStep;
import com.example.dto.DashboardDto.SetupProgress;
import com.example.dto.Summary.Character;
import com.example.dto.Summary.Event;
import com.example.service.NpcService;
import com.example.service.InventoryService;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for the dashboard read model.
 *
 * <p>The dashboard is a single glance at one campaign. This service aggregates the state that
 * otherwise lives across several owners - campaign metadata, the party's current location, the
 * roster of player characters, the campaign's quests, any encounter in progress, and the most
 * recent campaign events - into one {@link DashboardDto}. It owns no data of its own: every value
 * is resolved through the existing {@link CharacterService}, {@link QuestService},
 * {@link EncounterService}, {@link WorldService}, and {@link CampaignEventService} services, or the
 * {@link CampaignRepository}, so the dashboard always reflects the same persisted state the rest of
 * the app reads.</p>
 *
 * <p>Every field is best-effort. A campaign that has not yet produced a location, characters,
 * quests, an encounter, or events still yields a well-formed dashboard with an empty or {@code null}
 * collection for that piece, so the UI never reads stale or missing data.</p>
 */
@Service
public class DashboardService {

    private static final int RECENT_EVENT_LIMIT = 10;

    private final CampaignRepository campaigns;
    private final CharacterService characters;
    private final QuestService questService;
    private final EncounterService encounterService;
    private final WorldService world;
    private final CampaignEventService events;
    private final NpcService npcs;
    private final InventoryService inventory;

    public DashboardService(CampaignRepository campaigns,
                            CharacterService characters,
                            QuestService questService,
                            EncounterService encounterService,
                            WorldService world,
                            CampaignEventService events,
                            NpcService npcs,
                            InventoryService inventory) {
        this.campaigns = campaigns;
        this.characters = characters;
        this.questService = questService;
        this.encounterService = encounterService;
        this.world = world;
        this.events = events;
        this.npcs = npcs;
        this.inventory = inventory;
    }

    /**
     * Builds the dashboard for the given campaign.
     *
     * @param campaignId the owning campaign
     * @return the aggregated dashboard state (never {@code null})
     * @throws IllegalArgumentException when no campaign exists with the given id
     */
    public DashboardDto dashboard(Long campaignId) {
        Campaign campaign = requireCampaign(campaignId);

        CampaignDto campaignDto = CampaignDto.from(campaign);
        String location = currentLocationName(campaignId);
        List<Character> party = characters.list(campaignId).stream()
                .map(DashboardService::toCharacter)
                .toList();
        List<com.example.dto.Summary.Quest> activeQuests = questService.listQuests(campaignId)
                .stream()
                .map(DashboardService::toQuest)
                .toList();
        com.example.dto.Summary.Encounter encounter = toView(activeEncounter(campaignId));
        String summary = buildSummary(campaignDto, location, party, activeQuests, encounter);
        List<Event> recent = recentEvents(campaignId);
        SetupProgress setup = buildSetupProgress(campaignId);

        return new DashboardDto(campaignDto, location, party, activeQuests, encounter, summary,
                recent, setup);
    }

    /**
     * Measures how far the campaign has progressed through the end-to-end setup workflow and suggests
     * the next gaps to fill. The counts are resolved through the same services the rest of the app
     * uses, so a freshly created campaign reports zeros and a short list of "start here" next steps;
     * a fully set-up campaign reports every count populated and an empty next-step list.
     *
     * @param campaignId the owning campaign
     * @return the setup progress for the campaign
     */
    private SetupProgress buildSetupProgress(Long campaignId) {
        long characterCount = characters.list(campaignId).size();
        long regionCount = world.listRegions(campaignId).size();
        long locationCount = world.listLocations(campaignId).size();
        long settlementCount = world.listSettlements(campaignId).size();
        long npcCount = npcs.listNpcs(campaignId).size();
        long questCount = questService.listQuests(campaignId).size();
        long itemCount = inventory.listHoldings(campaignId, null, null).size();

        List<NextStep> nextSteps = new ArrayList<>();
        if (characterCount == 0) {
            nextSteps.add(new NextStep("Add your first player character", "/characters"));
        }
        if (regionCount == 0 || locationCount == 0) {
            nextSteps.add(new NextStep("Build the world: regions, locations, and settlements", "/world"));
        }
        if (npcCount == 0) {
            nextSteps.add(new NextStep("Populate the world with NPCs", "/npcs"));
        }
        if (questCount == 0) {
            nextSteps.add(new NextStep("Set the story in motion with a quest", "/quests"));
        }
        if (itemCount == 0) {
            nextSteps.add(new NextStep("Give the party some inventory", "/items"));
        }
        return new SetupProgress(characterCount, regionCount, locationCount, settlementCount,
                npcCount, questCount, itemCount, nextSteps);
    }

    private String currentLocationName(Long campaignId) {
        Optional<PartyLocation> location = world.findPartyLocation(campaignId);
        if (location.isEmpty() || location.get().getLocation() == null) {
            return null;
        }
        return location.get().getLocation().getName();
    }

    /**
     * Resolves the encounter the dashboard should surface: the active one when a combat is running,
     * otherwise the most recently recorded encounter, or {@code null} when there are none.
     */
    private Encounter activeEncounter(Long campaignId) {
        List<Encounter> listed = encounterService.listEncounters(campaignId);
        for (Encounter encounter : listed) {
            if (encounter.getStatus() == EncounterStatus.ACTIVE) {
                return encounter;
            }
        }
        return listed.isEmpty() ? null : listed.get(listed.size() - 1);
    }

    private static com.example.dto.Summary.Encounter toView(Encounter liveEncounter) {
        if (liveEncounter == null) {
            return null;
        }
        return new com.example.dto.Summary.Encounter(liveEncounter.getId(),
                nonBlank(liveEncounter.getName()), liveEncounter.getStatus().name());
    }

    private List<Event> recentEvents(Long campaignId) {
        List<CampaignEvent> all = events.listEvents(campaignId);
        int count = Math.min(RECENT_EVENT_LIMIT, all.size());
        List<Event> recent = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            CampaignEvent event = all.get(i);
            recent.add(new Event(event.getId(), event.getEventType().name(),
                    event.getDescription(), event.getTimestamp()));
        }
        return recent;
    }

    private static Character toCharacter(PlayerCharacter character) {
        return new Character(character.getId(), character.getName(), character.getCharacterClass(),
                character.getLevel(), character.getHitPoints(), character.getMaxHitPoints());
    }

    private static com.example.dto.Summary.Quest toQuest(Quest quest) {
        return new com.example.dto.Summary.Quest(quest.getId(), quest.getTitle(),
                quest.getStatus().name());
    }

    private static String nonBlank(String value) {
        return value != null && !value.isBlank() ? value : "Encounter";
    }

    private static String buildSummary(CampaignDto campaign, String location,
                                       List<Character> party,
                                       List<com.example.dto.Summary.Quest> quests,
                                       com.example.dto.Summary.Encounter encounter) {
        StringBuilder builder = new StringBuilder();
        builder.append("Campaign ").append(campaign.getTitle());
        if (location != null) {
            builder.append(" \u2014 the party is at ").append(location);
        } else {
            builder.append(" \u2014 the party has not set a location yet");
        }
        builder.append('.');
        builder.append(' ').append(party.size()).append(" character").append(
                party.size() == 1 ? "" : "s");
        if (encounter != null) {
            builder.append(' ').append("ACTIVE".equals(encounter.getStatus())
                    ? "an active encounter"
                    : "a resolved encounter");
        }
        builder.append('.');
        long active = quests.stream().filter(q -> "ACTIVE".equals(q.getStatus())).count();
        if (active > 0) {
            builder.append(' ').append(active).append(" active quest")
                    .append(active == 1 ? "" : "s");
        }
        return builder.toString();
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }
}
