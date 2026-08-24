package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.TravelRoute;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link TravelRoute} entities. No business logic; only persistence.
 */
@Repository
public interface TravelRouteRepository extends JpaRepository<TravelRoute, Long> {

    List<TravelRoute> findByCampaign(Campaign campaign);

    List<TravelRoute> findByFromId(Campaign campaign);

    Optional<TravelRoute> findByCampaignIdAndFromIdAndToId(Long campaignId, Long fromId, Long toId);
}
