package com.example.service;

import com.example.domain.CreatureTemplate;
import com.example.domain.Encounter;
import com.example.domain.EncounterDifficulty;

import java.util.List;

/**
 * The outcome of an automated encounter generation: the created {@link Encounter}
 * together with the details of how it was built.
 *
 * <p>It records the encounter itself, the enemy combatants instantiated into it, the
 * party that drove the size of the encounter (its resolved size and average level), the
 * difficulty that was applied, the threat budget that was targeted, and the templates
 * that were selected to fill that budget. Callers can use the encounter directly, or the
 * accompanying details to explain or audit what the generator produced.</p>
 */
public record EncounterGenerationResult(
        Encounter encounter,
        List<com.example.domain.Combatant> enemies,
        List<CreatureTemplate> templates,
        int partySize,
        int averageLevel,
        EncounterDifficulty difficulty,
        double budget) {

    /**
     * @return the number of enemies instantiated into the encounter
     */
    public int enemyCount() {
        return enemies == null ? 0 : enemies.size();
    }
}
