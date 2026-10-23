package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * What Lion-Camel Ridge's renderers share: names shown only near, a glowing skin (eyes, gold)
 * drawn full-bright, a walk, and the bow its little demons make to the Great Peng.
 */
final class LionCourt {

    private LionCourt() {}

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

    /** Legs and arms in a walk: returns the stride. */
    static float walk(ModelPart rightLeg, ModelPart leftLeg, float swing, float amount, float pace, float reach) {
        float stride = Mth.cos(swing * pace) * reach * amount;
        rightLeg.xRot = stride;
        leftLeg.xRot = -stride;
        return stride;
    }

    /** A bow to the Great Peng: the body bent forward from the waist, the head lowered. */
    static void bow(ModelPart body, ModelPart head) {
        body.xRot = 0.55f;
        head.xRot = 0.35f;
        head.yRot = 0;
    }
}
