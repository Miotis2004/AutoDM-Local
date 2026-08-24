import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CreateNpcRequest,
  Disposition,
  Npc,
  NpcRelationship,
  UpdateNpcRelationshipRequest,
} from '../models/npc';

/**
 * The front-end client for the back-end NPC system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/npcs}: listing the party's NPCs (all,
 * active, or filtered by disposition or relationship), inspecting a single NPC, creating NPCs, and
 * updating their relationship, location, notes, combat stats, and saving throws. NPC updates use
 * request parameters, so this service builds typed {@link HttpParams} for them.</p>
 *
 * <p>The {@link CampaignStore} owns the authoritative NPC list for the active campaign; this
 * service only performs the HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class NpcsService {
  private readonly http = inject(HttpClient);

  /**
   * @param campaignId the owning campaign
   * @return every NPC in the campaign
   */
  list(campaignId: number): Observable<Npc[]> {
    return this.http.get<Npc[]>(`/api/campaigns/${campaignId}/npcs`);
  }

  /**
   * @param campaignId the owning campaign
   * @return only the active NPCs in the campaign
   */
  listActive(campaignId: number): Observable<Npc[]> {
    return this.http.get<Npc[]>(`/api/campaigns/${campaignId}/npcs/active`);
  }

  /**
   * @param campaignId the owning campaign
   * @return the NPCs filtered by their disposition toward the party
   */
  listByDisposition(
    campaignId: number,
    disposition: Disposition,
  ): Observable<Npc[]> {
    return this.http.get<Npc[]>(
      `/api/campaigns/${campaignId}/npcs/disposition`,
      { params: new HttpParams().append('disposition', disposition) },
    );
  }

  /**
   * @param campaignId the owning campaign
   * @return the NPCs filtered by their relationship to the party
   */
  listByRelationship(
    campaignId: number,
    relationship: NpcRelationship,
  ): Observable<Npc[]> {
    return this.http.get<Npc[]>(
      `/api/campaigns/${campaignId}/npcs/relationship`,
      { params: new HttpParams().append('relationship', relationship) },
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param npcId the NPC to fetch
   * @return the NPC with the given id
   */
  get(campaignId: number, npcId: number): Observable<Npc> {
    return this.http.get<Npc>(
      `/api/campaigns/${campaignId}/npcs/${npcId}`,
    );
  }

  /**
   * Creates a new NPC.
   *
   * @param campaignId the owning campaign
   * @param request the NPC creation request
   * @return the created NPC
   */
  create(campaignId: number, request: CreateNpcRequest): Observable<Npc> {
    let params = new HttpParams()
      .append('name', request.name)
      .append('disposition', request.disposition ?? Disposition.NEUTRAL);
    for (const [key, value] of Object.entries(request)) {
      if (
        value === undefined ||
        value === null ||
        key === 'name' ||
        key === 'disposition'
      ) {
        continue;
      }
      params = params.append(key, String(value));
    }
    return this.http.post<Npc>(`/api/campaigns/${campaignId}/npcs`, {}, { params });
  }

  /**
   * Updates an NPC's story fields (description, role, disposition, faction).
   *
   * @param campaignId the owning campaign
   * @param npcId the NPC to update
   * @param fields the fields to change (any omitted field is left untouched)
   * @return the updated NPC
   */
  update(
    campaignId: number,
    npcId: number,
    fields: Partial<CreateNpcRequest>,
  ): Observable<Npc> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(fields)) {
      if (value === undefined || value === null || key === 'name' || key === 'notes') {
        continue;
      }
      params = params.append(key, String(value));
    }
    return this.http.put<Npc>(
      `/api/campaigns/${campaignId}/npcs/${npcId}`,
      {},
      { params },
    );
  }

  /**
   * Updates an NPC's relationship toward the party.
   *
   * @param campaignId the owning campaign
   * @param npcId the NPC to update
   * @param request the new relationship
   * @return the updated NPC
   */
  updateRelationship(
    campaignId: number,
    npcId: number,
    request: UpdateNpcRelationshipRequest,
  ): Observable<Npc> {
    return this.http.put<Npc>(
      `/api/campaigns/${campaignId}/npcs/${npcId}/relationship`,
      {},
      {
        params: new HttpParams().append(
          'relationship',
          request.relationship,
        ),
      },
    );
  }

  /**
   * Sets whether an NPC is active (alive/offstage).
   *
   * @param campaignId the owning campaign
   * @param npcId the NPC to update
   * @param active whether the NPC is active
   * @return the updated NPC
   */
  setActive(
    campaignId: number,
    npcId: number,
    active: boolean,
  ): Observable<Npc> {
    return this.http.put<Npc>(
      `/api/campaigns/${campaignId}/npcs/${npcId}/active`,
      {},
      { params: new HttpParams().append('active', String(active)) },
    );
  }

  /**
   * Patches an NPC's notes.
   *
   * @param campaignId the owning campaign
   * @param npcId the NPC to update
   * @param notes the new notes
   * @return the updated NPC
   */
  setNotes(
    campaignId: number,
    npcId: number,
    notes: string,
  ): Observable<Npc> {
    return this.http.put<Npc>(
      `/api/campaigns/${campaignId}/npcs/${npcId}/notes`,
      {},
      { params: new HttpParams().append('notes', notes) },
    );
  }

  /**
   * Deletes an NPC.
   *
   * @param campaignId the owning campaign
   * @param npcId the NPC to remove
   */
  delete(campaignId: number, npcId: number): Observable<void> {
    return this.http.delete<void>(
      `/api/campaigns/${campaignId}/npcs/${npcId}`,
    );
  }
}
