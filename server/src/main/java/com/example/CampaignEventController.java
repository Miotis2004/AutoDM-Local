package com.example;

import com.example.domain.CampaignEvent;
import com.example.domain.CampaignEventType;
import com.example.service.CampaignEventService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST surface for persistent campaign events.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link CampaignEventService} call. Recording and history logic lives in the service,
 * and persistence is what lets the campaign's event history reload across sessions.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class CampaignEventController {

    private final CampaignEventService events;

    public CampaignEventController(CampaignEventService events) {
        this.events = events;
    }

    // ------------------------------------------------------------------
    // Recording
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/events")
    public CampaignEvent recordEvent(@PathVariable Long campaignId,
                                     @RequestParam CampaignEventType type,
                                     @RequestParam(required = false) String description) {
        return events.recordEvent(campaignId, type, description);
    }

    // ------------------------------------------------------------------
    // Listing and inspection
    // ------------------------------------------------------------------

    @GetMapping("/{campaignId}/events")
    public List<CampaignEvent> listEvents(@PathVariable Long campaignId) {
        return events.listEvents(campaignId);
    }

    @GetMapping("/{campaignId}/events/by-type")
    public List<CampaignEvent> listEventsByType(@PathVariable Long campaignId,
                                                @RequestParam CampaignEventType type) {
        return events.listEventsByType(campaignId, type);
    }

    @GetMapping("/{campaignId}/events/{eventId}")
    public CampaignEvent getEvent(@PathVariable Long campaignId, @PathVariable Long eventId) {
        return events.getEvent(campaignId, eventId);
    }
}
