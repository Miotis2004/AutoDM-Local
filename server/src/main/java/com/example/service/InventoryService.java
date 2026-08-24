package com.example.service;

import com.example.db.CampaignRepository;
import com.example.db.InventoryItemRepository;
import com.example.db.InventoryTransferRepository;
import com.example.db.PlayerCharacterRepository;
import com.example.domain.Campaign;
import com.example.domain.InventoryItem;
import com.example.domain.InventoryOwnerKind;
import com.example.domain.PlayerCharacter;
import com.example.domain.InventoryTransfer;
import com.example.domain.ItemCategory;

import com.example.service.CampaignEventService;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for inventory items and their ownership.
 *
 * <p>This service is the single place where inventory holdings are created and
 * consulted, where an item's quantity, value, description, and equipped state are
 * changed, and where transfers of holdings between owners are applied. Every mutation
 * resolves its owning campaign, applies the change to a managed entity, and relies on
 * the repository to persist it, so inventory reloads across sessions within a
 * campaign.</p>
 */
@Service
public class InventoryService {

    private final CampaignRepository campaigns;
    private final PlayerCharacterRepository playerCharacters;
    private final InventoryItemRepository holdings;
    private final InventoryTransferRepository transfers;
    private final CampaignEventService events;
    private final DtoValidator validator;

    public InventoryService(CampaignRepository campaigns,
                            PlayerCharacterRepository playerCharacters,
                            InventoryItemRepository holdings,
                            InventoryTransferRepository transfers,
                            CampaignEventService events,
                            DtoValidator validator) {
        this.campaigns = campaigns;
        this.playerCharacters = playerCharacters;
        this.holdings = holdings;
        this.transfers = transfers;
        this.events = events;
        this.validator = validator;
    }

    // ------------------------------------------------------------------
    // Ownership resolution
    // ------------------------------------------------------------------

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private Long requireOwnerId(InventoryOwnerKind kind, Long ownerId, Long campaignId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Owner id is required");
        }
        if (kind == InventoryOwnerKind.CAMPAIGN) {
            if (!ownerId.equals(campaignId)) {
                throw new IllegalArgumentException(
                        "Campaign-owned inventory must be owned by the campaign itself");
            }
            return ownerId;
        }
        // PLAYER_CHARACTER: the character must exist and belong to this campaign.
        playerCharacters.findById(ownerId).filter(c ->
                c.getCampaign() != null && c.getCampaign().getId() != null
                        && c.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No player character with id " + ownerId + " in campaign " + campaignId));
        return ownerId;
    }

