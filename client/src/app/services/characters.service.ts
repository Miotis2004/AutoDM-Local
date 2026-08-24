import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Character,
  CreateCharacterRequest,
  UpdateCharacterStatsRequest,
  UpdateHitPointsRequest,
} from '../models/character';

/**
 * The front-end client for the back-end character system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/characters}: listing and inspecting the
 * campaign roster, creating characters, updating their identity and combat vitals. It maps the
 * parameter-based update endpoints onto small typed methods and returns {@link Character} values on
 * observables.</p>
 *
 * <p>The {@link CampaignStore} owns the authoritative roster for the active campaign; this service
 * only performs the HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class CharactersService {
  private readonly http = inject(HttpClient);

  /**
   * @param campaignId the owning campaign
   * @return every character in the campaign
   */
  list(campaignId: number): Observable<Character[]> {
    return this.http.get<Character[]>(
      `/api/campaigns/${campaignId}/characters`,
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param characterId the character to fetch
   * @return the character with the given id
   */
  get(campaignId: number, characterId: number): Observable<Character> {
    return this.http.get<Character>(
      `/api/campaigns/${campaignId}/characters/${characterId}`,
    );
  }

  /**
   * Creates a new character.
   *
   * @param campaignId the owning campaign
   * @param request the character creation request
   * @return the created character
   */
  create(campaignId: number, request: CreateCharacterRequest): Observable<Character> {
    return this.http.post<Character>(
      `/api/campaigns/${campaignId}/characters`,
      request,
    );
  }

  /**
   * Updates a character's identity fields.
   *
   * @param campaignId the owning campaign
   * @param characterId the character to update
   * @param fields the identity fields to change
   * @return the updated character
   */
  updateIdentity(
    campaignId: number,
    characterId: number,
    fields: Partial<{
      name: string;
      ancestry: string;
      characterClass: string;
      level: number;
      background: string;
      alignment: string;
    }>,
  ): Observable<Character> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(fields)) {
      if (value !== undefined && value !== null) {
        params = params.append(key, String(value));
      }
    }
    return this.http.put<Character>(
      `/api/campaigns/${campaignId}/characters/${characterId}`,
      {},
      { params },
    );
  }

  /**
   * Patches a character's combat stats.
   *
   * @param campaignId the owning campaign
   * @param characterId the character to update
   * @param stats the stat values to change (any omitted field is left untouched)
   * @return the updated character
   */
  updateStats(
    campaignId: number,
    characterId: number,
    stats: UpdateCharacterStatsRequest,
  ): Observable<Character> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(stats)) {
      if (value !== undefined && value !== null) {
        params = params.append(key, String(value));
      }
    }
    return this.http.patch<Character>(
      `/api/campaigns/${campaignId}/characters/${characterId}/stats`,
      {},
      { params },
    );
  }

  /**
   * Sets a character's current hit points.
   *
   * @param campaignId the owning campaign
   * @param characterId the character to update
   * @param request the new hit points
   * @return the updated character
   */
  setHitPoints(
    campaignId: number,
    characterId: number,
    request: UpdateHitPointsRequest,
  ): Observable<Character> {
    return this.http.patch<Character>(
      `/api/campaigns/${campaignId}/characters/${characterId}/hit-points`,
      {},
      { params: new HttpParams().append('hitPoints', String(request.hitPoints)) },
    );
  }

  /**
   * Deletes a character.
   *
   * @param campaignId the owning campaign
   * @param characterId the character to remove
   */
  delete(
    campaignId: number,
    characterId: number,
  ): Observable<void> {
    return this.http.delete<void>(
      `/api/campaigns/${campaignId}/characters/${characterId}`,
    );
  }
}
