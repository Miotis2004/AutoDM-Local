import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Campaign,
  CreateCampaignRequest,
  UpdateCampaignRequest,
} from '../models/campaign';

/**
 * The front-end client for the back-end campaign management system.
 *
 * <p>This service wraps {@code /api/campaign-management}: creating, listing, inspecting, editing,
 * archiving, deleting, and selecting campaigns. It returns raw {@link Campaign} values and never
 * caches them - the {@link CampaignStore} is the single authoritative home of the active campaign,
 * so campaign state is duplicated nowhere else.</p>
 *
 * <p>Every method returns an observable, so components can drive the UI directly from the HTTP
 * stream.</p>
 */
@Injectable({ providedIn: 'root' })
export class CampaignsService {
  private readonly http = inject(HttpClient);

  /**
   * @return every campaign, most recently created first as the back-end returns them
   */
  list(): Observable<Campaign[]> {
    return this.http.get<Campaign[]>('/api/campaign-management');
  }

  /**
   * @return the campaign currently marked active, if any
   */
  getActive(): Observable<Partial<Campaign>> {
    return this.http.get<Partial<Campaign>>('/api/campaign-management/active');
  }

  /**
   * @param id the campaign to fetch
   * @return the campaign with the given id
   */
  get(id: number): Observable<Campaign> {
    return this.http.get<Campaign>(`/api/campaign-management/${id}`);
  }

  /**
   * Creates a new campaign.
   *
   * @param request the campaign creation request
   * @return the created campaign
   */
  create(request: CreateCampaignRequest): Observable<Campaign> {
    return this.http.post<Campaign>('/api/campaign-management', request);
  }

  /**
   * Updates a campaign's metadata.
   *
   * @param id the campaign to update
   * @param request the update payload
   * @return the updated campaign
   */
  update(id: number, request: UpdateCampaignRequest): Observable<Campaign> {
    return this.http.put<Campaign>(`/api/campaign-management/${id}`, request);
  }

  /**
   * Archives a campaign, marking it inactive without removing it.
   *
   * @param id the campaign to archive
   * @return the archived campaign
   */
  archive(id: number): Observable<Campaign> {
    return this.http.post<Campaign>(`/api/campaign-management/${id}/archive`, null);
  }

  /**
   * Permanently deletes a campaign.
   *
   * @param id the campaign to delete
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/campaign-management/${id}`);
  }

  /**
   * Marks a campaign as the active one.
   *
   * @param id the campaign to activate
   * @return the activated campaign
   */
  select(id: number): Observable<Campaign> {
    return this.http.post<Campaign>(`/api/campaign-management/${id}/select`, null);
  }
}
