package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A single skill entry on a {@link PlayerCharacter}'s record.
 *
 * <p>Each entry records the skill's name, the total bonus the character applies
 * when making a skill check, and whether the character is
 * {@link #proficient} in that skill. Proficiency contributes to the skill bonus
 * through the character's {@link PlayerCharacter#getProficiencyBonus() proficiency
 * bonus}.</p>
 *
 * <p>This is an {@link Embeddable} row stored in the player character's skill
 * collection table.</p>
 */
@Embeddable
public class SkillEntry {

    @Column(name = "skill_name")
    private String name;

    @Column(name = "skill_bonus")
    private int bonus;

    @Column(name = "skill_proficient")
    private boolean proficient;

    public SkillEntry() {
        /* Required by JPA. */
    }

    public SkillEntry(String name, int bonus, boolean proficient) {
        this.name = name;
        this.bonus = bonus;
        this.proficient = proficient;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBonus() {
        return bonus;
    }

    public void setBonus(int bonus) {
        this.bonus = bonus;
    }

    public boolean isProficient() {
        return proficient;
    }

    public void setProficient(boolean proficient) {
        this.proficient = proficient;
    }
}
