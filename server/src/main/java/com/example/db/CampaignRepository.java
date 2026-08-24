package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.CampaignStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for {@link Campaign} entities.
 *
 * <p>This repository is the single point for persisting and retrieving campaigns.
 * It contains no business logic; it only exposes data access operations to the
 * service layer via Spring Data JPA.</p>
 */
@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatus(CampaignStatus status);

    List<Campaign> findByTitleContainingIgnoreCase(String title);
}
