package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Scene;
import com.example.domain.SceneStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link Scene} entities.
 *
 * <p>Scenes are campaign-scoped, so this repository is organised around the campaign
 * that owns them and the active-scene flag. It exposes the data access operations
 * needed to persist and retrieve scenes; it contains no business logic and only
 * depends on Spring Data JPA.</p>
 */
@Repository
public interface SceneRepository extends JpaRepository<Scene, Long> {

    List<Scene> findByCampaignOrderByCreatedAtAsc(Campaign campaign);

    /**
     * Finds the single active scene of a campaign, identified by its
     * {@link SceneStatus#ACTIVE} status. Expressed as an explicit JPQL query because the
     * active flag is an enum-valued {@link #status} property rather than a boolean.
     */
    @Query(
            "select s from Scene s "
                    + "where s.campaign = :campaign "
                    + "and s.status = com.example.domain.SceneStatus.ACTIVE")
    Optional<Scene> findByCampaignAndActiveTrue(@Param("campaign") Campaign campaign);
}
