package com.example;

import com.example.domain.Session;
import com.example.domain.SessionEventRef;
import com.example.service.SessionService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * REST surface for persistent play sessions owned by a campaign.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link SessionService} call. All session start/resume/end logic, event references,
 * and history handling lives in the service, and persistence is what lets session
 * history reload across application restarts within a campaign.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    // ------------------------------------------------------------------
    // Start, resume, and end
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/sessions")
    public Session startSession(@PathVariable Long campaignId) {
        return sessions.startSession(campaignId);
    }

    @PostMapping("/{campaignId}/sessions/resume")
    public Session resumeSession(@PathVariable Long campaignId) {
        return sessions.resumeSession(campaignId);
    }

    @PutMapping("/{campaignId}/sessions/{sessionId}/end")
    public Session endSession(@PathVariable Long campaignId, @PathVariable Long sessionId) {
        return sessions.endSession(campaignId, sessionId);
    }

    // ------------------------------------------------------------------
    // History
    // ------------------------------------------------------------------

    @GetMapping("/{campaignId}/sessions")
    public List<Session> listSessions(@PathVariable Long campaignId) {
        return sessions.listSessions(campaignId);
    }

    @GetMapping("/{campaignId}/sessions/{sessionId}")
    public Session getSession(@PathVariable Long campaignId, @PathVariable Long sessionId) {
        return sessions.getSession(campaignId, sessionId);
    }

    // ------------------------------------------------------------------
    // Event references
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/sessions/{sessionId}/events")
    public Session addSessionEvent(@PathVariable Long campaignId, @PathVariable Long sessionId,
                                   @RequestParam Long eventId,
                                   @RequestParam(required = false) String eventName) {
        return sessions.addSessionEvent(campaignId, sessionId, eventId, eventName);
    }

    @DeleteMapping("/{campaignId}/sessions/{sessionId}/events")
    public Session removeSessionEvent(@PathVariable Long campaignId, @PathVariable Long sessionId,
                                      @RequestParam Long eventId,
                                      @RequestParam(required = false) String eventName) {
        return sessions.removeSessionEvent(campaignId, sessionId, eventId, eventName);
    }

    @GetMapping("/{campaignId}/sessions/{sessionId}/events")
    public List<SessionEventRef> listSessionEvents(@PathVariable Long campaignId,
                                                   @PathVariable Long sessionId) {
        return new ArrayList<>(getSession(campaignId, sessionId).getEvents());
    }
}
