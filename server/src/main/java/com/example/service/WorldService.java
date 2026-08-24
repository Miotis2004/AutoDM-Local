package com.example.service;

import com.example.domain.Campaign;
import com.example.domain.Location;
import com.example.domain.PointOfInterest;
import com.example.domain.PointOfInterestCategory;
import com.example.domain.PartyLocation;
import com.example.domain.Region;
import com.example.domain.Settlement;
import com.example.domain.SettlementType;
import com.example.domain.TravelRoute;
import com.example.db.CampaignRepository;
import com.example.db.LocationRepository;
import com.example.db.PointOfInterestRepository;
import com.example.db.PartyLocationRepository;
import com.example.db.RegionRepository;
import com.example.db.SettlementRepository;
import com.example.db.TravelRouteRepository;
import com.example.dto.TravelPlan;
import com.example.service.CampaignEventService;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.PriorityQueue;

/**
 * Business and world logic for the world model owned by a campaign: regions,
 * locations, settlements, points of interest, travel routes, and the party's current
 * location.
 *
 * <p>This service is the single place where the world is created, explored, and
 * traversed. Every mutation resolves its owning campaign, applies the change to a
 * managed entity, and relies on the repositories to persist it, so the world reloads
 * across sessions. The {@link Location} is the canonical place node: settlements and
 * points of interest each reference one, routes connect two, and the party location
 * points at one.</p>
 */
@Service
public class WorldService {

    private final CampaignRepository campaigns;
    private final RegionRepository regions;
    private final LocationRepository locations;
    private final SettlementRepository settlements;
    private final PointOfInterestRepository pointsOfInterest;
    private final TravelRouteRepository travelRoutes;
    private final PartyLocationRepository partyLocations;
    private final CampaignEventService events;
    private final DtoValidator validator;

    public WorldService(
            CampaignRepository campaigns,
            RegionRepository regions,
            LocationRepository locations,
            SettlementRepository settlements,
            PointOfInterestRepository pointsOfInterest,
            TravelRouteRepository travelRoutes,
            PartyLocationRepository partyLocations,
            CampaignEventService events,
            DtoValidator validator) {
        this.campaigns = campaigns;
        this.regions = regions;
        this.locations = locations;
        this.settlements = settlements;
        this.pointsOfInterest = pointsOfInterest;
        this.travelRoutes = travelRoutes;
        this.partyLocations = partyLocations;
        this.events = events;
        this.validator = validator;
    }

    // ------------------------------------------------------------------
    // Campaign lookup
    // ------------------------------------------------------------------

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    // ------------------------------------------------------------------
    // Regions
    // ------------------------------------------------------------------

    public Region addRegion(Long campaignId, String name, String description) {
        Campaign campaign = requireCampaign(campaignId);
        return regions.save(new Region(campaign, name, description));
    }

    public List<Region> listRegions(Long campaignId) {
        Campaign campaign = requireCampaign(campaignId);
        return regions.findByCampaignOrderByName(campaign);
    }

    // ------------------------------------------------------------------
    // Locations
    // ------------------------------------------------------------------

    public Location addLocation(Long campaignId, String name, String description,
                                Long regionId, Double latitude, Double longitude) {
        Campaign campaign = requireCampaign(campaignId);
        Location location = new Location(campaign, name, description, false);
        if (regionId != null) {
            location.setRegion(regions.findById(regionId)
                    .orElseThrow(() -> new IllegalArgumentException("No region with id " + regionId)));
        }
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return locations.save(location);
    }

    public List<Location> listLocations(Long campaignId) {
        requireCampaign(campaignId);
        return locations.findByCampaignOrderByName(requireCampaign(campaignId));
    }

    /**
     * Marks a location discovered. Undiscovered locations are hidden from views that
     * filter by discovery, so this is what turns a place from "unknown" into "known".
     */
    public Location discoverLocation(Long campaignId, Long locationId) {
        Location location = requireLocation(campaignId, locationId);
        boolean wasUndiscovered = !location.isDiscovered();
        location.discover();
        locations.save(location);
        if (wasUndiscovered) {
            events.recordDiscovery(campaignId, location.getName(), location.getId(), "location");
        }
        return location;
    }

