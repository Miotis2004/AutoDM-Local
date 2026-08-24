import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CreateInventoryItemRequest,
  InventoryItem,
  InventoryOwnerKind,
  InventoryTransfer,
  TransferInventoryItemRequest,
} from '../models/item';

/**
 * The front-end client for the back-end inventory system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/inventory}: listing holdings and
 * transfers, creating holdings, adjusting quantities, flipping the equipped flag, transferring items
 * between owners, and removing holdings. Inventory mutations use request parameters, so this
 * service builds typed {@link HttpParams} for them.</p>
 *
 * <p>The {@link CampaignStore} owns the authoritative inventory for the active campaign; this
 * service only performs the HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class ItemsService {
  private readonly http = inject(HttpClient);

  /**
   * @param campaignId the owning campaign
   * @param ownerKind optional filter by owner kind
   * @param ownerId optional filter by owner id
   * @return every holding in the campaign
   */
  list(
    campaignId: number,
    ownerKind?: InventoryOwnerKind,
    ownerId?: number,
  ): Observable<InventoryItem[]> {
    let params = new HttpParams();
    if (ownerKind) {
      params = params.append('owner', ownerKind);
    }
    if (ownerId !== undefined) {
      params = params.append('ownerId', String(ownerId));
    }
    return this.http.get<InventoryItem[]>(
      `/api/campaigns/${campaignId}/inventory`,
      { params },
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param itemId the holding to fetch
   * @return the holding with the given id
   */
  get(campaignId: number, itemId: number): Observable<InventoryItem> {
    return this.http.get<InventoryItem>(
      `/api/campaigns/${campaignId}/inventory/${itemId}`,
    );
  }

  /**
   * Adds a holding to an owner's inventory.
   *
   * @param campaignId the owning campaign
   * @param request the holding creation request
   * @return the created holding
   */
  create(campaignId: number, request: CreateInventoryItemRequest): Observable<InventoryItem> {
    let params = new HttpParams()
      .append('name', request.name)
      .append('category', request.category)
      .append('owner', request.ownerKind)
      .append('ownerId', String(request.ownerId))
      .append('quantity', String(request.quantity ?? 1));
    if (request.value !== undefined) {
      params = params.append('value', String(request.value));
    }
    if (request.description) {
      params = params.append('description', request.description);
    }
    return this.http.post<InventoryItem>(
      `/api/campaigns/${campaignId}/inventory`,
      {},
      { params },
    );
  }

  /**
   * Updates a holding's catalogue attributes (name, category, value, description).
   *
   * @param campaignId the owning campaign
   * @param itemId the holding to update
   * @param fields the fields to change (any omitted field is left untouched)
   * @return the updated holding
   */
  update(
    campaignId: number,
    itemId: number,
    fields: Partial<CreateInventoryItemRequest>,
  ): Observable<InventoryItem> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(fields)) {
      if (value === undefined || value === null || key === 'ownerKind' || key === 'ownerId') {
        continue;
      }
      params = params.append(key, String(value));
    }
    return this.http.put<InventoryItem>(
      `/api/campaigns/${campaignId}/inventory/${itemId}`,
      {},
      { params },
    );
  }

  /**
   * Adjusts a holding's quantity by a delta (positive or negative).
   *
   * @param campaignId the owning campaign
   * @param itemId the holding to adjust
   * @param delta the quantity change
   * @return the updated holding
   */
  adjustQuantity(
    campaignId: number,
    itemId: number,
    delta: number,
  ): Observable<InventoryItem> {
    return this.http.patch<InventoryItem>(
      `/api/campaigns/${campaignId}/inventory/${itemId}/quantity`,
      {},
      { params: new HttpParams().append('delta', String(delta)) },
    );
  }

  /**
   * Sets whether a holding (owned by a player character) is equipped.
   *
   * @param campaignId the owning campaign
   * @param itemId the holding to update
   * @param equipped whether the holding is equipped
   * @return the updated holding
   */
  setEquipped(
    campaignId: number,
    itemId: number,
    equipped: boolean,
  ): Observable<InventoryItem> {
    return this.http.patch<InventoryItem>(
      `/api/campaigns/${campaignId}/inventory/${itemId}/equipped`,
      {},
      { params: new HttpParams().append('value', String(equipped)) },
    );
  }

  /**
   * Transfers a quantity of a holding between two owners.
   *
   * @param campaignId the owning campaign
   * @param request the transfer request
   * @return the recorded transfer
   */
  transfer(
    campaignId: number,
    request: TransferInventoryItemRequest,
  ): Observable<InventoryTransfer> {
    const params = new HttpParams()
      .append('itemId', String(request.itemId))
      .append('fromOwnerId', String(request.fromOwnerId))
      .append('fromOwnerKind', request.fromOwnerKind)
      .append('toOwnerId', String(request.toOwnerId))
      .append('toOwnerKind', request.toOwnerKind)
      .append('quantity', String(request.quantity));
    return this.http.post<InventoryTransfer>(
      `/api/campaigns/${campaignId}/inventory/transfer`,
      {},
      { params },
    );
  }

  /**
   * @param campaignId the owning campaign
   * @return the transfer history for the campaign
   */
  listTransfers(campaignId: number): Observable<InventoryTransfer[]> {
    return this.http.get<InventoryTransfer[]>(
      `/api/campaigns/${campaignId}/inventory/transfers`,
    );
  }

  /**
   * Removes a holding.
   *
   * @param campaignId the owning campaign
   * @param itemId the holding to remove
   */
  delete(campaignId: number, itemId: number): Observable<void> {
    return this.http.delete<void>(
      `/api/campaigns/${campaignId}/inventory/${itemId}`,
    );
  }
}
