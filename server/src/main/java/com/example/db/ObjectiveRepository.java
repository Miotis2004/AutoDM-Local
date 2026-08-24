package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Objective;
import com.example.domain.Quest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for {@link Objective} entities.
 *
 * <p>Every objective belongs to exactly one {@link Quest}, and therefore to exactly one
 * {@link Campaign}, so this repository is organised around the quests and campaigns that
 * own objectives. It exposes the data access operations needed to persist and retrieve
 * objectives; it contains no business logic and only depends on Spring Data JPA.</p>
 */
@Repository
public interface ObjectiveRepository extends JpaRepository<Objective, Long> {

    /**
     * Finds every objective of the given quest, ordered by description.
     *
     * @param quest the owning quest
     * @return all objectives of the quest, ordered by description (never {@code null})
     */
    List<Objective> findByQuestOrderByDescription(Quest quest);

    /**
     * Finds every objective owned by the given campaign, ordered by quest id then
     * description.
     *
     * @param campaign the owning campaign
     * @return all objectives owned by the campaign, ordered by quest id then
     *     description (never {@code null})
     */
    @Query("SELECT o from Objective o WHERE o.campaign = :campaign "
            + "ORDER BY o.quest.id, o.description")
    List<Objective> findByCampaignOrderByQuestIdDescription(@Param("campaign") Campaign campaign);
}
