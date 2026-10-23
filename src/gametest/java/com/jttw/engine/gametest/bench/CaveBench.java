package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.cave.LadyEarthFlowEntity;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.Labyrinth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The Bottomless Cave in a real world ({@code -Pbench.mode=cave}): finds the region nearest spawn
 * whose host was found, makes the ground round it, and reads what was built: a plan of each level
 * as the world has it (screenshots/zones/cave_level_N.png: open ways pale, lights yellow, the
 * palace and shrine tinted, rock dark), what fills the shaft, whether she is on her dais; then
 * pictures of the sinkhole, the shaft, the hub, a tunnel, the palace and the shrine.
 */
final class CaveBench {

    private CaveBench() {}

    private static final int HALF = 16384, STEP = 128, LOAD = 6;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks;
    private static int[] centre;
    private static Labyrinth lab;
    private static int surface;

    static List<String> report() {
        return report;
    }

    /** One server tick of the bench. Returns true when done. */
    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ticks++;
        switch (stage) {
            case 0 -> {
                long seed = level.getSeed();
                var source = level.getChunkSource().getGenerator().getBiomeSource();
                var sampler = level.getChunkSource().randomState().sampler();
                double best = Double.MAX_VALUE;
                int regions = 0, found = 0;
                java.util.Set<Long> seen = new java.util.HashSet<>();
                for (int x = -HALF; x < HALF; x += STEP) for (int z = -HALF; z < HALF; z += STEP) {
                    int[] c = Zones.BOTTOMLESS.centre(seed, x, z);
                    if (c == null || !seen.add(((long) c[0] << 32) ^ (c[1] & 0xFFFFFFFFL))) continue;
                    regions++;
                    if (!source.getNoiseBiome(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(8), QuartPos.fromBlock(c[1]), sampler)
                            .is(Zones.BOTTOMLESS_CAVE_BIOME)) continue;
                    found++;
                    double d = Math.hypot(c[0], c[1]);
                    if (d < best) { best = d; centre = c; }
                }
                report.add(String.format("cave regions within %d blocks of spawn: %d laid out, %d of them under mountain hosts; nearest at %s, %.0f blocks off",
                        HALF, regions, found, centre == null ? "-" : centre[0] + " " + centre[1], best));
                if (centre == null) return true;
                lab = Labyrinth.of(seed, centre[0], centre[1]);
                for (int cx = (centre[0] >> 4) - LOAD; cx <= (centre[0] >> 4) + LOAD; cx++)
                    for (int cz = (centre[1] >> 4) - LOAD; cz <= (centre[1] >> 4) + LOAD; cz++) level.setChunkForced(cx, cz, true);
                p.setGameMode(GameType.SPECTATOR);
                p.teleportTo(level, centre[0] + 0.5, 200, centre[1] + 0.5, 0f, 60f);
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                if (ticks < 200) return false;
                surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, centre[0] + 6, centre[1] + 6);
                for (int k = 0; k < Labyrinth.LEVELS; k++) plan(level, k);
                section(level);
                // what the world built against the plan
                byte[] col = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1];
                int agree = 0, all = 0;
                for (int x = centre[0] - 70; x <= centre[0] + 70; x += 3) for (int z = centre[1] - 70; z <= centre[1] + 70; z += 3) {
                    lab.column(x, z, col);
                    for (int k = 0; k < Labyrinth.LEVELS; k++) {
                        int y = Labyrinth.FLOOR[k] + 1;
                        byte c = col[y - Labyrinth.Y_MIN];
                        if (c == Labyrinth.UNTOUCHED) continue;
                        all++;
                        BlockState s = level.getBlockState(new BlockPos(x, y, z));
                        boolean openThere = !s.blocksMotion();
                        if (openThere == (c == Labyrinth.AIR || c == Labyrinth.LANTERN || c == Labyrinth.SOUL_LANTERN || c == Labyrinth.WATER)
                                || c >= Labyrinth.STEP_N && c <= Labyrinth.STEP_W) agree++;
                    }
                }
                report.add(String.format("the labyrinth as built agrees with its plan at %d of %d sampled blocks (%.1f%%)", agree, all, 100.0 * agree / Math.max(1, all)));
                int shaftOpen = 0, shaftSteps = 0;
                for (int y = Labyrinth.Y_MAX + 1; y < surface - 6; y++) {
                    if (level.getBlockState(new BlockPos(centre[0], y, centre[1])).isAir()) shaftOpen++;
                    for (int a = 0; a < 16; a++) {
                        double t = a * Math.PI / 8;
                        BlockState s = level.getBlockState(BlockPos.containing(centre[0] + Math.cos(t) * 3.2, y, centre[1] + Math.sin(t) * 3.2));
                        if (s.is(Blocks.MUD_BRICKS) || s.is(Blocks.MUD_BRICK_SLAB)) shaftSteps++;
                    }
                }
                report.add(String.format("the shaft: surface at y %d over the hub's floor at %d; its middle open for %d of %d blocks; %d stair blocks round it",
                        surface, Labyrinth.FLOOR[0], shaftOpen, surface - 6 - Labyrinth.Y_MAX - 1, shaftSteps));
                var bosses = level.getEntitiesOfClass(LadyEarthFlowEntity.class,
                        new AABB(new BlockPos(centre[0], Labyrinth.FLOOR[2], centre[1])).inflate(20, 12, 20));
                report.add("the palace: " + (bosses.isEmpty() ? "Lady Earth Flow NOT there" : "Lady Earth Flow at " + bosses.get(0).blockPosition()
                        + " (" + bosses.get(0).getDisplayName().getString() + ")"));
                int[] r = lab.refuge();
                report.add("her shrine at " + r[0] + " " + r[1] + " " + r[2] + "; the petal trail to it runs " + lab.trail().size() + " cells");
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                int cx = centre[0], cz = centre[1];
                int f0 = Labyrinth.FLOOR[0], f2 = Labyrinth.FLOOR[2];
                if (ticks == 1) p.teleportTo(level, cx + 0.5, surface + 22, cz + 22.5, 180f, 42f);
                if (ticks == 80) shoot("cave_sinkhole");
                if (ticks == 90) p.teleportTo(level, cx + 0.5, surface + 2, cz + 0.5, 0f, 90f);
                if (ticks == 170) shoot("cave_shaft_down");
                if (ticks == 180) p.teleportTo(level, cx + 3.6, Math.min(surface - 20, 90), cz + 0.5, 90f, 35f);
                if (ticks == 260) shoot("cave_shaft_inside");
                if (ticks == 270) p.teleportTo(level, cx + 7.5, f0 + 1.2, cz + 5.5, 125f, -12f);
                if (ticks == 350) shoot("cave_hub");
                if (ticks == 360) tunnel(p, level);
                if (ticks == 440) shoot("cave_tunnel");
                if (ticks == 445) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
                if (ticks == 520) shoot("cave_tunnel_night_vision");
                if (ticks == 525) p.removeEffect(MobEffects.NIGHT_VISION);
                if (ticks == 530) p.teleportTo(level, cx + 0.5, f2 + 5, cz + 14.5, 180f, 15f);
                if (ticks == 610) shoot("cave_palace");
                if (ticks == 620) {
                    int[] r = lab.refuge();
                    int[] d = Labyrinth.DIRS[lab.shrineGateDir()];
                    p.teleportTo(level, r[0] + 0.5 + d[0] * 4.2, r[1] + 1.6, r[2] + 0.5 + d[1] * 4.2,
                            (float) Math.toDegrees(Math.atan2(d[0], -d[1])), 15f);
                }
                if (ticks == 700) shoot("cave_shrine");
                if (ticks == 710) return true;
            }
            default -> { return true; }
        }
        return false;
    }

    /** Stands in the tunnel next to a lit cell on the top level, looking at it. */
    private static void tunnel(ServerPlayer p, ServerLevel level) {
        Labyrinth.Level l = lab.level(0);
        for (int i = 0; i < Labyrinth.GRID; i++) for (int j = 0; j < Labyrinth.GRID; j++) {
            if (!l.lit(i, j)) continue;
            for (int dir = 0; dir < 4; dir++) {
                if (!l.passage(i, j, dir)) continue;
                int[] a = lab.cellMiddle(i, j), b = lab.cellMiddle(i + Labyrinth.DIRS[dir][0], j + Labyrinth.DIRS[dir][1]);
                int[] d = Labyrinth.DIRS[dir];
                p.teleportTo(level, b[0] + 0.5 + d[0] * 1.5, Labyrinth.FLOOR[0], b[1] + 0.5 + d[1] * 1.5,
                        (float) Math.toDegrees(Math.atan2(d[0], -d[1])), 5f);
                report.add("the tunnel picture: from " + b[0] + " " + b[1] + " toward the lantern at " + a[0] + " " + a[1]);
                return;
            }
        }
    }

    /** Level k's plan as the world has it, a pixel a block, 3 pixels to a block. */
    private static void plan(ServerLevel level, int k) {
        int size = 2 * Labyrinth.HALF + 7, s = 3;
        BufferedImage img = new BufferedImage(size * s, size * s, BufferedImage.TYPE_INT_RGB);
        int open = 0;
        int y = Labyrinth.FLOOR[k] + 1;
        for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) {
            int x = centre[0] - Labyrinth.HALF - 3 + i, z = centre[1] - Labyrinth.HALF - 3 + j;
            BlockState here = level.getBlockState(new BlockPos(x, y, z)), top = level.getBlockState(new BlockPos(x, Labyrinth.FLOOR[k] + 3, z));
            int rgb;
            if (top.is(Blocks.LANTERN) || top.is(Blocks.SOUL_LANTERN) || here.getLightEmission() > 0) rgb = 0xF0C040;
            else if (!here.blocksMotion()) {
                open++;
                var room = lab.roomAt(k, x, z);
                rgb = room == null ? 0xD8D0C0 : room.kind() == Labyrinth.PALACE ? 0xE8A0B8 : room.kind() == Labyrinth.SHRINE ? 0xA0C0F0
                        : room.kind() == Labyrinth.HUB ? 0xB0E0B0 : 0xC8B898;
                if (here.is(Blocks.WATER)) rgb = 0x4060D0;
            } else if (here.getBlock().getDescriptionId().contains("stairs")) rgb = 0x8060A0;
            else rgb = 0x302820;
            for (int a = 0; a < s; a++) for (int b = 0; b < s; b++) img.setRGB(i * s + a, j * s + b, rgb);
        }
        write(img, "cave_level_" + k);
        report.add(String.format("level %d (walked at y %d): %d open blocks", k, Labyrinth.FLOOR[k], open));
    }

    /** A cut north to south through the centre, from under the palace to over the surface. */
    private static void section(ServerLevel level) {
        int w = 2 * Labyrinth.HALF + 7, top = Math.min(surface + 10, level.getMaxBuildHeight() - 1), bottom = -6, s = 3;
        BufferedImage img = new BufferedImage(w * s, (top - bottom + 1) * s, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < w; i++) for (int y = bottom; y <= top; y++) {
            BlockState b = level.getBlockState(new BlockPos(centre[0], y, centre[1] - Labyrinth.HALF - 3 + i));
            int rgb = b.isAir() ? (y > surface ? 0x90B8E8 : 0xD8D0C0) : b.getLightEmission() > 0 ? 0xF0C040
                    : b.is(Blocks.WATER) ? 0x4060D0 : b.is(Blocks.MUD_BRICKS) || b.is(Blocks.MUD_BRICK_SLAB) ? 0x8060A0 : 0x302820;
            for (int a = 0; a < s; a++) for (int c = 0; c < s; c++) img.setRGB(i * s + a, (top - y) * s + c, rgb);
        }
        write(img, "cave_section");
    }

    private static void write(BufferedImage img, String name) {
        File dir = new File(Minecraft.getInstance().gameDirectory, "screenshots/zones");
        dir.mkdirs();
        try {
            javax.imageio.ImageIO.write(img, "png", new File(dir, name + ".png"));
            report.add("  plan: screenshots/zones/" + name + ".png");
        } catch (java.io.IOException e) {
            report.add("  could not write " + name + ": " + e);
        }
    }

    private static void shoot(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.options.hideGui = true;
            new File(mc.gameDirectory, "screenshots/zones").mkdirs();
            Screenshot.grab(mc.gameDirectory, "zones/" + name + ".png", mc.getMainRenderTarget(), msg -> {});
        });
        report.add("  picture: screenshots/zones/" + name + ".png");
    }
}
