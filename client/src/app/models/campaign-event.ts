/**
 * Front-end view models for campaign events and their types.
 *
 * <p>Mirrors the back-end {@code CampaignEvent} entity and {@code CampaignEventType} enum exposed
 * by {@code /api/campaigns/{campaignId}/events}. The {@link CampaignEventsService} wraps those
 * endpoints.</p>
 */

/** The kind of significant moment a {@link CampaignEvent} records. */
export enum CampaignEventType {
  SESSION_START = 'SESSION_START',
  LOCATION_ENTRY = 'LOCATION_ENTRY',
  DISCOVERY = 'DISCOVERY',
  COMBAT = 'COMBAT',
  DAMAGE = 'DAMAGE',
  ITEM_ACQUISITION = 'ITEM_ACQUISITION',
  QUEST_CHANGE = 'QUEST_CHANGE',
  RELATIONSHIP_CHANGE = 'RELATIONSHIP_CHANGE',
  STANDING_CHANGE = 'STANDING_CHANGE',
  SESSION_END = 'SESSION_END',
  REST = 'REST',
  GAME_ACTION = 'GAME_ACTION',
}

/**
 * A single significant event within a campaign.
 *
 * <p>The {@link CampaignEventsService} returns these from the back-end; the {@link CampaignStore}
 * keeps the campaign's event list so components read campaign history from one authoritative
 * place.</p>
 */
export interface CampaignEvent {
  id: number;
  eventType: CampaignEventType;
  timestamp: string;
  description?: string;
  details?: string;
}
