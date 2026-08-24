import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CreateLocationRequest,
  CreatePointOfInterestRequest,
  CreateRegionRequest,
  CreateSettlementRequest,
  Location,
  PointOfInterest,
  PointOfInterestCategory,
  Region,
  Settlement,
  SettlementType,
} from '../models/world';

/**
 * The front-end client for the back-end world system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/...} world endpoints: regions,
 * locations, settlements, points of interest, travel routes, and the party's current location.
 * World mutations use request parameters, so this service builds typed {@link HttpParams} for them.</p>
 *
 * <p>The {@link CampaignStore} owns the authoritative world data for the active campaign; this
 * service only performs the HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class WorldService {
  private readonly http = inject(HttpClient);

  // ------------------------------------------------------------------
  // Regions
  // ------------------------------------------------------------------

  /**
   * @param campaignId the owning campaign
   * @return every region in the campaign
   */
  listRegions(campaignId: number): Observable<Region[]> {
    return this.http.get<Region[]>(`/api/campaigns/${campaignId}/regions`);
  }

  /**
   * Creates a new region.
   *
   * @param campaignId the owning campaign
   * @param request the region creation request
   * @return the created region
   */
  createRegion(campaignId: number, request: CreateRegionRequest): Observable<Region> {
    return this.http.post<Region>(
      `/api/campaigns/${campaignId}/regions`,
      {},
      { params: this.params(request) },
    );
  }

  // ------------------------------------------------------------------
  // Locations
  // ------------------------------------------------------------------

  /**
   * @param campaignId the owning campaign
   * @return every location in the campaign
   */
  listLocations(campaignId: number): Observable<Location[]> {
    return this.http.get<Location[]>(`/api/campaigns/${campaignId}/locations`);
  }

  /**
   * @param campaignId the owning campaign
   * @return only the discovered locations in the campaign
   */
  listDiscoveredLocations(campaignId: number): Observable<Location[]> {
    return this.http.get<Location[]>(
      `/api/campaigns/${campaignId}/locations/discovered`,
    );
  }

  /**
   * Creates a new location.
   *
   * @param campaignId the owning campaign
   * @param request the location creation request
   * @return the created location
   */
  createLocation(
    campaignId: number,
    request: CreateLocationRequest,
  ): Observable<Location> {
    return this.http.post<Location>(
      `/api/campaigns/${campaignId}/locations`,
      {},
      { params: this.params(request) },
    );
  }

  /**
   * Marks a location as discovered.
   *
   * @param campaignId the owning campaign
   * @param locationId the location to discover
   * @return the updated location
   */
  discover(campaignId: number, locationId: number): Observable<Location> {
    return this.http.post<Location>(
      `/api/campaigns/${campaignId}/locations/${locationId}/discover`,
      null,
    );
  }

  // ------------------------------------------------------------------
  // Settlements
  // ------------------------------------------------------------------

  /**
   * @param campaignId the owning campaign
   * @return every settlement in the campaign
   */
  listSettlements(campaignId: number): Observable<Settlement[]> {
    return this.http.get<Settlement[]>(
      `/api/campaigns/${campaignId}/settlements`,
    );
  }

  /**
   * Creates a new settlement.
   *
   * @param campaignId the owning campaign
   * @param request the settlement creation request
   * @return the created settlement
   */
  createSettlement(
    campaignId: number,
    request: CreateSettlementRequest,
  ): Observable<Settlement> {
    let params = new HttpParams()
      .append('name', request.name)
      .append('type', request.type ?? SettlementType.VILLAGE)
      .append('population', String(request.population ?? 0));
    if (request.regionId !== undefined && request.regionId !== null) {
      params = params.append('regionId', String(request.regionId));
    }
    return this.http.post<Settlement>(
      `/api/campaigns/${campaignId}/settlements`,
      {},
      { params },
    );
  }

  // ------------------------------------------------------------------
  // Points of interest
  // ------------------------------------------------------------------

  /**
   * Creates a new point of interest. The back-end creates the backing location for the
   * point of interest, so the caller only supplies the name, category and description.
   *
   * @param campaignId the owning campaign
   * @param request the point-of-interest creation request
   * @return the created point of interest
   */
  createPointOfInterest(
    campaignId: number,
    request: CreatePointOfInterestRequest,
  ): Observable<PointOfInterest> {
    let params = new HttpParams()
      .append('name', request.name)
      .append('category', request.category ?? PointOfInterestCategory.LANDMARK);
    if (request.description) {
      params = params.append('description', request.description);
    }
    return this.http.post<PointOfInterest>(
      `/api/campaigns/${campaignId}/points-of-interest`,
      {},
      { params },
    );
  }

  // ------------------------------------------------------------------
  // Points of interest
  // ------------------------------------------------------------------

  /**
   * @param campaignId the owning campaign
   * @return every point of interest in the campaign
   */
  listPointsOfInterest(campaignId: number): Observable<PointOfInterest[]> {
    return this.http.get<PointOfInterest[]>(
      `/api/campaigns/${campaignId}/points-of-interest`,
    );
  }

  // ------------------------------------------------------------------
  // Party location
  // ------------------------------------------------------------------

  /**
   * @param campaignId the owning campaign
   * @return the party's current location, if set
   */
  getPartyLocation(campaignId: number): Observable<Partial<{ locationId: number }>> {
    return this.http.get<Partial<{ locationId: number }>>(
      `/api/campaigns/${campaignId}/party-location`,
    );
  }

  /**
   * Sets the party's current location.
   *
   * @param campaignId the owning campaign
   * @param locationId the location the party has moved to
   * @return the updated party location
   */
  setPartyLocation(campaignId: number, locationId: number): Observable<unknown> {
    return this.http.post<unknown>(
      `/api/campaigns/${campaignId}/party-location`,
      {},
      { params: new HttpParams().append('locationId', String(locationId)) },
    );
  }

  /**
   * Builds request parameters from an object, skipping undefined and null values.
   */
  private params(fields: object): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(fields)) {
      if (value !== undefined && value !== null) {
        params = params.append(key, String(value));
      }
    }
    return params;
  }
}
