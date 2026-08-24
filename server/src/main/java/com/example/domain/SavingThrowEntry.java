package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A single saving throw entry on a {@link PlayerCharacter}'s record.
 *
 * <p>Each entry records the ability the saving throw is tied to (for example
 * {@code STR}, {@code DEX}, and so on), the total bonus the character applies,
 * and whether the character is {@link #proficient} in that saving throw.</p>
 *
 * <p>This is an {@link Embeddable} row stored in the player character's saving
 * throw collection table.</p>
 */
@Embeddable
public class SavingThrowEntry {

    @Column(name = "saving_ability")
    private String ability;

    @Column(name = "saving_bonus")
    private int bonus;

    @Column(name = "saving_proficient")
    private boolean proficient;

    public SavingThrowEntry() {
        /* Required by JPA. */
    }

    public SavingThrowEntry(String ability, int bonus, boolean proficient) {
        this.ability = ability;
        this.bonus = bonus;
        this.proficient = proficient;
    }

    public String getAbility() {
        return ability;
    }

    public void setAbility(String ability) {
        this.ability = ability;
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
