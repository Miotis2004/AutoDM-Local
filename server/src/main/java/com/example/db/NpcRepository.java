package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Disposition;
import com.example.domain.Npc;
import com.example.domain.NpcRelationship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link Npc} entities.
 *
 * <p>Every NPC is owned by exactly one {@link Campaign}, so this repository is
 * organised around the campaign that owns a set of NPCs. It exposes the data access
 * operations needed to persist and retrieve non-player characters; it contains no
 * business logic and only depends on Spring Data JPA.</p>
 */
@Repository
public interface NpcRepository extends JpaRepository<Npc, Long> {

    /**
     * Finds every NPC owned by the given campaign, ordered by name.
     *
     * @param campaign the owning campaign
     * @return all NPCs owned by the campaign, ordered by name (never {@code null})
     */
    List<Npc> findByCampaignOrderByName(Campaign campaign);

    /**
     * Finds every active NPC owned by the given campaign, ordered by name.
     *
     * @param campaign the owning campaign
     * @return all active NPCs owned by the campaign, ordered by name (never {@code null})
     */
    List<Npc> findByCampaignAndActiveTrueOrderByName(Campaign campaign);

    /**
     * Finds every NPC owned by the given campaign with the given disposition, ordered by
     * name.
     *
     * @param campaign    the owning campaign
     * @param disposition the disposition to match
     * @return the matching NPCs, ordered by name
     */
    List<Npc> findByCampaignAndDispositionOrderByName(Campaign campaign, Disposition disposition);

    /**
     * Finds a single active NPC owned by the campaign with the given name, if any.
     *
     * @param campaign the owning campaign
     * @param name     the NPC name to look up
     * @return the matching NPC, if present
     */
    Optional<Npc> findByCampaignAndNameAndActiveTrue(Campaign campaign, String name);

    /**
     * Finds every NPC owned by the given campaign with the given relationship to the
     * party, ordered by name.
     *
     * @param campaign     the owning campaign
     * @param relationship the relationship to match
     * @return the matching NPCs, ordered by name
     */
    List<Npc> findByCampaignAndRelationshipOrderByName(Campaign campaign, NpcRelationship relationship);
}
