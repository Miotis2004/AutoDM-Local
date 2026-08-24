package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The six core ability scores that define a {@link PlayerCharacter}'s physical
 * and mental capabilities. Each score is a raw value stored on the character's
 * record; modifiers are derived from the raw score when needed.
 *
 * <p>This is an {@link Embeddable} value object that is inlined into the
 * player character row rather than being its own table.</p>
 */
@Embeddable
public class AbilityScores {

    @Column(name = "ability_strength")
    private int strength;

    @Column(name = "ability_dexterity")
    private int dexterity;

    @Column(name = "ability_constitution")
    private int constitution;

    @Column(name = "ability_intelligence")
    private int intelligence;

    @Column(name = "ability_wisdom")
    private int wisdom;

    @Column(name = "ability_charisma")
    private int charisma;

    public AbilityScores() {
        /* Required by JPA. */
    }

    public AbilityScores(int strength, int dexterity, int constitution,
                         int intelligence, int wisdom, int charisma) {
        this.strength = strength;
        this.dexterity = dexterity;
        this.constitution = constitution;
        this.intelligence = intelligence;
        this.wisdom = wisdom;
        this.charisma = charisma;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public void setDexterity(int dexterity) {
        this.dexterity = dexterity;
    }

    public int getConstitution() {
        return constitution;
    }

    public void setConstitution(int constitution) {
        this.constitution = constitution;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }

    public int getWisdom() {
        return wisdom;
    }

    public void setWisdom(int wisdom) {
        this.wisdom = wisdom;
    }

    public int getCharisma() {
        return charisma;
    }

    public void setCharisma(int charisma) {
        this.charisma = charisma;
    }
}
