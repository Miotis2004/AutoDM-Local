package com.example;

import com.example.dto.CampaignDto;
import com.example.dto.CreateCampaignRequest;
import com.example.dto.UpdateCampaignRequest;
import com.example.service.CampaignService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * REST surface for campaign management.
 *
 * <p>This controller is intentionally thin: every method maps a single HTTP verb and
 * path onto exactly one {@link CampaignService} call. It performs no business logic,
 * data access, or game logic. Endpoints cover the full campaign lifecycle — create,
 * edit, archive, delete, and selecting the active campaign.</p>
 *
 * <p>The path is namespaced under {@code /api/campaign-management} so it does not
 * collide with the per-campaign event endpoints served from {@code /api/campaigns}.</p>
 */
@RestController
@RequestMapping("/api/campaign-management")
public class CampaignController {

    private final CampaignService campaigns;

    public CampaignController(CampaignService campaigns) {
        this.campaigns = campaigns;
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @PostMapping
    public CampaignDto create(@RequestBody CreateCampaignRequest request) {
        return campaigns.create(request);
    }

    // ------------------------------------------------------------------
    // Listing and inspection
    // ------------------------------------------------------------------

    @GetMapping
    public List<CampaignDto> list() {
        return campaigns.list();
    }

    @GetMapping("/active")
    public Optional<CampaignDto> getActive() {
        return campaigns.getActive();
    }

    @GetMapping("/{id}")
    public CampaignDto get(@PathVariable Long id) {
        return campaigns.get(id);
    }

    // ------------------------------------------------------------------
    // Edit
    // ------------------------------------------------------------------

    @PutMapping("/{id}")
    public CampaignDto update(@PathVariable Long id, @RequestBody UpdateCampaignRequest request) {
        return campaigns.update(id, request);
    }

    // ------------------------------------------------------------------
    // Archive
    // ------------------------------------------------------------------

    @PostMapping("/{id}/archive")
    public CampaignDto archive(@PathVariable Long id) {
        return campaigns.archive(id);
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        campaigns.delete(id);
    }

    // ------------------------------------------------------------------
    // Active selection
    // ------------------------------------------------------------------

    @PostMapping("/{id}/select")
    public CampaignDto select(@PathVariable Long id) {
        return campaigns.setActive(id);
    }
}
