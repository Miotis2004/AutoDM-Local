package com.example.db;

import com.example.domain.AmmunitionRecord;
import com.example.domain.Campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link AmmunitionRecord} entities.
 *
 * <p>Ammunition is a campaign-scoped resource, so this repository is organised
 * around the campaign that owns it. It exposes the data access operations needed to
 * persist and retrieve ammunition stacks; it contains no business logic and only
 * depends on Spring Data JPA.</p>
 */
@Repository
public interface AmmunitionRepository extends JpaRepository<AmmunitionRecord, Long> {

    List<AmmunitionRecord> findByCampaign(Campaign campaign);

    Optional<AmmunitionRecord> findByCampaignAndAmmoType(Campaign campaign, String ammoType);
}
