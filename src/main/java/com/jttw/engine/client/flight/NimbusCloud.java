package com.jttw.engine.client.flight;

import com.jttw.engine.JTTWEngine;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The cloud a flier rides: a heap of soft round puffs, each turned to face the eye, domed over
 * the middle and thinning to the edges. Nothing about it repeats: every flier's heap is laid out
 * their own way, each puff drifts and breathes on rhythms of its own that never fall back into
 * step, and the puffs round the edge slowly melt away and gather again somewhere else.
 */
public final class NimbusCloud {

    private NimbusCloud() {}

    static final ResourceLocation PUFF = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "nimbus_puff");
    /** Unshaded, so the puffs keep their own whites and greys whichever way they face. */
    private static final RenderType TYPE = RenderType.text(PUFF);

    /** Puffs that stay, the heart of the cloud; and those round the edge that come and go. */
    private static final int CORE = 11, DRIFTERS = 17;

    private static boolean textureMade;

    static RenderType renderType() {
        return TYPE;
    }

    /** A number in [0, 1) that is always the same for the same flier, puff and question. */
    static float h(long seed, int a, int b, int c) {
        long x = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (c * 0x165667B19E3779F9L);
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static float wave(float t, long seed, int i, int k, float amp) {
        // two sines at frequencies with no common beat: the drift never comes round again
        float f1 = 0.3f + 0.6f * h(seed, i, k, 1), f2 = f1 * 1.618f + 0.2f * h(seed, i, k, 2);
        return amp * (Mth.sin(t * f1 + 6.283f * h(seed, i, k, 3)) + 0.5f * Mth.sin(t * f2 + 6.283f * h(seed, i, k, 4)));
    }

    /** x, y, z, radius, spin, alpha, and red, green and blue of each puff, {@code t} seconds on. */
    private static List<float[]> puffs(long seed, float t, float presence) {
        List<float[]> out = new ArrayList<>(CORE + DRIFTERS);
        float spread = 1.4f - 0.4f * presence, grow = 0.55f + 0.45f * presence;
        float bob = 0.05f * Mth.sin(0.83f * t + 6.283f * h(seed, 0, 0, 5))
                + 0.025f * Mth.sin(1.37f * t + 6.283f * h(seed, 0, 0, 6))
                + 0.015f * Mth.sin(2.21f * t + 6.283f * h(seed, 0, 0, 7));
        for (int i = 0; i < CORE + DRIFTERS; i++) {
            boolean core = i < CORE;
            int life = 0;
            float there = 1;
            if (!core) {
                // each comes and goes over its own five to eleven seconds, and comes back elsewhere
                float span = 5 + 6 * h(seed, i, 0, 8), at = t + span * h(seed, i, 0, 9);
                life = (int) Math.floor(at / span);
                float f = at / span - life;
                there = Math.min(1, 2.5f * Mth.sin((float) Math.PI * f));
            }
            float angle = 6.283f * h(seed, i, life, 10);
            float out01 = core ? 0.65f * Mth.sqrt(h(seed, i, life, 11)) : 0.55f + 0.45f * h(seed, i, life, 11);
            float x = Mth.cos(angle) * out01 * 1.3f, z = Mth.sin(angle) * out01 * 1.1f;
            float y = -0.2f + 0.22f * (1 - out01 * out01) + 0.08f * (h(seed, i, life, 12) - 0.5f);
            float r = 0.48f + 0.34f * (1 - out01) + 0.2f * h(seed, i, life, 13);
            if (h(seed, i, life, 14) < 0.25f) { // some hang under the rest, filling it out below
                y -= 0.17f;
                r *= 0.8f;
            }
            x += wave(t, seed, i, 20, 0.06f);
            y += wave(t, seed, i, 21, 0.04f) + bob;
            z += wave(t, seed, i, 22, 0.06f);
            r *= 1 + wave(t, seed, i, 23, 0.08f);
            r *= core ? 1 : 0.7f + 0.3f * there;
            float spin = 6.283f * h(seed, i, life, 15) + t * (h(seed, i, life, 16) - 0.5f) * 0.5f;
            // the underside a shade greyer, the top bright
            float shade = 0.84f + 0.16f * Mth.clamp((y - bob + 0.38f) / 0.5f, 0, 1);
            out.add(new float[]{x * spread, y, z * spread, r * grow, spin, 0.9f * presence * there, shade, shade, Math.min(1, shade + 0.03f)});
        }
        return out;
    }

    /** Draws the cloud about the origin of {@code pose}, under the feet. */
    public static void draw(PoseStack pose, MultiBufferSource buffers, net.minecraft.world.entity.Entity p, float presence, int light) {
        if (presence <= 0) return;
        long seed = p.getUUID().getLeastSignificantBits() ^ p.getUUID().getMostSignificantBits();
        float t = (p.tickCount + Minecraft.getInstance().getFrameTime()) / 20f;
        drawPuffs(pose, buffers, puffs(seed, t, presence), Math.max(light, LightTexture.pack(14, 14))); // it glows a little, even at night
    }

    /**
     * Draws soft puffs about the origin of {@code pose}, each turned to face the eye: per puff x, y,
     * z, radius, spin, alpha, red, green, blue.
     */
    static void drawPuffs(PoseStack pose, MultiBufferSource buffers, List<float[]> puffs, int light) {
        makeTexture();
        Matrix4f m = pose.last().pose();
        // the eye's right and up, in the puffs' own frame
        Matrix3f toLocal = new Matrix3f(m).invert();
        Vector3f right = toLocal.transform(new Vector3f(1, 0, 0)).normalize();
        Vector3f up = toLocal.transform(new Vector3f(0, 1, 0)).normalize();
        // far ones first, so the near ones lie over them
        puffs.sort(Comparator.comparingDouble(c -> m.transformPosition(new Vector3f(c[0], c[1], c[2])).z));
        VertexConsumer vc = buffers.getBuffer(TYPE);
        float[][] corners = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
        for (float[] c : puffs) {
            if (c[5] <= 0.01f) continue;
            float cos = Mth.cos(c[4]) * c[3], sin = Mth.sin(c[4]) * c[3];
            Vector3f a = new Vector3f(right).mul(cos).add(new Vector3f(up).mul(sin));
            Vector3f b = new Vector3f(up).mul(cos).sub(new Vector3f(right).mul(sin));
            for (float[] k : corners) {
                vc.vertex(m, c[0] + a.x * k[0] + b.x * k[1], c[1] + a.y * k[0] + b.y * k[1], c[2] + a.z * k[0] + b.z * k[1])
                        .color(c[6], c[7], c[8], c[5]).uv(k[2], k[3]).uv2(light).endVertex();
            }
        }
    }

    /** Wisps torn off the back of the cloud when flying fast. */
    static void wisps(Player p, float presence) {
        double dx = p.getX() - p.xo, dy = p.getY() - p.yo, dz = p.getZ() - p.zo;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
        var rng = p.getRandom();
        if (speed < 0.25 || rng.nextFloat() > presence * Math.min(1, speed)) return;
        for (int i = 0; i < 2; i++) {
            p.level().addParticle(ParticleTypes.CLOUD,
                    p.getX() - dx + (rng.nextDouble() - 0.5) * 1.4, p.getY() - 0.1 + (rng.nextDouble() - 0.5) * 0.3,
                    p.getZ() - dz + (rng.nextDouble() - 0.5) * 1.4, -dx * 0.05, 0.005, -dz * 0.05);
        }
    }

    /** One soft puff, lumpy round its rim, drawn once into a texture of its own. */
    private static void makeTexture() {
        if (textureMade) return;
        textureMade = true;
        int size = 64;
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                float u = (px + 0.5f) / size * 2 - 1, v = (py + 0.5f) / size * 2 - 1;
                float d = Mth.sqrt(u * u + v * v), ang = (float) Math.atan2(v, u);
                float rim = 0.8f + 0.07f * Mth.sin(3 * ang + 0.5f) + 0.05f * Mth.sin(5 * ang + 2) + 0.03f * Mth.sin(9 * ang + 1);
                float dens = Mth.clamp((rim - d) / (rim * 0.6f), 0, 1);
                dens = dens * dens * (3 - 2 * dens);
                // a little billowing inside, and a little shadow toward the bottom
                float billow = 0.93f + 0.07f * Mth.sin(u * 7 + 1) * Mth.sin(v * 6 + 2);
                float light = Mth.clamp(billow - 0.08f * Math.max(0, v), 0, 1);
                int a = (int) (dens * 255), c = (int) (light * 255);
                img.setPixelRGBA(px, py, (a << 24) | (c << 16) | (c << 8) | c);
            }
        }
        DynamicTexture tex = new DynamicTexture(img);
        Minecraft.getInstance().getTextureManager().register(PUFF, tex);
        tex.setFilter(true, false);
    }
}
