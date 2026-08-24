package com.example;

import com.example.dto.DashboardDto;
import com.example.service.DashboardService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for the dashboard read model.
 *
 * <p>The dashboard summarises a single campaign at a glance: the active campaign, the party's
 * current location, the active characters, the current quests, any encounter in progress, a
 * one-line summary, and the most recent campaign events. Rather than making the front-end perform
 * several independent round trips, this endpoint aggregates everything into a single
 * {@link DashboardDto}, resolving each piece through the existing campaign services so the values
 * always reflect the same persisted state the rest of the app reads.</p>
 *
 * <p>The controller stays thin and only maps the {@code GET /api/campaigns/{campaignId}/dashboard}
 * request onto a single {@link DashboardService} call.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Returns the dashboard state for the given campaign.
     *
     * @param campaignId the owning campaign
     * @return the aggregated dashboard state
     * @throws IllegalArgumentException when no campaign exists with the given id
     */
    @GetMapping("/{campaignId}/dashboard")
    public DashboardDto dashboard(@PathVariable Long campaignId) {
        return dashboardService.dashboard(campaignId);
    }
}
