import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AddSessionEventRequest, Session } from '../models/session';

/**
 * The front-end client for the back-end session system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/sessions}: starting and resuming a
 * session, ending it, listing and inspecting sessions, and attaching or removing campaign events
 * from a session. Session-start and session-end record {@code SESSION_START} and
 * {@code SESSION_END} events through the back-end event system.</p>
 *
 * <p>The {@link CampaignStore} owns the authoritative session list for the active campaign; this
 * service only performs the HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class SessionsService {
  private readonly http = inject(HttpClient);

  /**
   * Starts a new session for the campaign.
   *
   * @param campaignId the owning campaign
   * @return the new session
   */
  start(campaignId: number): Observable<Session> {
    return this.http.post<Session>(
      `/api/campaigns/${campaignId}/sessions`,
      null,
    );
  }

  /**
   * Resumes the most recent session for the campaign.
   *
   * @param campaignId the owning campaign
   * @return the resumed session
   */
  resume(campaignId: number): Observable<Session> {
    return this.http.post<Session>(
      `/api/campaigns/${campaignId}/sessions/resume`,
      null,
    );
  }

  /**
   * Ends a session.
   *
   * @param campaignId the owning campaign
   * @param sessionId the session to end
   * @return the ended session
   */
  end(campaignId: number, sessionId: number): Observable<Session> {
    return this.http.put<Session>(
      `/api/campaigns/${campaignId}/sessions/${sessionId}/end`,
      null,
    );
  }

  /**
   * @param campaignId the owning campaign
   * @return every session in the campaign
   */
  list(campaignId: number): Observable<Session[]> {
    return this.http.get<Session[]>(`/api/campaigns/${campaignId}/sessions`);
  }

  /**
   * @param campaignId the owning campaign
   * @param sessionId the session to fetch
   * @return the session with the given id
   */
  get(campaignId: number, sessionId: number): Observable<Session> {
    return this.http.get<Session>(
      `/api/campaigns/${campaignId}/sessions/${sessionId}`,
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param sessionId the session whose events to list
   * @return the campaign events attached to the session
   */
  listEvents(campaignId: number, sessionId: number): Observable<Session['events']> {
    return this.http.get<Session['events']>(
      `/api/campaigns/${campaignId}/sessions/${sessionId}/events`,
    );
  }

  /**
   * Attaches a campaign event to a session.
   *
   * @param campaignId the owning campaign
   * @param sessionId the session to attach the event to
   * @param request the event reference to add
   * @return the updated session
   */
  addEvent(
    campaignId: number,
    sessionId: number,
    request: AddSessionEventRequest,
  ): Observable<Session> {
    return this.http.post<Session>(
      `/api/campaigns/${campaignId}/sessions/${sessionId}/events`,
      {},
      {
        params: new HttpParams()
          .append('eventId', String(request.eventId))
          .append(
            'eventName',
            request.eventName ?? '',
          ),
      },
    );
  }

}
