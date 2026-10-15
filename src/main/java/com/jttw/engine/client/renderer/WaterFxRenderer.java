package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.flight.NimbusCloud;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The Saurian Demon King's water, drawn where it can be read from his own eyes: the Tidal Lash as
 * a rope of water beads shooting from his hand to what it caught, and the Whirlpool as a turning
 * spiral of water laid on the ground.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class WaterFxRenderer {

    private WaterFxRenderer() {}

    private record Lash(int owner, int caught, Vec3 to, long start, int ticks) {}
    private record Pool(Vec3 at, float radius, long start, int ticks) {}

    private static final List<Lash> LASHES = new ArrayList<>();
    private static final List<Pool> POOLS = new ArrayList<>();

    private static final ResourceLocation SWIRL = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "whirlpool_swirl");
    private static final RenderType SWIRL_TYPE = RenderType.text(SWIRL);
    private static boolean swirlMade;

    /** Ticks the rope takes to shoot out to its end. */
    private static final float SHOOT = 2;

    public static void lash(int owner, int caught, Vec3 to, int ticks) {
        if (Minecraft.getInstance().level == null) return;
        LASHES.add(new Lash(owner, caught, to, Minecraft.getInstance().level.getGameTime(), ticks));
    }

    public static void whirlpool(Vec3 at, float radius, int ticks) {
        if (Minecraft.getInstance().level == null) return;
        POOLS.add(new Pool(at, radius, Minecraft.getInstance().level.getGameTime(), ticks));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            LASHES.clear();
            POOLS.clear();
            return;
        }
        long now = level.getGameTime();
        LASHES.removeIf(l -> now - l.start() > l.ticks());
        POOLS.removeIf(p -> now - p.start() > p.ticks());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || (LASHES.isEmpty() && POOLS.isEmpty())) return;
        float partial = event.getPartialTick();
        long now = mc.level.getGameTime();
        Camera cam = event.getCamera();
        Vec3 c = cam.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-c.x, -c.y, -c.z);
        for (Pool p : POOLS) drawPool(pose, buffers, p, now - p.start() + partial);
        buffers.endBatch(SWIRL_TYPE);
        for (Lash l : LASHES) drawLash(pose, buffers, l, now - l.start() + partial, partial, cam);
        buffers.endBatch(NimbusCloud.renderType());
        pose.popPose();
    }

    // ── the lash ───────────────────────────────────────────────────

    /** His hand: seen from his own eyes, low and to the right of the view; else by his body. */
    private static Vec3 hand(Entity owner, float partial, Camera cam) {
        Minecraft mc = Minecraft.getInstance();
        if (owner == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Vector3f look = cam.getLookVector(), up = cam.getUpVector(), left = cam.getLeftVector();
            return cam.getPosition().add(look.x() * 0.8, look.y() * 0.8, look.z() * 0.8)
                    .add(-left.x() * 0.45, -left.y() * 0.45, -left.z() * 0.45)
                    .add(-up.x() * 0.4, -up.y() * 0.4, -up.z() * 0.4);
        }
        Vec3 look = owner.getViewVector(partial);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        return owner.getEyePosition(partial).add(look.scale(0.4)).add(right.scale(0.35)).add(0, -0.35, 0);
    }

    private static void drawLash(PoseStack pose, MultiBufferSource buffers, Lash l, float age, float partial, Camera cam) {
        Minecraft mc = Minecraft.getInstance();
        Entity owner = mc.level.getEntity(l.owner());
        if (owner == null) return;
        Entity caught = l.caught() >= 0 ? mc.level.getEntity(l.caught()) : null;
        Vec3 from = hand(owner, partial, cam);
        Vec3 end = caught != null && caught.isAlive()
                ? caught.getPosition(partial).add(0, caught.getBbHeight() * 0.55, 0) : l.to();
        float shot = Mth.clamp(age / SHOOT, 0, 1), fade = Mth.clamp((l.ticks() - age) / 4f, 0, 1);
        if (fade <= 0) return;
        Vec3 d = end.subtract(from).scale(shot);
        double len = d.length();
        if (len < 0.05) return;
        Vec3 dir = d.scale(1 / len);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 lift = side.cross(dir);
        List<float[]> beads = new ArrayList<>();
        int n = Math.max(2, (int) (len / 0.16));
        for (int i = 0; i <= n; i++) {
            float u = i / (float) n;
            // a rope that writhes a little, more toward its free end, less as it settles
            float writhe = 0.07f * Mth.sin(u * 14 - age * 1.6f) * (0.3f + u) * fade;
            Vec3 at = from.add(d.scale(u)).add(side.scale(writhe)).add(lift.scale(writhe * 0.6));
            boolean light = (i % 3) == 0;
            float r = (0.09f + 0.035f * Mth.sin(u * 9 + age)) * (0.6f + 0.4f * (1 - u * 0.5f));
            beads.add(new float[]{(float) at.x, (float) at.y, (float) at.z, r, u * 7 + age * 0.3f, 0.85f * fade,
                    light ? 0.62f : 0.32f, light ? 0.86f : 0.64f, light ? 1f : 0.95f});
        }
        // where it struck, a burst of spray for its first moments
        if (shot >= 1 && age < SHOOT + 3) {
            Vec3 tip = from.add(d);
            float burst = 1 - (age - SHOOT) / 3f;
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3 + age;
                beads.add(new float[]{(float) (tip.x + Math.cos(a) * 0.25), (float) (tip.y + Math.sin(a * 1.3) * 0.2),
                        (float) (tip.z + Math.sin(a) * 0.25), 0.22f * burst + 0.1f, (float) a, 0.8f * burst * fade, 0.75f, 0.92f, 1f});
            }
        }
        int light = LevelRenderer.getLightColor(Minecraft.getInstance().level, BlockPos.containing(from.add(d.scale(0.5))));
        NimbusCloud.drawPuffs(pose, buffers, beads, Math.max(light, 0xA000A0));
    }

    // ── the whirlpool ──────────────────────────────────────────────

    private static void drawPool(PoseStack pose, MultiBufferSource buffers, Pool p, float age) {
        makeSwirl();
        float fade = Mth.clamp(age / 10f, 0, 1) * Mth.clamp((p.ticks() - age) / 15f, 0, 1);
        if (fade <= 0) return;
        int light = LevelRenderer.getLightColor(Minecraft.getInstance().level, BlockPos.containing(p.at().add(0, 0.5, 0)));
        VertexConsumer vc = buffers.getBuffer(SWIRL_TYPE);
        // two layers turning at their own pace, the inner faster: water drawn down and round
        layer(pose, vc, p.at(), 0.04f, p.radius(), -age * 0.07f, 0.95f * fade, light);
        layer(pose, vc, p.at(), 0.07f, p.radius() * 0.6f, -age * 0.12f + 1.3f, 0.7f * fade, light);
    }

    private static void layer(PoseStack pose, VertexConsumer vc, Vec3 at, float lift, float r, float angle, float alpha, int light) {
        pose.pushPose();
        pose.translate(at.x, at.y + lift, at.z);
        pose.mulPose(Axis.YP.rotation(angle));
        Matrix4f m = pose.last().pose();
        // both faces, so it shows from above and, through water, from below
        vc.vertex(m, -r, 0, -r).color(1f, 1f, 1f, alpha).uv(0, 0).uv2(light).endVertex();
        vc.vertex(m, -r, 0, r).color(1f, 1f, 1f, alpha).uv(0, 1).uv2(light).endVertex();
        vc.vertex(m, r, 0, r).color(1f, 1f, 1f, alpha).uv(1, 1).uv2(light).endVertex();
        vc.vertex(m, r, 0, -r).color(1f, 1f, 1f, alpha).uv(1, 0).uv2(light).endVertex();
        vc.vertex(m, r, 0, -r).color(1f, 1f, 1f, alpha).uv(1, 0).uv2(light).endVertex();
        vc.vertex(m, r, 0, r).color(1f, 1f, 1f, alpha).uv(1, 1).uv2(light).endVertex();
        vc.vertex(m, -r, 0, r).color(1f, 1f, 1f, alpha).uv(0, 1).uv2(light).endVertex();
        vc.vertex(m, -r, 0, -r).color(1f, 1f, 1f, alpha).uv(0, 0).uv2(light).endVertex();
        pose.popPose();
    }

    /** A spiral of water: foam-crested arms curling in to a dark, deep heart. Drawn once. */
    private static void makeSwirl() {
        if (swirlMade) return;
        swirlMade = true;
        int size = 128;
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float u = (x + 0.5f) / size * 2 - 1, v = (y + 0.5f) / size * 2 - 1;
                float r = Mth.sqrt(u * u + v * v);
                float theta = (float) Math.atan2(v, u);
                float phase = 4 * theta + 7 * (float) Math.log(r + 0.08);
                float crest = (float) Math.pow(0.5 + 0.5 * Math.cos(phase), 3);
                float edge = Mth.clamp((1 - r) / 0.22f, 0, 1);
                edge = edge * edge * (3 - 2 * edge);
                float a, cr, cg, cb;
                if (r < 0.16f) { // the heart: dark and deep
                    float k = r / 0.16f;
                    a = 0.9f;
                    cr = 12 + 20 * k; cg = 34 + 40 * k; cb = 70 + 60 * k;
                } else {
                    float foam = crest * (0.35f + 0.65f * r);
                    a = (0.35f + 0.6f * crest) * edge;
                    cr = Mth.lerp(foam, 28, 205); cg = Mth.lerp(foam, 92, 238); cb = Mth.lerp(foam, 150, 252);
                }
                int ia = (int) (Mth.clamp(a, 0, 1) * 255);
                img.setPixelRGBA(x, y, (ia << 24) | ((int) cb << 16) | ((int) cg << 8) | (int) cr);
            }
        }
        DynamicTexture tex = new DynamicTexture(img);
        Minecraft.getInstance().getTextureManager().register(SWIRL, tex);
        tex.setFilter(true, false);
    }
}
