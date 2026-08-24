/**
 * Front-end view model for the dashboard.
 *
 * <p>Mirrors the back-end {@code DashboardDto} returned by
 * {@code GET /api/campaigns/{campaignId}/dashboard}. The {@link DashboardService} wraps that
 * endpoint and returns one aggregated snapshot of the campaign: the active campaign, the party's
 * current location, the active characters, the current quests, any encounter in progress, a
 * one-line summary, and the most recent campaign events.</p>
 */

/** A compact character projection on the dashboard. */
export interface DashboardCharacter {
  id: number;
  name: string;
  characterClass?: string;
  level: number;
  hitPoints: number;
  maxHitPoints: number;
}

/** A compact quest projection on the dashboard. */
export interface DashboardQuest {
  id: number;
  title: string;
  status: string;
}

/** A compact encounter projection on the dashboard. */
export interface DashboardEncounter {
  id: number;
  name: string;
  status: string;
}

/** A compact campaign-event projection on the dashboard. */
export interface DashboardEvent {
  id: number;
  eventType: string;
  description?: string;
  timestamp: string;
}

/**
 * The full dashboard snapshot for one campaign, as returned by the back-end.
 *
 * <p>Every collection is optional or empty when the campaign has not yet produced that piece of
 * state, so a freshly created campaign still yields a complete, non-crashing dashboard.</p>
 */
export interface DashboardState {
  campaign: {
    id: number;
    title: string;
    description?: string;
    status: string;
    createdAt?: string;
    lastPlayedAt?: string;
    notes?: string;
  };
  location: string | null;
  characters: DashboardCharacter[];
  quests: DashboardQuest[];
  encounter: DashboardEncounter | null;
  summary: string;
  events: DashboardEvent[];
  setupProgress?: SetupProgress;
}

/**
 * A single suggested next step in the campaign setup workflow, as returned by the back-end
 * {@code GET /api/campaigns/{campaignId}/dashboard}. The {@code label} describes what to do and
 * {@code route} is an Angular route the dashboard can link to in order to do it.
 */
export interface SetupNextStep {
  label: string;
  route: string;
}

/**
 * Tracks how far a campaign has progressed through the end-to-end setup workflow: adding a party of
 * player characters, populating the world with regions, locations, and settlements, and adding the
 * NPCs, quests, and inventory items that give the world something to do. Every count is resolved
 * from the same persisted state the rest of the app reads, and {@code nextSteps} points the visitor
 * toward the next gap to fill.
 */
export interface SetupProgress {
  characters: number;
  regions: number;
  locations: number;
  settlements: number;
  npcs: number;
  quests: number;
  items: number;
  nextSteps: SetupNextStep[];
}
