package com.example.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The structured game state a {@link com.example.service.NarrativeTemplate} renders from.
 *
 * <p>A context is the bridge between the game's domain objects and a template: it carries the
 * typed objects a template is interested in ({@link SceneBrief}, {@link EngineResponse},
 * {@link DiceRollResult}, {@link EnemyActionOutcome}, {@link CampaignEvent}, and so on) alongside
 * a free-form data map for anything not modelled as a domain object. Templates ask for what they
 * need with the typed {@code ...()} accessors and read the rest from {@link #data()}.</p>
 *
 * <p>It is built with the {@link #builder()} and the {@code with...} helpers, or with one of the
 * small {@code for...} factory methods. Every accessor returns {@code null} for an absent value,
 * and every accessor is safe to call on a context that does not hold that value, so a template
 * only needs to guard the field it actually reads.</p>
 */
public final class NarrativeContext {

    private SceneBrief scene;
    private EngineResponse response;
    private PlayerActionInput input;
    private PlayerActionResolution resolution;
    private AbilityCheckResult check;
    private DiceRollResult roll;
    private EnemyActionOutcome combat;
    private CampaignEvent event;

    private final Map<String, Object> data = new LinkedHashMap<>();

    private NarrativeContext() {
        /* Built through the builder. */
    }

    /**
     * @return a new builder for a narrative context
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the scene brief held by this context, or {@code null} when none was supplied
     */
    public SceneBrief scene() {
        return scene;
    }

    /**
     * @return the engine response held by this context, or {@code null} when none was supplied
     */
    public EngineResponse response() {
        return response;
    }

    /**
     * @return the player action input held by this context, or {@code null} when none was supplied
     */
    public PlayerActionInput input() {
        return input;
    }

    /**
     * @return the player action resolution held by this context, or {@code null} when none was supplied
     */
    public PlayerActionResolution resolution() {
        return resolution;
    }

    /**
     * @return the ability check held by this context, or {@code null} when none was supplied
     */
    public AbilityCheckResult check() {
        return check;
    }

    /**
     * @return the dice roll held by this context, or {@code null} when none was supplied
     */
    public DiceRollResult roll() {
        return roll;
    }

    /**
     * @return the combat outcome held by this context, or {@code null} when none was supplied
     */
    public EnemyActionOutcome combat() {
        return combat;
    }

    /**
     * @return the campaign event held by this context, or {@code null} when none was supplied
     */
    public CampaignEvent event() {
        return event;
    }

    /**
     * @return an immutable view of the free-form structured data held by this context
     */
    public Map<String, Object> data() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    /**
     * Looks up a single free-form data value.
     *
     * @param key the key to look up
     * @return the value, or {@code null} when the key is absent
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * The mutable builder for {@link NarrativeContext}. The {@code with...} helpers set one value
     * at a time and return the builder, so a context can be assembled fluently.
     */
    public static final class Builder {
        private final NarrativeContext context = new NarrativeContext();

        /**
         * Sets the scene brief.
         *
         * @param scene the scene brief, or {@code null} to clear it
         * @return the builder
         */
        public Builder scene(SceneBrief scene) {
            context.scene = scene;
            return this;
        }

        /**
         * Sets the engine response.
         *
         * @param response the engine response, or {@code null} to clear it
         * @return the builder
         */
        public Builder response(EngineResponse response) {
            context.response = response;
            return this;
        }

        /**
         * Sets the player action input.
         *
         * @param input the player action input, or {@code null} to clear it
         * @return the builder
         */
        public Builder input(PlayerActionInput input) {
            context.input = input;
            return this;
        }

        /**
         * Sets the player action resolution.
         *
         * @param resolution the resolution, or {@code null} to clear it
         * @return the builder
         */
        public Builder resolution(PlayerActionResolution resolution) {
            context.resolution = resolution;
            return this;
        }

        /**
         * Sets the ability check.
         *
         * @param check the check, or {@code null} to clear it
         * @return the builder
         */
        public Builder check(AbilityCheckResult check) {
            context.check = check;
            return this;
        }

        /**
         * Sets the dice roll.
         *
         * @param roll the roll, or {@code null} to clear it
         * @return the builder
         */
        public Builder roll(DiceRollResult roll) {
            context.roll = roll;
            return this;
        }

        /**
         * Sets the combat outcome.
         *
         * @param combat the combat outcome, or {@code null} to clear it
         * @return the builder
         */
        public Builder combat(EnemyActionOutcome combat) {
            context.combat = combat;
            return this;
        }

        /**
         * Sets the campaign event.
         *
         * @param event the campaign event, or {@code null} to clear it
         * @return the builder
         */
        public Builder event(CampaignEvent event) {
            context.event = event;
            return this;
        }

        /**
         * Records a free-form data value.
         *
         * @param key   the key (never {@code null})
         * @param value the value (nullable to remove the key)
         * @return the builder
         */
        public Builder data(String key, Object value) {
            if (value == null) {
                context.data.remove(key);
            } else {
                context.data.put(key, value);
            }
            return this;
        }

        /**
         * Merges a whole map of free-form data into the context.
         *
         * @param values the values to record (nullable; ignored when {@code null})
         * @return the builder
         */
        public Builder data(Map<String, Object> values) {
            if (values != null) {
                values.forEach(context.data::put);
            }
            return this;
        }

        /**
         * Builds the context.
         *
         * @return the assembled context (never {@code null})
         */
        public NarrativeContext build() {
            return context;
        }
    }

    /**
     * Builds a context around a scene brief for DM narration.
     *
     * @param scene the scene brief (never {@code null})
     * @return a context holding the scene brief
     */
    public static NarrativeContext forScene(SceneBrief scene) {
        return builder().scene(scene).build();
    }

    /**
     * Builds a context around a full engine response for a player action.
     *
     * @param response the engine response (never {@code null})
     * @return a context holding the engine response
     */
    public static NarrativeContext forAction(EngineResponse response) {
        return builder().response(response).build();
    }

    /**
     * Builds a context around a dice roll.
     *
     * @param roll the roll result (never {@code null})
     * @return a context holding the dice roll
     */
    public static NarrativeContext forRoll(DiceRollResult roll) {
        return builder().roll(roll).build();
    }

    /**
     * Builds a context around a combat outcome.
     *
     * @param combat the combat outcome (never {@code null})
     * @return a context holding the combat outcome
     */
    public static NarrativeContext forCombat(EnemyActionOutcome combat) {
        return builder().combat(combat).build();
    }

    /**
     * Builds a context around a campaign event.
     *
     * @param event the campaign event (never {@code null})
     * @return a context holding the campaign event
     */
    public static NarrativeContext forEvent(CampaignEvent event) {
        return builder().event(event).build();
    }

    /**
     * Builds a context that carries only free-form data for a category. The owning template
     * rebuilds the typed object it needs from this data, so a caller that holds a structured
     * payload rather than a domain object can still render the category.
     *
     * @param category the category the data describes (never {@code null})
     * @param data     the structured data (nullable; stored as an empty map when {@code null})
     * @return a context holding the free-form data
     */
    public static NarrativeContext fromCategoryData(NarrativeCategory category, Map<String, Object> data) {
        if (category == null) {
            throw new IllegalArgumentException("A category is required to build a data context.");
        }
        NarrativeContext context = new NarrativeContext();
        if (data != null) {
            context.data.putAll(data);
        }
        return context;
    }
}
