package com.example.domain;

/**
 * The overall difficulty of an encounter, expressed as a multiplier on the party's
 * power budget.
 *
 * <p>Difficulty does not change which creatures can appear; instead it scales the
 * amount of "threat" the automated generator is allowed to field against the party.
 * A harder encounter may use more copies of a creature, a stronger mix, or a larger
 * total threat budget, while an easier one trims the same pool down. Each tier pairs
 * a human label with a {@link #budgetMultiplier()} used by the generator to turn the
 * party's combined level and size into a concrete threat budget.</p>
 *
 * <p>The tiers follow the familiar easy-to-hard progression. The multiplier is a plain
 * scaling factor, so the base power budget for a party is multiplied by the tier to
 * obtain the threat budget the generated encounter should approach.</p>
 */
public enum EncounterDifficulty {

    TRIVIAL("Trivial", 0.5),
    EASY("Easy", 0.75),
    MEDIUM("Medium", 1.0),
    HARD("Hard", 1.5),
    CHALLENGING("Challenging", 2.0);

    private final String label;
    private final double budgetMultiplier;

    EncounterDifficulty(String label, double budgetMultiplier) {
        this.label = label;
        this.budgetMultiplier = budgetMultiplier;
    }

    /**
     * @return a human-readable name for this difficulty, suitable for logging or display
     */
    public String label() {
        return label;
    }

    /**
     * The factor this tier applies to the party's combined power to produce a threat
     * budget. Higher tiers return a larger multiplier, so a harder difficulty allows a
     * proportionally stronger encounter.
     *
     * @return the budget multiplier for this difficulty
     */
    public double budgetMultiplier() {
        return budgetMultiplier;
    }

    /**
     * Resolves a possibly {@code null} difficulty to a concrete tier, defaulting to
     * {@link #MEDIUM} so that omitted input never leaves generation without a target.
     *
     * @param difficulty the requested difficulty, or {@code null} for the default
     * @return the requested difficulty, or {@link #MEDIUM} when {@code null}
     */
    public static EncounterDifficulty orDefault(EncounterDifficulty difficulty) {
        return difficulty == null ? MEDIUM : difficulty;
    }
}
