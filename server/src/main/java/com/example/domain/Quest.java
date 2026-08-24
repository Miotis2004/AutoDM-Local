package com.example.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * A quest is a tracked strand of campaign story: a set of {@link Objective}s the
 * players must complete, handed out by a {@link #giver}, offering {@link #rewards},
 * tied to related {@link #relatedLocations}, and annotated with free-form {@link
 * #notes}. Each quest is in exactly one {@link QuestStatus}: {@link QuestStatus#ACTIVE},
 * {@link QuestStatus#COMPLETED}, or {@link QuestStatus#FAILED}.
 *
 * <p>Every quest belongs to exactly one {@link Campaign}, so a quest exists only
 * inside the game that created it and never leaks into another campaign's state. The
 * objectives live in their own table ({@code quest_objectives}) and are managed through
 * the {@code campaign}'s repository, while the quest's own row stores its status,
 * giver, rewards, related locations, and notes.</p>
 */
@Entity
@Table(name = "quests")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestStatus status = QuestStatus.ACTIVE;

    /**
     * Who hands out the quest - typically an NPC or faction name. Kept as free-form
     * text so any identifier the campaign uses for a giver works.
     */
    @Column
    private String giver;

    @Column(columnDefinition = "TEXT")
    private String rewards;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * The locations in the campaign that this quest is related to, referenced by
     * location id. Present only when at least one related location has been set; a
     * quest with no related locations has an empty set here.
     */
    @Embedded
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "quest_related_locations",
            joinColumns = @JoinColumn(name = "quest_id"))
    private Set<QuestLocationRef> relatedLocations = new HashSet<>();

    public Quest() {
        /* Required by JPA. */
    }

    public Quest(Campaign campaign, String title) {
        this.campaign = campaign;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public void setStatus(QuestStatus status) {
        this.status = status;
    }

    public String getGiver() {
        return giver;
    }

    public void setGiver(String giver) {
        this.giver = giver;
    }

    public String getRewards() {
        return rewards;
    }

    public void setRewards(String rewards) {
        this.rewards = rewards;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Set<QuestLocationRef> getRelatedLocations() {
        return relatedLocations;
    }

    public void setRelatedLocations(Set<QuestLocationRef> relatedLocations) {
        this.relatedLocations = relatedLocations == null ? new HashSet<>() : relatedLocations;
    }

    /**
     * Adds a related location reference to this quest, if not already present.
     *
     * @return {@code true} if the location was newly added
     */
    public boolean addRelatedLocation(Long locationId) {
        return this.relatedLocations.add(new QuestLocationRef(locationId));
    }

    /**
     * Removes a related location reference from this quest, if present.
     *
     * @return {@code true} if the location was removed
     */
    public boolean removeRelatedLocation(Long locationId) {
        return this.relatedLocations.remove(new QuestLocationRef(locationId));
    }
}
