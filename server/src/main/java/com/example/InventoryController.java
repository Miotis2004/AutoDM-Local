package com.example;

import com.example.domain.Campaign;
import com.example.domain.InventoryItem;
import com.example.domain.InventoryOwnerKind;
import com.example.domain.InventoryTransfer;
import com.example.domain.ItemCategory;
import com.example.service.InventoryService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * REST surface for inventory items and ownership.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link InventoryService} call. All business logic — resolving the owning campaign,
 * validating the owner, adjusting stacks, and applying transfers — lives in the
 * service, and persistence is what lets inventory and its transfer history reload
 * across sessions within a campaign.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class InventoryController {

    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    // ------------------------------------------------------------------
    // Holdings: add, list, read
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/inventory")
    public InventoryItem addHolding(@PathVariable Long campaignId,
                                    @RequestBody AddRequest request) {
        return inventory.addHolding(campaignId, request.name, request.category, request.quantity,
                request.value, request.description, request.owner.kind, request.owner.id);
    }

    @GetMapping("/{campaignId}/inventory")
    public List<InventoryItem> listHoldings(@PathVariable Long campaignId,
                                            @RequestParam(required = false) InventoryOwnerKind owner,
                                            @RequestParam(required = false) Long ownerId) {
        return inventory.listHoldings(campaignId, owner, ownerId);
    }

    @GetMapping("/{campaignId}/inventory/{itemId}")
    public InventoryItem getHolding(@PathVariable Long campaignId, @PathVariable Long itemId) {
        return inventory.getHolding(campaignId, itemId);
    }

    // ------------------------------------------------------------------
    // Holdings: quantity and catalogue attributes
    // ------------------------------------------------------------------

    @PatchMapping("/{campaignId}/inventory/{itemId}/quantity")
    public InventoryItem adjustHolding(@PathVariable Long campaignId, @PathVariable Long itemId,
                                       @RequestParam int delta) {
        return inventory.adjustHolding(campaignId, itemId, delta);
    }

    @PutMapping("/{campaignId}/inventory/{itemId}/quantity")
    public InventoryItem setHoldingQuantity(@PathVariable Long campaignId, @PathVariable Long itemId,
                                            @RequestParam int quantity) {
        return inventory.setHoldingQuantity(campaignId, itemId, quantity);
    }

    @PutMapping("/{campaignId}/inventory/{itemId}")
    public InventoryItem updateHolding(@PathVariable Long campaignId, @PathVariable Long itemId,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) ItemCategory category,
                                       @RequestParam(required = false) Integer value,
                                       @RequestParam(required = false) String description) {
        return inventory.updateHolding(campaignId, itemId, name, category, value, description);
    }

    @PatchMapping("/{campaignId}/inventory/{itemId}/equipped")
    public InventoryItem setEquipped(@PathVariable Long campaignId, @PathVariable Long itemId,
                                     @RequestParam boolean value) {
        return inventory.setEquipped(campaignId, itemId, value);
    }

    // ------------------------------------------------------------------
    // Transfers
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/inventory/transfer")
    public InventoryTransfer transfer(@PathVariable Long campaignId,
                                      @RequestBody TransferRequest request) {
        return inventory.transfer(campaignId, request.itemId, request.quantity,
                request.from.kind, request.from.id, request.to.kind, request.to.id);
    }

    @GetMapping("/{campaignId}/inventory/transfers")
    public List<InventoryTransfer> listTransfers(@PathVariable Long campaignId) {
        return inventory.listTransfers(campaignId);
    }

    @GetMapping("/{campaignId}/inventory/transfers/{transferId}")
    public InventoryTransfer getTransfer(@PathVariable Long campaignId,
                                        @PathVariable Long transferId) {
        return inventory.getTransfer(campaignId, transferId);
    }

    @GetMapping("/{campaignId}/inventory/items/{itemId}/transfers")
    public List<InventoryTransfer> listTransfersForItem(@PathVariable Long campaignId,
                                                        @PathVariable Long itemId) {
        return inventory.listTransfersForItem(campaignId, itemId);
    }

    @DeleteMapping("/{campaignId}/inventory/{itemId}")
    public void removeHolding(@PathVariable Long campaignId, @PathVariable Long itemId) {
        inventory.removeHolding(campaignId, itemId);
    }

    // ------------------------------------------------------------------
    // Request bodies (HTTP concern: request shapes)
    // ------------------------------------------------------------------

    record OwnerRef(InventoryOwnerKind kind, Long id) {
    }

    record AddRequest(OwnerRef owner, String name, ItemCategory category, int quantity,
                      Integer value, String description) {
    }

    record TransferRequest(OwnerRef from, OwnerRef to, Long itemId, int quantity) {
    }
}
