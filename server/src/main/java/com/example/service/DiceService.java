package com.example.service;

import com.example.domain.DieResult;
import com.example.domain.DiceRollResult;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Business and game logic for rolling dice.
 *
 * <p>This service is the single owner of every dice rule in the application: which die
 * types are valid, how many dice may be combined, how modifiers are applied, and — most
 * importantly — how randomness is produced. All randomness is generated on the back-end with
 * a {@link SecureRandom}, never in the browser, so rolls are reproducible under audit and
 * cannot be tampered with by the client.</p>
 *
 * <p>The service is intentionally stateless. It performs no persistence, so a roll is a pure
 * function of its inputs plus freshly generated random values.</p>
 */
@Service
public class DiceService {

    private final SecureRandom random;

    /**
     * Creates the service. The {@link SecureRandom} backs every roll, so randomness always
     * originates on the back-end rather than in the browser.
     */
    public DiceService() {
        this(new SecureRandom());
    }

    /**
     * Package-visible constructor that injects the randomness source, primarily so the
     * generator can be observed in verification.
     *
     * @param random the entropy source used for every roll
     */
    DiceService(SecureRandom random) {
        this.random = random;
    }

    /**
     * Rolls one or more dice and applies a modifier.
     *
     * <p>Each entry in {@code dieSides} names a die to roll and the number of faces it has.
     * Supported face counts are d4, d6, d8, d10, d12, d20, and percentile (100). Multiple dice
     * may be combined in a single roll, and the {@code modifier} is added to the sum of the dice
     * (it may be negative to subtract).</p>
     *
     * @param dieSides   the number of faces for each die to roll; at least one is required
     * @param modifier   the value added to the sum of the dice (may be negative)
     * @return a {@link DiceRollResult} describing each die, the modifier, and the total
     * @throws IllegalArgumentException if no dice are supplied or any die is not supported
     */
    public DiceRollResult roll(List<Integer> dieSides, int modifier) {
        List<Integer> sides = normalizeSides(dieSides);
        if (sides.isEmpty()) {
            throw new IllegalArgumentException("A roll must include at least one die");
        }
        List<DieResult> rolled = new ArrayList<>(sides.size());
        for (int faces : sides) {
            rolled.add(DieResult.of(faces, rollWithin(faces)));
        }
        return DiceRollResult.of(rolled, modifier);
    }

    /**
     * Rolls a single die of the given size and applies no modifier.
     *
     * @param sides the number of faces for the die (one of the supported die types)
     * @return a roll result for a single die
     */
    public DiceRollResult rollSingle(int sides) {
        return roll(List.of(sides), 0);
    }

    /**
     * Renders a roll result as a compact, human-readable breakdown string, for example
     * {@code "2d20: [12, 7] + 3 = 22"} or {@code "d100: [41] = 41"}.
     *
     * @param result the roll result to summarize
     * @return the readable breakdown
     */
    public String summarize(DiceRollResult result) {
        StringBuilder label = new StringBuilder();
        if (result.percentile()) {
            label.append("percentile");
        } else {
            label.append(result.dice().size()).append('d').append(result.dice().get(0).sides());
        }
        label.append(": ").append(result.dice());
        int effective = result.modifier();
        if (effective > 0) {
            label.append(" + ").append(effective);
        } else if (effective < 0) {
            label.append(" - ").append(Math.abs(effective));
        }
        label.append(" = ").append(result.total());
        return label.toString();
    }

    /**
     * Returns the set of die face counts this service recognizes as valid dice.
     *
     * @return an immutable list of supported face counts (d4 through d20 and percentile)
     */
    public List<Integer> supportedDieSides() {
        return DiceRollResult.SUPPORTED_DIE_SIDES;
    }

    private List<Integer> normalizeSides(List<Integer> dieSides) {
        if (dieSides == null) {
            return List.of();
        }
        List<Integer> normalized = new ArrayList<>();
        for (int faces : dieSides) {
            if (!DiceRollResult.SUPPORTED_DIE_SIDES.contains(faces)) {
                throw new IllegalArgumentException(
                        "Unsupported die: d" + faces + ". Supported dice are "
                                + DiceRollResult.SUPPORTED_DIE_SIDES + ".");
            }
            normalized.add(faces);
        }
        return normalized;
    }

    /**
     * Rolls an inclusive random value in the range {@code 1..faces} using back-end randomness.
     *
     * @param faces the number of faces on the die (validated to be at least 2 by the caller)
     * @return a value between 1 and {@code faces} inclusive
     */
    private int rollWithin(int faces) {
        return random.nextInt(faces) + 1;
    }

    /**
     * A convenience map describing the supported dice, keyed by die label, for clients that want
     * a catalog of what the service can roll.
     *
     * @return a map of die label to description
     */
    public Map<String, String> dieCatalog() {
        return Map.of(
                "d4", "Four-sided die (minimum)",
                "d6", "Six-sided die",
                "d8", "Eight-sided die",
                "d10", "Ten-sided die",
                "d12", "Twelve-sided die",
                "d20", "Twenty-sided die",
                "d100", "Percentile die");
    }
}
