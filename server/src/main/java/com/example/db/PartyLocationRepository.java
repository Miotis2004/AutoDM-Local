package com.example.db;

import com.example.domain.PartyLocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for {@link PartyLocation} entities. No business logic; only persistence.
 */
@Repository
public interface PartyLocationRepository extends JpaRepository<PartyLocation, Long> {

    Optional<PartyLocation> findByCampaignId(Long campaignId);
}
