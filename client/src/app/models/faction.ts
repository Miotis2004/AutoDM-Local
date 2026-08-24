/**
 * Front-end view models for factions.
 *
 * <p>Mirrors the back-end {@code Faction} entity exposed by
 * {@code /api/campaigns/{campaignId}/factions}. The {@link FactionsService} wraps those
 * endpoints.</p>
 */

/** The general attitude a faction shows toward the party, matching the back-end {@code Disposition} enum. */
export enum Disposition {
  FRIENDLY = 'FRIENDLY',
  FRIENDLY_NEUTRAL = 'FRIENDLY_NEUTRAL',
  NEUTRAL = 'NEUTRAL',
  NEUTRAL_HOSTILE = 'NEUTRAL_HOSTILE',
  HOSTILE = 'HOSTILE',
}

/**
 * The durable standing the wider world holds toward a faction. Reuses the same closed set as an
 * NPC's relationship to the party, matching the back-end {@code NpcRelationship} enum.
 */
export enum NpcRelationship {
  ALLEY = 'ALLEY',
  FRIEND = 'FRIEND',
  NEUTRAL = 'NEUTRAL',
  FOE = 'FOE',
}

/**
 * A faction: a group, organization, or power that shares a common purpose within a campaign.
 *
 * <p>The {@link FactionsService} returns these from the back-end; the {@link CampaignStore} keeps
 * the campaign's faction list so components read faction state from one authoritative place.</p>
 */
export interface Faction {
  id: number;
  name: string;
  description?: string;
  disposition: Disposition;
  reputation: NpcRelationship;
  notes?: string;
}

/** Request body for creating a new faction. */
export interface CreateFactionRequest {
  name: string;
  description?: string;
  disposition: Disposition;
  reputation: NpcRelationship;
}

/** Request body for updating a faction's story fields. */
export interface UpdateFactionRequest {
  description?: string;
  disposition?: Disposition;
  reputation?: NpcRelationship;
}
