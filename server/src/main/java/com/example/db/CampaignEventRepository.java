package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.CampaignEvent;
import com.example.domain.CampaignEventType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for {@link CampaignEvent} entities.
 *
 * <p>Every campaign event is owned by exactly one {@link Campaign}, so this repository
 * is organised around the campaign that owns an event. It exposes the data access
 * operations needed to persist and retrieve events; it contains no business logic and
 * only depends on Spring Data JPA.</p>
 */
@Repository
public interface CampaignEventRepository extends JpaRepository<CampaignEvent, Long> {

    /**
     * Finds every event owned by the given campaign, most recent first.
     *
     * @param campaign the owning campaign
     * @return all events owned by the campaign, ordered by descending id (never
     *     {@code null})
     */
    List<CampaignEvent> findByCampaignOrderByIdDesc(Campaign campaign);

    /**
     * Finds every event of the given type owned by the given campaign, most recent
     * first.
     *
     * @param campaign the owning campaign
     * @param type     the event type to filter by
     * @return the events of the given type owned by the campaign, ordered by descending
     *     id (never {@code null})
     */
    List<CampaignEvent> findByCampaignAndEventTypeOrderByIdDesc(Campaign campaign, CampaignEventType type);
}
