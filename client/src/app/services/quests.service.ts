import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CreateObjectiveRequest,
  CreateQuestRequest,
  Objective,
  Quest,
  QuestStatus,
} from '../models/quest';

/**
 * The front-end client for the back-end quest system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/quests}: listing and inspecting quests,
 * creating quests, updating their identity and status, attaching related locations, and tracking
 * objective progress. Quest mutations use request parameters, so this service builds typed
 * {@link HttpParams} for them.</p>
 *
 * <p>The {@link CampaignStore} owns the authoritative quest list for the active campaign; this
 * service only performs the HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class QuestsService {
  private readonly http = inject(HttpClient);

  /**
   * @param campaignId the owning campaign
   * @return every quest in the campaign
   */
  list(campaignId: number): Observable<Quest[]> {
    return this.http.get<Quest[]>(`/api/campaigns/${campaignId}/quests`);
  }

  /**
   * @param campaignId the owning campaign
   * @param questId the quest to fetch
   * @return the quest with the given id
   */
  get(campaignId: number, questId: number): Observable<Quest> {
    return this.http.get<Quest>(`/api/campaigns/${campaignId}/quests/${questId}`);
  }

  /**
   * @param campaignId the owning campaign
   * @param objectiveId the objective to fetch
   * @return the objective with the given id
   */
  getObjectives(campaignId: number, questId: number): Observable<Objective[]> {
    return this.http.get<Objective[]>(
      `/api/campaigns/${campaignId}/quests/${questId}/objectives`,
    );
  }

  /**
   * Creates a new quest.
   *
   * @param campaignId the owning campaign
   * @param request the quest creation request
   * @return the created quest
   */
  create(campaignId: number, request: CreateQuestRequest): Observable<Quest> {
    let params = new HttpParams().append('title', request.title);
    for (const [key, value] of Object.entries(request)) {
      if (value === undefined || value === null || key === 'title') {
        continue;
      }
      params = params.append(key, String(value));
    }
    return this.http.post<Quest>(`/api/campaigns/${campaignId}/quests`, {}, { params });
  }

  /**
   * Updates a quest's identity and status.
   *
   * @param campaignId the owning campaign
   * @param questId the quest to update
   * @param fields the fields to change (any omitted field is left untouched)
   * @return the updated quest
   */
  update(
    campaignId: number,
    questId: number,
    fields: Partial<{
      title: string;
      description: string;
      giver: string;
      rewards: string;
      status: QuestStatus;
    }>,
  ): Observable<Quest> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(fields)) {
      if (value !== undefined && value !== null) {
        params = params.append(key, String(value));
      }
    }
    return this.http.put<Quest>(
      `/api/campaigns/${campaignId}/quests/${questId}`,
      {},
      { params },
    );
  }

  /**
   * Deletes a quest.
   *
   * @param campaignId the owning campaign
   * @param questId the quest to remove
   */
  delete(campaignId: number, questId: number): Observable<Quest> {
    return this.http.delete<Quest>(
      `/api/campaigns/${campaignId}/quests/${questId}`,
    );
  }

  /**
   * Completes a quest.
   *
   * @param campaignId the owning campaign
   * @param questId the quest to complete
   * @return the updated quest
   */
  complete(campaignId: number, questId: number): Observable<Quest> {
    return this.http.put<Quest>(
      `/api/campaigns/${campaignId}/quests/${questId}/complete`,
      {},
    );
  }

  /**
   * Marks a quest as failed.
   *
   * @param campaignId the owning campaign
   * @param questId the quest to fail
   * @return the updated quest
   */
  fail(campaignId: number, questId: number): Observable<Quest> {
    return this.http.put<Quest>(
      `/api/campaigns/${campaignId}/quests/${questId}/fail`,
      {},
    );
  }

  /**
   * Adds one objective to a quest.
   *
   * @param campaignId the owning campaign
   * @param questId the quest to add the objective to
   * @param request the objective creation request
   * @return the created objective
   */
  addObjective(
    campaignId: number,
    questId: number,
    request: CreateObjectiveRequest,
  ): Observable<Objective> {
    return this.http.post<Objective>(
      `/api/campaigns/${campaignId}/quests/${questId}/objectives`,
      {},
      {
        params: new HttpParams()
          .append('description', request.description)
          .append('targetCount', String(request.targetCount ?? 1)),
      },
    );
  }

  /**
   * Advances an objective's progress.
   *
   * @param campaignId the owning campaign
   * @param questId the owning quest
   * @param objectiveId the objective to advance
   * @param currentCount the new current count
   * @return the updated objective
   */
  setObjectiveProgress(
    campaignId: number,
    questId: number,
    objectiveId: number,
    currentCount: number,
  ): Observable<Objective> {
    return this.http.put<Objective>(
      `/api/campaigns/${campaignId}/quests/${questId}/objectives/${objectiveId}/progress`,
      {},
      {
        params: new HttpParams().append('currentCount', String(currentCount)),
      },
    );
  }
}
