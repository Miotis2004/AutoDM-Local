package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Disposition;
import com.example.domain.Faction;
import com.example.domain.NpcRelationship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link Faction} entities.
 *
 * <p>Every faction is owned by exactly one {@link Campaign}, so this repository is
 * organised around the campaign that owns a set of factions. It exposes the data
 * access operations needed to persist and retrieve factions; it contains no business
 * logic and only depends on Spring Data JPA.</p>
 */
@Repository
public interface FactionRepository extends JpaRepository<Faction, Long> {

    /**
     * Finds every faction owned by the given campaign, ordered by name.
     *
     * @param campaign the owning campaign
     * @return all factions owned by the campaign, ordered by name (never {@code null})
     */
    List<Faction> findByCampaignOrderByName(Campaign campaign);

    /**
     * Finds every faction owned by the given campaign with the given disposition,
     * ordered by name.
     *
     * @param campaign    the owning campaign
     * @param disposition the disposition to match
     * @return the matching factions, ordered by name
     */
    List<Faction> findByCampaignAndDispositionOrderByName(Campaign campaign, Disposition disposition);

    /**
     * Finds every faction owned by the given campaign with the given reputation,
     * ordered by name.
     *
     * @param campaign   the owning campaign
     * @param reputation the reputation to match
     * @return the matching factions, ordered by name
     */
    List<Faction> findByCampaignAndReputationOrderByName(Campaign campaign, NpcRelationship reputation);

    /**
     * Finds a single faction owned by the campaign with the given name, if any.
     *
     * @param campaign the owning campaign
     * @param name     the faction name to look up
     * @return the matching faction, if present
     */
    Optional<Faction> findByCampaignAndName(Campaign campaign, String name);
}
