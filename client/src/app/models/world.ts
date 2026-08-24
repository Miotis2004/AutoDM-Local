/**
 * Front-end view models for world contents.
 *
 * <p>Mirrors the back-end {@code Region}, {@code Location}, {@code Settlement},
 * {@code PointOfInterest} and party-location concepts exposed by
 * {@code /api/campaigns/{campaignId}/...}. The {@link WorldService} wraps those endpoints.</p>
 */

/** A region of the world, grouping many locations. */
export interface Region {
  id: number;
  name: string;
  description?: string;
}

/** A location within a region the party can visit and discover. */
export interface Location {
  id: number;
  name: string;
  description?: string;
  regionId?: number;
  discovered: boolean;
  latitude?: number;
  longitude?: number;
}

/** The kind of a settlement, matching the back-end {@code SettlementType} enum. */
export enum SettlementType {
  VILLAGE = 'VILLAGE',
  TOWN = 'TOWN',
  CITY = 'CITY',
  FORTRESS = 'FORTRESS',
}

/** A settlement built at a {@link Location}. */
export interface Settlement {
  id: number;
  name: string;
  type?: SettlementType;
  population?: number;
  locationId?: number;
}

/** The kind of a {@link PointOfInterest}, matching the back-end {@code PointOfInterestCategory} enum. */
export enum PointOfInterestCategory {
  TAVERN = 'TAVERN',
  TEMPLE = 'TEMPLE',
  MARKET = 'MARKET',
  CASTLE = 'CASTLE',
  DUNGEON = 'DUNGEON',
  INN = 'INN',
  GUILD = 'GUILD',
  LANDMARK = 'LANDMARK',
}

/** A point of interest attached to a {@link Location}. */
export interface PointOfInterest {
  id: number;
  name: string;
  description?: string;
  category?: PointOfInterestCategory;
  locationId?: number;
}

/** The party's current location within a campaign. */
export interface PartyLocation {
  locationId: number;
  notes?: string;
}

/** Request body for creating a {@link Region}. */
export interface CreateRegionRequest {
  name: string;
  description?: string;
}

/** Request body for creating a {@link Location}. */
export interface CreateLocationRequest {
  name: string;
  description?: string;
  regionId?: number;
  latitude?: number;
  longitude?: number;
}

/** Request body for creating a {@link Settlement}. */
export interface CreateSettlementRequest {
  name: string;
  type?: SettlementType;
  population?: number;
  locationId?: number;
  regionId?: number;
}

/** Request body for creating a {@link PointOfInterest}. */
export interface CreatePointOfInterestRequest {
  name: string;
  description?: string;
  category?: PointOfInterestCategory;
  locationId?: number;
}
