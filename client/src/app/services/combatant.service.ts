import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Combatant, CombatantKind, EnemyActionOutcome } from '../models/encounter';

/**
 * The front-end client for the back-end combatant system.
 *
 * <p>This service wraps {@code /api/campaigns/{campaignId}/combatants} and the encounter-turn
 * endpoints: creating combatants, joining them to an encounter, building and advancing the turn
 * order, applying damage and healing, and detecting when an encounter is complete. It exists so the
 * encounter surface can drive a fight turn by turn. The {@link CampaignStore} owns the authoritative
 * campaign data; this service only performs the HTTP round trips.</p>
 */
@Injectable({ providedIn: 'root' })
export class CombatantService {
  private readonly http = inject(HttpClient);

  /**
   * Creates a combatant in the campaign.
   *
   * @param campaignId the owning campaign
   * @param name the combatant's name
   * @param kind whether the combatant is a hero or an enemy
   * @param hitPoints the combatant's current hit points
   * @param maxHitPoints the combatant's maximum hit points
   * @return the newly created combatant
   */
  create(
    campaignId: number,
    name: string,
    kind: CombatantKind,
    hitPoints: number,
    maxHitPoints: number,
  ): Observable<Combatant> {
    let params = new HttpParams()
      .append('name', name)
      .append('kind', kind)
      .append('hitPoints', String(hitPoints))
      .append('maxHitPoints', String(maxHitPoints));
    return this.http.post<Combatant>(
      `/api/campaigns/${campaignId}/combatants`,
      {},
      { params },
    );
  }

  /**
   * Joins an existing combatant to an encounter so it takes part in the fight.
   *
   * @param campaignId the owning campaign
   * @param combatantId the combatant to join
   * @param encounterId the encounter to join
   * @return the joined combatant
   */
  join(
    campaignId: number,
    combatantId: number,
    encounterId: number,
  ): Observable<Combatant> {
    return this.http.post<Combatant>(
      `/api/campaigns/${campaignId}/combatants/${combatantId}/encounters/${encounterId}/join`,
      {},
    );
  }

  /**
   * Builds the turn order for an encounter: every living participant gets a 1-based position ordered
   * by descending initiative.
   *
   * @param campaignId the owning campaign
   * @param encounterId the encounter whose order to build
   * @return the combatants in turn order
   */
  buildTurnOrder(campaignId: number, encounterId: number): Observable<Combatant[]> {
    return this.http.post<Combatant[]>(
      `/api/campaigns/${campaignId}/encounters/${encounterId}/turn-order`,
      {},
    );
  }

  /**
   * Advances the encounter to the next turn, moving the current turn position around the round.
   *
   * @param campaignId the owning campaign
   * @param encounterId the encounter whose turn to advance
   * @return the encounter with its refreshed current turn
   */
  nextTurn(campaignId: number, encounterId: number): Observable<EncounterTurn> {
    return this.http.post<EncounterTurn>(
      `/api/campaigns/${campaignId}/encounters/${encounterId}/next-turn`,
      {},
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param encounterId the encounter to inspect
   * @return the combatant whose turn it is now, when a turn has been reached
   */
  currentCombatant(
    campaignId: number,
    encounterId: number,
  ): Observable<Combatant | null> {
    return this.http.get<Combatant | null>(
      `/api/campaigns/${campaignId}/encounters/${encounterId}/current-combatant`,
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param encounterId the encounter to check
   * @return {@code true} once every combatant on at least one side has been defeated
   */
  isComplete(campaignId: number, encounterId: number): Observable<boolean> {
    return this.http.get<boolean>(
      `/api/campaigns/${campaignId}/encounters/${encounterId}/complete`,
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param encounterId the encounter to inspect
   * @return the side still standing when the encounter is over, when determinable
   */
  winner(campaignId: number, encounterId: number): Observable<CombatantKind | null> {
    return this.http.get<CombatantKind | null>(
      `/api/campaigns/${campaignId}/encounters/${encounterId}/winner`,
    );
  }

  /**
   * Applies damage to a combatant, clamping hit points at zero and marking it defeated at zero.
   *
   * @param campaignId the owning campaign
   * @param combatantId the combatant to damage
   * @param delta the damage to apply (a positive number reduces hit points)
   * @return the damaged combatant
   */
  applyDamage(
    campaignId: number,
    combatantId: number,
    delta: number,
  ): Observable<Combatant> {
    let params = new HttpParams().append('delta', String(delta));
    return this.http.post<Combatant>(
      `/api/campaigns/${campaignId}/combatants/${combatantId}/damage`,
      {},
      { params },
    );
  }

  /**
   * Heals a combatant, restoring hit points up to the maximum.
   *
   * @param campaignId the owning campaign
   * @param combatantId the combatant to heal
   * @param amount the hit points to restore
   * @return the healed combatant
   */
  heal(campaignId: number, combatantId: number, amount: number): Observable<Combatant> {
    let params = new HttpParams().append('amount', String(amount));
    return this.http.post<Combatant>(
      `/api/campaigns/${campaignId}/combatants/${combatantId}/heal`,
      {},
      { params },
    );
  }

  /**
   * Drives an enemy's attack against the living targets of its opposition.
   *
   * @param campaignId the owning campaign
   * @param combatantId the enemy taking the attack
   * @param attackBonus the enemy's attack bonus added to its d20 roll (defaults to 0)
   * @param damage the damage applied when the attack lands (defaults to 0)
   * @param difficulty the roll total required to land (defaults to 10)
   * @param damageType the kind of damage the attack deals (defaults to PHYSICAL)
   * @return the outcome describing what happened
   */
  enemyAttack(
    campaignId: number,
    combatantId: number,
    attackBonus = 0,
    damage = 0,
    difficulty = 10,
  ): Observable<EnemyActionOutcome> {
    let params = new HttpParams()
      .append('attackBonus', String(attackBonus))
      .append('damage', String(damage))
      .append('difficulty', String(difficulty));
    return this.http.post<EnemyActionOutcome>(
      `/api/campaigns/${campaignId}/combatants/${combatantId}/attack`,
      {},
      { params },
    );
  }

  /**
   * @param campaignId the owning campaign
   * @param encounterId the encounter to inspect
   * @return every combatant taking part in the encounter, ordered by turn order when known
   */
  listOfEncounter(
    campaignId: number,
    encounterId: number,
  ): Observable<Combatant[]> {
    return this.http.get<Combatant[]>(
      `/api/campaigns/${campaignId}/encounters/${encounterId}/combatants`,
    );
  }
}

/**
 * The encounter plus whose turn it is now, as returned by the next-turn endpoint.
 */
export interface EncounterTurn {
  id: number;
  status: string;
  currentTurn: number | null;
}
