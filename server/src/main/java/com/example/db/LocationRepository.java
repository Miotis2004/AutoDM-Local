package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for {@link Location} entities. No business logic; only persistence.
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByCampaignOrderByName(Campaign campaign);

    List<Location> findByCampaignIdAndDiscovered(Long campaignId, boolean discovered);

    List<Location> findByCampaignIdAndRegionIdIsNull(Long campaignId);
}
