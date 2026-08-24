package com.example;

import com.example.domain.DiceRollResult;
import com.example.service.DiceService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST surface for rolling dice.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single
 * {@link DiceService} call. All validation, randomness, and total calculation lives in the
 * service, and the browser never generates any randomness.</p>
 */
@RestController
@RequestMapping("/api/dice")
public class DiceController {

    private final DiceService dice;

    public DiceController(DiceService dice) {
        this.dice = dice;
    }

    /**
     * Rolls one or more dice and applies a modifier.
     *
     * <p>Pass one {@code sides} per die to roll (repeatable), for example
     * {@code POST /api/dice/roll?sides=20&sides=20&modifier=+3} to roll {@code 2d20+3}.
     * Supported face counts are d4, d6, d8, d10, d12, d20, and percentile (100).</p>
     *
     * @param sides    the number of faces for each die to roll (repeatable; at least one required)
     * @param modifier the value added to the sum of the dice (may be negative; defaults to 0)
     * @return a {@link DiceRollResult} with the individual dice, modifier, and total
     */
    @PostMapping("/roll")
    public DiceRollResult roll(
            @RequestParam("sides") List<Integer> sides,
            @RequestParam(defaultValue = "0") int modifier) {
        return dice.roll(sides, modifier);
    }

    /**
     * Rolls a single die of the requested size with no modifier.
     *
     * @param sides the number of faces for the die (one of the supported die types)
     * @return a roll result for a single die
     */
    @PostMapping("/roll/single")
    public DiceRollResult rollSingle(@RequestParam("sides") int sides) {
        return dice.rollSingle(sides);
    }

    /**
     * Returns the list of supported dice so clients know which face counts are valid.
     *
     * @return a map of die label to description
     */
    @GetMapping("/catalog")
    public Map<String, String> catalog() {
        return dice.dieCatalog();
    }
}
