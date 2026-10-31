package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.s2c.EyeLaserBeamPacket;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Fiery Golden Eyes' laser, drawn ({@link com.jttw.engine.character.wukong.EyeLaser}): at
 * once, two fine beams of gold from his two eyes (they follow his head, and in his own first-person
 * view leave from just below and either side of the eye), meeting a little before him in one
 * shaft of fire that flares out to its full width and runs to where the beam stopped. The shaft is
 * nested tubes, drawn additively: a white-hot core, gold, then orange and a red haze, rippling as
 * pulses run down it; it flashes as it fires and fades and narrows over half a second.
 *
 * <p>Along it, sparks and flames; the blocks it burnt away fly off as glowing chunks (their own
 * colour, red-hot) and crumbs; where it ends, a flare with its rays, a burst of light, lava and
 * smoke, and the faces left there scorched: glowing first, then charred, cooling over seconds.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class EyeLaserRenderer {

    private EyeLaserRenderer() {}

    /** How long a beam shows, in ticks: a flash at once, then fading. */
    public static final int DURATION = 11;
    /** How long a scorch lasts, in ticks: glowing for the first part, charred after. */
    private static final int SCORCH_TICKS = 140, SCORCH_GLOW = 30;

    /** Additive light: no texture, no depth written, seen from both sides. */
    private static final RenderType GLOW = RenderType.create("jttw_eye_laser_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1 << 18, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    /** Soot laid on block faces: blended, pulled toward the camera so it sits on them. */
    private static final RenderType SOOT = RenderType.create("jttw_eye_laser_soot", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1 << 16, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private record Beam(EyeLaserBeamPacket p, long start) {}

    /** A glowing chunk of what the beam burnt away, flying off and cooling. */
    private static final class Chunk {
        Vec3 pos, prev, vel;
        final float size, r, g, b;
        final int life;
        int age;
        final float spin;

        Chunk(Vec3 pos, Vec3 vel, float size, float r, float g, float b, int life, float spin) {
            this.pos = this.prev = pos;
            this.vel = vel;
            this.size = size;
            this.r = r; this.g = g; this.b = b;
            this.life = life;
            this.spin = spin;
        }
    }

    /** Where a beam ended against something: the faces there, once the blocks it took are gone. */
    private static final class Scorch {
        final Vec3 at, dir;
        final float radius;
        final long start;
        List<double[]> faces; // x0,y0,z0, x1,y1,z1, x2,y2,z2, x3,y3,z3, closeness

        Scorch(Vec3 at, Vec3 dir, float radius, long start) {
            this.at = at;
            this.dir = dir;
            this.radius = radius;
            this.start = start;
        }
    }

    private static final List<Beam> BEAMS = new ArrayList<>();
    private static final List<Chunk> CHUNKS = new ArrayList<>();
    private static final List<Scorch> SCORCHES = new ArrayList<>();

    /** A beam the server says fired: shown at once, its sparks, debris and burst thrown out now. */
    public static void add(EyeLaserBeamPacket p) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        long now = level.getGameTime();
        BEAMS.add(new Beam(p, now));
        RandomSource rnd = level.getRandom();
        Vec3 d = p.to.subtract(p.from);
        double len = d.length();
        if (len < 1e-3) return;
        Vec3 dir = d.scale(1 / len);
        Vec3[] basis = basis(dir);
        float r = p.radius;
        int lv = p.output;

        // sparks and flames along it, thrown out from its axis
        for (double t = 1.0; t < len; t += 0.55 - 0.05 * lv) {
            Vec3 at = p.from.add(dir.scale(t));
            double a = rnd.nextDouble() * Mth.TWO_PI;
            Vec3 out = basis[0].scale(Math.cos(a)).add(basis[1].scale(Math.sin(a)));
            Vec3 spot = at.add(out.scale(r * rnd.nextDouble()));
            level.addParticle(ParticleTypes.FLAME, true, spot.x, spot.y, spot.z,
                    out.x * 0.06 + dir.x * 0.05, out.y * 0.06 + 0.02, out.z * 0.06 + dir.z * 0.05);
            if (rnd.nextFloat() < 0.45f) {
                Vec3 s = at.add(out.scale(r * 0.8));
                double v = 0.12 + 0.05 * lv;
                level.addParticle(ParticleTypes.END_ROD, true, s.x, s.y, s.z, out.x * v, out.y * v + 0.03, out.z * v);
            }
            if (rnd.nextFloat() < 0.12f + 0.04f * lv) {
                level.addParticle(ParticleTypes.LAVA, true, spot.x, spot.y, spot.z, 0, 0, 0);
            }
        }

        // what it burnt away: crumbs, and chunks of it flying off red-hot
        for (EyeLaserBeamPacket.Debris db : p.debris) {
            BlockState state = Block.stateById(db.state());
            if (state.isAir()) continue;
            Vec3 c = Vec3.atCenterOf(db.pos());
            Vec3 axis = p.from.add(dir.scale(Mth.clamp(c.subtract(p.from).dot(dir), 0, len)));
            Vec3 away = c.subtract(axis);
            away = away.lengthSqr() < 1e-4 ? basis[0] : away.normalize();
            for (int i = 0; i < 3; i++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), true,
                        c.x + rnd.nextGaussian() * 0.3, c.y + rnd.nextGaussian() * 0.3, c.z + rnd.nextGaussian() * 0.3,
                        away.x * 0.2, away.y * 0.2 + 0.1, away.z * 0.2);
            }
            int colour = state.getMapColor(level, db.pos()).col;
            float br = ((colour >> 16) & 255) / 255f, bg = ((colour >> 8) & 255) / 255f, bb = (colour & 255) / 255f;
            int n = 2 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) {
                double speed = 0.18 + 0.08 * lv * rnd.nextDouble();
                Vec3 v = away.scale(speed).add(dir.scale(0.1 * rnd.nextGaussian()))
                        .add(rnd.nextGaussian() * 0.06, 0.12 + rnd.nextDouble() * 0.18, rnd.nextGaussian() * 0.06);
                Vec3 at = c.add(rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25);
                // its own colour, heated: drawn toward a red-hot orange
                CHUNKS.add(new Chunk(at, v, 0.06f + rnd.nextFloat() * 0.09f,
                        Mth.lerp(0.65f, br, 1f), Mth.lerp(0.65f, bg, 0.5f), Mth.lerp(0.65f, bb, 0.12f),
                        24 + rnd.nextInt(26), (rnd.nextFloat() - 0.5f) * 0.8f));
            }
        }

        // where it ends: a flash, flames and lava, smoke, and the faces there scorched
        Vec3 e = p.to;
        level.addParticle(ParticleTypes.FLASH, true, e.x, e.y, e.z, 0, 0, 0);
        if (lv >= 3) level.addParticle(lv >= 4 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, true, e.x, e.y, e.z, 0, 0, 0);
        int burst = 10 + 6 * lv;
        for (int i = 0; i < burst; i++) {
            Vec3 v = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize().scale(0.1 + 0.05 * lv)
                    .subtract(dir.scale(0.1));
            level.addParticle(ParticleTypes.FLAME, true, e.x, e.y, e.z, v.x, v.y, v.z);
            if (i % 2 == 0) level.addParticle(ParticleTypes.LAVA, true, e.x, e.y, e.z, 0, 0, 0);
            if (i % 3 == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, true, e.x, e.y, e.z, v.x * 0.4, 0.04, v.z * 0.4);
        }
        SCORCHES.add(new Scorch(e, dir, r * 1.6f + 1.0f, now));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            BEAMS.clear();
            CHUNKS.clear();
            SCORCHES.clear();
            return;
        }
        if (Minecraft.getInstance().isPaused()) return;
        long now = level.getGameTime();
        BEAMS.removeIf(b -> now - b.start() > DURATION);
        SCORCHES.removeIf(s -> now - s.start > SCORCH_TICKS);
        RandomSource rnd = level.getRandom();
        // a beam still showing keeps throwing a few sparks
        for (Beam b : BEAMS) {
            Vec3 d = b.p().to.subtract(b.p().from);
            for (int i = 0; i < 2 + b.p().output; i++) {
                Vec3 at = b.p().from.add(d.scale(0.1 + 0.9 * rnd.nextDouble()));
                level.addParticle(ParticleTypes.SMALL_FLAME, true, at.x, at.y, at.z,
                        rnd.nextGaussian() * 0.03, 0.03, rnd.nextGaussian() * 0.03);
            }
        }
        for (Iterator<Chunk> it = CHUNKS.iterator(); it.hasNext(); ) {
            Chunk c = it.next();
            if (++c.age > c.life) { it.remove(); continue; }
            c.prev = c.pos;
            Vec3 next = c.pos.add(c.vel);
            BlockPos below = BlockPos.containing(next);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                // it lands: skids, and bounces a little
                c.vel = new Vec3(c.vel.x * 0.5, -c.vel.y * 0.25, c.vel.z * 0.5);
                next = c.pos;
            }
            c.pos = next;
            c.vel = c.vel.scale(0.94).add(0, -0.035, 0);
            if (c.age % 6 == 0 && rnd.nextFloat() < 0.5f) {
                level.addParticle(ParticleTypes.SMOKE, c.pos.x, c.pos.y, c.pos.z, 0, 0.02, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || (BEAMS.isEmpty() && CHUNKS.isEmpty() && SCORCHES.isEmpty())) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        Camera cam = event.getCamera();
        Vec3 c = cam.getPosition();
        // positions are taken relative to the camera in double precision (far from the origin too)
        camX = c.x; camY = c.y; camZ = c.z;
        Matrix4f m = new PoseStack().last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        if (!SCORCHES.isEmpty()) {
            VertexConsumer soot = buffers.getBuffer(SOOT);
            for (Scorch s : SCORCHES) drawSoot(m, soot, s, now - s.start + partial);
            buffers.endBatch(SOOT);
        }
        VertexConsumer vc = buffers.getBuffer(GLOW);
        for (Scorch s : SCORCHES) drawEmbers(m, vc, s, now - s.start + partial);
        for (Beam b : BEAMS) drawBeam(m, vc, b, now - b.start() + partial, partial, cam);
        for (Chunk ch : CHUNKS) drawChunk(m, vc, ch, partial);
        buffers.endBatch(GLOW);
    }

    // ── the beam ───────────────────────────────────────────────────

    /** How bright the beam is, by its age: a flash, then fading out. */
    private static float fade(float age) {
        if (age < 2) return 1f;
        float f = 1 - (age - 2) / (DURATION - 2);
        return Mth.clamp(f * f * (3 - 2 * f), 0, 1);
    }

    private static void drawBeam(Matrix4f m, VertexConsumer vc, Beam b, float age, float partial, Camera cam) {
        EyeLaserBeamPacket p = b.p();
        float fade = fade(age);
        if (fade <= 0) return;
        float flash = Mth.clamp(1 - age / 2.5f, 0, 1); // the moment it fires
        float width = (0.55f + 0.45f * fade) * (1 + 0.35f * flash);
        float r = p.radius * width;
        int lv = p.output;
        Minecraft mc = Minecraft.getInstance();
        Entity owner = mc.level.getEntity(p.owner);

        // his eyes, and the point ahead where their beams meet
        Vec3[] eyes;
        Vec3 look;
        float eyeR;
        boolean firstPerson = owner != null && owner == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        if (firstPerson) {
            Vector3f lk = cam.getLookVector(), up = cam.getUpVector(), left = cam.getLeftVector();
            look = new Vec3(lk.x(), lk.y(), lk.z());
            Vec3 u = new Vec3(up.x(), up.y(), up.z()), l = new Vec3(left.x(), left.y(), left.z());
            Vec3 base = cam.getPosition().add(look.scale(0.3)).subtract(u.scale(0.1));
            eyes = new Vec3[]{base.add(l.scale(0.11)), base.subtract(l.scale(0.11))};
            eyeR = 0.012f + 0.003f * lv;
        } else if (owner != null) {
            float s = Mth.clamp(owner.getBbHeight() / 1.8f, 0.3f, 6f);
            look = owner.getViewVector(partial);
            double yaw = Math.toRadians(owner.getViewYRot(partial));
            Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
            Vec3 up = right.cross(look);
            Vec3 neck = owner.getPosition(partial).add(0, owner.getEyeHeight() - 0.21 * s, 0);
            Vec3 mid = neck.add(up.scale(0.205 * s)).add(look.scale(0.26 * s));
            eyes = new Vec3[]{mid.add(right.scale(0.117 * s)), mid.subtract(right.scale(0.117 * s))};
            eyeR = (0.03f + 0.008f * lv) * s;
        } else {
            look = p.to.subtract(p.from).normalize();
            Vec3 right = basis(look)[0];
            eyes = new Vec3[]{p.from.add(right.scale(0.117)), p.from.subtract(right.scale(0.117))};
            eyeR = 0.03f + 0.008f * lv;
        }
        Vec3 mid = eyes[0].add(eyes[1]).scale(0.5);
        double reach = p.to.distanceTo(mid);
        double conv = Math.min(reach, Mth.clamp(reach * 0.2, 0.9, 2.4));
        Vec3 meet = mid.add(look.scale(conv));

        // the two beams from his eyes, each a fine core in a little glow
        for (Vec3 eye : eyes) {
            layers(m, vc, eye, meet, eyeR * width, eyeR * width * 1.6f, fade, age, 0, false);
            flare(m, vc, eye, cam, eyeR * (firstPerson ? 2.2f : 4.5f) * (1 + flash), 1f, 0.85f, 0.4f, 0.9f * fade);
        }
        if (reach <= conv + 0.05) return;

        // the shaft: flaring out from where they meet to its full width over two blocks, then on to its end
        Vec3 dir = p.to.subtract(meet).normalize();
        double len = p.to.distanceTo(meet);
        double flareLen = Math.min(len, 2.0);
        Vec3 full = meet.add(dir.scale(flareLen));
        layers(m, vc, meet, full, eyeR * width * 1.8f, r, fade, age, 0, true);
        if (len > flareLen) layers(m, vc, full, p.to, r, r, fade, age, (float) flareLen, true);

        // where it ends: a flare with rays, brightest as it fires
        float tipR = (r * 2.4f + 0.6f) * (1 + 0.8f * flash);
        flare(m, vc, p.to, cam, tipR, 1f, 0.7f, 0.25f, 0.85f * fade);
        flare(m, vc, p.to, cam, tipR * 0.45f, 1f, 0.97f, 0.85f, fade);
        rays(m, vc, p.to, cam, tipR * (1.6f + 0.15f * lv), age, 0.55f * fade);
        if (flash > 0) flare(m, vc, p.to, cam, tipR * 2.2f, 1f, 0.8f, 0.4f, 0.5f * flash);
    }

    /**
     * The beam's layers between two points, nested tubes from a white-hot core to a red haze,
     * radius {@code r0} at {@code a} to {@code r1} at {@code b}; pulses run along it from his eyes.
     */
    private static void layers(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, float r0, float r1,
                               float fade, float age, float offset, boolean haze) {
        //                       radius, r, g, b, alpha
        float[][] shells = haze ? new float[][]{
                {0.22f, 1f, 1f, 0.93f, 0.95f},
                {0.45f, 1f, 0.93f, 0.6f, 0.55f},
                {0.75f, 1f, 0.74f, 0.22f, 0.36f},
                {1.15f, 1f, 0.5f, 0.08f, 0.2f},
                {1.8f, 0.9f, 0.26f, 0.03f, 0.09f}}
                : new float[][]{
                {0.35f, 1f, 1f, 0.9f, 0.95f},
                {1.0f, 1f, 0.8f, 0.3f, 0.45f}};
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1e-3) return;
        int segs = Math.max(1, (int) Math.ceil(len / 0.75));
        for (float[] sh : shells) {
            for (int i = 0; i < segs; i++) {
                float u0 = i / (float) segs, u1 = (i + 1) / (float) segs;
                float p0 = pulse(offset + u0 * (float) len, age), p1 = pulse(offset + u1 * (float) len, age);
                float rr0 = Mth.lerp(u0, r0, r1) * sh[0], rr1 = Mth.lerp(u1, r0, r1) * sh[0];
                tube(m, vc, a.add(d.scale(u0)), a.add(d.scale(u1)), Math.max(rr0, 0.004f), Math.max(rr1, 0.004f),
                        sh[1], sh[2], sh[3], sh[4] * fade * p0, sh[4] * fade * p1);
            }
        }
    }

    /** Brightness running along the beam: pulses racing out from his eyes, and a flicker. */
    private static float pulse(float along, float age) {
        return 0.82f + 0.18f * Mth.sin(along * 1.1f - age * 3.2f) + 0.06f * Mth.sin(along * 3.7f + age * 7f);
    }

    private static final int SIDES = 12;

    /** A tube from {@code a} (radius r0) to {@code b} (r1), open at its ends. */
    private static void tube(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, float r0, float r1,
                             float cr, float cg, float cb, float alpha0, float alpha1) {
        Vec3 dir = b.subtract(a).normalize();
        Vec3[] bs = basis(dir);
        for (int i = 0; i < SIDES; i++) {
            double t0 = i * Mth.TWO_PI / SIDES, t1 = (i + 1) * Mth.TWO_PI / SIDES;
            Vec3 o0 = bs[0].scale(Math.cos(t0)).add(bs[1].scale(Math.sin(t0)));
            Vec3 o1 = bs[0].scale(Math.cos(t1)).add(bs[1].scale(Math.sin(t1)));
            vertex(m, vc, a.add(o0.scale(r0)), cr, cg, cb, alpha0);
            vertex(m, vc, a.add(o1.scale(r0)), cr, cg, cb, alpha0);
            vertex(m, vc, b.add(o1.scale(r1)), cr, cg, cb, alpha1);
            vertex(m, vc, b.add(o0.scale(r1)), cr, cg, cb, alpha1);
        }
    }

    /** A soft round glow facing the camera: bright at its heart, nothing at its rim. */
    private static void flare(Matrix4f m, VertexConsumer vc, Vec3 at, Camera cam, float radius,
                              float cr, float cg, float cb, float alpha) {
        if (alpha <= 0.002f || radius <= 0) return;
        Vector3f up = cam.getUpVector(), left = cam.getLeftVector();
        Vec3 u = new Vec3(up.x(), up.y(), up.z()), l = new Vec3(left.x(), left.y(), left.z());
        int n = 16;
        for (int i = 0; i < n; i++) {
            double t0 = i * Mth.TWO_PI / n, t1 = (i + 1) * Mth.TWO_PI / n;
            // two rings: a bright middle falling off, then a faint rim
            Vec3 a0 = at.add(l.scale(Math.cos(t0) * radius * 0.4)).add(u.scale(Math.sin(t0) * radius * 0.4));
            Vec3 a1 = at.add(l.scale(Math.cos(t1) * radius * 0.4)).add(u.scale(Math.sin(t1) * radius * 0.4));
            Vec3 b0 = at.add(l.scale(Math.cos(t0) * radius)).add(u.scale(Math.sin(t0) * radius));
            Vec3 b1 = at.add(l.scale(Math.cos(t1) * radius)).add(u.scale(Math.sin(t1) * radius));
            vertex(m, vc, at, cr, cg, cb, alpha);
            vertex(m, vc, a0, cr, cg, cb, alpha * 0.45f);
            vertex(m, vc, a1, cr, cg, cb, alpha * 0.45f);
            vertex(m, vc, at, cr, cg, cb, alpha);
            vertex(m, vc, a0, cr, cg, cb, alpha * 0.45f);
            vertex(m, vc, b0, cr, cg, cb, 0);
            vertex(m, vc, b1, cr, cg, cb, 0);
            vertex(m, vc, a1, cr, cg, cb, alpha * 0.45f);
        }
    }

    /** Thin rays of light from a point, turning slowly, facing the camera. */
    private static void rays(Matrix4f m, VertexConsumer vc, Vec3 at, Camera cam, float length, float age, float alpha) {
        if (alpha <= 0.002f) return;
        Vector3f up = cam.getUpVector(), left = cam.getLeftVector();
        Vec3 u = new Vec3(up.x(), up.y(), up.z()), l = new Vec3(left.x(), left.y(), left.z());
        int n = 8;
        for (int i = 0; i < n; i++) {
            double t = i * Mth.TWO_PI / n + age * 0.05 + (i % 2) * 0.2;
            float len = length * (i % 2 == 0 ? 1f : 0.55f);
            Vec3 out = l.scale(Math.cos(t)).add(u.scale(Math.sin(t)));
            Vec3 side = l.scale(-Math.sin(t)).add(u.scale(Math.cos(t))).scale(len * 0.035);
            Vec3 tip = at.add(out.scale(len));
            vertex(m, vc, at.add(side), 1f, 0.9f, 0.55f, alpha);
            vertex(m, vc, at.subtract(side), 1f, 0.9f, 0.55f, alpha);
            vertex(m, vc, tip, 1f, 0.6f, 0.2f, 0);
            vertex(m, vc, tip, 1f, 0.6f, 0.2f, 0);
        }
    }

    // ── debris ─────────────────────────────────────────────────────

    private static void drawChunk(Matrix4f m, VertexConsumer vc, Chunk c, float partial) {
        float k = (c.age + partial) / c.life;
        float heat = Mth.clamp(1 - k, 0, 1);
        Vec3 at = c.prev.lerp(c.pos, partial);
        float s = c.size * (0.6f + 0.4f * heat);
        float alpha = 0.95f * heat;
        // cooling: from its own colour red-hot toward a dull red
        float r = c.r, g = Mth.lerp(1 - heat, c.g, c.g * 0.35f), b = Mth.lerp(1 - heat, c.b, 0.02f);
        float spin = c.spin * (c.age + partial);
        Vec3 x = new Vec3(Math.cos(spin), 0, Math.sin(spin)).scale(s), y = new Vec3(0, s, 0),
                z = new Vec3(-Math.sin(spin), 0, Math.cos(spin)).scale(s);
        box(m, vc, at, x, y, z, r, g, b, alpha);
        box(m, vc, at, x.scale(1.9), y.scale(1.9), z.scale(1.9), r, g * 0.8f, b * 0.5f, alpha * 0.18f); // its glow
    }

    /** A box about {@code c} with these half-axes. */
    private static void box(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 x, Vec3 y, Vec3 z,
                            float r, float g, float b, float a) {
        Vec3[] ax = {x, y, z};
        for (int i = 0; i < 3; i++) {
            Vec3 n = ax[i], u = ax[(i + 1) % 3], v = ax[(i + 2) % 3];
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                Vec3 f = c.add(n.scale(sgn));
                vertex(m, vc, f.subtract(u).subtract(v), r, g, b, a);
                vertex(m, vc, f.add(u).subtract(v), r, g, b, a);
                vertex(m, vc, f.add(u).add(v), r, g, b, a);
                vertex(m, vc, f.subtract(u).add(v), r, g, b, a);
            }
        }
    }

    // ── the scorch ─────────────────────────────────────────────────

    /** The faces near where it ended that face open air: found once the burnt blocks are gone. */
    private static List<double[]> faces(Scorch s) {
        ClientLevel level = Minecraft.getInstance().level;
        List<double[]> out = new ArrayList<>();
        int reach = (int) Math.ceil(s.radius);
        BlockPos c = BlockPos.containing(s.at);
        for (int dx = -reach; dx <= reach; dx++) for (int dy = -reach; dy <= reach; dy++) for (int dz = -reach; dz <= reach; dz++) {
            BlockPos pos = c.offset(dx, dy, dz);
            BlockState state = level.getBlockState(pos);
            if (!state.isSolidRender(level, pos)) continue;
            for (Direction face : Direction.values()) {
                BlockPos open = pos.relative(face);
                if (level.getBlockState(open).isSolidRender(level, open)) continue;
                Vec3 centre = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
                double d = centre.distanceTo(s.at);
                if (d > s.radius) continue;
                double close = 1 - d / s.radius;
                double[] q = new double[13];
                Vec3 n = Vec3.atLowerCornerOf(face.getNormal());
                Vec3 u = Math.abs(n.y) > 0.5 ? new Vec3(0.5, 0, 0) : new Vec3(0, 0.5, 0);
                Vec3 v = n.cross(u);
                Vec3[] corners = {centre.subtract(u).subtract(v), centre.add(u).subtract(v), centre.add(u).add(v), centre.subtract(u).add(v)};
                for (int k = 0; k < 4; k++) {
                    Vec3 lift = corners[k].add(n.scale(0.004));
                    q[k * 3] = lift.x; q[k * 3 + 1] = lift.y; q[k * 3 + 2] = lift.z;
                }
                q[12] = close;
                out.add(q);
            }
        }
        return out;
    }

    private static void drawSoot(Matrix4f m, VertexConsumer vc, Scorch s, float age) {
        if (age < 2) return; // the blocks it burnt away are not gone yet here
        if (s.faces == null) s.faces = faces(s);
        float fade = Mth.clamp((SCORCH_TICKS - age) / 40f, 0, 1) * Mth.clamp((age - 2) / 4f, 0, 1);
        for (double[] q : s.faces) {
            float a = 0.8f * fade * Mth.clamp((float) q[12] * 1.6f, 0, 1);
            quad(m, vc, q, 0.07f, 0.045f, 0.03f, a);
        }
    }

    private static void drawEmbers(Matrix4f m, VertexConsumer vc, Scorch s, float age) {
        if (s.faces == null || age > SCORCH_GLOW) return;
        float heat = 1 - age / SCORCH_GLOW;
        for (double[] q : s.faces) {
            float a = heat * heat * Mth.clamp((float) q[12] * 1.4f, 0, 1) * 0.85f;
            quad(m, vc, q, 1f, 0.35f + 0.3f * heat, 0.06f, a);
        }
    }

    private static void quad(Matrix4f m, VertexConsumer vc, double[] q, float r, float g, float b, float a) {
        if (a <= 0.002f) return;
        for (int k = 0; k < 4; k++) vc.addVertex(m, (float) (q[k * 3] - camX), (float) (q[k * 3 + 1] - camY), (float) (q[k * 3 + 2] - camZ)).setColor(r, g, b, a);
    }

    // ── helpers ────────────────────────────────────────────────────

    /** Two unit vectors at right angles to {@code dir} and each other. */
    private static Vec3[] basis(Vec3 dir) {
        Vec3 ref = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 a = dir.cross(ref).normalize();
        Vec3 b = dir.cross(a).normalize();
        return new Vec3[]{a, b};
    }

    private static double camX, camY, camZ;

    private static void vertex(Matrix4f m, VertexConsumer vc, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) (p.x - camX), (float) (p.y - camY), (float) (p.z - camZ)).setColor(r, g, b, Mth.clamp(a, 0, 1));
    }
}
