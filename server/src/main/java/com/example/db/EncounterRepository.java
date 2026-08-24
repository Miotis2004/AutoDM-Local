package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Encounter;
import com.example.domain.EncounterStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for {@link Encounter} entities.
 *
 * <p>Encounters are campaign-scoped, so this repository is organised around the
 * campaign that owns them. It exposes the data access operations needed to persist
 * and retrieve encounters; it contains no business logic and only depends on Spring
 * Data JPA.</p>
 */
@Repository
public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    List<Encounter> findByCampaignOrderByCreatedAtAsc(Campaign campaign);

    List<Encounter> findByCampaignAndStatus(Campaign campaign, EncounterStatus status);
}
