package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.InventoryItem;
import com.example.domain.InventoryOwnerKind;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link InventoryItem} holdings.
 *
 * <p>A holding is owned by one owner, identified by its owner kind plus owner id, so
 * this repository is organised around finding the holding a given owner holds of a
 * given item. It exposes only the data-access operations the service needs and
 * contains no business logic.</p>
 */
@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    /**
     * All holdings in the campaign, ordered by owner id then name.
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.campaign = :campaign "
            + "ORDER BY i.ownerId, i.name")
    List<InventoryItem> findByCampaignOrderByOwnerIdAndName(@Param("campaign") Campaign campaign);

    /**
     * All holdings of a given owner (a campaign or a player character) in the
     * campaign, ordered by name.
     */
    List<InventoryItem> findByCampaignAndOwnerKindOrderByName(
            Campaign campaign, InventoryOwnerKind ownerKind);

    /**
     * The single holding in the campaign that the given owner holds of the named
     * item, if one exists.
     */
    Optional<InventoryItem> findByCampaignAndOwnerIdAndName(
            Campaign campaign, Long ownerId, String name);

    Optional<InventoryItem> findByCampaignAndOwnerKindAndOwnerIdAndName(
            Campaign campaign, InventoryOwnerKind ownerKind, Long ownerId, String name);
}
