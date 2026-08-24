package com.example.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A compact, read-only snapshot of the scene the Dungeon Master engine is currently presenting.
 *
 * <p>The engine shows this brief to the players before, and around, each action. It carries the
 * active scene's title and narrative together with the names of the characters and NPCs involved
 * in, and the combatants present in, the scene, so a player (or an LLM provider) can reason about
 * "what is happening right now" without pulling the full persistence model into the resolution
 * path.</p>
 *
 * <p>This is a plain, immutable value holder. Every field is never {@code null}: the scene title
 * and narrative default to empty strings and the name lists default to empty lists.</p>
 */
public record SceneBrief(
        /** The scene's database id, or {@code null} when the scene has not yet been persisted. */
        Long sceneId,
        /** The scene's title (never {@code null}; empty when unknown). */
        String sceneTitle,
        /** The scene's free-form narrative (never {@code null}; empty when unknown). */
        String sceneNarrative,
        /** The names of the characters and NPCs involved in the scene (never {@code null}). */
        List<String> involvedNames,
        /** The names of the combatants present in the scene (never {@code null}). */
        List<String> combatantNames
) {

    /**
     * Builds a brief for the given scene and its involved characters and combatants.
     *
     * @param scene          the scene to summarise (never {@code null})
     * @param involvedNames  the names of the characters and NPCs involved in the scene (nullable)
     * @param combatants     the combatants present in the scene (may be {@code null} or empty)
     * @return the scene brief (never {@code null})
     */
    public static SceneBrief of(
            Scene scene, Set<String> involvedNames, List<Combatant> combatants) {
        return new SceneBrief(
                scene.getId(),
                scene.getTitle() == null ? "" : scene.getTitle(),
                scene.getNarrative() == null ? "" : scene.getNarrative(),
                involvedNames == null ? List.of() : new ArrayList<>(involvedNames),
                namesOfCombatants(combatants));
    }

    private static List<String> namesOfCombatants(List<Combatant> combatants) {
        if (combatants == null) {
            return List.of();
        }
        return combatants.stream()
                .filter(combatant -> combatant != null && combatant.getName() != null)
                .map(Combatant::getName)
                .toList();
    }
}
