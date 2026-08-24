package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Quest;
import com.example.domain.QuestStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for {@link Quest} entities.
 *
 * <p>Every quest is owned by exactly one {@link Campaign}, so this repository is
 * organised around the campaign that owns a set of quests. It exposes the data access
 * operations needed to persist and retrieve quests; it contains no business logic and
 * only depends on Spring Data JPA.</p>
 */
@Repository
public interface QuestRepository extends JpaRepository<Quest, Long> {

    /**
     * Finds every quest owned by the given campaign, ordered by title.
     *
     * @param campaign the owning campaign
     * @return all quests owned by the campaign, ordered by title (never {@code null})
     */
    List<Quest> findByCampaignOrderByTitle(Campaign campaign);

    /**
     * Finds every quest owned by the given campaign in the given status, ordered by
     * title.
     *
     * @param campaign the owning campaign
     * @param status   the quest status to match
     * @return the matching quests, ordered by title (never {@code null})
     */
    List<Quest> findByCampaignAndStatusOrderByTitle(Campaign campaign, QuestStatus status);
}
