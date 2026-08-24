package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A single, independently tracked objective within a {@link Quest}. Each
 * {@link Objective} records what the players must accomplish
 * ({@link #description}), how much of it must be done ({@link #targetCount}), how
 * much has been done so far ({@link #currentCount}), and its own
 * {@link #status}.
 *
 * <p>The objective is its own entity, persisted in the {@code quest_objectives} table,
 * rather than an embedded collection, so that per-objective completion tracking
 * survives across sessions and a single objective can be updated or queried without
 * touching its sibling objectives. Every objective belongs to exactly one {@link
 * Quest}, and therefore to exactly one {@link Campaign}, so a objective exists only
 * inside the game that created it and never leaks into another campaign.</p>
 */
@Entity
@Table(
        name = "quest_objectives",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"quest_id", "description"}))
public class Objective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @Column(nullable = false)
    private String description;

    @Column(name = "target_count", nullable = false)
    private int targetCount = 1;

    @Column(name = "current_count")
    private int currentCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ObjectiveStatus status = ObjectiveStatus.INCOMPLETE;

    public Objective() {
        /* Required by JPA. */
    }

    public Objective(Campaign campaign, Quest quest, String description, int targetCount) {
        this.campaign = campaign;
        this.quest = quest;
        this.description = description;
        this.targetCount = targetCount;
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

    public Quest getQuest() {
        return quest;
    }

    public void setQuest(Quest quest) {
        this.quest = quest;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(int targetCount) {
        this.targetCount = targetCount;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    /**
     * Sets the raw progress value and recomputes the {@link #status} from it. Progress
     * is clamped to the range {@code [0, targetCount]}, so a value above the target
     * simply marks the objective complete rather than overflowing it.
     */
    public void setCurrentCount(int currentCount) {
        if (currentCount < 0) {
            currentCount = 0;
        }
        if (currentCount > targetCount) {
            currentCount = targetCount;
        }
        this.currentCount = currentCount;
        recomputeStatus();
    }

    public ObjectiveStatus getStatus() {
        return status;
    }

    public void setStatus(ObjectiveStatus status) {
        this.status = status;
    }

    /**
     * @return {@code true} when this objective has reached its target progress
     */
    public boolean isComplete() {
        return currentCount >= targetCount;
    }

    /**
     * Recomputes {@link #status} from the current progress against the target.
     */
    public void recomputeStatus() {
        this.status = isComplete()
                ? ObjectiveStatus.COMPLETE
                : ObjectiveStatus.INCOMPLETE;
    }
}