    public List<Location> listDiscoveredLocations(Long campaignId) {
        requireCampaign(campaignId);
        return locations.findByCampaignIdAndDiscovered(campaignId, true);
    }

    // ------------------------------------------------------------------
    // Settlements
    // ------------------------------------------------------------------

    public Settlement addSettlement(Long campaignId, String name, String description,
                                    SettlementType type, int population, Long regionId) {
        validator.requireNonNegativeQuantity("Settlement population", population);
        Location location = addLocation(campaignId, name, description, regionId, null, null);
        Campaign campaign = requireCampaign(campaignId);
        return settlements.save(new Settlement(campaign, location, type, population));
    }

    public List<Settlement> listSettlements(Long campaignId) {
        requireCampaign(campaignId);
        return settlements.findByCampaign(requireCampaign(campaignId));
    }

    // ------------------------------------------------------------------
    // Points of interest
    // ------------------------------------------------------------------

    public PointOfInterest addPointOfInterest(Long campaignId, String name, String description,
                                              PointOfInterestCategory category,
                                              Long settlementId, Long regionId) {
        Location location = addLocation(campaignId, name, description, regionId, null, null);
        Campaign campaign = requireCampaign(campaignId);
        Settlement settlement = null;
        if (settlementId != null) {
            settlement = settlements.findById(settlementId)
                    .orElseThrow(() -> new IllegalArgumentException("No settlement with id " + settlementId));
        }
        return pointsOfInterest.save(new PointOfInterest(campaign, location, category, settlement));
    }

    public List<PointOfInterest> listPointsOfInterest(Long campaignId) {
        requireCampaign(campaignId);
        return pointsOfInterest.findByCampaign(requireCampaign(campaignId));
    }

    // ------------------------------------------------------------------
    // Travel routes
    // ------------------------------------------------------------------

    public TravelRoute addTravelRoute(Long campaignId, Long fromId, Long toId,
                                      Double distanceKm, Integer travelMinutes) {
        validator.requirePositiveIdentifier("location", fromId);
        validator.requirePositiveIdentifier("location", toId);
        validator.requireNonNegativeQuantity("Travel distance (km)", distanceKm);
        validator.requireNonNegativeQuantity("Travel time (minutes)", travelMinutes);
        Campaign campaign = requireCampaign(campaignId);
        Location from = requireLocation(campaignId, fromId);
        Location to = requireLocation(campaignId, toId);
        return travelRoutes.save(new TravelRoute(campaign, from, to, distanceKm, travelMinutes));
    }

    public List<TravelRoute> listTravelRoutes(Long campaignId) {
        requireCampaign(campaignId);
        return travelRoutes.findByCampaign(requireCampaign(campaignId));
    }

