/**
 * Front-end view models for quests and their objectives.
 *
 * <p>Mirrors the back-end {@code Quest} and {@code Objective} entities exposed by
 * {@code /api/campaigns/{campaignId}/quests}. The {@link QuestsService} wraps those endpoints.</p>
 */

/** The durable status of a quest, matching the back-end {@code QuestStatus} enum. */
export enum QuestStatus {
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}

/** The completion state of a single {@link Objective}. */
export enum ObjectiveStatus {
  INCOMPLETE = 'INCOMPLETE',
  COMPLETE = 'COMPLETE',
}

/**
 * One tracked objective within a {@link Quest}.
 *
 * <p>Its {@link currentCount} against {@link targetCount} derives an {@link ObjectiveStatus}
 * independently of its siblings.</p>
 */
export interface Objective {
  id: number;
  description: string;
  targetCount: number;
  currentCount: number;
  status: ObjectiveStatus;
}

/**
 * A quest - a strand of campaign story - plus its tracked objectives.
 *
 * <p>The {@link QuestsService} returns these from the back-end; the {@link CampaignStore} keeps the
 * campaign's quest list so components read quest state from one authoritative place.</p>
 */
export interface Quest {
  id: number;
  title: string;
  description?: string;
  status: QuestStatus;
  giver?: string;
  rewards?: string;
  notes?: string;
  relatedLocations?: number[];
  objectives?: Objective[];
}

/** Request body for creating a new quest. */
export interface CreateQuestRequest {
  title: string;
  description?: string;
  giver?: string;
  rewards?: string;
  notes?: string;
}

/** Request body for creating one objective on a quest. */
export interface CreateObjectiveRequest {
  description: string;
  targetCount?: number;
}
