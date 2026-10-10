package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.ClientRingVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Renders the Ring of Fire visual — ground rune quad + flame particles.
 *
 * The rune is drawn as two explicit textured quads (outer ring + inner ring)
 * so UV is always (0,0)→(1,1) regardless of box dimensions, avoiding the
 * negative texOffs UV issue that caused the purple/black missing-texture look.
 *
 * Texture: assets/jttw_engine/textures/rune_fire.png  (64×64)
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FireRingVisualRenderer {

    private FireRingVisualRenderer() {}

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/rune_ring.png");

    // Matches the server-side ring radius
    private static final float OUTER_RADIUS = 5.0f;
    // "inside" bone is scaled 0.75 in your bbmodel animation
    private static final float INNER_RADIUS = OUTER_RADIUS * 0.75f;

    // Y offset above the player's feet so the quad sits on the ground
    private static final float GROUND_Y = 0.02f;

    // Full-bright so the rune glows regardless of ambient light
    private static final int FULL_BRIGHT = 0xF000F0;

    // Rotation speed — outer rotates forward, inner rotates backward
    private static float outerAngle = 0f;
    private static float innerAngle = 0f;

    // ── Particles ─────────────────────────────────────────────────────────────
    private static final int    PILLARS    = 24;
    private static final double TWO_PI     = Math.PI * 2.0;
    private static final Random RNG        = new Random();
    private static double phaseOffset      = 0.0;

    // =========================================================================
    // Rune render — RenderPlayerEvent.Post
    // =========================================================================

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        float presence = ClientRingVisuals.presence(player.getUUID(), event.getPartialTick());
        if (presence <= 0) return;

        PoseStack      pose = event.getPoseStack();
        MultiBufferSource buf = event.getMultiBufferSource();
        VertexConsumer vc   = buf.getBuffer(RenderType.entityTranslucent(TEX)); // it fades: drawn through
        float grow = 0.6f + 0.4f * presence; // and swells into place as it fades in

        // Outer rune — rotates forward
        pose.pushPose();
        pose.translate(0.0, GROUND_Y, 0.0);
        pose.mulPose(Axis.YP.rotation(outerAngle));
        drawQuad(pose, vc, OUTER_RADIUS * grow, presence);
        pose.popPose();

        // Inner rune — counter-rotates at 75% size
        pose.pushPose();
        pose.translate(0.0, GROUND_Y + 0.01, 0.0); // tiny Z-fight offset
        pose.mulPose(Axis.YP.rotation(innerAngle));
        drawQuad(pose, vc, INNER_RADIUS * grow, presence);
        pose.popPose();
    }

    /**
     * Draws a flat textured quad centred at the current PoseStack origin.
     * Two faces (up + down) so it's visible from both sides.
     * UV is always (0,0)→(1,1) — the full texture sheet.
     */
    private static void drawQuad(PoseStack pose, VertexConsumer vc, float r, float presence) {
        var matrix = pose.last().pose();
        var normal = pose.last().normal();
        float a = 0.9f * presence;

        // Face up
        vc.vertex(matrix, -r, 0f, -r).color(1f,1f,1f,a).uv(0f,0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(normal, 0f,1f,0f).endVertex();
        vc.vertex(matrix, -r, 0f,  r).color(1f,1f,1f,a).uv(0f,1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(normal, 0f,1f,0f).endVertex();
        vc.vertex(matrix,  r, 0f,  r).color(1f,1f,1f,a).uv(1f,1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(normal, 0f,1f,0f).endVertex();
        vc.vertex(matrix,  r, 0f, -r).color(1f,1f,1f,a).uv(1f,0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(normal, 0f,1f,0f).endVertex();

        // Face down (reversed winding)
        vc.vertex(matrix,  r, 0f, -r).color(1f,1f,1f,a).uv(1f,0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(normal, 0f,-1f,0f).endVertex();
        vc.vertex(matrix,  r, 0f,  r).color(1f,1f,1f,a).uv(1f,1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(normal, 0f,-1f,0f).endVertex();
        vc.vertex(matrix, -r, 0f,  r).color(1f,1f,1f,a).uv(0f,1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(normal, 0f,-1f,0f).endVertex();
        vc.vertex(matrix, -r, 0f, -r).color(1f,1f,1f,a).uv(0f,0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(normal, 0f,-1f,0f).endVertex();
    }

    // =========================================================================
    // Tick — advance rotation + spawn particles
    // =========================================================================

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        // Advance rotation angles each tick
        outerAngle += 0.01f;
        innerAngle -= 0.015f;
        phaseOffset += TWO_PI / 600.0;

        ClientRingVisuals.tickFade();
        for (Player player : mc.level.players()) {
            float presence = ClientRingVisuals.presence(player.getUUID(), 1f);
            if (presence <= 0) continue;
            spawnRingParticles(mc, player, presence);
        }
    }

    private static void spawnRingParticles(Minecraft mc, Player player, float presence) {
        Vec3 origin = player.position();

        for (int i = 0; i < PILLARS; i++) {
            if (RNG.nextFloat() > presence) continue; // its flames thin as it fades
            double angle = phaseOffset + (TWO_PI * i / PILLARS);
            double cos   = Math.cos(angle);
            double sin   = Math.sin(angle);
            double bx    = origin.x + cos * OUTER_RADIUS;
            double bz    = origin.z + sin * OUTER_RADIUS;

            mc.level.addParticle(ParticleTypes.FLAME,
                    bx + RNG.nextGaussian() * 0.08, origin.y + 0.05,
                    bz + RNG.nextGaussian() * 0.08,
                    cos * 0.02, 0.04 + RNG.nextDouble() * 0.04, sin * 0.02);

            if (i % 2 == 0) {
                mc.level.addParticle(ParticleTypes.SMALL_FLAME,
                        bx + RNG.nextGaussian() * 0.06, origin.y + 0.6,
                        bz + RNG.nextGaussian() * 0.06,
                        cos * 0.01, 0.05 + RNG.nextDouble() * 0.03, sin * 0.01);
            }
            if (i % 3 == 0) {
                mc.level.addParticle(ParticleTypes.SMOKE,
                        bx + RNG.nextGaussian() * 0.12, origin.y + 1.2,
                        bz + RNG.nextGaussian() * 0.12,
                        cos * 0.005, 0.06 + RNG.nextDouble() * 0.04, sin * 0.005);
            }
            if (RNG.nextInt(8) == 0) {
                mc.level.addParticle(ParticleTypes.DRIPPING_LAVA,
                        bx, origin.y + 0.6 + RNG.nextDouble() * 0.4, bz,
                        cos * 0.04, 0.08, sin * 0.04);
            }
        }
    }
}