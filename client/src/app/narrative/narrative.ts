/**
 * Front-end view models for the narrative template system.
 *
 * <p>The back-end renders every moment of play - DM narration, a player action, a dice roll, a
 * combat beat, or any other system event - into a single structured line through the narrative
 * templates. This is that line's shape on the client, mirroring
 * {@code server/.../domain/NarrativeEntry}. The game log consumes {@link NarrativeEntry} values and
 * renders them; nothing here owns narrative rules, it only renders what the back-end produced.</p>
 */

/** The five categories a {@link NarrativeEntry} can belong to, matching the back-end enum. */
export enum NarrativeCategory {
  /** The Dungeon Master's descriptive narration of a scene or moment. */
  DM_NARRATION = 'DM_NARRATION',
  /** A player action and the mechanical verdict the engine returned for it. */
  PLAYER_ACTION = 'PLAYER_ACTION',
  /** A dice roll and its total and outcome. */
  DICE_RESULT = 'DICE_RESULT',
  /** A combat beat: an attack, damage, healing, or similar combat moment. */
  COMBAT_EVENT = 'COMBAT_EVENT',
  /** Any other campaign or system moment recorded through the event system. */
  SYSTEM_EVENT = 'SYSTEM_EVENT',
}

/** Human-readable label shown in the game log header for each category. */
export const CATEGORY_LABELS: Record<NarrativeCategory, string> = {
  [NarrativeCategory.DM_NARRATION]: 'DM narration',
  [NarrativeCategory.PLAYER_ACTION]: 'Player action',
  [NarrativeCategory.DICE_RESULT]: 'Dice roll',
  [NarrativeCategory.COMBAT_EVENT]: 'Combat',
  [NarrativeCategory.SYSTEM_EVENT]: 'System',
};

/**
 * One structured line for the game log, as produced by a back-end narrative template.
 *
 * <p>This is exactly what the game log consumes. {@link category} groups the line, {@link message}
 * is the readable line shown, {@link title} is an optional header, {@link timestamp} records when
 * the moment occurred, and {@link data} is the structured game state the line was rendered from so
 * the log (or a future view) can render whatever it likes.</p>
 */
export interface NarrativeEntry {
  category: NarrativeCategory;
  title: string;
  message: string;
  timestamp: string | null;
  data: Record<string, unknown>;
}
