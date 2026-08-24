/**
 * Front-end view models for the Dungeon Master engine.
 *
 * <p>Mirrors the back-end {@code EngineResponse} and {@code SceneBrief} values returned by
 * {@code /api/campaigns/{campaignId}/scenes/{sceneId}/action} (and the scene-advancement endpoint).
 * The {@link DungeonMasterService} wraps those endpoints.</p>
 */

/**
 * A compact snapshot of the active scene, as presented by the engine before resolving an action.
 *
 * <p>Mirrors the back-end {@code SceneBrief} record: the scene identity and narrative plus the
 * names of the characters and combatants present.</p>
 */
export interface SceneBrief {
  sceneId: number;
  sceneTitle: string;
  sceneNarrative: string;
  involvedNames: string[];
  combatants: string[];
}

/** A resolved ability check backing a player action, or absent when none applies. */
export interface AbilityCheckResult {
  statistic: string;
  modifier: number;
  roll: number;
  total: number;
  difficulty: number;
  success: boolean;
}

/** A pending hit-point change the world should apply when an action resolves. */
export interface StateChange {
  combatantId?: number;
  hitPointsChange: number;
}

/**
 * The complete result of running one player action through the Dungeon Master engine.
 *
 * <p>Mirrors the back-end {@code EngineResponse} record: the presented {@link SceneBrief}, whether
 * the action was recognised, any validation errors, the resolved ability check, the narrative
 * response the players see, the pending {@link StateChange}, and the world effects the action
 * triggered.</p>
 */
export interface EngineResponse {
  scene: SceneBrief;
  recognized: boolean;
  validationErrors: string[];
  check?: AbilityCheckResult;
  response: string;
  stateChange?: StateChange;
  effects: string[];
}

/** The action parameters accepted by {@code POST /api/campaigns/{campaignId}/scenes/{sceneId}/action}. */
export interface PlayerActionRequest {
  action: string;
  statistic?: number;
  modifier?: number;
  difficulty?: number;
}

/** The parameters accepted by {@code POST /api/campaigns/{campaignId}/scenes/advance}. */
export interface AdvanceSceneRequest {
  campaignId: number;
}
