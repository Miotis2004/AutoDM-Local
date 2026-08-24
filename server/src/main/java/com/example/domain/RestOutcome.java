package com.example.domain;

import java.util.List;

/**
 * The result of a short or long rest taken by a {@link Campaign}.
 *
 * <p>This is a service-level result value (not a persisted entity) that reports, in a
 * single immutably-shaped record, exactly what a rest accomplished: whether a long rest
 * or a short rest was taken, how much health was restored, which temporary conditions
 * were cleared, which limited-use abilities were refreshed and by how many uses, and the
 * campaign plus any active session whose state the rest advanced.</p>
 */
public record RestOutcome(
        boolean longRest,
        boolean healthRestored,
        int hitPoints,
        int maxHitPoints,
        boolean unconsciousCleared,
        int conditionsCleared,
        List<LimitedUseAbility> restoredAbilities,
        int usesRestored,
        Campaign campaign,
        Session session) {
}
