package com.example.service;

import com.example.db.CampaignRepository;
import com.example.db.PlayerCharacterRepository;
import com.example.db.CreatureTemplateRepository;
import com.example.domain.Campaign;
import com.example.domain.Combatant;
import com.example.domain.CombatantKind;
import com.example.domain.Encounter;
import com.example.domain.EncounterDifficulty;
import com.example.domain.PlayerCharacter;
import com.example.domain.Scene;
import com.example.domain.CreatureTemplate;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automated encounter generation.
 *
 * <p>This service turns a campaign's party and catalogue of foes into a ready-to-fight
 * {@link Encounter}. It is the "basic automated rule" for encounters: given a location,
 * a difficulty, and (optionally) an explicit party strength, it sizes an encounter to the
 * party and fills it with enemies drawn from the campaign's available {@link
 * CreatureTemplate}s.</p>
 *
 * <p>Generation considers four things:</p>
 * <ul>
 *   <li><b>Party strength</b> &mdash; the number of player characters and their average
 *       level (both overridable), which sets the base power of the party;</li>
 *   <li><b>Difficulty</b> &mdash; an {@link EncounterDifficulty} that scales the party's
 *       power into a concrete threat budget; the default is {@link EncounterDifficulty#MEDIUM};</li>
 *   <li><b>Available enemy definitions</b> &mdash; the campaign's {@link CreatureTemplate}s,
 *       optionally narrowed to a specific set; and</li>
 *   <li><b>Campaign state</b> &mdash; the owning campaign is always resolved first, so a
 *       generation call only ever acts against a real, owned campaign.</li>
 * </ul>
 *
 * <p>The budget is the party's combined level and size (its "power") multiplied by the
 * difficulty's budget multiplier. Each template contributes a threat score derived from its
 * combat profile. Selection fills the budget with templates, one of each first and then
 * repeating the cheapest template while budget remains, so a harder difficulty fields a
 * proportionally stronger encounter. Every selected template is instantiated into one or
 * more {@link Combatant} enemies of {@link CombatantKind#ENEMY} joined to the encounter,
 * which is what puts the generated encounter into the encounter engine.</p>
 *
 * <p>Generation is a pure orchestration over the other services: {@link SceneService} opens
 * a scene, {@link EncounterService} creates the encounter, and {@link CombatantService}
 * instantiates the enemies. All mutations are persisted through those services, so a
 * generated encounter reloads across sessions within a campaign.</p>
 */
@Service
public class EncounterGenerator {

    /**
     * Per (party member, level) base power used before the difficulty multiplier. A
     * plain constant keeps the scaling easy to read and tune.
     */
    private static final int PARTY_MEMBER_BASE_POWER = 250;

    /**
     * The default party assumed when a campaign has no player characters and no explicit
     * strength is supplied: a standard four-member party of first-level heroes.
     */
    private static final int DEFAULT_PARTY_SIZE = 4;

    /** The default level assumed under the same empty-party condition. */
    private static final int DEFAULT_PARTY_LEVEL = 1;

    private final CampaignRepository campaigns;
    private final CreatureTemplateRepository templates;
    private final PlayerCharacterRepository party;
    private final EncounterService encounters;
    private final SceneService scenes;
    private final CombatantService combatants;

    public EncounterGenerator(CampaignRepository campaigns,
                              CreatureTemplateRepository templates,
                              PlayerCharacterRepository party,
                              EncounterService encounters,
                              SceneService scenes,
                              CombatantService combatants) {
        this.campaigns = campaigns;
        this.templates = templates;
        this.party = party;
        this.encounters = encounters;
        this.scenes = scenes;
        this.combatants = combatants;
    }

    // ------------------------------------------------------------------
    // Generation
    // ------------------------------------------------------------------

    /**
     * Generates an encounter for the given location, opening a fresh scene for it.
     *
     * @param campaignId the owning campaign
     * @param locationId the location the encounter takes place in
     * @param difficulty the difficulty (defaults to {@link EncounterDifficulty#MEDIUM}
     *                   when {@code null})
     * @return the generated encounter, refreshed with its enemy combatants (never {@code null})
     */
    public Encounter generateEncounter(Long campaignId, Long locationId, EncounterDifficulty difficulty) {
        return generateEncounter(campaignId, locationId, difficulty, null, null, null);
    }

    /**
     * Generates an encounter for the given location, opening a fresh scene for it.
     *
     * @param campaignId     the owning campaign
     * @param locationId     the location the encounter takes place in
     * @param difficulty     the difficulty (defaults to {@link EncounterDifficulty#MEDIUM} when {@code null})
     * @param partySize      an explicit party size, or {@code null} to derive from the party
     * @param averageLevel   an explicit average party level, or {@code null} to derive it
     * @param templateIds    an optional narrowing to specific templates, or {@code null} for all
     * @return the generated encounter, refreshed with its enemy combatants (never {@code null})
     */
    public Encounter generateEncounter(Long campaignId, Long locationId, EncounterDifficulty difficulty,
                                       Integer partySize, Integer averageLevel, List<Long> templateIds) {
        requireCampaign(campaignId);
        Scene scene = scenes.createScene(
                campaignId, "Generated encounter", "Automatically generated encounter", null, null);
        Encounter encounter = encounters.createEncounter(campaignId, scene.getId(), locationId);
        generateInto(campaignId, encounter.getId(), difficulty, partySize, averageLevel, templateIds);
        return encounters.getEncounter(campaignId, encounter.getId());
    }

    /**
     * Generates an encounter against an existing scene rather than opening a new one.
     *
     * @param campaignId the owning campaign
     * @param sceneId    the scene the encounter takes place in
     * @param locationId the location the encounter takes place in
     * @param difficulty the difficulty (defaults to {@link EncounterDifficulty#MEDIUM} when {@code null})
     * @return the generated encounter, refreshed with its enemy combatants (never {@code null})
     */
    public Encounter generateIntoScene(Long campaignId, Long sceneId, Long locationId, EncounterDifficulty difficulty) {
        requireCampaign(campaignId);
        scenes.getScene(campaignId, sceneId);
        Encounter encounter = encounters.createEncounter(campaignId, sceneId, locationId);
        generateInto(campaignId, encounter.getId(), difficulty, null, null, null);
        return encounters.getEncounter(campaignId, encounter.getId());
    }

    /**
     * Instantiates the enemies for a generated encounter into an existing, owned
     * {@link Encounter}. This is the step that puts the encounter into the combat engine:
     * each selected template becomes one or more {@link Combatant} enemies of
     * {@link CombatantKind#ENEMY} joined to the encounter.
     *
     * @param campaignId     the owning campaign
     * @param encounterId    the owned encounter to fill with enemies
     * @param difficulty     the difficulty (defaults to {@link EncounterDifficulty#MEDIUM} when {@code null})
     * @param partySize      an explicit party size, or {@code null} to derive from the party
     * @param averageLevel   an explicit average party level, or {@code null} to derive it
     * @param templateIds    an optional narrowing to specific templates, or {@code null} for all
     * @return the details of what was generated (never {@code null})
     */
    public EncounterGenerationResult generateInto(Long campaignId, Long encounterId, EncounterDifficulty difficulty,
                                                  Integer partySize, Integer averageLevel, List<Long> templateIds) {
        EncounterDifficulty resolved = EncounterDifficulty.orDefault(difficulty);
        int size = resolvePartySize(campaignId, partySize);
        int level = resolveAverageLevel(campaignId, averageLevel);
        double budget = threatBudget(size, level, resolved);
        List<CreatureTemplate> pool = availableTemplates(campaignId, templateIds);
        List<CreatureTemplate> chosen = selectForBudget(pool, budget);
        List<Combatant> enemies = instantiate(campaignId, encounterId, chosen);
        return new EncounterGenerationResult(
                encounters.getEncounter(campaignId, encounterId),
                enemies,
                chosen,
                size,
                level,
                resolved,
                budget);
    }

    // ------------------------------------------------------------------
    // Party strength
    // ------------------------------------------------------------------

    /**
     * @return the resolved party size: the explicit override, otherwise the number of
     *         player characters in the campaign, otherwise a default four-member party
     */
    public int resolvePartySize(Long campaignId, Integer partySize) {
        Campaign campaign = requireCampaign(campaignId);
        if (partySize != null) {
            return partySize;
        }
        long count = party.countByCampaign(campaign);
        return count > 0 ? (int) count : DEFAULT_PARTY_SIZE;
    }

    /**
     * @return the resolved average party level: the explicit override, otherwise the
     *         rounded-up mean of the party's character levels, otherwise {@link
     *         #DEFAULT_PARTY_LEVEL}
     */
    public int resolveAverageLevel(Long campaignId, Integer averageLevel) {
        Campaign campaign = requireCampaign(campaignId);
        if (averageLevel != null) {
            return averageLevel;
        }
        List<PlayerCharacter> characters = party.findByCampaign(campaign);
        if (characters.isEmpty()) {
            return DEFAULT_PARTY_LEVEL;
        }
        int sum = 0;
        for (PlayerCharacter character : characters) {
            sum += character.getLevel();
        }
        return Math.max(1, (sum + characters.size() - 1) / characters.size());
    }

    /**
     * The threat budget a party faces: the combined power of its members (size times
     * average level times a fixed per-member base) scaled by the difficulty's multiplier.
     *
     * @param partySize    the resolved party size
     * @param averageLevel the resolved average level
     * @param difficulty   the resolved difficulty
     * @return the threat budget to approximate
     */
    public double threatBudget(int partySize, int averageLevel, EncounterDifficulty difficulty) {
        int power = Math.max(1, partySize) * Math.max(1, averageLevel) * PARTY_MEMBER_BASE_POWER;
        return power * difficulty.budgetMultiplier();
    }

    // ------------------------------------------------------------------
    // Template pool and selection
    // ------------------------------------------------------------------

    private List<CreatureTemplate> availableTemplates(Long campaignId, List<Long> templateIds) {
        List<CreatureTemplate> all = templates.findByCampaignOrderByName(requireCampaign(campaignId));
        if (templateIds == null || templateIds.isEmpty()) {
            return all;
        }
        List<CreatureTemplate> narrowed = new ArrayList<>();
        for (Long id : templateIds) {
            templates.findById(id).ifPresent(narrowed::add);
        }
        return narrowed;
    }

    /**
     * Fills the given threat budget with templates. It takes one of each template, in
     * ascending threat order, while each fits, and then keeps adding the cheapest
     * template while budget remains, so a larger (harder) budget fields a stronger
     * encounter. An empty pool yields no selections.
     *
     * @param pool   the templates to choose from (never {@code null})
     * @param budget the threat budget to approximate
     * @return the selected templates in instantiation order (never {@code null})
     */
    public List<CreatureTemplate> selectForBudget(List<CreatureTemplate> pool, double budget) {
        List<CreatureTemplate> sorted = pool == null ? List.of() : new ArrayList<>(pool);
        sorted.sort(Comparator.comparingInt(this::threatScore));
        List<CreatureTemplate> chosen = new ArrayList<>();
        double remaining = budget;
        for (CreatureTemplate template : sorted) {
            int cost = threatScore(template);
            if (cost <= remaining) {
                chosen.add(template);
                remaining -= cost;
            }
        }
        if (remaining >= 1 && !sorted.isEmpty()) {
            CreatureTemplate cheapest = sorted.get(0);
            int cost = threatScore(cheapest);
            while (cost > 0 && remaining >= cost) {
                chosen.add(cheapest);
                remaining -= cost;
            }
        }
        return chosen;
    }

    /**
     * A template's threat score: the sum of its combat profile values. Nullable fields
     * contribute nothing, so a template that carries only some of a profile scores on
     * what it has.
     *
     * @param template the template to score
     * @return its threat score
     */
    public int threatScore(CreatureTemplate template) {
        if (template == null) {
            return 0;
        }
        return nonNull(template.getHealth())
                + nonNull(template.getDefense())
                + nonNull(template.getAttack())
                + nonNull(template.getDamage());
    }

    // ------------------------------------------------------------------
    // Instantiation
    // ------------------------------------------------------------------

    private List<Combatant> instantiate(Long campaignId, Long encounterId, List<CreatureTemplate> chosen) {
        List<Combatant> enemies = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (CreatureTemplate template : chosen) {
            String key = template.getName();
            int count = seen.merge(key, 1, Integer::sum);
            String name = count > 1 ? key + " " + count : key;
            int hp = nonNull(template.getHealth(), 1);
            Combatant enemy = combatants.createCombatant(campaignId, name, CombatantKind.ENEMY, hp, hp);
            combatants.addToEncounter(campaignId, enemy.getId(), encounterId);
            enemies.add(enemy);
        }
        return enemies;
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("No campaign with id " + campaignId));
    }

    private static int nonNull(Integer value) {
        return value == null ? 0 : value;
    }

    private static int nonNull(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
