package com.example.service;

import com.example.db.CampaignEventRepository;
import com.example.db.CampaignRepository;
import com.example.db.CharacterVitalsRepository;
import com.example.db.ConditionRepository;
import com.example.db.LimitedUseAbilityRepository;
import com.example.db.SessionRepository;
import com.example.domain.Campaign;
import com.example.domain.CampaignEvent;
import com.example.domain.CampaignEventType;
import com.example.domain.CampaignStatus;
import com.example.domain.CharacterVitals;
import com.example.domain.ConditionRecord;
import com.example.domain.LimitedUseAbility;
import com.example.domain.RestOutcome;
import com.example.domain.Session;
import com.example.domain.SessionStatus;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business and game logic for short and long rest operations.
 *
 * <p>A rest is a single, atomic operation that does four things and persists every
 * change so it survives across sessions and application restarts:</p>
 *
 * <ol>
 *   <li><b>Restores health.</b> The campaign's {@link CharacterVitals} hit points are
 *   raised back to the maximum and an unconscious character is woken (a rest never
 *   reverses death).</li>
 *   <li><b>Clears selected temporary conditions.</b> A long rest clears every condition;
 *   a short rest clears every condition that is not maintained by concentration, since
 *   concentration is a sustained, active effort rather than something that fades with
 *   rest.</li>
 *   <li><b>Restores selected limited-use resources.</b> Each {@link LimitedUseAbility}
 *   records whether it recovers on a short rest, a long rest, or both; only the abilities
 *   selected by the rest being taken are recharged to their maximum.</li>
 *   <li><b>Advances campaign and session state.</b> The campaign's {@code last_played_at}
 *   date is set and a draft campaign moves to {@link CampaignStatus#IN_PROGRESS}, and, when
 *   an active {@link Session} is open, a rest event reference is recorded on it. A
 *   {@link CampaignEvent} of type {@link CampaignEventType#REST} captures the moment for
 *   the campaign's durable history.</li>
 * </ol>
 *
 * <p>All of these effects are written through the repositories, so a rest is fully
 * persisted in the SQLite database (server/src/main/resources/schema.sql, loaded by
 * Hibernate at bootstrap).</p>
 */
@Service
public class RestService {

    private final CampaignRepository campaigns;
    private final CharacterVitalsRepository vitals;
    private final ConditionRepository conditions;
    private final LimitedUseAbilityRepository limitedAbilities;
    private final SessionRepository sessions;
    private final CampaignEventRepository events;

    public RestService(CampaignRepository campaigns,
                       CharacterVitalsRepository vitals,
                       ConditionRepository conditions,
                       LimitedUseAbilityRepository limitedAbilities,
                       SessionRepository sessions,
                       CampaignEventRepository events) {
        this.campaigns = campaigns;
        this.vitals = vitals;
        this.conditions = conditions;
        this.limitedAbilities = limitedAbilities;
        this.sessions = sessions;
        this.events = events;
    }

    // ------------------------------------------------------------------
    // Rest
    // ------------------------------------------------------------------

    /**
     * Takes a short or long rest for the given campaign and returns a summary of what the
     * rest accomplished. Every effect — health restoration, condition clearing, ability
     * recovery, and campaign/session state advancement — is applied to managed entities
     * and persisted through the repositories.
     *
     * @param campaignId the campaign that rests
     * @param longRest   {@code true} for a long rest, {@code false} for a short rest
     * @return the outcome of the rest (never {@code null})
     * @throws IllegalArgumentException if there is no campaign with the given id
     */
    public RestOutcome takeRest(Long campaignId, boolean longRest) {
        Campaign campaign = requireCampaign(campaignId);

        boolean unconsciousCleared = restoreHealth(campaign);
        int conditionsCleared = clearConditions(campaign, longRest);
        AbilityRest abilities = restoreAbilities(campaign, longRest);

        CampaignEvent recorded = advanceCampaignState(campaign, longRest,
                conditionsCleared, abilities.restored.size());
        Session session = referenceOnActiveSession(campaign, recorded);

        return new RestOutcome(
                longRest,
                true,
                currentHitPoints(campaign),
                maxHitPoints(campaign),
                unconsciousCleared,
                conditionsCleared,
                abilities.restored,
                abilities.usesRestored,
                campaign,
                session);
    }

    // ------------------------------------------------------------------
    // Individual rest effects
    // ------------------------------------------------------------------

    /**
     * Restores the campaign's hit points to the maximum and wakes an unconscious
     * character.
     *
     * @return {@code true} when the character was unconscious and has now been woken
     */
    private boolean restoreHealth(Campaign campaign) {
        CharacterVitals current = findOrCreateVitals(campaign);
        if (current.getHitPoints() < current.getMaxHitPoints()) {
            current.setHitPoints(current.getMaxHitPoints());
        }
        boolean wasUnconscious = current.isUnconscious();
        if (wasUnconscious) {
            // A rest restores the body and wakes the character, but never reverses death.
            current.setUnconscious(false);
        }
        vitals.save(current);
        return wasUnconscious;
    }

    /**
     * Removes the temporary conditions a rest clears. A long rest clears every condition;
     * a short rest clears every condition that is not maintained by concentration, since
     * concentration is a sustained, active effort rather than something that fades with
     * rest.
     *
     * @return the number of conditions cleared
     */
    private int clearConditions(Campaign campaign, boolean longRest) {
        List<ConditionRecord> toClear = new ArrayList<>();
        for (ConditionRecord condition : conditions.findByCampaign(campaign)) {
            if (longRest || !condition.isConcentration()) {
                toClear.add(condition);
            }
        }
        for (ConditionRecord condition : toClear) {
            conditions.delete(condition);
        }
        return toClear.size();
    }

    /**
     * Recharges every limited-use ability selected by the rest being taken to its maximum,
     * leaving unselected abilities untouched.
     *
     * @return the restored abilities and the total number of uses restored
     */
    private AbilityRest restoreAbilities(Campaign campaign, boolean longRest) {
        List<LimitedUseAbility> restored = new ArrayList<>();
        int usesRestored = 0;
        for (LimitedUseAbility ability : limitedAbilities.findByCampaign(campaign)) {
            boolean eligible = longRest ? ability.isRecoversOnLongRest() : ability.isRecoversOnShortRest();
            if (eligible) {
                int before = ability.getUsesRemaining();
                ability.recharge(ability.getMaxUses());
                usesRestored += ability.getMaxUses() - before;
                restored.add(ability);
                limitedAbilities.save(ability);
            }
        }
        return new AbilityRest(restored, usesRestored);
    }

    // ------------------------------------------------------------------
    // Campaign and session state
    // ------------------------------------------------------------------

    /**
     * Advances the campaign's durable state: stamps the last-played date, moves a draft
     * campaign into progress, and records a {@link CampaignEventType#REST} event.
     *
     * @param campaign         the campaign that rested
     * @param longRest         whether the rest was a long rest
     * @param conditionsCleared how many conditions the rest cleared
     * @param abilitiesRestored how many abilities the rest restored
     * @return the recorded campaign event
     */
    private CampaignEvent advanceCampaignState(Campaign campaign, boolean longRest,
                                               int conditionsCleared, int abilitiesRestored) {
        campaign.setLastPlayedAt(LocalDate.now());
        if (campaign.getStatus() == CampaignStatus.DRAFT) {
            campaign.setStatus(CampaignStatus.IN_PROGRESS);
        }
        campaigns.save(campaign);

        String label = longRest ? "Long rest" : "Short rest";
        String details = "{\"longRest\":" + longRest
                + ",\"conditionsCleared\":" + conditionsCleared
                + ",\"abilitiesRestored\":" + abilitiesRestored
                + "}";
        return events.save(new CampaignEvent(campaign, CampaignEventType.REST, LocalDateTime.now())
                .withDescription(label)
                .withDetails(details));
    }

    /**
     * References the given rest event on the campaign's open session, if any, so the rest
     * is reflected in session history as well as campaign event history. If no session is
     * currently open, a new active session is started so the rest is recorded.
     *
     * @return the updated session (never {@code null})
     */
    private Session referenceOnActiveSession(Campaign campaign, CampaignEvent event) {
        Session session = sessions.findFirstByCampaignAndStatusOrderByIdDesc(campaign, SessionStatus.ACTIVE)
                .orElseGet(() -> sessions.save(new Session(campaign, LocalDateTime.now())));
        session.addEvent(event.getId(), event.getDescription());
        return sessions.save(session);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private CharacterVitals findOrCreateVitals(Campaign campaign) {
        return vitals.findByCampaign(campaign).stream().findFirst()
                .orElseGet(() -> new CharacterVitals(campaign, 0, 0, 0, false, false));
    }

    private int currentHitPoints(Campaign campaign) {
        return findOrCreateVitals(campaign).getHitPoints();
    }

    private int maxHitPoints(Campaign campaign) {
        return findOrCreateVitals(campaign).getMaxHitPoints();
    }

    /**
     * A small result holder collecting the abilities a rest restored and the total number
     * of uses that were restored.
     *
     * @param restored    the abilities that were recharged by the rest
     * @param usesRestored the total uses restored across those abilities
     */
    private record AbilityRest(List<LimitedUseAbility> restored, int usesRestored) {
    }
}
