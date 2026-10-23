package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** What the Flaming Mountains' renderers share: names shown only near, and a glowing skin drawn full-bright. */
final class FireCourt {

    private FireCourt() {}

    static final double NAME_RANGE = 24;

    static boolean near(EntityRenderDispatcher d, Entity e) {
        return d.distanceToSqr(e) < NAME_RANGE * NAME_RANGE;
    }

    /** The glowing parts of a skin ({@code textures/entity/<name>_glow.png}), drawn full-bright. */
    static <T extends LivingEntity, M extends EntityModel<T>> EyesLayer<T, M> glow(RenderLayerParent<T, M> parent, String name) {
        RenderType type = RenderType.eyes(JTTWEngine.id("textures/entity/" + name + "_glow.png"));
        return new EyesLayer<>(parent) {
            @Override
            public RenderType renderType() {
                return type;
            }
        };
    }
}
