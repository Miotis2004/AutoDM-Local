package com.example.db;

import com.example.domain.Campaign;
import com.example.domain.Session;
import com.example.domain.SessionStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link Session} entities.
 *
 * <p>Every session is owned by exactly one {@link Campaign}, so this repository is
 * organised around the campaign that owns a session and the session itself. It exposes
 * the data access operations needed to persist and retrieve sessions; it contains no
 * business logic and only depends on Spring Data JPA.</p>
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Finds the currently open session for the given campaign, if any. A campaign has
     * at most one active session at a time.
     *
     * @param campaign the owning campaign
     * @return the active session, if one is open (never {@code null})
     */
    Optional<Session> findFirstByCampaignAndStatusOrderByIdDesc(Campaign campaign, SessionStatus status);

    /**
     * Finds every session owned by the given campaign, most recent first.
     *
     * @param campaign the owning campaign
     * @return all sessions owned by the campaign, ordered by descending id (never
     *     {@code null})
     */
    List<Session> findByCampaignIdOrderByStartTimeDesc(Campaign campaign);
}
