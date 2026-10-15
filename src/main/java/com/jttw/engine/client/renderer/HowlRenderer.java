package com.jttw.engine.client.renderer;

import com.jttw.engine.entity.HowlProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Xiaotian Quan's howl: a cone of sound, narrow where it left the hound's jaws and flaring wide at
 * its front, glowing faintly, with ripples racing along it toward what it will strike.
 */
public class HowlRenderer extends EntityRenderer<HowlProjectileEntity> {

    /** How far the cone trails behind its front, and how wide its mouth is, in blocks. */
    private static final float LENGTH = 2.2f, MOUTH = 0.75f;
    private static final int SIDES = 18, RINGS = 3;
    /** Pale gold, the colour of a celestial hound's voice. */
    private static final float R = 1f, G = 0.9f, B = 0.62f;

    public HowlRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(HowlProjectileEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 v = e.getDeltaMovement();
        Vec3 dir = v.lengthSqr() > 1e-6 ? v.normalize() : Vec3.directionFromRotation(e.getXRot(), e.getYRot());
        float age = e.tickCount + partial;
        // it swells out of the jaws over its first few ticks
        float grow = Mth.clamp(age / 4f, 0.25f, 1f);
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotationTo(0, 0, 1, (float) dir.x, (float) dir.y, (float) dir.z));
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        float length = LENGTH * grow, mouth = MOUTH * grow;
        // the cone's skin, from its point behind to its mouth in front, fading toward the point
        for (int i = 0; i < SIDES; i++) {
            float a0 = i * Mth.TWO_PI / SIDES, a1 = (i + 1) * Mth.TWO_PI / SIDES;
            band(vc, m, a0, a1, -length, 0.02f, 0, mouth, 0.0f, 0.22f);
        }
        // ripples racing forward along it
        for (int k = 0; k < RINGS; k++) {
            float u = ((age * 0.35f) + k / (float) RINGS) % 1f; // 0 at the point, 1 at the mouth
            float z = -length + u * length, r = 0.02f + u * (mouth - 0.02f), w = 0.06f + 0.1f * u;
            float alpha = 0.55f * Mth.sin(u * Mth.PI);
            for (int i = 0; i < SIDES; i++) {
                float a0 = i * Mth.TWO_PI / SIDES, a1 = (i + 1) * Mth.TWO_PI / SIDES;
                band(vc, m, a0, a1, z - w, r * (1 - w / Math.max(0.01f, length)), z, r, alpha, alpha);
            }
        }
        // the mouth's bright rim
        for (int i = 0; i < SIDES; i++) {
            float a0 = i * Mth.TWO_PI / SIDES, a1 = (i + 1) * Mth.TWO_PI / SIDES;
            band(vc, m, a0, a1, -0.12f, mouth * 0.96f, 0, mouth * 1.04f, 0.5f, 0.5f);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    /** A strip of the cone between two angles, from radius r0 at z0 to r1 at z1, both faces. */
    private static void band(VertexConsumer vc, Matrix4f m, float a0, float a1, float z0, float r0, float z1, float r1, float alpha0, float alpha1) {
        float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
        vc.addVertex(m, c0 * r0, s0 * r0, z0).setColor(R, G, B, alpha0);
        vc.addVertex(m, c1 * r0, s1 * r0, z0).setColor(R, G, B, alpha0);
        vc.addVertex(m, c1 * r1, s1 * r1, z1).setColor(R, G, B, alpha1);
        vc.addVertex(m, c0 * r1, s0 * r1, z1).setColor(R, G, B, alpha1);
        vc.addVertex(m, c0 * r1, s0 * r1, z1).setColor(R, G, B, alpha1);
        vc.addVertex(m, c1 * r1, s1 * r1, z1).setColor(R, G, B, alpha1);
        vc.addVertex(m, c1 * r0, s1 * r0, z0).setColor(R, G, B, alpha0);
        vc.addVertex(m, c0 * r0, s0 * r0, z0).setColor(R, G, B, alpha0);
    }

    @Override
    public ResourceLocation getTextureLocation(HowlProjectileEntity e) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    }
}
