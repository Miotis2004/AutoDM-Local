import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Scene, SceneStatus } from '../models/scene';

/**
 * The front-end client for the back-end scene system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/scenes}: listing the scenes a campaign
 * holds and fetching a single scene. It exists so the play screen can find the scene currently in
 * focus - the one scene whose {@link SceneStatus} is {@code ACTIVE} - and supply the id that
 * {@link DungeonMasterService} requires to resolve a player action against the right scene.</p>
 *
 * <p>The {@link CampaignStore} owns the authoritative campaign data; this service only performs the
 * HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class SceneService {
  private readonly http = inject(HttpClient);

  /**
   * @param campaignId the owning campaign
   * @return every scene in the campaign, oldest first
   */
  list(campaignId: number): Observable<Scene[]> {
    return this.http.get<Scene[]>(`/api/campaigns/${campaignId}/scenes`);
  }

  /**
   * @param campaignId the owning campaign
   * @param sceneId the scene to fetch
   * @return the requested scene, when it exists
   */
  get(campaignId: number, sceneId: number): Observable<Scene> {
    return this.http.get<Scene>(`/api/campaigns/${campaignId}/scenes/${sceneId}`);
  }

  /**
   * @param scenes the scenes to consult
   * @return the scene currently in focus, or {@code null} when the campaign has no active scene
   */
  activeScene(scenes: Scene[]): Scene | null {
    return (
      scenes.find((scene) => scene.status === SceneStatus.ACTIVE) ?? null
    );
  }
}
