package com.example.service;

import com.example.db.CampaignRepository;
import com.example.db.CombatConditionRepository;
import com.example.db.CombatantRepository;
import com.example.domain.Campaign;
import com.example.domain.CombatCondition;
import com.example.domain.Combatant;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for {@link CombatCondition}s (status effects) applied to
 * {@link Combatant}s within a campaign.
 *
 * <p>This service is the single place where combat conditions are created, toggled,
 * advanced round-by-round, and consulted. Every mutation resolves its owning
 * campaign, applies the change to a managed entity, and relies on the repository to
 * persist it, so status effects reload across application restarts within a campaign.</p>
 */
@Service
public class CombatConditionService {

    private final CampaignRepository campaigns;
    private final CombatConditionRepository conditions;
    private final CombatantRepository combatants;

    public CombatConditionService(CampaignRepository campaigns,
                                  CombatConditionRepository conditions,
                                  CombatantRepository combatants) {
        this.campaigns = campaigns;
        this.conditions = conditions;
        this.combatants = combatants;
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private Combatant requireOwnedCombatant(Long campaignId, Long combatantId) {
        return combatants.findById(combatantId)
                .filter(c -> c.getCampaign() != null && c.getCampaign().getId() != null
                        && c.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No combatant with id " + combatantId));
    }

    /**
     * Creates a condition applied to a combatant, unless an identical active condition
     * (same name on the same combatant) already exists, in which case the existing one
     * is returned.
     *
     * @return the condition (never {@code null})
     */
    public CombatCondition addCondition(Long campaignId, Long combatantId, String name,
                                        String description, Integer duration, String source) {
        Campaign campaign = requireCampaign(campaignId);
        Combatant combatant = requireOwnedCombatant(campaignId, combatantId);
        List<CombatCondition> existing = conditions.findByCampaignIdAndCombatantIdAndName(
                campaignId, combatantId, name);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        CombatCondition condition = new CombatCondition(campaign, combatant, name, description,
                duration, source);
        return conditions.save(condition);
    }

    /**
     * Toggles the {@link CombatCondition#isActive()} flag of a condition.
     *
     * @return the updated condition (never {@code null})
     */
    public CombatCondition toggle(Long campaignId, Long conditionId, boolean active) {
        requireCampaign(campaignId);
        CombatCondition condition = conditions.findById(conditionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No condition with id " + conditionId));
        if (condition.getCampaign() == null || condition.getCampaign().getId() == null
                || !condition.getCampaign().getId().equals(campaignId)) {
            throw new IllegalArgumentException("No condition with id " + conditionId);
        }
        condition.setActive(active);
        return conditions.save(condition);
    }

    /**
     * Advances every active condition owned by the campaign by one round, deactivating
     * any whose duration has run out.
     *
     * @return the conditions that were advanced (never {@code null})
     */
    public List<CombatCondition> advanceRounds(Long campaignId) {
        requireCampaign(campaignId);
        List<CombatCondition> all = conditions.findByCampaignOrderByCreatedAtAsc(
                requireCampaign(campaignId));
        for (CombatCondition condition : all) {
            if (condition.isActive()) {
                condition.advanceOneRound();
            }
        }
        return conditions.saveAll(all);
    }

    public CombatCondition getCondition(Long campaignId, Long conditionId) {
        requireCampaign(campaignId);
        return conditions.findById(conditionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No condition with id " + conditionId));
    }

    public List<CombatCondition> listConditions(Long campaignId) {
        requireCampaign(campaignId);
        return conditions.findByCampaignOrderByCreatedAtAsc(requireCampaign(campaignId));
    }

    public List<CombatCondition> listConditionsOnCombatant(Long campaignId, Long combatantId) {
        requireOwnedCombatant(campaignId, combatantId);
        return conditions.findByCombatantIdOrderByCreatedAtAsc(combatantId);
    }
}
