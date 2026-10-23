package com.jttw.engine.client.renderer;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

/** What the Mystical Waters' renderers share. */
final class SeaCourt {

    private SeaCourt() {}

    /** Names over heads show only this close, so a patrol is not a wall of labels. */
    static final double NAME_RANGE = 24;

    static boolean near(EntityRenderDispatcher d, Entity e) {
        return d.distanceToSqr(e) < NAME_RANGE * NAME_RANGE;
    }
}
