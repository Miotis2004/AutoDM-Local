import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { Campaign } from '../../models/campaign';
import { CampaignEvent, CampaignEventType } from '../../models/campaign-event';
import { CampaignsService } from '../../services/campaigns.service';
import { CampaignEventsService } from '../../services/campaign-event.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * Sentinel value selecting every event type, alongside the real {@link CampaignEventType} values.
 * Selecting {@link ALL_EVENTS} means "every event type".
 */
export const ALL_EVENTS = 'ALL' as const;

/**
 * A filter option: the {@link ALL_EVENTS} sentinel (meaning "every event type") or a specific
 * {@link CampaignEventType}.
 */
export type HistoryFilter = typeof ALL_EVENTS | CampaignEventType;

/**
 * History: inspect prior campaign events and session history.
 *
 * <p>This is the dedicated surface for reviewing what a campaign has done. It lists the
 * {@link CampaignEvent}s the back-end has recorded for a campaign - each with its
 * {@link CampaignEvent#eventType type} and {@link CampaignEvent#timestamp timestamp} - drawn from
 * the back-end {@code /api/campaigns/{campaignId}/events} endpoints through the
 * {@link CampaignEventsService} (backed by the {@link CampaignStore}'s per-campaign event cache).</p>
 *
 * <p>Events can be filtered by {@link CampaignEventType} and viewed per campaign: a campaign
 * selector loads that campaign's events, and the list is additionally narrowed by a type filter so
 * a visitor can inspect only the moments that matter (sessions, combat, quests, and so on).</p>
 */
@Component({
  selector: 'app-history',
  standalone: true,
  templateUrl: './history.component.html',
  styleUrl: './history.component.css',
  imports: [CommonModule, RouterLink],
})
export class HistoryComponent implements OnInit, OnDestroy {
  private readonly title = inject(Title);
  private readonly campaigns = inject(CampaignsService);
  private readonly events = inject(CampaignEventsService);
  private readonly store = inject(CampaignStore);

  private readonly subscription = new Subscription();

  /** Every campaign, for the campaign selector. */
  campaignsList: Campaign[] = [];

  /** The id of the campaign whose events are currently shown, or {@code null} while none is selected. */
  campaignId: number | null = null;
  /** The title of the campaign whose events are currently shown, when known. */
  campaignTitle: string | null = null;

  /** All events for the selected campaign, most recent first. */
  allEvents: CampaignEvent[] = [];

  /** True while the initial event list is being fetched. */
  loading = true;

  /** True while a campaign switch is in flight. */
  submitting = false;

  /** Error surfaced by the most recent request, if any. */
  error: string | null = null;

  /** The event type the list is currently filtered by, or {@link ALL_EVENTS}. */
  filter: HistoryFilter = ALL_EVENTS;

