package com.example;

import com.example.domain.AbilityCheckResult;
import com.example.service.AbilityCheckService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for resolving ability and skill checks.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link AbilityCheckService} call. All calculation, roll generation, and the success/failure
 * comparison live in the service, and the browser never generates any randomness.</p>
 */
@RestController
@RequestMapping("/api/ability-checks")
public class AbilityCheckController {

    private final AbilityCheckService abilityChecks;

    public AbilityCheckController(AbilityCheckService abilityChecks) {
        this.abilityChecks = abilityChecks;
    }

    /**
     * Resolves an ability or skill check from explicit inputs.
     *
     * <p>Pass the character's ability statistic, the modifier applied to it, the value that was
     * already generated on the die, and the difficulty class. For example
     * {@code POST /api/ability-checks/resolve?statistic=16&modifier=+3&roll=12&difficulty=15}.</p>
     *
     * @param statistic  the character's raw ability score
     * @param modifier   the modifier applied to the ability (may be negative)
     * @param roll       the value generated on the die
     * @param difficulty the difficulty class the total must meet or exceed
     * @return a {@link AbilityCheckResult} with the total and success/failure outcome
     */
    @PostMapping("/resolve")
    public AbilityCheckResult resolve(
            @RequestParam int statistic,
            @RequestParam int modifier,
            @RequestParam int roll,
            @RequestParam int difficulty) {
        return abilityChecks.resolve(statistic, modifier, roll, difficulty);
    }

    /**
     * Rolls a single die on the back-end and resolves an ability or skill check from the result.
     *
     * <p>The die is generated server-side (defaults to a d20), so the roll cannot be influenced by
     * the client. For example
     * {@code POST /api/ability-checks/roll?statistic=8&modifier=+3&sides=20&difficulty=15}.</p>
     *
     * @param statistic  the character's raw ability score
     * @param modifier   the modifier applied to the ability (may be negative)
     * @param sides      the number of faces on the die to roll (defaults to 20)
     * @param difficulty the difficulty class the total must meet or exceed
     * @return a {@link AbilityCheckResult} with the total and success/failure outcome
     */
    @PostMapping("/roll")
    public AbilityCheckResult roll(
            @RequestParam int statistic,
            @RequestParam int modifier,
            @RequestParam(defaultValue = "20") int sides,
            @RequestParam int difficulty) {
        return abilityChecks.resolveWithDie(statistic, modifier, sides, difficulty);
    }
}
