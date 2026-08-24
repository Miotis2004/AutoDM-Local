/**
 * Front-end view models for campaign management.
 *
 * <p>These mirror the back-end {@code CampaignDto} and its request payloads. Campaigns live under
 * {@code /api/campaign-management}; the {@link CampaignsService} wraps those endpoints and the
 * {@link CampaignStore} holds the single authoritative view of the active campaign so the rest of
 * the application never duplicates campaign state.</p>
 */

/** Lifecycle state of a campaign, matching the back-end {@code CampaignStatus} enum. */
export enum CampaignStatus {
  /** Not yet started. */
  DRAFT = 'DRAFT',
  /** Currently in play. */
  IN_PROGRESS = 'IN_PROGRESS',
  /** Finished. */
  COMPLETED = 'COMPLETED',
  /** Archived and no longer active. */
  ARCHIVED = 'ARCHIVED',
}

/**
 * A campaign, as returned by the back-end {@code CampaignDto}.
 *
 * <p>Carries only the public metadata - id, title, description, status, dates and notes - not the
 * internal game state, which lives elsewhere in the {@link CampaignStore}.</p>
 */
export interface Campaign {
  id: number;
  title: string;
  description: string;
  status: CampaignStatus;
  createdAt: string;
  lastPlayedAt: string;
  notes: string;
}

/** Request body for creating a new campaign (title required; the rest optional). */
export interface CreateCampaignRequest {
  title: string;
  description?: string;
  status?: CampaignStatus;
  notes?: string;
}

/**
 * Request body for updating an existing campaign.
 *
 * <p>Every field is optional; a field that is left {@code null} is left untouched on the back-end.</p>
 */
export interface UpdateCampaignRequest {
  title?: string;
  description?: string;
  status?: CampaignStatus;
  lastPlayedAt?: string;
  notes?: string;
}
