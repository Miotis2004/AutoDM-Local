package com.example.domain;

import java.util.List;

/**
 * The outcome of rolling one or more dice.
 *
 * <p>This is a plain, immutable value holder describing a single roll request and its
 * result: the individual dice that were rolled (each a {@link DieResult} with its number of
 * faces and the value that die showed), the applied modifier, and the final total, which is
 * the sum of every die value plus the modifier.</p>
 *
 * <p>Percentile rolls are represented as ordinary dice with 100 faces; the
 * {@link #percentile()} flag is set whenever any die in the roll is a percentile die so
 * callers can render them distinctly.</p>
 */
public record DiceRollResult(
        List<DieResult> dice,
        int modifier,
        int total,
        boolean percentile) {

    /** The supported game-die face counts: d4, d6, d8, d10, d12, d20, and percentile (d100). */
    public static final List<Integer> SUPPORTED_DIE_SIDES =
            List.of(4, 6, 8, 10, 12, 20, 100);

    /**
     * Builds a roll result, validating the request and computing the total.
     *
     * @param dice      the individual dice that were rolled; at least one is required
     * @param modifier  the value added to (or subtracted from) the sum of the dice
     * @return a completed roll result whose total is the sum of the dice plus the modifier
     */
    public static DiceRollResult of(List<DieResult> dice, int modifier) {
        if (dice == null || dice.isEmpty()) {
            throw new IllegalArgumentException("A roll must include at least one die");
        }
        boolean percentile = dice.stream().anyMatch(die -> die.sides() == 100);
        int total = dice.stream().mapToInt(DieResult::value).sum() + modifier;
        return new DiceRollResult(dice, modifier, total, percentile);
    }
}
