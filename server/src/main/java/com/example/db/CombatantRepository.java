package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Combatant;
import com.example.domain.CombatantKind;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for {@link Combatant} entities.
 *
 * <p>Combatants are campaign-scoped, so this repository is organised around the
 * campaign that owns them. It exposes the data access operations needed to persist
 * and retrieve combatants; it contains no business logic and only depends on Spring
 * Data JPA.</p>
 *
 * <p>Two ordering lookups use native queries because the turn-order column is named
 * {@code order}, a SQL reserved word. A derived {@code OrderByOrderAsc} query renders
 * {@code ORDER BY order} unquoted, which is a syntax error against SQLite. The native
 * queries quote the identifier ({@code "order"}) so the ordering SQL parses.</p>
 */
@Repository
public interface CombatantRepository extends JpaRepository<Combatant, Long> {

    @Query(value = "SELECT * FROM combatants WHERE campaign_id = :campaignId ORDER BY \"order\" ASC",
            nativeQuery = true)
    List<Combatant> findByCampaignOrderByOrderAsc(@Param("campaignId") Long campaignId);

    List<Combatant> findByCampaignAndKind(Campaign campaign, CombatantKind kind);

    @Query(value = "SELECT * FROM combatants WHERE campaign_id = :campaignId"
            + " AND encounter_id = :encounterId ORDER BY \"order\" ASC", nativeQuery = true)
    List<Combatant> findByCampaignIdAndEncounterIdOrderByOrderAsc(
            @Param("campaignId") Long campaignId, @Param("encounterId") Long encounterId);

    List<Combatant> findByCampaignIdAndSceneId(Long campaignId, Long sceneId);
}
