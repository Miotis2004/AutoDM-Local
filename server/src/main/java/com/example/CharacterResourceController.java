package com.example;

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
import com.example.service.CharacterResourceService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * REST surface for persistent character resources.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link CharacterResourceService} call. All business logic — resolving the owning
 * campaign, applying the change, and persisting it — lives in the service, and
 * persistence is what lets resource changes reload across sessions.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class CharacterResourceController {

    private final CharacterResourceService resources;

    public CharacterResourceController(CharacterResourceService resources) {
        this.resources = resources;
    }

    // ------------------------------------------------------------------
    // Vitals: health, temporary health, death / unconscious state
    // ------------------------------------------------------------------

    @GetMapping("/{campaignId}/vitals")
    public Optional<CharacterVitals> getVitals(@PathVariable Long campaignId) {
        return Optional.ofNullable(resources.findOrCreateVitals(campaignId));
    }

    @PostMapping("/{campaignId}/vitals")
    public CharacterVitals createVitals(@PathVariable Long campaignId,
                                        @RequestBody VitalsRequest request) {
        return resources.createVitals(campaignId, request.hitPoints, request.maxHitPoints,
                request.temporaryHealth, request.unconscious, request.dead);
    }

    @PatchMapping("/{campaignId}/vitals/damage")
    public CharacterVitals applyDamage(@PathVariable Long campaignId,
                                       @RequestParam int delta) {
        return resources.applyDamage(campaignId, delta);
    }

    @PatchMapping("/{campaignId}/vitals/heal")
    public CharacterVitals heal(@PathVariable Long campaignId, @RequestParam int amount) {
        return resources.heal(campaignId, amount);
    }

    @PatchMapping("/{campaignId}/vitals/temp-health")
    public CharacterVitals addTemporaryHealth(@PathVariable Long campaignId,
                                              @RequestParam int amount) {
        return resources.addTemporaryHealth(campaignId, amount);
    }

    @PatchMapping("/{campaignId}/vitals/unconscious")
    public CharacterVitals setUnconscious(@PathVariable Long campaignId,
                                          @RequestParam boolean value) {
        return resources.setUnconscious(campaignId, value);
    }

    @PatchMapping("/{campaignId}/vitals/dead")
    public CharacterVitals setDead(@PathVariable Long campaignId, @RequestParam boolean value) {
        return resources.setDead(campaignId, value);
    }

    // ------------------------------------------------------------------
    // Limited-use abilities
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/abilities")
    public LimitedUseAbility addAbility(@PathVariable Long campaignId,
                                        @RequestBody AbilityRequest request) {
        return resources.addLimitedAbility(campaignId, request.name, request.maxUses,
                request.usesRemaining, request.recoversOnLongRest, request.recoversOnShortRest);
    }

    @PostMapping("/abilities/{abilityId}/use")
    public LimitedUseAbility useAbility(@PathVariable Long abilityId) {
        return resources.useLimitedAbility(abilityId);
    }

    @PostMapping("/abilities/{abilityId}/recharge")
    public LimitedUseAbility rechargeAbility(@PathVariable Long abilityId,
                                             @RequestParam int amount) {
        return resources.rechargeLimitedAbility(abilityId, amount);
    }

    @PostMapping("/{campaignId}/rest")
    public List<LimitedUseAbility> takeRest(@PathVariable Long campaignId,
                                                      @RequestParam(defaultValue = "long") String type) {
        return resources.takeRest("long".equalsIgnoreCase(type));
    }

    // ------------------------------------------------------------------
    // Spell / power resources
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/spells")
    public SpellPowerResource addSpell(@PathVariable Long campaignId,
                                       @RequestBody SpellRequest request) {
        return resources.addSpellResource(campaignId, request.name, request.maxPoints,
                request.pointsRemaining, request.slotLevel, request.concentration);
    }

    @PatchMapping("/spells/{resourceId}/use")
    public SpellPowerResource useSpells(@PathVariable Long resourceId,
                                        @RequestParam int amount) {
        return resources.useSpellPoints(resourceId, amount);
    }

    @PatchMapping("/spells/{resourceId}/restore")
    public SpellPowerResource restoreSpells(@PathVariable Long resourceId,
                                            @RequestParam int amount) {
        return resources.restoreSpellPoints(resourceId, amount);
    }

    // ------------------------------------------------------------------
    // Ammunition
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/ammo")
    public AmmunitionRecord addAmmunition(@PathVariable Long campaignId,
                                          @RequestBody AmmoRequest request) {
        return resources.addAmmunition(campaignId, request.ammoType, request.count);
    }

    @PatchMapping("/ammo/{recordId}/spend")
    public AmmunitionRecord spendAmmunition(@PathVariable Long recordId,
                                            @RequestParam int amount) {
        return resources.spendAmmunition(recordId, amount);
    }

    // ------------------------------------------------------------------
    // Consumables
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/consumables")
    public ConsumableRecord addConsumable(@PathVariable Long campaignId,
                                          @RequestBody ConsumableRequest request) {
        return resources.addConsumable(campaignId, request.name, request.category, request.count);
    }

    @PatchMapping("/consumables/{recordId}/consume")
    public ConsumableRecord consumeConsumable(@PathVariable Long recordId,
                                              @RequestParam int amount) {
        return resources.consumeConsumable(recordId, amount);
    }

    // ------------------------------------------------------------------
    // Currency
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/currency")
    public CurrencyRecord addCurrency(@PathVariable Long campaignId,
                                      @RequestBody CurrencyRequest request) {
        return resources.addCurrency(campaignId, request.currencyUnit, request.amount);
    }

    @PatchMapping("/currency/{recordId}/adjust")
    public CurrencyRecord adjustCurrency(@PathVariable Long recordId,
                                         @RequestParam int delta) {
        return resources.adjustCurrency(recordId, delta);
    }

    // ------------------------------------------------------------------
    // Conditions
    // ------------------------------------------------------------------

    @PostMapping("/{campaignId}/conditions")
    public ConditionRecord addCondition(@PathVariable Long campaignId,
                                        @RequestBody ConditionRequest request) {
        return resources.addCondition(campaignId, request.name, request.conditionKind,
                request.source, request.concentration, request.stackable, request.remainingRounds);
    }

    @DeleteMapping("/conditions/{recordId}")
    public void removeCondition(@PathVariable Long recordId) {
        resources.removeCondition(recordId);
    }

    @PostMapping("/{campaignId}/conditions/advance")
    public List<ConditionRecord> advanceConditions(@PathVariable Long campaignId) {
        return resources.advanceConditions(campaignId);
    }

    // ------------------------------------------------------------------
    // Request bodies (HTTP concern: request shapes)
    // ------------------------------------------------------------------

    record VitalsRequest(int hitPoints, int maxHitPoints, int temporaryHealth,
                         boolean unconscious, boolean dead) {
    }

    record AbilityRequest(String name, int maxUses, int usesRemaining,
                          boolean recoversOnLongRest, boolean recoversOnShortRest) {
    }

    record SpellRequest(String name, int maxPoints, int pointsRemaining,
                        Integer slotLevel, boolean concentration) {
    }

    record AmmoRequest(String ammoType, int count) {
    }

    record ConsumableRequest(String name, String category, int count) {
    }

    record CurrencyRequest(CurrencyUnit currencyUnit, int amount) {
    }

    record ConditionRequest(String name, ConditionKind conditionKind, String source,
                            boolean concentration, boolean stackable, Integer remainingRounds) {
    }
}
