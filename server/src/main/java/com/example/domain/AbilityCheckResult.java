package com.example.domain;

/**
 * The outcome of resolving a single ability or skill check.
 *
 * <p>An ability check combines four inputs: the character's {@link #statistic()} (the raw
 * ability score), the {@link #modifier()} applied to that ability, the {@link #roll()} (the
 * value generated on the die), and the {@link #difficulty()} (the difficulty class the check
 * must meet or beat). The {@link #total()} is the sum of the statistic, the modifier, and the
 * roll, and the {@link #outcome()} is {@link AbilityCheckOutcome#SUCCESS} when the total meets
 * or exceeds the difficulty and {@link AbilityCheckOutcome#FAILURE} otherwise.</p>
 *
 * <p>This is a plain, immutable value holder intended to be consumed by Dungeon Master logic:
 * callers read {@link #total()}, {@link #difficulty()}, and {@link #outcome()} to decide what
 * happens next in the game.</p>
 */
public record AbilityCheckResult(
        int statistic,
        int modifier,
        int roll,
        int total,
        int difficulty,
        AbilityCheckOutcome outcome) {

    /**
     * Resolves an ability check by combining the statistic, modifier, roll, and difficulty.
     *
     * <p>The total is the sum of the statistic, the modifier, and the roll. The check succeeds
     * when that total meets or exceeds the difficulty class and fails otherwise.</p>
     *
     * @param statistic the character's raw ability score for the checked ability
     * @param modifier  the modifier applied to the ability (may be negative)
     * @param roll      the value generated on the die (typically 1..20 for a d20)
     * @param difficulty the difficulty class the total must meet or exceed
     * @return a resolved {@link AbilityCheckResult} whose total and outcome are derived from the
     *         inputs
     */
    public static AbilityCheckResult of(int statistic, int modifier, int roll, int difficulty) {
        int total = statistic + modifier + roll;
        AbilityCheckOutcome outcome = total >= difficulty ? AbilityCheckOutcome.SUCCESS : AbilityCheckOutcome.FAILURE;
        return new AbilityCheckResult(statistic, modifier, roll, total, difficulty, outcome);
    }

    /**
     * The number of points the total exceeded (a positive value) or fell short of (a negative
     * value) the difficulty class. A total that lands exactly on the difficulty class has a
     * margin of zero.
     *
     * @return the total minus the difficulty
     */
    public int margin() {
        return total - difficulty;
    }
}
