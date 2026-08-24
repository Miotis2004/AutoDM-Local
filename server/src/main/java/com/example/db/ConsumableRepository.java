package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.ConsumableRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link ConsumableRecord} entities.
 *
 * <p>Consumables are campaign-scoped, so this repository is organised around the
 * campaign that owns them. It exposes the data access operations needed to persist
 * and retrieve consumable stacks; it contains no business logic and only depends on
 * Spring Data JPA.</p>
 */
@Repository
public interface ConsumableRepository extends JpaRepository<ConsumableRecord, Long> {

    List<ConsumableRecord> findByCampaign(Campaign campaign);

    Optional<ConsumableRecord> findByCampaignAndName(Campaign campaign, String name);
}
