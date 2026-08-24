import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CampaignEvent, CampaignEventType } from '../models/campaign-event';

/**
 * The front-end client for the back-end campaign event system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/events}: recording events, listing them
 * (all, or filtered by type), and inspecting a single event. Used by the history screen to render
 * the campaign's chronology.</p>
 *
 * <p>The {@link CampaignStore} owns the authoritative event list for the active campaign; this
 * service only performs the HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class CampaignEventsService {
  private readonly http = inject(HttpClient);

  /**
   * Records a new campaign event.
   *
   * @param campaignId the owning campaign
   * @param type the event type
   * @param description optional human-readable description
   * @return the recorded event
   */
  record(
    campaignId: number,
    type: CampaignEventType,
    description?: string,
  ): Observable<CampaignEvent> {
    let params = new HttpParams()
      .append('type', type)
      .append('description', description ?? '');
    return this.http.post<CampaignEvent>(
      `/api/campaigns/${campaignId}/events`,
      {},
      { params },
    );
  }

  /**
   * @param campaignId the owning campaign
   * @return every recorded event for the campaign, most recent first
   */
  list(campaignId: number): Observable<CampaignEvent[]> {
    return this.http.get<CampaignEvent[]>(
      `/api/campaigns/${campaignId}/events`,
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param type the event type to filter by
   * @return the events of the given type
   */
  listByType(
    campaignId: number,
    type: CampaignEventType,
  ): Observable<CampaignEvent[]> {
    return this.http.get<CampaignEvent[]>(
      `/api/campaigns/${campaignId}/events/by-type`,
      { params: new HttpParams().append('type', type) },
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param eventId the event to fetch
   * @return the event with the given id
   */
  get(
    campaignId: number,
    eventId: number,
  ): Observable<CampaignEvent> {
    return this.http.get<CampaignEvent>(
      `/api/campaigns/${campaignId}/events/${eventId}`,
    );
  }
}
