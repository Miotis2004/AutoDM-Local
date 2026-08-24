/**
 * Front-end view models for non-player characters.
 *
 * <p>Mirrors the back-end {@code Npc} entity and the NPC request payloads exposed by
 * {@code /api/campaigns/{campaignId}/npcs}. The {@link NpcsService} wraps those endpoints.</p>
 */

/** A disposition toward the party, matching the back-end {@code Disposition} enum. */
export enum Disposition {
  FRIENDLY = 'FRIENDLY',
  NEUTRAL = 'NEUTRAL',
  HOSTILE = 'HOSTILE',
}

/** The relationship of an NPC to the party, matching the back-end {@code NpcRelationship}. */
export enum NpcRelationship {
  ALLEY = 'ALLEY',
  FRIEND = 'FRIEND',
  NEUTRAL = 'NEUTRAL',
  FOE = 'FOE',
}

/**
 * A non-player character owned by a campaign.
 *
 * <p>The {@link NpcsService} returns these from the back-end; the {@link CampaignStore} keeps the
 * campaign's NPC list so components read NPC state from one authoritative place.</p>
 */
export interface Npc {
  id: number;
  name: string;
  description?: string;
  role?: string;
  disposition?: Disposition;
  faction?: string;
  location?: string;
  active: boolean;
  relationship?: NpcRelationship;
  notes?: string;
  /** Optional combat profile; absent for purely social NPCs. */
  combatStats?: NpcCombatStats;
}

/** The optional combat profile carried by an {@link Npc}. */
export interface NpcCombatStats {
  hitPoints: number;
  maxHitPoints: number;
  armorClass: number;
  movement: number;
  proficiencyBonus: number;
  attack: number;
  damage: number;
  initiativeBonus: number;
}

/** Request body for creating a new NPC. */
export interface CreateNpcRequest {
  name: string;
  description?: string;
  role?: string;
  disposition?: Disposition;
  faction?: string;
  location?: string;
  notes?: string;
}

/** Request body for patching an NPC's relationship toward the party. */
export interface UpdateNpcRelationshipRequest {
  relationship: NpcRelationship;
}