    public void removeTravelRoute(Long routeId) {
        travelRoutes.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("No travel route with id " + routeId));
        travelRoutes.deleteById(routeId);
    }

    // ------------------------------------------------------------------
    // Current party location
    // ------------------------------------------------------------------

    public PartyLocation setPartyLocation(Long campaignId, Long locationId) {
        validator.requirePositiveIdentifier("location", locationId);
        Location location = requireLocation(campaignId, locationId);
        PartyLocation partyLocation = partyLocations.findByCampaignId(campaignId)
                .orElseGet(() -> new PartyLocation(campaignId, location));
        boolean moved = partyLocation.getLocation() == null
                || !location.getId().equals(partyLocation.getLocation().getId());
        partyLocation.setLocation(location);
        partyLocations.save(partyLocation);
        if (moved) {
            events.recordLocationEntry(campaignId, location.getId(), location.getName());
        }
        return partyLocation;
    }

    public Optional<PartyLocation> findPartyLocation(Long campaignId) {
        return partyLocations.findByCampaignId(campaignId);
    }

    // ------------------------------------------------------------------
    // Travel planning
    // ------------------------------------------------------------------

    /**
     * Plans the cheapest journey between two locations in a campaign by travelling
     * through the directed {@link TravelRoute} graph. Cheapest is measured in travel
     * time; the associated distance is summed along the chosen path.
     *
     * <p>The route graph is rebuilt from the campaign's routes on every call so that
     * freshly created or removed routes are reflected immediately. When no sequence of
     * routes connects the origin and the destination, the returned plan is marked
     * {@link TravelPlan#reachable() unreachable}.</p>
     *
     * @param campaignId the owning campaign
     * @param fromId     the origin location id
     * @param toId       the destination location id
     * @return the travel plan (never {@code null})
     */
    public TravelPlan computeTravel(Long campaignId, Long fromId, Long toId) {
        requireCampaign(campaignId);
        requireLocation(campaignId, fromId);
        requireLocation(campaignId, toId);

        if (fromId.equals(toId)) {
            return new TravelPlan(fromId, toId, true, 0, 0.0);
        }

        Map<Long, Map<Long, RouteEdge>> adjacency = buildAdjacency(campaignId);
        return dijkstra(adjacency, fromId, toId);
    }

    private Map<Long, Map<Long, RouteEdge>> buildAdjacency(Long campaignId) {
        Map<Long, Map<Long, RouteEdge>> adjacency = new HashMap<>();
        for (TravelRoute route : travelRoutes.findByCampaign(requireCampaign(campaignId))) {
            adjacency.computeIfAbsent(route.getFrom().getId(), k -> new HashMap<>())
                    .put(route.getTo().getId(), new RouteEdge(route));
        }
        return adjacency;
    }

    private TravelPlan dijkstra(
            Map<Long, Map<Long, RouteEdge>> adjacency, Long fromId, Long toId) {
        Set<Long> visited = new HashSet<>();
        PriorityQueue<RouteEdge> frontier =
                new PriorityQueue<>(Comparator.comparingInt(e -> e.minutes));
        frontier.add(new RouteEdge(fromId, toId, 0, 0.0));

        while (!frontier.isEmpty()) {
            RouteEdge edge = frontier.poll();
            if (visited.contains(edge.to)) {
                continue;
            }
            visited.add(edge.to);
            if (edge.to.equals(toId)) {
                return new TravelPlan(fromId, toId, true, edge.minutes, edge.distance);
            }
            Map<Long, RouteEdge> neighbours = adjacency.get(edge.to);
            if (neighbours != null) {
                for (RouteEdge next : neighbours.values()) {
                    if (!visited.contains(next.to)) {
                        frontier.add(next.withAccumulated(edge.minutes, edge.distance));
                    }
                }
            }
        }

        return TravelPlan.unreachable(fromId, toId);
    }

    /**
     * A single directed step in the travel graph: one {@link TravelRoute} or a
     * synthetic starting step with zero accumulated cost.
     */
    private static final class RouteEdge {
        private final Long from;
        private final Long to;
        private final Integer minutes;
        private final Double distance;

        RouteEdge(TravelRoute route) {
            this(route.getFrom().getId(), route.getTo().getId(),
                    route.getTravelMinutes() == null ? 0 : route.getTravelMinutes(),
                    route.getDistanceKm() == null ? 0.0 : route.getDistanceKm());
        }

        RouteEdge(Long from, Long to, Integer minutes, Double distance) {
            this.from = from;
            this.to = to;
            this.minutes = minutes;
            this.distance = distance;
        }

        RouteEdge withAccumulated(Integer accumulatedMinutes, Double accumulatedDistance) {
            return new RouteEdge(from, to,
                    accumulatedMinutes + minutes, accumulatedDistance + distance);
        }
    }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    private Location requireLocation(Long campaignId, Long locationId) {
        return locations.findById(locationId)
                .filter(l -> l.getCampaign() != null && l.getCampaign().getId() != null
                        && l.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException("No location with id " + locationId));
    }
}
