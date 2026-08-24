package com.example.service;

import com.example.domain.AmmunitionRecord;
import com.example.domain.Campaign;
import com.example.domain.CharacterVitals;
import com.example.domain.ConditionKind;
import com.example.domain.ConditionRecord;
import com.example.domain.ConsumableRecord;
import com.example.domain.CurrencyRecord;
import com.example.domain.CurrencyUnit;
import com.example.domain.LimitedUseAbility;
import com.example.domain.SpellPowerResource;
import com.example.db.AmmunitionRepository;
import com.example.db.CampaignRepository;
import com.example.db.CharacterVitalsRepository;
import com.example.db.ConditionRepository;
import com.example.db.ConsumableRepository;
import com.example.db.CurrencyRepository;
import com.example.db.LimitedUseAbilityRepository;
import com.example.db.SpellPowerResourceRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business and game logic for persistent character resources owned by a campaign.
 *
 * <p>This service is the single place where resource changes are expressed and
 * persisted. Every mutation resolves its owning campaign, applies the change to a
 * managed entity, and relies on the repository to write the result back to
 * storage. Because the resources are campaign-scoped entities, the same changes can
 * be reloaded across sessions simply by reading them back through this service or
 * the repositories.</p>
 */
@Service
public class CharacterResourceService {

    private final CampaignRepository campaigns;
    private final CharacterVitalsRepository vitals;
    private final LimitedUseAbilityRepository limitedAbilities;
    private final SpellPowerResourceRepository spellResources;
    private final AmmunitionRepository ammunition;
    private final ConsumableRepository consumables;
    private final CurrencyRepository currency;
    private final ConditionRepository conditions;

    public CharacterResourceService(CampaignRepository campaigns,
                                    CharacterVitalsRepository vitals,
                                    LimitedUseAbilityRepository limitedAbilities,
                                    SpellPowerResourceRepository spellResources,
                                    AmmunitionRepository ammunition,
                                    ConsumableRepository consumables,
                                    CurrencyRepository currency,
                                    ConditionRepository conditions) {
        this.campaigns = campaigns;
        this.vitals = vitals;
        this.limitedAbilities = limitedAbilities;
        this.spellResources = spellResources;
        this.ammunition = ammunition;
        this.consumables = consumables;
        this.currency = currency;
        this.conditions = conditions;
    }

    // ------------------------------------------------------------------
    // Campaign lookup
    // ------------------------------------------------------------------

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    // ------------------------------------------------------------------
    // Vitals: health, temporary health, death / unconscious state
    // ------------------------------------------------------------------

    /**
     * Returns the campaign's current vitals, or {@link #createVitals(Long, int, int,
     * int, boolean, boolean)} a fresh set if none exists yet.
     */
    public CharacterVitals findOrCreateVitals(Long campaignId) {
        Campaign campaign = requireCampaign(campaignId);
        return vitals.findByCampaign(campaign).stream().findFirst()
                .orElseGet(() -> new CharacterVitals(campaign, 0, 0, 0, false, false));
    }

    public CharacterVitals createVitals(Long campaignId, int hitPoints, int maxHitPoints,
                                        int temporaryHealth, boolean unconscious, boolean dead) {
        Campaign campaign = requireCampaign(campaignId);
        List<CharacterVitals> existing = vitals.findByCampaign(campaign);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return vitals.save(new CharacterVitals(campaign, hitPoints, maxHitPoints,
                temporaryHealth, unconscious, dead));
    }

    public CharacterVitals applyDamage(Long campaignId, int delta) {
        CharacterVitals current = findOrCreateVitals(campaignId);
        int remaining = delta;
        // Temporary health absorbs damage before current hit points.
        int absorbedByTemp = Math.min(remaining, current.getTemporaryHealth());
        current.setTemporaryHealth(current.getTemporaryHealth() - absorbedByTemp);
        remaining -= absorbedByTemp;
        current.setHitPoints(Math.max(0, current.getHitPoints() - remaining));
        return vitals.save(current);
    }

    public CharacterVitals heal(Long campaignId, int amount) {
        CharacterVitals current = findOrCreateVitals(campaignId);
        current.setHitPoints(Math.min(current.getMaxHitPoints(),
                current.getHitPoints() + amount));
        return vitals.save(current);
    }

    public CharacterVitals addTemporaryHealth(Long campaignId, int amount) {
        CharacterVitals current = findOrCreateVitals(campaignId);
        current.setTemporaryHealth(current.getTemporaryHealth() + amount);
        return vitals.save(current);
    }

    public CharacterVitals setUnconscious(Long campaignId, boolean unconscious) {
        CharacterVitals current = findOrCreateVitals(campaignId);
        current.setUnconscious(unconscious);
        return vitals.save(current);
    }

    public CharacterVitals setDead(Long campaignId, boolean dead) {
        CharacterVitals current = findOrCreateVitals(campaignId);
        current.setDead(dead);
        return vitals.save(current);
    }

    // ------------------------------------------------------------------
    // Limited-use abilities
    // ------------------------------------------------------------------

    public LimitedUseAbility addLimitedAbility(Long campaignId, String name, int maxUses,
                                               int usesRemaining, boolean recoversOnLongRest,
                                               boolean recoversOnShortRest) {
        Campaign campaign = requireCampaign(campaignId);
        return limitedAbilities.save(new LimitedUseAbility(campaign, name, maxUses,
                usesRemaining, recoversOnLongRest, recoversOnShortRest));
    }

