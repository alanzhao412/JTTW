package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.world.Zones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Where the zones were placed in a real world, and whether each stands whole
 * ({@code -Pbench.mode=zones}, {@code -Pbench.seed=N} for another world): each zone's region (the
 * four seas' too), its distance from spawn, the vanilla biome it was put on, and how long finding
 * them took; a map of the 20x20 km round spawn, the zones over vanilla's biomes, with their rings
 * ({@code screenshots/zones/unique_zones_map_<seed>.png}); then a visit to each region, once its
 * ground is made: its biome there, the bosses in it, and a picture (for Fengdu, three more: up its
 * road, before its Gate of Ghosts, and over the city).
 */
final class ZoneBench {

    private ZoneBench() {}

    private static final int HALF = 10240, STEP = 40, LOAD = 5;
    private static final List<String> report = new ArrayList<>();
    private static final List<Zones.Zone> visitZones = new ArrayList<>();
    private static final List<int[]> visits = new ArrayList<>();
    private static int stage, ticks, visit;

    static List<String> report() {
        return report;
    }

    /** Vanilla's biome at (x, y, z), the zones left out (the source asked by climate alone). */
    private static Holder<Biome> vanilla(MultiNoiseBiomeSource source, Climate.Sampler sampler, int x, int y, int z) {
        return source.getNoiseBiome(sampler.sample(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)));
    }

    private static String name(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.location().getPath()).orElse("?");
    }

    /** A height the zone takes, to read its biome at. */
    private static int heightOf(Zones.Zone zone) {
        if (zone.vertical().contains(64)) return 64;
        return zone.vertical().maxY() < 64 ? 0 : Math.max(zone.vertical().minY() + 16, 100);
    }

    /** One server tick of the bench. Returns true when done. */
    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long seed = Zones.seed();
        ticks++;
        if (stage == 0) {
            if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource source)) {
                report.add("the overworld's biome source is not multi-noise: no zones");
                return true;
            }
            Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
            long start = System.nanoTime();
            Zones.settle(seed, source, sampler, true); // found again, to time it (the same places: it is deterministic)
            double ms = (System.nanoTime() - start) / 1e6;
            report.add(String.format("seed %d: the zones placed in %.0f ms; spawn at %s", seed, ms, level.getSharedSpawnPos()));
            for (Zones.Zone zone : Zones.MAP.zones()) {
                for (int[] c : zone.centres(seed)) {
                    Holder<Biome> under = vanilla(source, sampler, c[0], zone.hostY(), c[1]);
                    Holder<Biome> placed = source.getNoiseBiome(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(heightOf(zone)), QuartPos.fromBlock(c[1]), sampler);
                    report.add(String.format("  %-18s at %6d %6d, %5.0f blocks from spawn (its ring %d-%d), %s; on %s (host: %s); the source places %s there",
                            zone.biome().location().getPath(), c[0], c[1], Math.hypot(c[0], c[1]), zone.layout().near(), zone.layout().far(),
                            com.jttw.engine.world.EarthGod.wind(0, 0, c[0], c[1]), name(under), under.is(zone.hosts()) ? "yes" : "NO", name(placed)));
                    visitZones.add(zone);
                    visits.add(c);
                }
            }
            String notes = Zones.MAP.placement(seed).notes();
            report.add("  widenings: " + (notes.isEmpty() ? "none" : notes));
            map(seed, source, sampler);
            p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            stage = 1;
            ticks = 0;
            return false;
        }
        if (visit >= visits.size()) return true;
        Zones.Zone zone = visitZones.get(visit);
        int[] c = visits.get(visit);
        String id = zone.biome().location().getPath()
                + (zone.layout().perSea() ? "_" + com.jttw.engine.world.waters.Sea.of(c[0], c[1]).name().toLowerCase() : "");
        switch (stage) {
            case 1 -> {
                if (ticks == 1) p.teleportTo(level, c[0] + 0.5, 250, c[1] + 0.5, 0f, 60f);
                int cx = c[0] >> 4, cz = c[1] >> 4;
                boolean loaded = true;
                for (int x = cx - LOAD; x <= cx + LOAD && loaded; x++) for (int z = cz - LOAD; z <= cz + LOAD; z++) {
                    if (level.getChunkSource().getChunkNow(x, z) == null) { loaded = false; break; }
                }
                if (!loaded && ticks < 20 * 180) return false;
                if (ticks < 60) return false; // its entities joining
                List<ZoneBoss> bosses = level.getEntitiesOfClass(ZoneBoss.class,
                        new AABB(c[0] - 128, level.getMinBuildHeight(), c[1] - 128, c[0] + 128, level.getMaxBuildHeight(), c[1] + 128));
                StringBuilder who = new StringBuilder();
                for (ZoneBoss b : bosses) who.append(b.getName().getString()).append(" at ").append(b.blockPosition().toShortString()).append("; ");
                boolean biome = level.getBiome(new BlockPos(c[0], heightOf(zone), c[1])).is(zone.biome());
                report.add(String.format("visit %s at %d %d (%s in %.0f s): its biome at the centre: %s; bosses: %s",
                        id, c[0], c[1], loaded ? "made" : "NOT all made", ticks / 20.0, biome ? "yes" : "NO",
                        bosses.isEmpty() ? "NONE" : who.toString()));
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, c[0], c[1]);
                int eye = Math.min(level.getMaxBuildHeight() - 10, Math.max(top, zone.vertical().minY() > 100 ? 240 : top) + 55);
                p.teleportTo(level, c[0] + 0.5, eye, c[1] + 170.5, 180f, 22f);
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                if (ticks == 100) shoot("unique_" + id + "_" + seed);
                if (ticks == 110) {
                    if (zone == Zones.FENGDU) {
                        stage = 3; // and closer
                    } else {
                        visit++;
                        stage = 1;
                    }
                    ticks = 0;
                }
            }
            case 3 -> {
                // Fengdu close: up the road to its archway and gate; on its terrace before the Gate of Ghosts; from over the city
                int floor = com.jttw.engine.world.feature.FengduFeature.cityFloor(level, c[0], c[1]);
                int t = com.jttw.engine.world.fengdu.FengduCity.TERRACE_H;
                if (ticks == 1) {
                    BlockPos veil = new BlockPos(c[0], floor + t + 1, c[1] + com.jttw.engine.world.fengdu.FengduCity.GHOST_Z);
                    report.add(String.format("  fengdu: its floor y %d; the Gate of Ghosts' veil at %s: %s", floor, veil.toShortString(),
                            level.getBlockState(veil).is(com.jttw.engine.registry.ModBlocks.GHOST_GATE.get()) ? "yes" : "NO"));
                    p.teleportTo(level, c[0] + 9.5, floor + 9, c[1] + com.jttw.engine.world.fengdu.FengduCity.ARCH_Z + 26.5, 172f, 6f);
                }
                if (ticks == 90) shoot("unique_fengdu_road_" + seed);
                if (ticks == 100) p.teleportTo(level, c[0] + 4.5, floor + t + 2.6, c[1] + com.jttw.engine.world.fengdu.FengduCity.RETURN_Z + 8.5, 164f, -8f);
                if (ticks == 180) shoot("unique_fengdu_gate_" + seed);
                if (ticks == 190) p.teleportTo(level, c[0] + 0.5, floor + 64, c[1] + com.jttw.engine.world.fengdu.FengduCity.OUTER + 52.5, 180f, 42f);
                if (ticks == 280) shoot("unique_fengdu_above_" + seed);
                if (ticks == 290) {
                    visit++;
                    stage = 1;
                    ticks = 0;
                }
            }
            default -> { return true; }
        }
        return false;
    }

    /** The map: vanilla's biomes, the zones over them, their rings, spawn; written as a PNG. */
    private static void map(long seed, MultiNoiseBiomeSource source, Climate.Sampler sampler) {
        int n = HALF * 2 / STEP;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            int x = -HALF + i * STEP, z = -HALF + j * STEP;
            Color base = colour(name(vanilla(source, sampler, x, 64, z)));
            for (Zones.Zone zone : Zones.MAP.zones()) {
                if (zone.depth(seed, x, z) > 0) base = zoneColour(zone);
            }
            img.setRGB(i, j, base.getRGB());
        }
        Graphics2D g = img.createGraphics();
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4f, 4f}, 0f));
        g.setColor(new Color(255, 255, 255, 120));
        for (Zones.Zone zone : Zones.MAP.zones()) {
            for (int r : new int[]{zone.layout().near(), zone.layout().far()}) {
                int rr = r / STEP;
                g.drawOval(n / 2 - rr, n / 2 - rr, rr * 2, rr * 2);
            }
        }
        g.setColor(Color.WHITE);
        g.fillRect(n / 2 - 3, n / 2 - 1, 7, 3);
        g.fillRect(n / 2 - 1, n / 2 - 3, 3, 7);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        for (Zones.Zone zone : Zones.MAP.zones()) {
            for (int[] c : zone.centres(seed)) {
                int px = (c[0] + HALF) / STEP, pz = (c[1] + HALF) / STEP;
                g.setColor(Color.BLACK);
                g.drawString(label(zone), px + 7, pz + 5);
                g.setColor(zoneColour(zone).brighter());
                g.drawString(label(zone), px + 6, pz + 4);
            }
        }
        g.setColor(Color.WHITE);
        g.drawString("seed " + seed + "  (" + HALF * 2 / 1000 + " km square; rings every zone's band)", 6, 14);
        g.dispose();
        File dir = new File(Minecraft.getInstance().gameDirectory, "screenshots/zones");
        dir.mkdirs();
        File out = new File(dir, "unique_zones_map_" + seed + ".png");
        try {
            ImageIO.write(img, "png", out);
            report.add(String.format("  map (%d x %d blocks, a pixel each %d, in %.1f s): %s", HALF * 2, HALF * 2, STEP,
                    (System.nanoTime() - start) / 1e9, "screenshots/zones/" + out.getName()));
        } catch (java.io.IOException e) {
            report.add("  map NOT written: " + e);
        }
    }

    private static String label(Zones.Zone zone) {
        if (zone == Zones.WHITE_BONE) return "White Bone Ridge";
        if (zone == Zones.BOTTOMLESS) return "Bottomless Cave";
        if (zone == Zones.MYSTICAL_WATERS) return "Dragon King";
        if (zone == Zones.FIRE_MOUNTAIN) return "Flaming Mountains";
        if (zone == Zones.MOUNT_WUZHUANG) return "Mount Wuzhuang";
        if (zone == Zones.LION_CAMEL) return "Lion-Camel Ridge";
        if (zone == Zones.FENGDU) return "Fengdu";
        return zone.biome().location().getPath();
    }

    private static Color zoneColour(Zones.Zone zone) {
        if (zone == Zones.WHITE_BONE) return new Color(250, 246, 225);
        if (zone == Zones.BOTTOMLESS) return new Color(170, 90, 230);
        if (zone == Zones.MYSTICAL_WATERS) return new Color(0, 230, 230);
        if (zone == Zones.FIRE_MOUNTAIN) return new Color(255, 70, 0);
        if (zone == Zones.MOUNT_WUZHUANG) return new Color(255, 215, 0);
        if (zone == Zones.LION_CAMEL) return new Color(230, 0, 130);
        if (zone == Zones.FENGDU) return new Color(60, 200, 170);
        return Color.MAGENTA;
    }

    /** A vanilla biome's colour on the map, by its name. */
    private static Color colour(String b) {
        if (b.contains("deep") && b.contains("ocean")) return new Color(18, 36, 100);
        if (b.contains("ocean")) return new Color(40, 80, 160);
        if (b.contains("river")) return new Color(60, 110, 200);
        if (b.contains("beach") || b.contains("shore")) return new Color(220, 210, 150);
        if (b.contains("desert")) return new Color(225, 205, 130);
        if (b.contains("badlands")) return new Color(200, 110, 60);
        if (b.contains("savanna")) return new Color(175, 165, 85);
        if (b.contains("snowy") || b.contains("frozen") || b.contains("ice")) return new Color(225, 235, 240);
        if (b.contains("peaks") || b.contains("slopes") || b.contains("stony") || b.contains("windswept") || b.contains("grove")) return new Color(140, 140, 140);
        if (b.contains("cherry")) return new Color(230, 170, 200);
        if (b.contains("meadow")) return new Color(130, 190, 90);
        if (b.contains("jungle")) return new Color(40, 130, 40);
        if (b.contains("swamp") || b.contains("mangrove")) return new Color(70, 100, 60);
        if (b.contains("taiga")) return new Color(60, 110, 80);
        if (b.contains("mushroom")) return new Color(150, 100, 150);
        if (b.contains("forest")) return new Color(50, 120, 50);
        return new Color(90, 160, 70);
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
