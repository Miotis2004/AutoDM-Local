package com.example.domain;

/**
 * The kind of damage an attack or ability deals.
 *
 * <p>Damage types let the combat system describe what kind of harm a landed attack
 * inflicts — the raw hit-point reduction is only half of the story. The canonical types
 * follow the usual fantasy role-playing conventions: the bruising impact of a
 * {@link #PHYSICAL} blow, the spread of {@link #FIRE}, the bite of {@link #COLD}, the
 * crack of {@link #THUNDER}, and so on. The list is deliberately small and open: callers
 * choose the value that best fits the moment and may extend it.</p>
 *
 * <p>Every attack carries a damage type. When an attack does not specify one it is treated
 * as {@link #PHYSICAL}, the default kind of harm dealt by weapons and unarmed strikes.</p>
 */
public enum DamageType {
    /** A mundane, bludgeoning/slicing strike — the default kind of physical harm. */
    PHYSICAL,
    /** Harm from fire, heat, or flame. */
    FIRE,
    /** Harm from cold, ice, or freezing cold. */
    COLD,
    /** Harm from electricity or a crackling discharge. */
    LIGHTNING,
    /** Harm from thunder or a devastating sonic boom. */
    THUNDER,
    /** Harm from acid or corrosive substance. */
    ACID,
    /** Harm from poison or toxin. */
    POISON,
    /** Harm woven from arcane or magical energy. */
    MAGIC,
    /** Harm from radiant, holy light. */
    RADIANT,
    /** Harm drawn from necromancy or the grave. */
    NECROTIC
}
