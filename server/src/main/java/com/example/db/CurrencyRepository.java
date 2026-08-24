package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.CurrencyRecord;
import com.example.domain.CurrencyUnit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link CurrencyRecord} entities.
 *
 * <p>Currency is a campaign-scoped resource, so this repository is organised around
 * the campaign that owns it. It exposes the data access operations needed to persist
 * and retrieve currency stacks; it contains no business logic and only depends on
 * Spring Data JPA.</p>
 */
@Repository
public interface CurrencyRepository extends JpaRepository<CurrencyRecord, Long> {

    List<CurrencyRecord> findByCampaign(Campaign campaign);

    Optional<CurrencyRecord> findByCampaignAndCurrencyUnit(Campaign campaign, CurrencyUnit currencyUnit);
}