    public LimitedUseAbility useLimitedAbility(Long abilityId) {
        LimitedUseAbility ability = limitedAbilities.findById(abilityId)
                .orElseThrow(() -> new IllegalArgumentException("No ability with id " + abilityId));
        ability.useOnce();
        return limitedAbilities.save(ability);
    }

    public LimitedUseAbility rechargeLimitedAbility(Long abilityId, int amount) {
        LimitedUseAbility ability = limitedAbilities.findById(abilityId)
                .orElseThrow(() -> new IllegalArgumentException("No ability with id " + abilityId));
        ability.recharge(amount);
        return limitedAbilities.save(ability);
    }

    /**
     * Restores all of the campaign's limited-use abilities according to the rest they
     * are configured to recover on.
     */
    public List<LimitedUseAbility> takeRest(boolean isLongRest) {
        List<LimitedUseAbility> all = limitedAbilities.findAll();
        for (LimitedUseAbility ability : all) {
            if (isLongRest ? ability.isRecoversOnLongRest() : ability.isRecoversOnShortRest()) {
                ability.recharge(ability.getMaxUses());
                limitedAbilities.save(ability);
            }
        }
        return all;
    }

    // ------------------------------------------------------------------
    // Spell / power resources
    // ------------------------------------------------------------------

    public SpellPowerResource addSpellResource(Long campaignId, String name, int maxPoints,
                                               int pointsRemaining, Integer slotLevel,
                                               boolean concentration) {
        Campaign campaign = requireCampaign(campaignId);
        return spellResources.save(new SpellPowerResource(campaign, name, maxPoints,
                pointsRemaining, slotLevel, concentration));
    }

    public SpellPowerResource useSpellPoints(Long resourceId, int amount) {
        SpellPowerResource resource = spellResources.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("No resource with id " + resourceId));
        resource.spend(amount);
        return spellResources.save(resource);
    }

    public SpellPowerResource restoreSpellPoints(Long resourceId, int amount) {
        SpellPowerResource resource = spellResources.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("No resource with id " + resourceId));
        resource.restore(amount);
        return spellResources.save(resource);
    }

    // ------------------------------------------------------------------
    // Ammunition
    // ------------------------------------------------------------------

    public AmmunitionRecord addAmmunition(Long campaignId, String ammoType, int count) {
        Campaign campaign = requireCampaign(campaignId);
        return ammunition.findByCampaignAndAmmoType(campaign, ammoType).map(existing -> {
            existing.setCount(existing.getCount() + count);
            return ammunition.save(existing);
        }).orElseGet(() -> ammunition.save(
                new AmmunitionRecord(campaign, ammoType, count)));
    }

    public AmmunitionRecord spendAmmunition(Long recordId, int count) {
        AmmunitionRecord record = ammunition.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("No ammunition with id " + recordId));
        record.spend(count);
        return ammunition.save(record);
    }

    // ------------------------------------------------------------------
    // Consumables
    // ------------------------------------------------------------------

    public ConsumableRecord addConsumable(Long campaignId, String name, String category,
                                          int count) {
        Campaign campaign = requireCampaign(campaignId);
        return consumables.findByCampaignAndName(campaign, name).map(existing -> {
            existing.setCount(existing.getCount() + count);
            if (category != null) {
                existing.setCategory(category);
            }
            return consumables.save(existing);
        }).orElseGet(() -> consumables.save(
                new ConsumableRecord(campaign, name, category, count)));
    }

    public ConsumableRecord consumeConsumable(Long recordId, int count) {
        ConsumableRecord record = consumables.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("No consumable with id " + recordId));
        record.consume(count);
        return consumables.save(record);
    }

    // ------------------------------------------------------------------
    // Currency
    // ------------------------------------------------------------------

    public CurrencyRecord addCurrency(Long campaignId, CurrencyUnit unit, int amount) {
        Campaign campaign = requireCampaign(campaignId);
        return currency.findByCampaignAndCurrencyUnit(campaign, unit).map(existing -> {
            existing.adjust(amount);
            return currency.save(existing);
        }).orElseGet(() -> currency.save(new CurrencyRecord(campaign, unit, amount)));
    }

    public CurrencyRecord adjustCurrency(Long recordId, int delta) {
        CurrencyRecord record = currency.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("No currency with id " + recordId));
        record.adjust(delta);
        return currency.save(record);
    }

    // ------------------------------------------------------------------
    // Conditions
    // ------------------------------------------------------------------

    public ConditionRecord addCondition(Long campaignId, String name, ConditionKind conditionKind,
                                        String source, boolean concentration, boolean stackable,
                                        Integer remainingRounds) {
        Campaign campaign = requireCampaign(campaignId);
        return conditions.save(new ConditionRecord(campaign, name, conditionKind, source,
                concentration, stackable, remainingRounds));
    }

    public void removeCondition(Long recordId) {
        conditions.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("No condition with id " + recordId));
        conditions.deleteById(recordId);
    }

    /**
     * Advances every condition on the campaign by one round and persists the results.
     * Conditions without a finite duration are left untouched. A condition that runs
     * out of rounds is removed from the campaign.
     *
     * @return the conditions whose duration expired as a result
     */
    public List<ConditionRecord> advanceConditions(Long campaignId) {
        Campaign campaign = requireCampaign(campaignId);
        List<ConditionRecord> expired = new ArrayList<>();
        for (ConditionRecord condition : conditions.findByCampaign(campaign)) {
            boolean wasFinite = condition.getRemainingRounds() != null;
            condition.advanceOneRound();
            if (wasFinite && condition.getRemainingRounds() == null) {
                expired.add(condition);
                conditions.delete(condition);
            }
        }
        return expired;
    }
}
