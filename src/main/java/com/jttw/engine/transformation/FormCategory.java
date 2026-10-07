package com.jttw.engine.transformation;

/**
 * What kind of form a transformation target is, which decides which transformation menus
 * may offer it ({@link TransformationSchool}). {@link FormClassifier} sorts entities into these.
 */
public enum FormCategory {
    /** Non-hostile mobs that live on land (cows, wolves, villagers, iron golems...). */
    NEUTRAL_LAND("Land"),
    /** Non-hostile mobs that live in water (squid, fish, dolphins, axolotls...). */
    NEUTRAL_WATER("Water"),
    /** Non-hostile mobs that fly (bees, parrots, bats, allays...). */
    NEUTRAL_AIR("Sky"),
    /** Hostile mobs that live on land (zombies, skeletons, endermen...). Not bosses. */
    HOSTILE_LAND("Hostile"),
    /** Hostile mobs that live in water (drowned, guardians). Not bosses. */
    HOSTILE_WATER("Hostile Sea"),
    /** Hostile mobs that fly (phantoms, ghasts, blazes, vexes). Not bosses. */
    HOSTILE_AIR("Hostile Sky"),
    /** Bosses: Ender Dragon, Wither, Elder Guardian, Warden, and anything tagged as a boss. */
    BOSS("Bosses"),
    /** Another player's appearance. */
    PLAYER("Players"),
    /** Non-living entities: minecarts, boats, arrows, falling blocks... */
    OBJECT("Objects");

    /** Short label for the menu tab. */
    public final String label;

    FormCategory(String label) {
        this.label = label;
    }
}
