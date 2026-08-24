package com.example.service;

import com.example.domain.AbilityCheckResult;
import com.example.domain.Combatant;
import com.example.domain.Campaign;
import com.example.domain.CampaignEvent;
import com.example.domain.CampaignEventType;
import com.example.domain.DamageType;
import com.example.domain.DiceRollResult;
import com.example.domain.DieResult;
import com.example.domain.EngineResponse;
import com.example.domain.EnemyActionOutcome;
import com.example.domain.NarrativeCategory;
import com.example.domain.NarrativeContext;
import com.example.domain.NarrativeEntry;
import com.example.domain.PlayerActionResolution;
import com.example.domain.SceneBrief;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The registry and renderer of {@link NarrativeTemplate}s.
 *
 * <p>This service owns every template that turns structured game state into a line for the game
 * log. It is, by design, the one place the five canonical categories are wired: {@link
 * NarrativeCategory#DM_NARRATION}, {@link NarrativeCategory#PLAYER_ACTION}, {@link
 * NarrativeCategory#DICE_RESULT}, {@link NarrativeCategory#COMBAT_EVENT}, and {@link
 * NarrativeCategory#SYSTEM_EVENT}. Each is rendered by a small, pure template that reads only the
 * field of the {@link NarrativeContext} it cares about and returns a {@link NarrativeEntry}.</p>
 *
 * <p>Templates are <em>data-driven</em>: a template reads the structured state from whichever form
 * it is given. When a caller supplies the fully-typed domain object (the way the rest of the
 * back-end does, e.g. the {@link EngineResponse} a player action resolves to) the template renders
 * straight from it; when a caller supplies only free-form data (the way the REST surface does, for
 * a game log fed from a JSON payload) the template rebuilds the typed object from the data map.
 * That keeps a single template usable both from the game engine and from a structured payload.</p>
 *
 * <p>The registry is open and extensible. A campaign (or a verification) can register its own
 * template for an existing category with {@link #register} to change how that category renders, or
 * add a brand new {@link NarrativeCategory} by supplying a constant on that enum and
 * registering a template for it; the REST surface already passes categories through by name, so
 * no endpoint needs to change to pick up the new category.</p>
 *
 * <p>Rendering never mutates game state: every template is a pure function of its context, exactly
 * like the {@link DiceService} and {@link AbilityCheckService} the templates draw from.</p>
 */
@Service
public class NarrativeTemplates {

    private final Map<NarrativeCategory, NarrativeTemplate> templates = new TreeMap<>();

    /**
     * Creates the service and installs the five canonical templates, one per category.
     *
     * @param dice the shared dice service, used to summarise a {@link DiceRollResult}
     */
    public NarrativeTemplates(DiceService dice) {
        register(NarrativeCategory.DM_NARRATION, this::renderScene);
        register(NarrativeCategory.PLAYER_ACTION, this::renderAction);
        register(NarrativeCategory.DICE_RESULT, this::renderRoll);
        register(NarrativeCategory.COMBAT_EVENT, this::renderCombat);
        register(NarrativeCategory.SYSTEM_EVENT, this::renderEvent);
        this.dice = dice;
    }

    private final DiceService dice;

    /**
     * Registers (or replaces) the template for a category.
     *
     * @param category the category to template (never {@code null})
     * @param template the template to install (never {@code null})
     */
    public void register(NarrativeCategory category, NarrativeTemplate template) {
        if (category == null || template == null) {
            throw new IllegalArgumentException("Both a category and a template are required to register.");
        }
        templates.put(category, template);
    }

    /**
     * Removes the template for a category, if one is registered.
     *
     * @param category the category to unregister (never {@code null})
     */
    public void unregister(NarrativeCategory category) {
        templates.remove(category);
    }

    /**
     * @param category the category to query
     * @return {@code true} when a template is registered for the category
     */
    public boolean hasTemplate(NarrativeCategory category) {
        return templates.containsKey(category);
    }

    /**
     * @return an immutable, ordered view of the categories that currently have a template
     */
    public List<NarrativeCategory> categories() {
        return List.copyOf(templates.keySet());
    }

    /**
     * Renders a single category from a context.
     *
     * @param category the category to render (never {@code null})
     * @param context  the structured game state (never {@code null})
     * @return the rendered entry (never {@code null}), or an empty fallback entry when no template
     *         is registered for the category
     */
    public NarrativeEntry render(NarrativeCategory category, NarrativeContext context) {
        NarrativeTemplate template = templates.get(category);
        if (template == null) {
            return NarrativeEntry.of(category,
                    "No narrative template is registered for " + category.name() + ".");
        }
        return template.render(context == null ? NarrativeContext.builder().build() : context);
    }

    /**
     * Renders every category that has a template and for which the context supplies the matching
     * state, in category order.
     *
     * @param context the structured game state (never {@code null})
     * @return the entries the context produces, in category order (never {@code null})
     */
    public List<NarrativeEntry> renderAll(NarrativeContext context) {
        List<NarrativeEntry> entries = new ArrayList<>();
        for (Map.Entry<NarrativeCategory, NarrativeTemplate> entry : templates.entrySet()) {
            if (appliesTo(entry.getKey(), context)) {
                entries.add(entry.getValue().render(context));
            }
        }
        return entries;
    }

    /**
     * @param category  the category to test
     * @param context   the context to test
     * @return {@code true} when the category's template can render the given context (the context
     *         holds the state that category expects, either as a typed reference or as data)
     */
    private boolean appliesTo(NarrativeCategory category, NarrativeContext context) {
        if (context == null) {
            return false;
        }
        switch (category) {
            case DM_NARRATION:
                return context.scene() != null || context.data().containsKey("title");
            case PLAYER_ACTION:
                return context.response() != null || context.data().containsKey("recognized");
            case DICE_RESULT:
                return context.roll() != null || context.data().containsKey("total");
            case COMBAT_EVENT:
                return context.combat() != null || context.data().containsKey("hit");
            case SYSTEM_EVENT:
                return context.event() != null || context.data().containsKey("eventType");
            default:
                return false;
        }
    }

    // ------------------------------------------------------------------
    // The five canonical templates
    // ------------------------------------------------------------------

    /**
     * Renders DM narration from a scene: the scene's narrative when present, otherwise a short
     * "you are here" line naming the scene and its combatants.
     *
     * @param context the context holding a scene brief (or scene data)
     * @return the narration entry
     */
    private NarrativeEntry renderScene(NarrativeContext context) {
        SceneBrief scene = context.scene();
        if (scene == null) {
            scene = sceneFromData(context.data());
        }
        String title = scene == null || scene.sceneTitle() == null ? "" : scene.sceneTitle();
        String narrative = scene == null || scene.sceneNarrative() == null ? "" : scene.sceneNarrative();

        String message;
        if (narrative.isEmpty()) {
            message = "You are in " + title + ".";
        } else {
            StringBuilder line = new StringBuilder("- ").append(narrative);
            List<String> present = presentCombatants(scene);
            if (!present.isEmpty()) {
                line.append(" ").append(present).append(')');
            }
            message = line.toString();
        }

        return new NarrativeEntry(NarrativeCategory.DM_NARRATION, title, message, null,
                sceneData(scene));
    }

    /**
     * Renders a player action from an engine response: the engine's own narrative text when the
     * action was recognized, or the validation problem when it was not.
     *
     * @param context the context holding an engine response (or action data)
     * @return the player-action entry
     */
    private NarrativeEntry renderAction(NarrativeContext context) {
        EngineResponse response = context.response();
        if (response == null) {
            response = actionFromData(context.data());
        }
        boolean recognized = Boolean.TRUE.equals(response != null && response.recognized());
        AbilityCheckResult check = response != null ? response.check() : null;

        String title;
        String message;
        if (!recognized) {
            title = "Unrecognized";
            List<String> errors = response != null ? response.validationErrors() : List.of();
            message = errors.isEmpty() ? "Nothing happens." : errors.get(0);
        } else {
            title = "Player action";
            message = response.response();
        }

        return new NarrativeEntry(NarrativeCategory.PLAYER_ACTION, title, message, null,
                actionData(recognized, check));
    }

    /**
     * Renders a dice roll from a roll, using the shared dice service's summary.
     *
     * @param context the context holding a dice roll (or roll data)
     * @return the dice-result entry
     */
    private NarrativeEntry renderRoll(NarrativeContext context) {
        DiceRollResult roll = context.roll();
        if (roll == null) {
            roll = rollFromData(context.data());
        }
        String message = dice.summarize(roll);
        return new NarrativeEntry(NarrativeCategory.DICE_RESULT, "Dice roll", message, null,
                rollData(roll));
    }

    /**
     * Renders a combat beat from a combat outcome: an attack landing (and whether it fell),
     * missing, or a no-op.
     *
     * @param context the context holding a combat outcome (or combat data)
     * @return the combat entry
     */
    private NarrativeEntry renderCombat(NarrativeContext context) {
        EnemyActionOutcome outcome = context.combat();
        if (outcome == null) {
            outcome = combatFromData(context.data());
        }
        String attacker = outcome.attacker() != null ? outcome.attacker().getName() : "An enemy";
        String message;
        if (!outcome.actionTaken()) {
            message = attacker + " has no valid target.";
        } else if (outcome.hit()) {
            Combatant target = outcome.target();
            String who = target != null ? target.getName() : "its target";
            String verb = outcome.targetDefeated() ? "defeats" : "hits";
            message = attacker + " " + verb + " " + who + " for " + outcome.damageApplied()
                    + " " + outcome.damageType() + " damage";
        } else {
            message = attacker + " misses.";
        }
        return new NarrativeEntry(NarrativeCategory.COMBAT_EVENT, "Combat", message, null,
                combatData(outcome));
    }

    /**
     * Renders a system event from a campaign event: its description as the line and its event type
     * as the title, falling back to the type name when no description was recorded.
     *
     * @param context the context holding a campaign event (or event data)
     * @return the system entry
     */
    private NarrativeEntry renderEvent(NarrativeContext context) {
        CampaignEvent event = context.event();
        if (event == null) {
            event = eventFromData(context.data());
        }
        CampaignEventType type = event != null ? event.getEventType() : null;
        String description = event != null ? event.getDescription() : null;
        String title = type != null ? type.name() : "Event";
        String message = (description == null || description.isEmpty())
                ? (type != null ? "A " + type.name().toLowerCase(Locale.ROOT) + " event occurred."
                        : "A system event occurred.")
                : description;
        return new NarrativeEntry(NarrativeCategory.SYSTEM_EVENT, title, message, null,
                eventData(event));
    }

    // ------------------------------------------------------------------
    // Rebuilding typed objects from free-form data (the data-driven path)
    // ------------------------------------------------------------------

    private static SceneBrief sceneFromData(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        return new SceneBrief(
                asLong(data.get("sceneId")),
                asText(data.get("title")),
                asText(data.get("narrative")),
                asNames(data.get("involvedNames")),
                asNames(data.get("combatantNames")));
    }

    private static EngineResponse actionFromData(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        boolean recognized = Boolean.TRUE.equals(data.get("recognized"));
        AbilityCheckResult check = checkFromData(data);
        return new EngineResponse(
                null,
                recognized,
                recognized ? List.of() : List.of(asText(data.getOrDefault("message", ""))),
                check,
                asText(data.get("response")),
                null,
                data.get("effects") == null ? List.of() : (List<String>) data.get("effects"));
    }

    private static AbilityCheckResult checkFromData(Map<String, Object> data) {
        Object statistic = data.get("statistic");
        Object modifier = data.get("modifier");
        Object roll = data.get("roll");
        Object difficulty = data.get("difficulty");
        if (statistic == null || roll == null || difficulty == null) {
            return null;
        }
        return AbilityCheckResult.of(asInt(statistic), asInt(modifier), asInt(roll), asInt(difficulty));
    }

    private static DiceRollResult rollFromData(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object dice = data.get("dice");
        if (!(dice instanceof List)) {
            return null;
        }
        List<DieResult> rolled = new ArrayList<>();
        for (Object entry : (List<Object>) dice) {
            if (entry instanceof Map) {
                Map<String, Object> die = (Map<String, Object>) entry;
                rolled.add(DieResult.of(asInt(die.get("sides")), asInt(die.get("value"))));
            }
        }
        return DiceRollResult.of(rolled, asInt(data.get("modifier")));
    }

    private static EnemyActionOutcome combatFromData(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Combatant attacker = combatantFromData(data, "attacker");
        Combatant target = combatantFromData(data, "target");
        boolean taken = Boolean.TRUE.equals(data.get("actionTaken"));
        DamageType type = parseDamageType(data.get("damageType"));
        return new EnemyActionOutcome(
                taken,
                attacker,
                target,
                Boolean.TRUE.equals(data.get("hit")),
                asInt(data.get("attackTotal")),
                asInt(data.get("difficulty")),
                asInt(data.getOrDefault("damageApplied", 0)),
                Boolean.TRUE.equals(data.get("targetDefeated")),
                type);
    }

    private static Combatant combatantFromData(Map<String, Object> data, String key) {
        Object name = data.get(key);
        if (name == null) {
            return null;
        }
        Combatant combatant = new Combatant();
        combatant.setName(asText(name));
        return combatant;
    }

    private static CampaignEvent eventFromData(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object type = data.get("eventType");
        if (type == null) {
            return null;
        }
        CampaignEvent event = new CampaignEvent();
        event.setCampaign(new Campaign());
        event.setEventType(parseEventType(type));
        event.setDescription(asText(data.get("description")));
        event.setDetails(asText(data.get("details")));
        return event;
    }

    private static DamageType parseDamageType(Object value) {
        if (value == null) {
            return DamageType.PHYSICAL;
        }
        try {
            return DamageType.valueOf(asText(value).toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return DamageType.PHYSICAL;
        }
    }

    private static CampaignEventType parseEventType(Object value) {
        try {
            return CampaignEventType.valueOf(asText(value).toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            // An unrecognised event type is recorded as a session moment rather than failing the render.
            return CampaignEventType.SESSION_START;
        }
    }

    // ------------------------------------------------------------------
    // Data maps consumed by the frontend game log
    // ------------------------------------------------------------------

    private static List<String> presentCombatants(SceneBrief scene) {
        if (scene == null || scene.combatantNames() == null || scene.combatantNames().isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(scene.combatantNames());
    }

    private static Map<String, Object> sceneData(SceneBrief scene) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (scene != null) {
            data.put("sceneId", scene.sceneId());
            data.put("title", scene.sceneTitle());
            data.put("narrative", scene.sceneNarrative());
            data.put("involvedNames", scene.involvedNames());
            data.put("combatantNames", scene.combatantNames());
        }
        return data;
    }

    private static Map<String, Object> actionData(boolean recognized, AbilityCheckResult check) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recognized", recognized);
        if (check != null) {
            data.put("statistic", check.statistic());
            data.put("modifier", check.modifier());
            data.put("roll", check.roll());
            data.put("total", check.total());
            data.put("difficulty", check.difficulty());
            data.put("outcome", check.outcome().name());
        }
        return data;
    }

    private static Map<String, Object> rollData(DiceRollResult roll) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Object> dice = new ArrayList<>();
        for (DieResult die : roll.dice()) {
            Map<String, Object> dieData = new LinkedHashMap<>();
            dieData.put("sides", die.sides());
            dieData.put("value", die.value());
            dice.add(dieData);
        }
        data.put("dice", dice);
        data.put("modifier", roll.modifier());
        data.put("total", roll.total());
        data.put("percentile", roll.percentile());
        return data;
    }

    private static Map<String, Object> combatData(EnemyActionOutcome outcome) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("actionTaken", outcome.actionTaken());
        data.put("attacker", outcome.attacker() != null ? outcome.attacker().getName() : null);
        data.put("attackerId", outcome.attacker() != null ? outcome.attacker().getId() : null);
        data.put("target", outcome.target() != null ? outcome.target().getName() : null);
        data.put("targetId", outcome.target() != null ? outcome.target().getId() : null);
        data.put("hit", outcome.hit());
        data.put("damageApplied", outcome.damageApplied());
        data.put("damageType", outcome.damageType());
        data.put("targetDefeated", outcome.targetDefeated());
        return data;
    }

    private static Map<String, Object> eventData(CampaignEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (event != null) {
            data.put("id", event.getId());
            data.put("eventType", event.getEventType().name());
            data.put("description", event.getDescription());
            data.put("timestamp", event.getTimestamp());
            data.put("details", event.getDetails());
        }
        return data;
    }

    // ------------------------------------------------------------------
    // Small data coercion helpers
    // ------------------------------------------------------------------

    private static String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(asText(value));
    }

    private static int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(asText(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> asNames(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List) {
            List<String> names = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                names.add(String.valueOf(item));
            }
            return names;
        }
        return List.of(asText(value));
    }

}
