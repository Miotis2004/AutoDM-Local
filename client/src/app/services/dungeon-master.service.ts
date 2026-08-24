import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AdvanceSceneRequest, EngineResponse, PlayerActionRequest } from '../models/dm';

/**
 * The front-end client for the Dungeon Master engine.
 *
 * <p>This service wraps the two engine endpoints: {@code POST
 * /api/campaigns/{campaignId}/scenes/{sceneId}/action}, which runs a player action through the
 * engine and returns an {@link EngineResponse}, and {@code POST
 * /api/campaigns/{campaignId}/scenes/advance}, which moves the active scene focus to the next
 * scene. The engine parameters are request parameters, so this service builds typed
 * {@link HttpParams} for them.</p>
 *
 * <p>The {@link CampaignStore} holds the active campaign and its current scene, so this service
 * reads that state instead of components threading identifiers through call sites.</p>
 */
@Injectable({ providedIn: 'root' })
export class DungeonMasterService {
  private readonly http = inject(HttpClient);

  /**
   * Runs a player action through the engine.
   *
   * @param campaignId the owning campaign
   * @param sceneId the active scene the action resolves against
   * @param request the action and its optional resolution parameters
   * @return the complete engine response
   */
  resolveAction(
    campaignId: number,
    sceneId: number,
    request: PlayerActionRequest,
  ): Observable<EngineResponse> {
    let params = new HttpParams().append('action', request.action);
    if (request.statistic !== undefined) {
      params = params.append('statistic', String(request.statistic));
    }
    if (request.modifier !== undefined) {
      params = params.append('modifier', String(request.modifier));
    }
    if (request.difficulty !== undefined) {
      params = params.append('difficulty', String(request.difficulty));
    }
    return this.http.post<EngineResponse>(
      `/api/campaigns/${campaignId}/scenes/${sceneId}/action`,
      {},
      { params },
    );
  }

  /**
   * Advances the active scene to the next one.
   *
   * @param request the advance request carrying the owning campaign
   * @return the engine response for the advancement
   */
  advanceScene(request: AdvanceSceneRequest): Observable<EngineResponse> {
    return this.http.post<EngineResponse>(
      `/api/campaigns/${request.campaignId}/scenes/advance`,
      null,
    );
  }
}
