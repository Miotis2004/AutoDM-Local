package com.example;

import com.example.domain.RestOutcome;
import com.example.service.RestService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for short and long rest operations.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link RestService} call. All game logic — restoring health, clearing temporary
 * conditions, recovering limited-use resources, and advancing campaign and session
 * state — lives in the service. The single endpoint accepts a campaign id and a rest
 * type (a long rest or a short rest) and returns a {@link RestOutcome} describing what
 * the rest accomplished.</p>
 */
@RestController
@RequestMapping("/api/rest")
public class RestOperationsController {

    private final RestService rest;

    public RestOperationsController(RestService rest) {
        this.rest = rest;
    }

    /**
     * Takes a rest for the given campaign.
     *
     * @param campaignId the campaign that rests
     * @param type       the rest type: {@code "long"} for a long rest (the default) or
     *                   {@code "short"} for a short rest
     * @return a {@link RestOutcome} describing what the rest accomplished
     */
    @PostMapping
    public RestOutcome takeRest(@RequestParam Long campaignId,
                                @RequestParam(defaultValue = "long") String type) {
        return rest.takeRest(campaignId, "long".equalsIgnoreCase(type));
    }
}
