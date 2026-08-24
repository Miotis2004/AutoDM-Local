package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.LimitedUseAbility;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for {@link LimitedUseAbility} entities.
 *
 * <p>Limited-use abilities are campaign-scoped, so this repository is organised
 * around the campaign that owns them. It exposes the data access operations needed
 * to persist and retrieve these abilities; it contains no business logic and only
 * depends on Spring Data JPA.</p>
 */
@Repository
public interface LimitedUseAbilityRepository extends JpaRepository<LimitedUseAbility, Long> {

    List<LimitedUseAbility> findByCampaign(Campaign campaign);
}
