/**
 * Front-end view models for game sessions.
 *
 * <p>Mirrors the back-end {@code Session} entity exposed by
 * {@code /api/campaigns/{campaignId}/sessions}. The {@link SessionsService} wraps those endpoints.
 * Sessions bracket play: a session starts with a {@code SESSION_START} event and ends with a
 * {@code SESSION_END} event.</p>
 */

/**
 * A game session - a contiguous period of play for a campaign.
 *
 * <p>The {@link SessionsService} returns these from the back-end; the {@link CampaignStore} keeps
 * the campaign's sessions so components read session state from one authoritative place.</p>
 */
export interface Session {
  id: number;
  startTime?: string;
  endTime?: string;
  status: SessionStatus;
  events: SessionEventRef[];
}

/** Lifecycle state of a session, matching the back-end {@code SessionStatus} enum. */
export enum SessionStatus {
  ACTIVE = 'ACTIVE',
  ENDED = 'ENDED',
}

/** A reference to a campaign event attached to a session. */
export interface SessionEventRef {
  eventId: number;
  eventName: string;
}

/** Request body when attaching an event to a session. */
export interface AddSessionEventRequest {
  eventId: number;
  eventName?: string;
}
