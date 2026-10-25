package com.jttw.engine.entity.zone;

import net.minecraft.world.entity.Entity;

/**
 * A creature that carries a level, as the zones' creatures, their bosses and Heaven's soldiers do
 * (the "· Lv" in their names). What weighs one creature against a player's level reads it here.
 */
public interface MobLevel {

    /** Its level. */
    int mobLevel();

    /**
     * The level of any entity: its own, if it carries one; otherwise 0, so a vanilla mob, which
     * has none, stands below every player.
     */
    static int of(Entity e) {
        return e instanceof MobLevel m ? m.mobLevel() : 0;
    }
}
