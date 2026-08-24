package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.CombatCondition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link CombatCondition} entities.
 *
 * <p>Combat conditions are campaign-scoped, so this repository is organised around
 * the campaign that owns them and the combatant they are applied to. It exposes the
 * data access operations needed to persist and retrieve conditions; it contains no
 * business logic and only depends on Spring Data JPA.</p>
 */
@Repository
public interface CombatConditionRepository extends JpaRepository<CombatCondition, Long> {

    List<CombatCondition> findByCampaignOrderByCreatedAtAsc(Campaign campaign);

    List<CombatCondition> findByCombatantIdOrderByCreatedAtAsc(Long combatantId);

    List<CombatCondition> findByCampaignIdAndCombatantIdAndName(
            Long campaignId, Long combatantId, String name);
}
