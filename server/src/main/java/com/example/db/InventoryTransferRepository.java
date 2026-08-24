package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.InventoryTransfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for {@link InventoryTransfer} records.
 *
 * <p>Transfers are immutable audit rows, so this repository only needs to persist and
 * read them back. It is organised around reading the transfer history of a campaign,
 * and it contains no business logic.</p>
 */
@Repository
public interface InventoryTransferRepository extends JpaRepository<InventoryTransfer, Long> {

    /**
     * Every transfer recorded in the campaign, newest first.
     */
    List<InventoryTransfer> findByCampaignOrderByIdDesc(Campaign campaign);
}
