import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CreateFactionRequest,
  Disposition,
  Faction,
  NpcRelationship,
  UpdateFactionRequest,
} from '../models/faction';

/**
 * The front-end client for the back-end faction system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/factions}: listing factions, inspecting
 * a single faction, creating factions, updating their story fields, and removing factions. Faction
 * mutations use request parameters, so this service builds typed {@link HttpParams} for them.</p>
 *
 * <p>The {@link CampaignStore} owns the authoritative faction list for the active campaign; this
 * service only performs the HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class FactionsService {
  private readonly http = inject(HttpClient);

  /**
   * @param campaignId the owning campaign
   * @return every faction in the campaign
   */
  list(campaignId: number): Observable<Faction[]> {
    return this.http.get<Faction[]>(`/api/campaigns/${campaignId}/factions`);
  }

  /**
   * @param campaignId the owning campaign
   * @param factionId the faction to fetch
   * @return the faction with the given id
   */
  get(campaignId: number, factionId: number): Observable<Faction> {
    return this.http.get<Faction>(
      `/api/campaigns/${campaignId}/factions/${factionId}`,
    );
  }

  /**
   * Creates a new faction.
   *
   * @param campaignId the owning campaign
   * @param request the faction creation request
   * @return the created faction
   */
  create(campaignId: number, request: CreateFactionRequest): Observable<Faction> {
    let params = new HttpParams()
      .append('name', request.name)
      .append('disposition', request.disposition ?? Disposition.NEUTRAL)
      .append('reputation', request.reputation ?? NpcRelationship.NEUTRAL);
    if (request.description) {
      params = params.append('description', request.description);
    }
    return this.http.post<Faction>(
      `/api/campaigns/${campaignId}/factions`,
      {},
      { params },
    );
  }

  /**
   * Updates a faction's story fields.
   *
   * @param campaignId the owning campaign
   * @param factionId the faction to update
   * @param fields the fields to change (any omitted field is left untouched)
   * @return the updated faction
   */
  update(
    campaignId: number,
    factionId: number,
    fields: UpdateFactionRequest,
  ): Observable<Faction> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(fields)) {
      if (value === undefined || value === null || key === 'name') {
        continue;
      }
      params = params.append(key, String(value));
    }
    return this.http.put<Faction>(
      `/api/campaigns/${campaignId}/factions/${factionId}`,
      {},
      { params },
    );
  }

  /**
   * Removes a faction.
   *
   * @param campaignId the owning campaign
   * @param factionId the faction to remove
   */
  delete(campaignId: number, factionId: number): Observable<void> {
    return this.http.delete<void>(
      `/api/campaigns/${campaignId}/factions/${factionId}`,
    );
  }
}
