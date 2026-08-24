package com.example.service;

import com.example.domain.AbilityCheckResult;
import com.example.domain.DiceRollResult;

import org.springframework.stereotype.Service;

/**
 * Business and game logic for resolving ability and skill checks.
 *
 * <p>This service is the single owner of the ability-check rule. It combines a character's
 * ability statistic, the modifier applied to that ability, a freshly generated die roll, and a
 * difficulty class into a single {@link AbilityCheckResult}: the total (statistic + modifier +
 * roll) is compared against the difficulty and classified as
 * {@link com.example.domain.AbilityCheckOutcome#SUCCESS} or
 * {@link com.example.domain.AbilityCheckOutcome#FAILURE}.</p>
 *
 * <p>Resolution is a pure function of its inputs plus freshly generated randomness, so it owns
 * no state and performs no persistence. The die roll is generated on the back-end through the
 * {@link DiceService}, never in the browser, so checks cannot be tampered with by the client.</p>
 */
@Service
public class AbilityCheckService {

    private final DiceService dice;

    /**
     * Creates the service, backing roll generation with the {@link DiceService}.
     */
    public AbilityCheckService() {
        this(new DiceService());
    }

    /**
     * Package-visible constructor that injects the dice service, so roll generation can be
     * observed in verification.
     *
     * @param dice the dice service used to generate rolls for checks
     */
    AbilityCheckService(DiceService dice) {
        this.dice = dice;
    }

    /**
     * Resolves an ability or skill check from its four inputs.
     *
     * <p>The total is the sum of the character statistic, the modifier, and the roll. The check
     * succeeds when that total meets or exceeds the difficulty class and fails otherwise.</p>
     *
     * @param statistic  the character's raw ability score for the checked ability
     * @param modifier   the modifier applied to the ability (may be negative)
     * @param roll       the value generated on the die (for example 1..20 for a d20)
     * @param difficulty the difficulty class the total must meet or exceed
     * @return a resolved {@link AbilityCheckResult} with the total and success/failure outcome
     */
    public AbilityCheckResult resolve(int statistic, int modifier, int roll, int difficulty) {
        return AbilityCheckResult.of(statistic, modifier, roll, difficulty);
    }

    /**
     * Resolves an ability or skill check by rolling a single die on the back-end.
     *
     * <p>The die is generated server-side through the {@link DiceService} (never in the browser),
     * so the roll cannot be influenced by the client. The single die result's value is used as the
     * check's roll.</p>
     *
     * @param statistic  the character's raw ability score for the checked ability
     * @param modifier   the modifier applied to the ability (may be negative)
     * @param sides      the number of faces on the die to roll (one of the {@link DiceService}
     *                   supported dice, for example 20 for a d20)
     * @param difficulty the difficulty class the total must meet or exceed
     * @return a resolved {@link AbilityCheckResult} with the total and success/failure outcome
     * @throws IllegalArgumentException if {@code sides} is not a supported die
     */
    public AbilityCheckResult resolveWithDie(int statistic, int modifier, int sides, int difficulty) {
        DiceRollResult roll = dice.rollSingle(sides);
        return resolve(statistic, modifier, roll.total(), difficulty);
    }

    /**
     * Renders an ability check as a compact, human-readable breakdown string, for example
     * {@code "Strength (+2) roll 15 vs DC 15 => 19: SUCCESS"}.
     *
     * @param result the check result to summarize
     * @return the readable breakdown
     */
    public String summarize(AbilityCheckResult result) {
        return "Statistic " + result.statistic()
                + " (+" + result.modifier() + ") roll " + result.roll()
                + " vs DC " + result.difficulty()
                + " => " + result.total() + ": " + result.outcome();
    }
}
