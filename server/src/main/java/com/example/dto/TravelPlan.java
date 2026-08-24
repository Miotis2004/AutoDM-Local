package com.example.dto;

/**
 * The result of planning a journey between two {@link com.example.domain.Location}s
 * across a campaign's {@link com.example.domain.TravelRoute} graph.
 *
 * <p>The journey is computed as the cheapest path (by travel time) through the directed
 * route graph. When no path exists the returned {@link #reachable()} flag is {@code false}
 * and the totals are {@code null}.</p>
 */
public record TravelPlan(
        Long fromId,
        Long toId,
        boolean reachable,
        Integer totalTravelMinutes,
        Double totalDistanceKm) {

    /**
     * Builds an unreachable plan for the given endpoints.
     *
     * @param fromId the origin location id
     * @param toId   the destination location id
     * @return an unreachable plan
     */
    public static TravelPlan unreachable(Long fromId, Long toId) {
        return new TravelPlan(fromId, toId, false, null, null);
    }
}
