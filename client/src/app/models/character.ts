/**
 * Front-end view models for player characters.
 *
 * <p>Mirrors the back-end {@code PlayerCharacter} entity and the character request payloads exposed
 * by {@code /api/campaigns/{campaignId}/characters}. The {@link CharactersService} wraps those
 * endpoints.</p>
 */

/** Ability scores used by a player character's combat profile. */
export interface AbilityScores {
  strength: number;
  dexterity: number;
  constitution: number;
  intelligence: number;
  wisdom: number;
  charisma: number;
}

/**
 * A player character owned by a campaign.
 *
 * <p>The {@link CharactersService} returns these from the back-end; the {@link CampaignStore} keeps
 * the campaign's roster so components read character state from one authoritative place.</p>
 */
export interface Character {
  id: number;
  name: string;
  ancestry?: string;
  characterClass?: string;
  level: number;
  background?: string;
  alignment?: string;
  hitPoints: number;
  maxHitPoints: number;
  armorClass: number;
  movement: number;
  proficiencyBonus: number;
  abilityScores: AbilityScores;
}

/** Request body for creating a new character. */
export interface CreateCharacterRequest {
  name: string;
  level?: number;
  ancestry?: string;
  characterClass?: string;
  background?: string;
  alignment?: string;
}

/** Query/body values applied when patching a character's combat stats. */
export interface UpdateCharacterStatsRequest {
  hitPoints?: number;
  maxHitPoints?: number;
  armorClass?: number;
  movement?: number;
  proficiencyBonus?: number;
}

/** Request body for patching a character's hit points. */
export interface UpdateHitPointsRequest {
  hitPoints: number;
}
