package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.PlayerCharacter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access abstraction for {@link PlayerCharacter} entities.
 *
 * <p>Every player character is owned by exactly one {@link Campaign}, so this
 * repository is organised around the campaign that owns a set of characters. It
 * exposes the data access operations needed to persist and retrieve player
 * characters; it contains no business logic and only depends on Spring Data JPA.</p>
 */
@Repository
public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, Long> {

    /**
     * Finds every character owned by the given campaign, in no particular order.
     *
     * @param campaign the owning campaign
     * @return all player characters owned by the campaign (never {@code null})
     */
    List<PlayerCharacter> findByCampaign(Campaign campaign);

    /**
     * Counts how many characters are owned by the given campaign. This is the
     * operation that makes the one-to-many (many characters per campaign)
     * relationship easy to reason about from the service layer.
     *
     * @param campaign the owning campaign
     * @return the number of player characters owned by the campaign
     */
    long countByCampaign(Campaign campaign);
}
