/**
 * Front-end view models for creature and enemy templates.
 *
 * <p>Mirrors the back-end {@code CreatureTemplate} entity exposed by
 * {@code /api/campaigns/{campaignId}/creature-templates}. The {@link CreatureTemplatesService}
 * wraps those endpoints.</p>
 */

/**
 * A reusable creature or enemy blueprint.
 *
 * <p>The {@link CreatureTemplatesService} returns these from the back-end; the {@link CampaignStore}
 * keeps the campaign's catalogue of templates so components read template state from one
 * authoritative place.</p>
 */
export interface CreatureTemplate {
  id: number;
  name: string;
  description?: string;
  /** Hit points the creature starts each fight with. Absent when unset. */
  health?: number;
  /** Armor class or equivalent defence. Absent when unset. */
  defense?: number;
  /** Attack bonus. Absent when unset. */
  attack?: number;
  /** Damage output of the creature's primary attack. Absent when unset. */
  damage?: number;
  /** Modifier applied when the creature rolls initiative. Absent when unset. */
  initiativeModifier?: number;
  /** Free-form tactical or roleplay guidance for running the creature. */
  behaviorNotes?: string;
}

/** Request body for creating a new creature template. */
export interface CreateCreatureTemplateRequest {
  name: string;
  description?: string;
  health?: number;
  defense?: number;
  attack?: number;
  damage?: number;
  initiativeModifier?: number;
  behaviorNotes?: string;
}

/** Request body for updating a creature template's fields. */
export interface UpdateCreatureTemplateRequest {
  description?: string;
  health?: number;
  defense?: number;
  attack?: number;
  damage?: number;
  initiativeModifier?: number;
  behaviorNotes?: string;
}
