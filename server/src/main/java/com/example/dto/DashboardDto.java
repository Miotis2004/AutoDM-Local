package com.example.dto;

import com.example.dto.Summary.Character;
import com.example.dto.Summary.Encounter;
import com.example.dto.Summary.Event;
import com.example.dto.Summary.Quest;

import java.util.List;

/**
 * Aggregated read model for the dashboard.
 *
 * <p>The dashboard summarises a single campaign at a glance, so this DTO bundles every piece of
 * state a glance needs into one payload: the active {@link CampaignDto} (the active campaign and
 * its summary), the party's current {@link #location}, the {@link #characters} in the party, the
 * campaign's {@link #quests}, any {@link #encounter} in progress, and the most recent
 * {@link #events}. Every field is optional or empty when the campaign has not yet produced that
 * kind of state, so a freshly created campaign still renders a complete, non-crashing dashboard.</p>
 */
public class DashboardDto {

    private CampaignDto campaign;
    private String location;
    private List<Character> characters;
    private List<Quest> quests;
    private Encounter encounter;
    private String summary;
    private List<Event> events;
    private SetupProgress setupProgress;

    public DashboardDto() {
        /* Required by Jackson. */
    }

    public DashboardDto(CampaignDto campaign, String location, List<Character> characters,
                        List<Quest> quests, Encounter encounter, String summary, List<Event> events,
                        SetupProgress setupProgress) {
        this.campaign = campaign;
        this.location = location;
        this.characters = characters;
        this.quests = quests;
        this.encounter = encounter;
        this.summary = summary;
        this.events = events;
        this.setupProgress = setupProgress;
    }

    public CampaignDto getCampaign() {
        return campaign;
    }

    public void setCampaign(CampaignDto campaign) {
        this.campaign = campaign;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<Character> getCharacters() {
        return characters;
    }

    public void setCharacters(List<Character> characters) {
        this.characters = characters;
    }

    public List<Quest> getQuests() {
        return quests;
    }

    public void setQuests(List<Quest> quests) {
        this.quests = quests;
    }

    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public SetupProgress getSetupProgress() {
        return setupProgress;
    }

    public void setSetupProgress(SetupProgress setupProgress) {
        this.setupProgress = setupProgress;
    }

    /**
     * Tracks how far a campaign has moved through the end-to-end setup workflow: adding a party of
     * player characters, populating the world with regions, locations, and settlements, and adding
     * the NPCs, quests, and inventory items that give the world something to do. Every count is
     * resolved from the same persisted state the rest of the app reads, and the {@link NextStep}s
     * point the operator toward the next gap to fill.
     */
    public static class SetupProgress {
        private long characters;
        private long regions;
        private long locations;
        private long settlements;
        private long npcs;
        private long quests;
        private long items;
        private List<NextStep> nextSteps;

        public SetupProgress(long characters, long regions, long locations, long settlements,
                             long npcs, long quests, long items, List<NextStep> nextSteps) {
            this.characters = characters;
            this.regions = regions;
            this.locations = locations;
            this.settlements = settlements;
            this.npcs = npcs;
            this.quests = quests;
            this.items = items;
            this.nextSteps = nextSteps;
        }

        public long getCharacters() {
            return characters;
        }

        public void setCharacters(long characters) {
            this.characters = characters;
        }

        public long getRegions() {
            return regions;
        }

        public void setRegions(long regions) {
            this.regions = regions;
        }

        public long getLocations() {
            return locations;
        }

        public void setLocations(long locations) {
            this.locations = locations;
        }

        public long getSettlements() {
            return settlements;
        }

        public void setSettlements(long settlements) {
            this.settlements = settlements;
        }

        public long getNpcs() {
            return npcs;
        }

        public void setNpcs(long npcs) {
            this.npcs = npcs;
        }

        public long getItemCount() {
            return items;
        }

        public void setItemCount(long items) {
            this.items = items;
        }

        public List<NextStep> getNextSteps() {
            return nextSteps;
        }

        public void setNextSteps(List<NextStep> nextSteps) {
            this.nextSteps = nextSteps;
        }
    }

    /**
     * A single suggested next step in the setup workflow: a human-readable {@code label} describing
     * what to do and a {@code route} the front-end can navigate to in order to do it.
     */
    public static class NextStep {
        private String label;
        private String route;

        public NextStep(String label, String route) {
            this.label = label;
            this.route = route;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getRoute() {
            return route;
        }

        public void setRoute(String route) {
            this.route = route;
        }
    }
}
