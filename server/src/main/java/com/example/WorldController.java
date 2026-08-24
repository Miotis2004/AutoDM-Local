package com.example;

import com.example.domain.Location;
import com.example.domain.PointOfInterest;
import com.example.domain.PointOfInterestCategory;
import com.example.domain.PartyLocation;
import com.example.domain.Region;
import com.example.domain.Settlement;
import com.example.domain.SettlementType;
import com.example.domain.TravelRoute;
import com.example.dto.TravelPlan;
import com.example.service.WorldService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * REST surface for persistent world state.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link WorldService} call. All world construction, exploration, and travel logic
 * lives in the service, and persistence is what lets the world reload across sessions.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class WorldController {

    private final WorldService world;

    public WorldController(WorldService world) {
        this.world = world;
    }

    // ------------------------------------------------------------------
    // Regions
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/regions")
    public Region addRegion(@PathVariable Long campaignId,
                            @RequestParam String name,
                            @RequestParam(required = false) String description) {
        return world.addRegion(campaignId, name, description);
    }

    @GetMapping("/{campaignId}/regions")
    public List<Region> listRegions(@PathVariable Long campaignId) {
        return world.listRegions(campaignId);
    }

    // ------------------------------------------------------------------
    // Locations
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/locations")
    public Location addLocation(@PathVariable Long campaignId,
                                @RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) Long regionId,
                                @RequestParam(required = false) Double latitude,
                                @RequestParam(required = false) Double longitude) {
        return world.addLocation(campaignId, name, description, regionId, latitude, longitude);
    }

    @GetMapping("/{campaignId}/locations")
    public List<Location> listLocations(@PathVariable Long campaignId) {
        return world.listLocations(campaignId);
    }

    @GetMapping("/{campaignId}/locations/discovered")
    public List<Location> listDiscoveredLocations(@PathVariable Long campaignId) {
        return world.listDiscoveredLocations(campaignId);
    }

    @PostMapping("/{campaignId}/locations/{locationId}/discover")
    public Location discoverLocation(@PathVariable Long campaignId, @PathVariable Long locationId) {
        return world.discoverLocation(campaignId, locationId);
    }

    // ------------------------------------------------------------------
    // Settlements
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/settlements")
    public Settlement addSettlement(@PathVariable Long campaignId,
                                    @RequestParam String name,
                                    @RequestParam(required = false) String description,
                                    @RequestParam SettlementType type,
                                    @RequestParam(defaultValue = "0") int population,
                                    @RequestParam(required = false) Long regionId) {
        return world.addSettlement(campaignId, name, description, type, population, regionId);
    }

    @GetMapping("/{campaignId}/settlements")
    public List<Settlement> listSettlements(@PathVariable Long campaignId) {
        return world.listSettlements(campaignId);
    }

    // ------------------------------------------------------------------
    // Points of interest
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/points-of-interest")
    public PointOfInterest addPointOfInterest(@PathVariable Long campaignId,
                                              @RequestParam String name,
                                              @RequestParam(required = false) String description,
                                              @RequestParam PointOfInterestCategory category,
                                              @RequestParam(required = false) Long settlementId,
                                              @RequestParam(required = false) Long regionId) {
        return world.addPointOfInterest(campaignId, name, description, category, settlementId, regionId);
    }

    @GetMapping("/{campaignId}/points-of-interest")
    public List<PointOfInterest> listPointsOfInterest(@PathVariable Long campaignId) {
        return world.listPointsOfInterest(campaignId);
    }

    // ------------------------------------------------------------------
    // Travel routes
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/routes")
    public TravelRoute addTravelRoute(@PathVariable Long campaignId,
                                      @RequestParam Long fromId,
                                      @RequestParam Long toId,
                                      @RequestParam(required = false) Double distanceKm,
                                      @RequestParam(required = false) Integer travelMinutes) {
        return world.addTravelRoute(campaignId, fromId, toId, distanceKm, travelMinutes);
    }

    @GetMapping("/{campaignId}/routes")
    public List<TravelRoute> listTravelRoutes(@PathVariable Long campaignId) {
        return world.listTravelRoutes(campaignId);
    }

    @DeleteMapping("/{campaignId}/routes/{routeId}")
    public void removeTravelRoute(@PathVariable Long campaignId, @PathVariable Long routeId) {
        world.removeTravelRoute(routeId);
    }

    @GetMapping("/{campaignId}/routes/{fromId}/to/{toId}/plan")
    public TravelPlan planRoute(
            @PathVariable Long campaignId, @PathVariable Long fromId, @PathVariable Long toId) {
        return world.computeTravel(campaignId, fromId, toId);
    }

    // ------------------------------------------------------------------
    // Current party location
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/party-location")
    public PartyLocation setPartyLocation(@PathVariable Long campaignId,
                                          @RequestParam Long locationId) {
        return world.setPartyLocation(campaignId, locationId);
    }

    @GetMapping("/{campaignId}/party-location")
    public Optional<PartyLocation> findPartyLocation(@PathVariable Long campaignId) {
        return world.findPartyLocation(campaignId);
    }
}
