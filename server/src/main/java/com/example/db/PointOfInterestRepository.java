package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.PointOfInterest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for {@link PointOfInterest} entities. No business logic; only persistence.
 */
@Repository
public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, Long> {

    List<PointOfInterest> findByCampaign(Campaign campaign);
}
