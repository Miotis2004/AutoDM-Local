import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CreateCreatureTemplateRequest,
  CreatureTemplate,
  UpdateCreatureTemplateRequest,
} from '../models/creature-template';

/**
 * The front-end client for the back-end creature template system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/creature-templates}: listing templates,
 * inspecting a single template, creating templates, instantiating an enemy from a template, and
 * removing templates. Template mutations use request parameters, so this service builds typed
 * {@link HttpParams} for them.</p>
 *
 * <p>The {@link CampaignStore} owns the authoritative template catalogue for the active campaign;
 * this service only performs the HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class CreatureTemplatesService {
  private readonly http = inject(HttpClient);

  /**
   * @param campaignId the owning campaign
   * @return every creature template in the campaign
   */
  list(campaignId: number): Observable<CreatureTemplate[]> {
    return this.http.get<CreatureTemplate[]>(
      `/api/campaigns/${campaignId}/creature-templates`,
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param templateId the template to fetch
   * @return the template with the given id
   */
  get(
    campaignId: number,
    templateId: number,
  ): Observable<CreatureTemplate> {
    return this.http.get<CreatureTemplate>(
      `/api/campaigns/${campaignId}/creature-templates/${templateId}`,
    );
  }

  /**
   * Creates a new creature template.
   *
   * @param campaignId the owning campaign
   * @param request the template creation request
   * @return the created template
   */
  create(
    campaignId: number,
    request: CreateCreatureTemplateRequest,
  ): Observable<CreatureTemplate> {
    let params = new HttpParams()
      .append('name', request.name)
      .append('health', String(request.health ?? 0))
      .append('defense', String(request.defense ?? 0))
      .append('attack', String(request.attack ?? 0))
      .append('damage', String(request.damage ?? 0))
      .append(
        'initiativeModifier',
        String(request.initiativeModifier ?? 0),
      );
    if (request.description) {
      params = params.append('description', request.description);
    }
    if (request.behaviorNotes) {
      params = params.append('behaviorNotes', request.behaviorNotes);
    }
    return this.http.post<CreatureTemplate>(
      `/api/campaigns/${campaignId}/creature-templates`,
      {},
      { params },
    );
  }

  /**
   * Updates a template's mutable fields (combat profile, description, behaviour notes).
   *
   * <p>The template name is not mutable through this endpoint; it is left untouched on the
   * back-end. Only non-empty fields in {@code patch} are sent.</p>
   *
   * @param campaignId the owning campaign
   * @param templateId the template to update
   * @param patch the fields to change (omitted fields are left untouched)
   * @return the updated template
   */
  update(
    campaignId: number,
    templateId: number,
    patch: Omit<CreateCreatureTemplateRequest, 'name'>,
  ): Observable<CreatureTemplate> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(patch)) {
      if (value === undefined || value === null || value === '') {
        continue;
      }
      params = params.append(key, String(value));
    }
    return this.http.put<CreatureTemplate>(
      `/api/campaigns/${campaignId}/creature-templates/${templateId}`,
      {},
      { params },
    );
  }

  /**
   * Instantiates an NPC enemy from this template.
   *
   * @param campaignId the owning campaign
   * @param templateId the template to instantiate from
   * @param enemyName optional name for the instantiated enemy
   * @return the instantiated enemy
   */
  instantiateEnemy(
    campaignId: number,
    templateId: number,
    enemyName?: string,
  ): Observable<import('../models/npc').Npc> {
    let params = new HttpParams();
    if (enemyName) {
      params = params.append('enemyName', enemyName);
    }
    return this.http.post<import('../models/npc').Npc>(
      `/api/campaigns/${campaignId}/creature-templates/${templateId}/instantiate`,
      {},
      { params },
    );
  }

  /**
   * Removes a template.
   *
   * @param campaignId the owning campaign
   * @param templateId the template to remove
   */
  delete(
    campaignId: number,
    templateId: number,
  ): Observable<void> {
    return this.http.delete<void>(
      `/api/campaigns/${campaignId}/creature-templates/${templateId}`,
    );
  }
}
