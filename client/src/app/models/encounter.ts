/**
 * Front-end view models for encounters, combatants, and the turn order they form.
 *
 * <p>Mirrors the back-end {@code Encounter}, {@code Combatant}, {@code EncounterStatus}, and
 * {@code CombatantKind} entities exposed by {@code /api/campaigns/{campaignId}/encounters} and
 * {@code /api/campaigns/{campaignId}/combatants}. The {@link EncounterService} and
 * {@link CombatantService} wrap those endpoints.</p>
 */

/** The lifecycle state of an encounter, matching the back-end {@code EncounterStatus} enum. */
export enum EncounterStatus {
  /** The encounter has been created but not yet started. */
  SCHEDULED = 'SCHEDULED',
  /** The encounter is currently in progress. */
  ACTIVE = 'ACTIVE',
  /** The encounter has been finished. */
  FINISHED = 'FINISHED',
}

/**
 * The side a combatant fights for, matching the back-end {@code CombatantKind} enum.
 *
 * <p>Player combatants are the party's heroes; enemy combatants are the adversaries they face.
 * This is what the encounter surface uses to separate the two columns and to decide the winner.</p>
 */
export enum CombatantKind {
  /** A hero of the party. */
  PLAYER = 'PLAYER',
  /** An adversary. */
  ENEMY = 'ENEMY',
}

/**
 * A single participant in an encounter: a hero or an enemy.
 *
 * <p>The {@link CombatantService} returns these from the back-end. {@link hitPoints} and
 * {@link maxHitPoints} drive the health shown on the encounter surface; {@link order} and
 * {@link initiative} establish the turn order, which the surface renders in {@link order} position.</p>
 */
export interface Combatant {
  id: number;
  name: string;
  kind: CombatantKind;
  hitPoints: number;
  maxHitPoints: number;
  initiative?: number | null;
  order?: number | null;
  defeated: boolean;
}

/**
 * An encounter: the container that turns combat into something trackable.
 *
 * <p>The {@link EncounterService} returns these from the back-end. {@link status} names whether it
 * is scheduled, active, or finished, and {@link currentTurn} names whose turn-order slot it is now.
 * The surface uses these to drive start, progress, and completion.</p>
 */
export interface Encounter {
  id: number;
  name?: string;
  status: EncounterStatus;
  currentTurn?: number | null;
  sceneId?: number;
  locationId?: number;
  createdAt?: string;
}

/**
 * The outcome of a single enemy action, as returned by the enemy-attack endpoint.
 *
 * <p>The encounter surface renders this as a combat event: what the enemy did, whether its attack
 * landed, the roll and difficulty, the damage applied, and whether the target fell.</p>
 */
export interface EnemyActionOutcome {
  actionTaken: boolean;
  attacker: Combatant;
  target: Combatant | null;
  hit: boolean;
  attackRollTotal: number;
  difficulty: number;
  damageApplied: number;
  targetDefeated: boolean;
  damageType: string;
}