  ngOnInit(): void {
    this.title.setTitle('AutoDM - History');
    this.subscription.add(
      this.campaigns.list().subscribe({
        next: (list) => (this.campaignsList = list ?? []),
        error: () => (this.campaignsList = []),
      }),
    );
    this.subscription.add(
      this.store.activeCampaign$.subscribe((campaign) => {
        this.campaignId = campaign?.id ?? null;
        this.campaignTitle = campaign?.title ?? null;
        this.error = null;
        // Keep the filter in sync with whatever the store has cached for the active campaign.
        if (this.campaignId != null) {
          this.filter = ALL_EVENTS;
          this.loadFor(this.campaignId);
        } else {
          this.allEvents = [];
          this.loading = false;
        }
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Loads the events for the given campaign, applying the current type filter afterwards.
   *
   * @param campaignId the campaign whose events to load
   */
  loadFor(campaignId: number): void {
    if (campaignId === null) {
      return;
    }
    this.loading = true;
    this.error = null;
    this.events.list(campaignId).subscribe({
      next: (events) => {
        this.allEvents = this.order(events ?? []);
        this.loading = false;
      },
      error: (err: { message?: string }) => {
        this.allEvents = [];
        this.loading = false;
        this.error = err?.message ?? 'Failed to load campaign history.';
      },
    });
  }

  /**
   * Selects a campaign and loads its events. Switching campaigns re-queries the back-end so the
   * history always reflects the chosen campaign.
   *
   * @param campaignId the campaign to inspect, or {@code null} to clear
   */
  selectCampaign(campaignId: number | null): void {
    if (campaignId === null) {
      this.allEvents = [];
      this.campaignId = null;
      this.campaignTitle = null;
      this.loading = false;
      this.error = null;
      return;
    }
    this.submitting = true;
    this.error = null;
    this.events.list(campaignId).subscribe({
      next: (events) => {
        this.campaignId = campaignId;
        this.allEvents = this.order(events ?? []);
        this.campaignTitle =
          this.campaignsList.find((c) => c.id === campaignId)?.title ?? null;
        this.submitting = false;
        this.loading = false;
      },
      error: (err: { message?: string }) => {
        this.submitting = false;
        this.loading = false;
        this.error = err?.message ?? 'Failed to load campaign history.';
      },
    });
  }

  /**
   * Sets the event type filter.
   *
   * @param type the selected option value (an event type, or {@code ALL})
   */
  setFilter(type: string): void {
    this.filter = type as HistoryFilter;
  }

  /**
   * The events remaining after applying the current type filter.
   *
   * @return the filtered events
   */
  get filteredEvents(): CampaignEvent[] {
    if (this.filter === ALL_EVENTS) {
      return this.allEvents;
    }
    return this.allEvents.filter((event) => event.eventType === this.filter);
  }

  /** Whether there are any events for the selected campaign. */
  get hasEvents(): boolean {
    return this.allEvents.length > 0;
  }

  /** Whether there are any events matching the current filter. */
  get hasFilteredEvents(): boolean {
    return this.filteredEvents.length > 0;
  }

  /**
   * Orders events most recent first, so the history reads chronologically backwards.
   *
   * @param events the events as returned by the back-end
   * @return the ordered events
   */
  private order(events: CampaignEvent[]): CampaignEvent[] {
    return [...events].sort((a, b) => {
      const at = this.parseTime(a.timestamp);
      const bt = this.parseTime(b.timestamp);
      if (at !== null && bt !== null) {
        return bt - at;
      }
      return (b.id ?? 0) - (a.id ?? 0);
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
   * Renders an event's timestamp as a short local datetime string.
   *
   * @param value an ISO timestamp, possibly empty
   * @return a formatted datetime, or {@code '-'}
   */
  formatTime(value: string | undefined): string {
    const ms = this.parseTime(value);
    if (ms === null) {
      return '-';
    }
    return new Date(ms).toLocaleString();
  }

  /**
   * Renders an event's timestamp as a compact date.
   *
   * @param value an ISO timestamp, possibly empty
   * @return a formatted date, or {@code '-'}
   */
  formatDate(value: string | undefined): string {
    const ms = this.parseTime(value);
    if (ms === null) {
      return '-';
    }
    return new Date(ms).toLocaleDateString();
  }

  /**
   * A short label for an event type, suitable for a badge.
   *
   * @param type the event type
   * @return a human-readable label
   */
  typeLabel(type: CampaignEventType): string {
    return type.replace(/_/g, ' ');
  }

  /**
   * The badge modifier class for an event type, keyed by the event's own type name.
   *
   * @param type the event type
   * @return the class name
   */
  typeBadge(type: CampaignEventType): string {
    switch (type) {
      case CampaignEventType.SESSION_START:
      case CampaignEventType.SESSION_END:
        return 'event__badge badge--session';
      case CampaignEventType.COMBAT:
      case CampaignEventType.DAMAGE:
        return 'event__badge badge--combat';
      case CampaignEventType.DISCOVERY:
      case CampaignEventType.LOCATION_ENTRY:
        return 'event__badge badge--location';
      case CampaignEventType.QUEST_CHANGE:
        return 'event__badge badge--quest';
      case CampaignEventType.ITEM_ACQUISITION:
        return 'event__badge badge--item';
      case CampaignEventType.RELATIONSHIP_CHANGE:
      case CampaignEventType.STANDING_CHANGE:
        return 'event__badge badge--faction';
      case CampaignEventType.REST:
        return 'event__badge badge--rest';
      case CampaignEventType.GAME_ACTION:
        return 'event__badge badge--action';
      default:
        return 'event__badge';
    }
  }

  /** The list of filter options shown in the type selector, beginning with "All types". */
  get filterOptions(): HistoryFilter[] {
    const types = Object.values(CampaignEventType);
    const unique = [...new Set(types)];
    return [ALL_EVENTS, ...unique];
  }

  /** The label for a filter option. */
  filterLabel(option: HistoryFilter): string {
    if (option === ALL_EVENTS) {
      return 'All types';
    }
    return this.typeLabel(option);
  }

  /** Whether a type filter other than "all" is currently active. */
  get hasFilter(): boolean {
    return this.filter !== ALL_EVENTS;
  }

  /**
   * A human-readable summary for an event's recorded detail.
   *
   * @param event the event
   * @return the description, or the type label when unset
   */
  eventDescription(event: CampaignEvent): string {
    return event.description ?? this.typeLabel(event.eventType);
  }
}
