/**
 * Barrel for the front-end HTTP services and the campaign state store.
 *
 * <p>Importing from {@code /services} gives features a single, stable surface: the services that
 * wrap the back-end REST endpoints and the {@link CampaignStore} that owns the authoritative
 * campaign state. The individual services and their models remain importable from their own files
 * when a feature needs a specific type.</p>
 */

export { CampaignsService } from './campaigns.service';
export { CharactersService } from './characters.service';
export { NpcsService } from './npcs.service';
export { WorldService } from './world.service';
export { QuestsService } from './quests.service';
export { ItemsService } from './items.service';
export { SessionsService } from './sessions.service';
export { FactionsService } from './faction.service';
export { CreatureTemplatesService } from './creature-template.service';
export { CampaignEventsService } from './campaign-event.service';
export { DungeonMasterService } from './dungeon-master.service';
export { SceneService } from './scene.service';
export { DashboardService } from './dashboard.service';
export { CampaignStore } from './campaign-store.service';
