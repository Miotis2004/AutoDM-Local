package com.example.service;

import com.example.domain.Campaign;
import com.example.domain.Session;
import com.example.domain.SessionStatus;
import com.example.db.CampaignRepository;
import com.example.db.SessionRepository;

import com.example.service.CampaignEventService;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic for persistent play sessions owned by a campaign.
 *
 * <p>This service is the single place where sessions are started, resumed, ended, and
 * consulted. Every mutation resolves its owning campaign, applies the change to a
 * managed entity, and relies on the repository to persist it, so session history
 * reloads across application restarts within a campaign.</p>
 */
@Service
public class SessionService {

    private final CampaignRepository campaigns;
    private final SessionRepository sessions;
    private final CampaignEventService events;

    public SessionService(
            CampaignRepository campaigns,
            SessionRepository sessions,
            CampaignEventService events) {
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.events = events;
    }

    // ------------------------------------------------------------------
    // Campaign / session lookup
    // ------------------------------------------------------------------

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private Session requireOwnedSession(Long campaignId, Long sessionId) {
        return sessions.findById(sessionId)
                .filter(s -> s.getCampaign() != null && s.getCampaign().getId() != null
                        && s.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No session with id " + sessionId));
    }

    // ------------------------------------------------------------------
    // Start, resume, and end
    // ------------------------------------------------------------------

    /**
     * Starts a new active session for the given campaign at the current time.
     *
     * <p>If an active session is already open for the campaign, that session is
     * returned unchanged rather than starting a duplicate, so a campaign always has at
     * most one active session.
     *
     * @param campaignId the campaign to start a session for
     * @return the active session (never {@code null})
     */
    public Session startSession(Long campaignId) {
        return withActiveSession(campaignId, LocalDateTime.now());
    }

    /**
     * Resumes the given campaign's game. If an active session is already open it is
     * returned; otherwise a new active session is started. This lets a game pick up
     * where it left off regardless of whether the previous session was merely open or
     * has already been ended.
     *
     * @param campaignId the campaign to resume
     * @return the active session (never {@code null})
     */
    public Session resumeSession(Long campaignId) {
        return withActiveSession(campaignId, LocalDateTime.now());
    }

    private Session withActiveSession(Long campaignId, LocalDateTime now) {
        Campaign campaign = requireCampaign(campaignId);
        return sessions.findFirstByCampaignAndStatusOrderByIdDesc(campaign, SessionStatus.ACTIVE)
                .orElseGet(() -> {
                    Session session = sessions.save(new Session(campaign, now));
                    events.recordSessionStart(campaignId);
                    return session;
                });
    }

    /**
     * Ends the given session, stamping the end time with the current time and moving
     * it to {@link SessionStatus#ENDED}. The session must belong to the given campaign.
     *
     * @param campaignId the owning campaign
     * @param sessionId  the session to end
     * @return the ended session (never {@code null})
     */
    public Session endSession(Long campaignId, Long sessionId) {
        Session session = requireOwnedSession(campaignId, sessionId);
        session.endAt(LocalDateTime.now());
        events.recordSessionEnd(campaignId);
        return sessions.save(session);
    }

    // ------------------------------------------------------------------
    // Event references
    // ------------------------------------------------------------------

    /**
     * References an event on the given session. The session must belong to the given
     * campaign. Idempotent for an event already referenced.
     *
     * @return the updated session (never {@code null})
     */
    public Session addSessionEvent(Long campaignId, Long sessionId, Long eventId, String eventName) {
        Session session = requireOwnedSession(campaignId, sessionId);
        session.addEvent(eventId, eventName);
        return sessions.save(session);
    }

    /**
     * Removes an event reference from the given session. The session must belong to the
     * given campaign.
     *
     * @return the updated session (never {@code null})
     */
    public Session removeSessionEvent(Long campaignId, Long sessionId, Long eventId, String eventName) {
        Session session = requireOwnedSession(campaignId, sessionId);
        session.removeEvent(eventId, eventName);
        return sessions.save(session);
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    public Session getSession(Long campaignId, Long sessionId) {
        return requireOwnedSession(campaignId, sessionId);
    }

    /**
     * Returns the session history for the given campaign, most recent session first.
     */
    public List<Session> listSessions(Long campaignId) {
        return sessions.findByCampaignIdOrderByStartTimeDesc(requireCampaign(campaignId));
    }
}
