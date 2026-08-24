import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Encounter } from '../models/encounter';

/**
 * The front-end client for the back-end encounter system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/encounters}: creating, starting, and
 * finishing encounters, listing them, and inspecting a single encounter. It exists so the
 * encounter surface can drive a fight from creation through the final turn. The {@link
 * CampaignStore} owns the authoritative campaign data; this service only performs the HTTP round
 * trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class EncounterService {
  private readonly http = inject(HttpClient);

  /**
   * Creates an encounter anchored to a scene and a location.
   *
   * @param campaignId the owning campaign
   * @param sceneId the scene the encounter takes place within
   * @param locationId the location the encounter takes place in
   * @return the newly created encounter
   */
  create(
    campaignId: number,
    sceneId: number,
    locationId: number,
  ): Observable<Encounter> {
    let params = new HttpParams()
      .append('sceneId', String(sceneId))
      .append('locationId', String(locationId));
    return this.http.post<Encounter>(
      `/api/campaigns/${campaignId}/encounters`,
      {},
      { params },
    );
  }

  /**
   * Starts a scheduled encounter, marking it active.
   *
   * @param campaignId the owning campaign
   * @param encounterId the encounter to start
   * @return the started encounter
   */
  begin(campaignId: number, encounterId: number): Observable<Encounter> {
    return this.http.post<Encounter>(
      `/api/campaigns/${campaignId}/encounters/${encounterId}/begin`,
      {},
    );
  }

  /**
   * Finishes an encounter, marking it complete.
   *
   * @param campaignId the owning campaign
   * @param encounterId the encounter to finish
   * @return the finished encounter
   */
  finish(campaignId: number, encounterId: number): Observable<Encounter> {
    return this.http.post<Encounter>(
      `/api/campaigns/${campaignId}/encounters/${encounterId}/finish`,
      {},
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param encounterId the encounter to fetch
   * @return the requested encounter, when it exists
   */
  get(campaignId: number, encounterId: number): Observable<Encounter> {
    return this.http.get<Encounter>(
      `/api/campaigns/${campaignId}/encounters/${encounterId}`,
    );
  }

  /**
   * @param campaignId the owning campaign
   * @return every encounter in the campaign, oldest first
   */
  list(campaignId: number): Observable<Encounter[]> {
    return this.http.get<Encounter[]>(`/api/campaigns/${campaignId}/encounters`);
  }
}
