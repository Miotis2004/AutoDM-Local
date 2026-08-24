package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Region;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for {@link Region} entities. No business logic; only persistence.
 */
@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findByCampaignOrderByName(Campaign campaign);
}
