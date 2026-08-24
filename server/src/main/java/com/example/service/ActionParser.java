package com.example.service;

import com.example.domain.PlayerAction;
import com.example.domain.PlayerActionType;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Interprets free-form player text into a structured {@link PlayerAction}.
 *
 * <p>This is the game's natural-language interpretation seam. The {@link
 * com.example.service.DungeonMasterEngine} receives a raw, free-form {@link
 * com.example.domain.PlayerActionInput}; before it can validate or resolve that action it needs to
 * know <em>what</em> the player meant. The {@code ActionParser} answers that by turning a sentence
 * into a structured {@link PlayerActionType} plus any detail the type can hold (a governing skill,
 * a target).</p>
 *
 * <p>The parser is deliberately a small, explicit keyword matcher: it is predictable, easy to read,
 * and safe. It is also the single place a richer interpreter can be swapped in later - an LLM
 * provider could replace {@link #parse(String)} alone to gain nuance without touching validation,
 * resolution, or the REST surface. Anything the parser does not recognise is reported as
 * {@code null}, which lets the engine fall back to its existing behaviour instead of failing.</p>
 */
@Service
public class ActionParser {

    /** Detects the action type from the action's lower-cased tokens, using the first match. */
    public PlayerAction parse(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        List<String> tokens = List.of(action.toLowerCase(Locale.ROOT).split("\\s+"));

        PlayerActionType type = recognizeType(tokens);
        if (type == null) {
            return null;
        }
        return new PlayerAction(type, action.trim(), extractTarget(tokens), extractSkill(tokens));
    }

    /**
     * Finds the structured type the tokens express. Recognised verbs win over generic ones, so a
     * sentence like "I want to interact with the altar but persuade it" resolves to the specific
     * social attempt rather than the catch-all interact.
     *
     * @param tokens the lower-cased tokens of the action
     * @return the structured type, or {@code null} when none of the keywords are present
     */
    private PlayerActionType recognizeType(List<String> tokens) {
        for (PlayerActionType type : SPECIFIC_FIRST) {
            for (String keyword : keywordsFor(type)) {
                if (tokens.contains(keyword)) {
                    return type;
                }
            }
        }
        return null;
    }

    /**
     * Extracts a target name from the words following "at" or "against", for example the "guard"
     * in "attack at the guard".
     *
     * @param tokens the lower-cased tokens of the action
     * @return the first named target, or {@code null} when none follows a target preposition
     */
    private String extractTarget(List<String> tokens) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            if ("at".equals(tokens.get(i)) || "against".equals(tokens.get(i))) {
                String next = tokens.get(i + 1);
                if (isNameToken(next)) {
                    return capitalize(next);
                }
            }
        }
        return null;
    }

    /**
     * Extracts a governing skill when the action names one, for example the "Athletics" in "make a
     * Athletics check to jump the fountain".
     *
     * @param tokens the lower-cased tokens of the action
     * @return the named skill, or {@code null} when none is present
     */
    private String extractSkill(List<String> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (isSkillTrigger(tokens.get(i))) {
                String candidate = at(tokens, i + 1);
                if (candidate != null && isSkillName(candidate)) {
                    return capitalize(candidate);
                }
            }
        }
        return null;
    }

    /**
     * A name-like token is one that is not a common article or preposition, so the parser ignores
     * words like "the" or "a" when looking for a target.
     *
     * @param token the token to test
     * @return {@code true} when the token could be part of a name
     */
    private boolean isNameToken(String token) {
        return !List.of("the", "a", "an", "some", "any", "that", "this", "with", "and", "of", "in")
                .contains(token);
    }

    /** @param token a token that is a skill trigger word such as "check", "skill", or "roll". */
    private boolean isSkillTrigger(String token) {
        return List.of("check", "skill", "roll", "save").contains(token);
    }

    /** The common ability and skill names a player might name for a {@link PlayerActionType#SKILL}. */
    private static final java.util.Set<String> SKILL_NAMES = java.util.Set.of(
            "athletics", "acrobatics", "stealth", "perception", "insight", "arcana",
            "history", "religion", "nature", "deception", "persuasion", "intimidation",
            "animal handling", "medicine", "investigation", "survival", "performance");

    /**
     * @param token a lower-cased skill word, matching one of the common ability or skill names
     */
    private boolean isSkillName(String token) {
        return SKILL_NAMES.contains(token);
    }

    private String at(List<String> tokens, int index) {
        return index < tokens.size() ? tokens.get(index) : null;
    }

    private String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /**
     * The specific-first detection order. More specific action verbs are tested before generic
     * ones, so the first match is the most precise type the action expresses.
     */
    private static final List<PlayerActionType> SPECIFIC_FIRST = List.of(
            PlayerActionType.ATTACK,
            PlayerActionType.TALK,
            PlayerActionType.USE_ITEM,
            PlayerActionType.REST,
            PlayerActionType.SEARCH,
            PlayerActionType.INVESTIGATE,
            PlayerActionType.TRAVEL,
            PlayerActionType.SKILL,
            PlayerActionType.INTERACT);

    private static final java.util.Map<PlayerActionType, List<String>> KEYWORDS = java.util.Map.of(
            PlayerActionType.ATTACK, List.of("attack", "hit", "strike", "shoot", "fight", "slay", "kill"),
            PlayerActionType.TALK, List.of(
                    "persuade", "deceive", "intimidate", "talk", "negotiate", "plead", "lie",
                    "charm", "coerce", "convince", "interrogate", "flirt"),
            PlayerActionType.USE_ITEM, List.of(
                    "use", "drink", "eat", "smoke", "apply", "quaff", "activate", "ignite",
                    "heal", "cure", "mend", "restore", "treat"),
            PlayerActionType.REST, List.of("rest", "sleep", "downtime", "camp"),
            PlayerActionType.SEARCH, List.of("search", "look for", "rummage", "snoop", "scan"),
            PlayerActionType.INVESTIGATE, List.of(
                    "investigate", "examine", "inspect", "analyze", "analyse", "study", "read"),
            PlayerActionType.TRAVEL, List.of(
                    "travel", "go", "journey", "flee", "run", "escape", "retreat", "head to",
                    "walk to", "ride to"),
            PlayerActionType.SKILL, List.of("check", "skill", "roll", "save"),
            PlayerActionType.INTERACT, List.of(
                    "interact", "push", "pull", "lift", "move", "press", "turn", "drop", "throw",
                    "kick", "knock", "open", "close", "lock", "unlock", "drag"));

    /**
     * @param type the action type whose keywords to return
     * @return the keywords recognised for that type (never {@code null})
     */
    private List<String> keywordsFor(PlayerActionType type) {
        return KEYWORDS.getOrDefault(type, List.of());
    }
}
