import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { DashboardState } from '../models/dashboard';

/**
 * The front-end client for the dashboard.
 *
 * <p>This service wraps {@code GET /api/campaigns/{campaignId}/dashboard}, which returns a single
 * {@link DashboardState} aggregating the active campaign, the party's current location, the active
 * characters, the current quests, any encounter in progress, a one-line summary, and the most
 * recent campaign events. Everything the dashboard needs lives in that one payload, so the component
 * performs a single HTTP round trip per campaign instead of scattering requests across the other
 * services.</p>
 *
 * <p>The service only performs the HTTP round trip; the dashboard component owns how the snapshot
 * is displayed.</p>
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  /**
   * Fetches the dashboard snapshot for a campaign.
   *
   * @param campaignId the owning campaign
   * @return the aggregated dashboard state
   */
  getDashboard(campaignId: number): Observable<DashboardState> {
    return this.http.get<DashboardState>(`/api/campaigns/${campaignId}/dashboard`);
  }
}
