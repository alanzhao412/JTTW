package com.jttw.engine.client.flight;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Erlang Shen does not ride a cloud. He leaps through wind and fog (Kuangfeng Shenwu): he flies
 * wrapped in a dark squall, a dense fog churning round him and streaming out behind, the faster
 * he goes the longer, with smoke torn off its tail.
 */
final class SquallFog {

    private SquallFog() {}

    private static final int PUFFS = 40;

    /** Draws the squall about a flier, {@code pose} at their feet and square to the world. */
    static void draw(PoseStack pose, MultiBufferSource buffers, Player p, float amount, float move, int light) {
        if (amount <= 0) return;
        long seed = p.getUUID().getMostSignificantBits() * 31 + p.getUUID().getLeastSignificantBits();
        float t = (p.tickCount + Minecraft.getInstance().getFrameTime()) / 20f;
        // the way he is going, or else the way he looks
        Vec3 way = new Vec3(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo);
        way = way.lengthSqr() > 1.0e-4 ? way.normalize() : p.getLookAngle();
        float trail = 0.6f + 4.5f * move;
        List<float[]> puffs = new ArrayList<>(PUFFS);
        for (int i = 0; i < PUFFS; i++) {
            // each puff is born about him and swept back along the trail, over and over, never in step
            float rate = (0.5f + 0.7f * NimbusCloud.h(seed, i, 0, 1)) * (0.6f + 1.4f * move);
            float u = (t * rate + NimbusCloud.h(seed, i, 0, 2)) % 1f;
            int life = (int) (t * rate + NimbusCloud.h(seed, i, 0, 2));
            // swirling round him, a squall's turning
            float angle = 6.283f * NimbusCloud.h(seed, i, life, 3) + t * (1.2f + NimbusCloud.h(seed, i, life, 4));
            float reach = 0.45f + 0.5f * NimbusCloud.h(seed, i, life, 5);
            float x = Mth.cos(angle) * reach, z = Mth.sin(angle) * reach;
            float y = 0.1f + 1.7f * NimbusCloud.h(seed, i, life, 6);
            x -= (float) way.x * u * trail;
            y -= (float) way.y * u * trail;
            z -= (float) way.z * u * trail;
            float r = (0.55f + 0.45f * NimbusCloud.h(seed, i, life, 7)) * (0.7f + 0.8f * u);
            float fade = Mth.sin((float) Math.PI * Math.min(1, u * 1.3f + 0.15f));
            float dark = 0.2f + 0.16f * NimbusCloud.h(seed, i, life, 8);
            float spin = 6.283f * NimbusCloud.h(seed, i, life, 9) + t * 0.8f;
            puffs.add(new float[]{x, y, z, r, spin, 0.72f * amount * fade, dark, dark + 0.02f, dark + 0.05f});
        }
        NimbusCloud.drawPuffs(pose, buffers, puffs, light);
    }

    /** Dark smoke torn off the squall's tail at speed. */
    static void smoke(Player p, float amount) {
        double dx = p.getX() - p.xo, dy = p.getY() - p.yo, dz = p.getZ() - p.zo;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
        var rng = p.getRandom();
        // only at speed, where it is left behind as a trail: hovering, it would only rise off him
        if (speed < 0.25 || rng.nextFloat() > amount) return;
        for (int i = 0; i < 3; i++) {
            p.level().addParticle(rng.nextBoolean() ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                    p.getX() - dx * 2 + (rng.nextDouble() - 0.5) * 1.2, p.getY() + rng.nextDouble() * 1.8 - dy * 2,
                    p.getZ() - dz * 2 + (rng.nextDouble() - 0.5) * 1.2, -dx * 0.1, 0.01, -dz * 0.1);
        }
    }
}
