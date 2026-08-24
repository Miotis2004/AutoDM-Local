package com.example.domain;

/**
 * The kind of participant a {@link Combatant} represents.
 *
 * <p>Player combatant are the party's heroes; enemy combatants are the adversaries
 * they face. The distinction is what lets the game tell whose side a participant
 * fights for when resolving combat and status effects.</p>
 */
public enum CombatantKind {
    PLAYER,
    ENEMY
}
