import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { Campaign } from '../../models/campaign';
import { Session, SessionEventRef, SessionStatus } from '../../models/session';
import { SessionsService } from '../../services/sessions.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * Sessions: start, resume, and end sessions, and review session history.
 *
 * <p>This is the dedicated surface for the play-session lifecycle. A session brackets a contiguous
 * period of play for a campaign: it starts with a {@code SESSION_START} event and ends with a
 * {@code SESSION_END} event, and every session the campaign has ever run is listed here as history.
 *
 * <p>When a campaign is active the visitor can start a new session, resume the most recent session,
 * or end the currently active session. Every session is shown with its status, start and end times,
 * its duration, and the campaign events it recorded; any session can be expanded to reveal the
 * events attached to it. When no campaign is active the view invites the visitor to open one first.
 */
@Component({
  selector: 'app-sessions',
  standalone: true,
  templateUrl: './sessions.component.html',
  styleUrl: './sessions.component.css',
  imports: [CommonModule, RouterLink],
})
export class SessionsComponent implements OnInit, OnDestroy {
  private readonly title = inject(Title);
  private readonly sessions = inject(SessionsService);
  private readonly store = inject(CampaignStore);

  private readonly subscription = new Subscription();

  /** The active campaign whose sessions this page manages, or {@code null} while none is selected. */
  campaignId: number | null = null;
  /** The active campaign title, for labels, when known. */
  campaignTitle: string | null = null;

  /** Every session for the active campaign, newest first. */
  sessionsList: Session[] = [];

  /** True while the initial session list is being fetched. */
  loading = true;

  /** True while a start/resume/end request is in flight, disabling the controls. */
  submitting = false;

  /** Error surfaced by the most recent request, if any. */
  error: string | null = null;

  /** The id of the session whose event list is expanded, if any. */
  expandedId: number | null = null;
  /** The events attached to the expanded session. */
  expandedEvents: SessionEventRef[] = [];
  /** True while the expanded session's events are being fetched. */
  expandedLoading = false;

  /** The session currently marked active, when one exists. */
  get activeSession(): Session | null {
    return this.sessionsList.find((session) => this.isActive(session)) ?? null;
  }

  /**
   * @param session the session to test
   * @return whether the session is currently active
   */
  isActive(session: Session): boolean {
    return session.status === SessionStatus.ACTIVE;
  }

  /** Whether there is a session that can be resumed (any non-active session). */
  get canResume(): boolean {
    return this.sessionsList.some((session) => session.status !== SessionStatus.ACTIVE);
  }

