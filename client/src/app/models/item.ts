/**
 * Front-end view models for inventory items and ownership.
 *
 * <p>Mirrors the back-end {@code InventoryItem} and {@code InventoryTransfer} entities exposed by
 * {@code /api/campaigns/{campaignId}/inventory}. The {@link ItemsService} wraps those endpoints.</p>
 */

/** The category of an inventory item, matching the back-end {@code ItemCategory} enum. */
export enum ItemCategory {
  WEAPON = 'WEAPON',
  ARMOR = 'ARMOR',
  CONSUMABLE = 'CONSUMABLE',
  QUEST_ITEM = 'QUEST_ITEM',
  MISCELLANEOUS = 'MISCELLANEOUS',
}

/** Who holds an inventory holding, matching the back-end {@code InventoryOwnerKind}. */
export enum InventoryOwnerKind {
  /** The shared, campaign-wide stash. */
  CAMPAIGN = 'CAMPAIGN',
  /** A single player character's goods. */
  PLAYER = 'PLAYER',
}

/**
 * One inventory holding: a category, quantity, value, equipped state, description, and its owner.
 *
 * <p>The {@link ItemsService} returns these from the back-end; the {@link CampaignStore} keeps the
 * campaign's inventory so components read item state from one authoritative place.</p>
 */
export interface InventoryItem {
  id: number;
  name: string;
  category: ItemCategory;
  quantity: number;
  value: number;
  equipped: boolean;
  description?: string;
  ownerKind: InventoryOwnerKind;
  ownerId: number;
}

/** An immutable hand-off of a holding between two owners. */
export interface InventoryTransfer {
  id: number;
  itemId: number;
  itemName: string;
  fromOwnerKind: InventoryOwnerKind;
  fromOwnerId: number;
  toOwnerKind: InventoryOwnerKind;
  toOwnerId: number;
  quantity: number;
  timestamp?: string;
}

/** Request body for adding an item to an owner's inventory. */
export interface CreateInventoryItemRequest {
  name: string;
  category: ItemCategory;
  quantity?: number;
  value?: number;
  description?: string;
  ownerKind: InventoryOwnerKind;
  ownerId: number;
}

/** Request body for transferring a quantity of an item between owners. */
export interface TransferInventoryItemRequest {
  itemId: number;
  fromOwnerId: number;
  fromOwnerKind: InventoryOwnerKind;
  toOwnerId: number;
  toOwnerKind: InventoryOwnerKind;
  quantity: number;
}
