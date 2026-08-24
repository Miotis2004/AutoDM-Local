package com.example.domain;

/**
 * The outcome of a single die within a {@link DiceRollResult}.
 *
 * <p>A die is described by the number of faces it has ({@link #sides()}, for example 20 for
 * a d20 or 100 for a percentile die) and the value that was rolled on it, which is always in
 * the inclusive range {@code 1..sides}.</p>
 */
public record DieResult(int sides, int value) {

    /**
     * Creates a die result, validating that the roll landed within the die's faces.
     *
     * @param sides the number of faces the die has (at least 2)
     * @param value the value rolled on the die (between 1 and {@code sides})
     * @return a validated die result
     */
    public static DieResult of(int sides, int value) {
        if (sides < 2) {
            throw new IllegalArgumentException("A die must have at least 2 faces, got " + sides);
        }
        if (value < 1 || value > sides) {
            throw new IllegalArgumentException(
                    "Rolled value " + value + " is out of range for a d" + sides);
        }
        return new DieResult(sides, value);
    }
}
