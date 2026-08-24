package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for {@link Settlement} entities. No business logic; only persistence.
 */
@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByCampaign(Campaign campaign);
}
