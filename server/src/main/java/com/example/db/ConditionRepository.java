package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.ConditionKind;
import com.example.domain.ConditionRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for {@link ConditionRecord} entities.
 *
 * <p>Conditions are campaign-scoped, so this repository is organised around the
 * campaign that owns them. It exposes the data access operations needed to persist
 * and retrieve conditions; it contains no business logic and only depends on Spring
 * Data JPA.</p>
 */
@Repository
public interface ConditionRepository extends JpaRepository<ConditionRecord, Long> {

    List<ConditionRecord> findByCampaign(Campaign campaign);

    List<ConditionRecord> findByCampaignAndConditionKind(Campaign campaign, ConditionKind conditionKind);
}
