/**
 * Front-end view models for scenes.
 *
 * <p>Mirrors the back-end {@code Scene} entity and {@code SceneStatus} enum exposed by
 * {@code /api/campaigns/{campaignId}/scenes}. The {@link SceneService} wraps those endpoints.</p>
 */

/** The lifecycle state of a scene, matching the back-end {@code SceneStatus} enum. */
export enum SceneStatus {
  /** The scene has been created but is not yet in focus. */
  READY = 'READY',
  /** The scene is the one currently in focus. */
  ACTIVE = 'ACTIVE',
  /** The scene's play has finished. */
  COMPLETED = 'COMPLETED',
}

/**
 * A single, contiguous slice of in-game time owned by a campaign: a title, a free-form narrative,
 * the location it takes place in, the encounter it references when combat is in progress, and its
 * lifecycle status.
 *
 * <p>The {@link SceneService} returns these from the back-end. The play screen uses {@link status}
 * to find the scene currently in focus, which supplies the scene id that {@link DungeonMasterService}
 * needs to resolve a player action.</p>
 */
export interface Scene {
  id: number;
  title: string;
  narrative?: string;
  locationId?: number;
  encounterId?: number;
  status: SceneStatus;
  createdAt?: string;
}
