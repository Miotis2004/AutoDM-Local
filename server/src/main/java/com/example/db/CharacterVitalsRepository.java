package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.CharacterVitals;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link CharacterVitals} entities.
 *
 * <p>Vitals are campaign-scoped, so this repository is organised around the campaign
 * that owns them. It exposes the data access operations needed to persist and
 * retrieve character vitals; it contains no business logic and only depends on
 * Spring Data JPA.</p>
 */
@Repository
public interface CharacterVitalsRepository extends JpaRepository<CharacterVitals, Long> {

    List<CharacterVitals> findByCampaign(Campaign campaign);

    Optional<CharacterVitals> findByCampaignAndId(Campaign campaign, Long id);
}