  /** True while any campaign sessions exist. */
  get hasSessions(): boolean {
    return this.sessionsList.length > 0;
  }

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Sessions');
    this.subscription.add(
      this.store.activeCampaign$.subscribe((campaign) => {
        this.campaignId = campaign?.id ?? null;
        this.campaignTitle = campaign?.title ?? null;
        this.expandedId = null;
        this.expandedEvents = [];
        this.error = null;
        if (this.campaignId != null) {
          this.load();
        } else {
          this.sessionsList = [];
          this.loading = false;
        }
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Fetches the full session history for the active campaign and clears transient state.
   */
  load(): void {
    if (this.campaignId === null) {
      return;
    }
    this.loading = true;
    this.error = null;
    this.sessions.list(this.campaignId).subscribe({
      next: (sessions) => {
        this.sessionsList = this.order(sessions);
        this.loading = false;
      },
      error: (err: { message?: string }) => {
        this.sessionsList = [];
        this.loading = false;
        this.error = err?.message ?? 'Failed to load session history.';
      },
    });
  }

  /**
   * Starts a new session for the active campaign.
   */
  startSession(): void {
    this.mutate(() => this.sessions.start(this.campaignId!));
  }

  /**
   * Resumes the most recent session for the active campaign.
   */
  resumeSession(): void {
    if (!this.canResume) {
      return;
    }
    this.mutate(() => this.sessions.resume(this.campaignId!));
  }

  /**
   * Ends the currently active session, if any.
   */
  endSession(): void {
    const session = this.activeSession;
    if (!session) {
      return;
    }
    this.mutate(() => this.sessions.end(this.campaignId!, session.id));
  }

  /**
   * Toggles the expanded event list for a session.
   *
   * @param session the session to expand or collapse
   */
  toggleSession(session: Session): void {
    if (this.expandedId === session.id) {
      this.expandedId = null;
      this.expandedEvents = [];
      return;
    }
    this.expandedId = session.id;
    this.expandedEvents = [];
    this.expandedLoading = true;
    this.sessions.listEvents(this.campaignId!, session.id).subscribe({
      next: (events) => {
        this.expandedEvents = events ?? [];
        this.expandedLoading = false;
      },
      error: () => {
        this.expandedEvents = [];
        this.expandedLoading = false;
      },
    });
  }

  /**
   * Orders sessions newest first, with the active session kept at the top.
   *
   * @param sessions the sessions as returned by the back-end
   * @return the ordered sessions
   */
  private order(sessions: Session[]): Session[] {
    const active = sessions.filter(
      (session) => session.status === SessionStatus.ACTIVE,
    );
    const ended = sessions
      .filter((session) => session.status !== SessionStatus.ACTIVE)
      .sort((a, b) => {
        const at = this.parseTime(a.startTime);
        const bt = this.parseTime(b.startTime);
        return (bt ?? 0) - (at ?? 0);
      });
    return [...active, ...ended];
  }

  /**
   * Runs a mutating session action, refreshing the history once it settles.
   *
   * @param action the session operation to perform
   */
  private mutate(action: () => import('rxjs').Observable<Session>): void {
    if (this.campaignId === null || this.submitting) {
      return;
    }
    this.submitting = true;
    this.error = null;
    action().subscribe({
      next: () => {
        this.submitting = false;
        this.expandedId = null;
        this.expandedEvents = [];
        this.load();
      },
      error: (err: { message?: string }) => {
        this.submitting = false;
        this.error = err?.message ?? 'The session action could not be completed.';
      },
    });
  }

  /**
   * Parses an ISO time string into an epoch-millisecond number, tolerating empties.
   *
   * @param value an ISO timestamp, possibly empty
   * @return the parsed number, or {@code null} when it cannot be parsed
   */
  private parseTime(value: string | undefined): number | null {
    if (!value) {
      return null;
    }
    const ms = Date.parse(value);
    return Number.isNaN(ms) ? null : ms;
  }

  /**
   * Formats a status for display as a badge.
   *
   * @param status the session status
   * @return the label
   */
  statusLabel(status: SessionStatus): string {
    switch (status) {
      case SessionStatus.ACTIVE:
        return 'Active';
      case SessionStatus.ENDED:
        return 'Ended';
      default:
        return status;
    }
  }

  /**
   * Returns the badge modifier class for a status.
   *
   * @param status the session status
   * @return the class name
   */
  statusBadge(status: SessionStatus): string {
    return status === SessionStatus.ACTIVE ? 'badge--active' : 'badge--ended';
  }

  /**
   * Renders a start/end timestamp, falling back to a placeholder.
   *
   * @param value an ISO timestamp, possibly empty
   * @return a short local datetime string, or {@code '—'}
   */
  formatTime(value: string | undefined): string {
    const ms = this.parseTime(value);
    if (ms === null) {
      return '—';
    }
    return new Date(ms).toLocaleString();
  }

  /**
   * Computes the duration between a session's start and end times.
   *
   * @param session the session to measure
   * @return a human-readable duration, or {@code '—'} when it cannot be computed
   */
  duration(session: Session): string {
    const start = this.parseTime(session.startTime);
    const end = this.parseTime(session.endTime);
    if (start === null || end === null) {
      return '—';
    }
    return this.formatDuration(end - start);
  }

  /**
   * Formats a millisecond span as an {@code h mm} or {@code mm} string.
   *
   * @param millis the span in milliseconds
   * @return the formatted duration
   */
  private formatDuration(millis: number): string {
    const totalSeconds = Math.floor(millis / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    if (hours > 0) {
      return `${hours} h ${minutes} m`;
    }
    return `${minutes} m`;
  }

  /**
   * The number of campaign events recorded within a session.
   *
   * @param session the session
   * @return the event count
   */
  eventCount(session: Session): number {
    return session.events?.length ?? 0;
  }

  /**
   * The label describing a session's recorded events.
   *
   * @param session the session
   * @return the label
   */
  eventLabel(session: Session): string {
    const count = this.eventCount(session);
    return count === 0
      ? 'No events recorded'
      : `${count} event${count === 1 ? '' : 's'} recorded`;
  }

  /**
   * The label for an event attached to a session.
   *
   * @param event an event reference attached to a session
   * @return the event name, or a fallback
   */
  eventEventLabel(event: SessionEventRef): string {
    return event.eventName ?? `Event ${event.eventId}`;
  }
}