    private InventoryItem requireOwnedHolding(Long campaignId, Long itemId) {
        return holdings.findById(itemId)
                .filter(h -> h.getCampaign() != null && h.getCampaign().getId() != null
                        && h.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No holding with id " + itemId));
    }

    // ------------------------------------------------------------------
    // Holdings
    // ------------------------------------------------------------------

    /**
     * Adds a holding of a named item to the given owner, or, if that owner already
     * holds a stack of the same named item in the campaign, adds to that existing
     * stack. Returns the (new or updated) holding.
     *
     * @param quantity the number of items to add (must be at least one)
     */
    public InventoryItem addHolding(Long campaignId, String name, ItemCategory category,
                                    int quantity, Integer value, String description,
                                    InventoryOwnerKind ownerKind, Long ownerId) {
        validator.requirePositiveQuantity("Item quantity", quantity);
        Campaign campaign = requireCampaign(campaignId);
        Long resolvedId = requireOwnerId(ownerKind, ownerId, campaignId);
        InventoryItem existing = holdings.findByCampaignAndOwnerKindAndOwnerIdAndName(
                campaign, ownerKind, resolvedId, name).orElseGet(
                () -> new InventoryItem(campaign, name, category, 0, 0, null,
                        ownerKind, resolvedId));
        boolean newlyAcquired = existing.getId() == null;
        if (category != null) {
            existing.setCategory(category);
        }
        if (value != null) {
            existing.setValue(value);
        }
        if (description != null) {
            existing.setDescription(description);
        }
        existing.adjust(quantity);
        InventoryItem saved = holdings.save(existing);
        if (newlyAcquired) {
            events.recordItemAcquisition(campaignId, saved.getName(),
                    ownerKind != null ? ownerKind.name() : null, saved.getOwnerId());
        }
        return saved;
    }

    public InventoryItem getHolding(Long campaignId, Long itemId) {
        return requireOwnedHolding(campaignId, itemId);
    }

    /**
     * Lists every holding in the campaign. When {@code ownerKind} is supplied the
     * result is restricted to that owner; otherwise all owners are returned.
     */
    public List<InventoryItem> listHoldings(Long campaignId, InventoryOwnerKind ownerKind,
                                            Long ownerId) {
        Campaign campaign = requireCampaign(campaignId);
        if (ownerKind != null) {
            Long resolvedId = requireOwnerId(ownerKind, ownerId, campaignId);
            return holdings.findByCampaignAndOwnerKindOrderByName(campaign, ownerKind);
        }
        return holdings.findByCampaignOrderByOwnerIdAndName(campaign);
    }

    /**
     * Changes the number of items in a holding by {@code delta}, clamping at zero.
     * Returns the updated holding.
     */
    public InventoryItem adjustHolding(Long campaignId, Long itemId, int delta) {
        InventoryItem holding = requireOwnedHolding(campaignId, itemId);
        // A negative delta would remove more items than the holding contains; the clamp at
        // zero already prevents a negative total, but a negative delta itself is impossible
        // and is refused so callers cannot request an impossible adjustment.
        validator.requireNonNegativeQuantity("Adjustment quantity", delta);
        holding.adjust(delta);
        return holdings.save(holding);
    }

    /**
     * Sets the number of items in a holding to {@code quantity}, clamped at zero.
     * Returns the updated holding.
     */
    public InventoryItem setHoldingQuantity(Long campaignId, Long itemId, int quantity) {
        InventoryItem holding = requireOwnedHolding(campaignId, itemId);
        validator.requireNonNegativeQuantity("Holding quantity", quantity);
        holding.setQuantity(quantity);
        return holdings.save(holding);
    }

    /**
     * Updates any of the mutable catalogue attributes of a holding. Only the fields
     * that are {@code non-null} are changed, so this doubles as a partial update.
     * Returns the updated holding.
     */
    public InventoryItem updateHolding(Long campaignId, Long itemId, String name,
                                       ItemCategory category, Integer value, String description) {
        InventoryItem holding = requireOwnedHolding(campaignId, itemId);
        if (name != null) {
            holding.setName(name);
        }
        if (category != null) {
            holding.setCategory(category);
        }
        if (value != null) {
            holding.setValue(value);
        }
        if (description != null) {
            holding.setDescription(description);
        }
        return holdings.save(holding);
    }

    /**
     * Sets whether a holding is equipped. A campaign-owned stack is never equipped, so
     * equipping such a holding is a no-op that leaves it unchanged. Returns the
     * updated holding.
     */
    public InventoryItem setEquipped(Long campaignId, Long itemId, boolean equipped) {
        InventoryItem holding = requireOwnedHolding(campaignId, itemId);
        if (holding.getOwnerKind() != InventoryOwnerKind.PLAYER_CHARACTER) {
            return holding;
        }
        holding.setEquipped(equipped);
        return holdings.save(holding);
    }

    // ------------------------------------------------------------------
    // Transfers
    // ------------------------------------------------------------------

    /**
     * Moves {@code quantity} of the given holding from one owner to another within the
     * same campaign, recording an immutable {@link InventoryTransfer} for the move.
     *
     * <p>The source holding is reduced by the quantity moved (removed entirely if that
     * empties it); the destination holding is created if necessary and increased by
     * the same quantity. Returns the newly recorded transfer.</p>
     *
     * @param quantity the number of items to transfer (at most the source stack size)
     */
    public InventoryTransfer transfer(Long campaignId, Long itemId, int quantity,
                                      InventoryOwnerKind fromOwnerKind, Long fromOwnerId,
                                      InventoryOwnerKind toOwnerKind, Long toOwnerId) {
        validator.requirePositiveQuantity("Transfer quantity", quantity);
        if (fromOwnerKind == toOwnerKind && fromOwnerId != null
                && fromOwnerId.equals(toOwnerId)) {
            throw new IllegalArgumentException("Cannot transfer an item to itself");
        }
        Campaign campaign = requireCampaign(campaignId);
        InventoryItem holding = requireOwnedHolding(campaignId, itemId);

        Long fromId = requireOwnerId(fromOwnerKind, fromOwnerId, campaignId);
        Long toId = requireOwnerId(toOwnerKind, toOwnerId, campaignId);

        if (holding.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Holding " + itemId + " has only " + holding.getQuantity() + " items");
        }

        InventoryTransfer record = new InventoryTransfer(campaign, holding, quantity,
                fromOwnerKind, fromId, toOwnerKind, toId);

        holding.remove(quantity);
        if (holding.getQuantity() == 0) {
            holdings.delete(holding);
        } else {
            holdings.save(holding);
        }

        InventoryItem destination = holdings.findByCampaignAndOwnerKindAndOwnerIdAndName(
                campaign, toOwnerKind, toId, holding.getName()).orElseGet(
                () -> new InventoryItem(campaign, holding.getName(), holding.getCategory(),
                        0, holding.getValue(), holding.getDescription(), toOwnerKind, toId));
        destination.adjust(quantity);
        holdings.save(destination);

        return transfers.save(record);
    }

    /**
     * Fetches a single recorded transfer, scoped to the campaign so a transfer from
     * another campaign cannot be read this way.
     */
    public InventoryTransfer getTransfer(Long campaignId, Long transferId) {
        Campaign campaign = requireCampaign(campaignId);
        return transfers.findById(transferId)
                .filter(t -> t.getCampaign() != null
                        && t.getCampaign().getId() != null
                        && t.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No transfer with id " + transferId + " in campaign " + campaignId));
    }

    /**
     * Lists the transfer history of the campaign, newest first.
     */
    public List<InventoryTransfer> listTransfers(Long campaignId) {
        return transfers.findByCampaignOrderByIdDesc(requireCampaign(campaignId));
    }

    /**
     * Lists only the transfers that moved the given holding, newest first. When the
     * holding does not belong to the campaign the result is empty.
     */
    public List<InventoryTransfer> listTransfersForItem(Long campaignId, Long itemId) {
        requireOwnedHolding(campaignId, itemId);
        return transfers.findByCampaignOrderByIdDesc(requireCampaign(campaignId)).stream()
                .filter(t -> t.getItem() != null && itemId.equals(t.getItem().getId()))
                .toList();
    }

    /**
     * Deletes the given holding from the campaign.
     */
    public void removeHolding(Long campaignId, Long itemId) {
        holdings.delete(requireOwnedHolding(campaignId, itemId));
    }
}
