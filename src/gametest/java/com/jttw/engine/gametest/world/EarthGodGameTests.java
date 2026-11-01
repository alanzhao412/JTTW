package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.entity.waters.DragonKingEntity;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.hell.TenCourts;
import com.jttw.engine.world.waters.Sea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/**
 * One of each zone a world, and the Earth Gods who say where: the world's placement found with its
 * biome source, on host ground; the zone for each level; the Earth God's Luopan pointing there; the
 * four seas and their fallen kings; Hell and the other worlds.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class EarthGodGameTests {

    @GameTest(template = PLATFORM)
    public static void theWorldsZonesArePlacedOnceEachOnTheirHosts(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        long seed = level.getSeed();
        if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource source)) {
            h.succeed(); // a flat test world places no zones
            return;
        }
        check(h, Zones.MAP.settled(seed), "found with the world's biome source");
        var sampler = level.getChunkSource().randomState().sampler();
        String notes = Zones.MAP.placement(seed).notes();
        for (Zones.Zone zone : Zones.MAP.zones()) {
            int[][] cs = zone.centres(seed);
            eq(h, zone == Zones.MYSTICAL_WATERS ? 4 : 1, cs.length, zone.id() + "'s regions");
            for (int[] c : cs) {
                var host = source.getNoiseBiome(sampler.sample(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(zone.hostY()), QuartPos.fromBlock(c[1])));
                check(h, host.is(zone.hosts()) || notes.contains("off its hosts"), zone.id() + " on its hosts at " + c[0] + " " + c[1]);
                int y = zone.vertical().contains(64) ? 64 : zone.vertical().maxY() < 64 ? 0 : 200;
                check(h, source.getNoiseBiome(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(y), QuartPos.fromBlock(c[1]), sampler).is(zone.biome()),
                        zone.id() + "'s biome is placed at its centre");
            }
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void eachLevelHasItsZone(GameTestHelper h) {
        eq(h, Zones.WHITE_BONE, EarthGod.zoneFor(1), "under the first band, the first zone");
        eq(h, Zones.WHITE_BONE, EarthGod.zoneFor(36), "36");
        eq(h, Zones.BOTTOMLESS, EarthGod.zoneFor(37), "37");
        eq(h, Zones.MYSTICAL_WATERS, EarthGod.zoneFor(50), "50");
        eq(h, Zones.HELL, EarthGod.zoneFor(60), "60");
        eq(h, Zones.FENGDU, EarthGod.overworldZoneFor(60), "Hell's band is sent to Fengdu, the way down");
        eq(h, Zones.FIRE_MOUNTAIN, EarthGod.zoneFor(70), "70");
        eq(h, Zones.MOUNT_WUZHUANG, EarthGod.zoneFor(80), "80");
        eq(h, Zones.LION_CAMEL, EarthGod.zoneFor(100), "100");
        eq(h, Zones.LION_CAMEL, EarthGod.zoneFor(150), "past the last band, the last zone");
        for (int lv = 1; lv <= 150; lv++) check(h, EarthGod.zoneFor(lv) != Zones.FENGDU, "Fengdu is no zone of a band: " + lv);
        eq(h, "east", EarthGod.wind(0, 0, 100, 3), "east");
        eq(h, "north-west", EarthGod.wind(0, 0, -100, -100), "north-west");
        eq(h, "south", EarthGod.wind(0, 0, 0, 100), "+z is south");
        eq(h, 1400L, EarthGod.roughly(1437), "to the hundred");
        eq(h, 350L, EarthGod.roughly(362), "to the fifty");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theLuopanPointsToTheZoneForItsHoldersLevel(GameTestHelper h) {
        ServerPlayer p = player(h);
        long seed = h.getLevel().getSeed();
        if (!h.getLevel().dimension().equals(Level.OVERWORLD)) {
            h.succeed();
            return;
        }
        ItemStack luopan = new ItemStack(ModItems.EARTH_GOD_LUOPAN.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, luopan);
        PlayerLevel.setLevel(p, 25);
        luopan.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        LodestoneTracker t = luopan.get(DataComponents.LODESTONE_TRACKER);
        check(h, t != null && t.target().isPresent(), "it points somewhere");
        Zones.Zone chosen = EarthGod.chosenFor(p.position(), 25, seed, Set.of());
        check(h, EarthGod.zonesFor(25).contains(chosen), "at 25, a zone of the first band: " + chosen);
        eq(h, EarthGod.targetOf(chosen, seed, chosen.centres(seed)[0]), t.target().get().pos(),
                "at 25, to the nearest zone of the band (" + chosen.id() + "), where its needle marks");
        eq(h, Level.OVERWORLD, t.target().get().dimension(), "in the overworld");
        check(h, p.getCooldowns().isOnCooldown(ModItems.EARTH_GOD_LUOPAN.get()), "a pause before the next calling");
        // held at another level, its needle turns as it ticks
        PlayerLevel.setLevel(p, 95);
        p.tickCount = 0;
        luopan.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        int[] lion = Zones.LION_CAMEL.centres(seed)[0];
        eq(h, new BlockPos(lion[0], 64, lion[1] + com.jttw.engine.world.lion.LionFortress.DOOR_LZ), luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos(),
                "to Lion-Camel Ridge at 95: the great door of its fortress");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void forTheSeasTheNearestWhoseKingLives(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        int[][] seas = Zones.MYSTICAL_WATERS.centres(seed);
        int[] east = seas[Sea.EAST.ordinal()];
        // standing by the Eastern Sea: sent there, unless Ao Guang is dead
        eq(h, Sea.EAST, Sea.of(east[0], east[1]), "the east sea's region");
        int[] got = EarthGod.regionFor(Zones.MYSTICAL_WATERS, seed, east[0], east[1], EnumSet.noneOf(Sea.class));
        eq(h, Sea.EAST, Sea.of(got[0], got[1]), "the nearest");
        got = EarthGod.regionFor(Zones.MYSTICAL_WATERS, seed, east[0], east[1], EnumSet.of(Sea.EAST));
        check(h, Sea.of(got[0], got[1]) != Sea.EAST, "Ao Guang dead: another sea");
        got = EarthGod.regionFor(Zones.MYSTICAL_WATERS, seed, east[0], east[1], EnumSet.allOf(Sea.class));
        eq(h, Sea.EAST, Sea.of(got[0], got[1]), "all four dead: the nearest still");
        var answer = EarthGod.answer(Level.OVERWORLD, new Vec3(east[0] + 0.5, 64, east[1] + 0.5), 50, seed, Set.of());
        check(h, answer.words().getString().contains("Ao Guang"), "it names the Eastern Sea's king: " + answer.words().getString());
        check(h, answer.words().getString().contains("You stand in that country now"), "and says the asker is there");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aDragonKingsDeathIsRemembered(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        DragonKingEntity king = h.spawn(ModEntities.AO_SHUN.get(), new BlockPos(4, 2, 4));
        king.setNoAi(true);
        onEnd(h, king::discard);
        king.die(h.getLevel().damageSources().generic());
        check(h, EarthGod.FallenKings.of(overworld).seas().contains(Sea.NORTH), "Ao Shun's sea remembered as kingless");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void inHellAndOtherWorldsTheEarthGodsSayOtherwise(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        var hell = EarthGod.answer(JTTWDimensions.HELL, new Vec3(0, 64, 0), 60, seed, Set.of());
        check(h, hell.target() != null && hell.target().dimension().equals(JTTWDimensions.HELL), "in Hell: a place in Hell");
        eq(h, TenCourts.GATE_Z, hell.target().pos().getZ(), "the Ten Courts' gate");
        check(h, hell.zone() == null, "no zone");
        var nether = EarthGod.answer(Level.NETHER, new Vec3(0, 64, 0), 40, seed, Set.of());
        check(h, nether.target() == null, "in the Nether: nowhere (the needle spins)");
        var diyu = EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, 60, seed, Set.of());
        eq(h, Zones.FENGDU, diyu.zone(), "Hell's band in the overworld: Fengdu, the way down");
        check(h, diyu.words().getString().contains("Diyu") && diyu.words().getString().contains("Fengdu"), "told of Diyu, and of Fengdu");
        h.succeed();
    }
    @GameTest(template = PLATFORM)
    public static void theEarthGodNamesEveryZoneOfTheBand(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        eq(h, Set.of(Zones.WHITE_BONE, Zones.FLOWER_FRUIT, Zones.BLACK_WIND), Set.copyOf(EarthGod.zonesFor(25)), "25: the ridge, the island, Black Wind");
        eq(h, Set.of(Zones.WHITE_BONE, Zones.FLOWER_FRUIT, Zones.BLACK_WIND), Set.copyOf(EarthGod.zonesFor(5)), "under the bands: the first band's");
        eq(h, Set.of(Zones.WHITE_BONE, Zones.BLACK_WIND), Set.copyOf(EarthGod.zonesFor(33)), "33: past the island's band");
        eq(h, Set.of(Zones.MOUNT_WUZHUANG, Zones.SPIDER_CAVE), Set.copyOf(EarthGod.zonesFor(80)), "80: Mount Wuzhuang and Silk-Web Ridge");
        eq(h, java.util.List.of(Zones.LION_CAMEL), EarthGod.zonesFor(95), "95: one zone");
        check(h, EarthGod.zonesFor(60).isEmpty(), "Hell's band: no zone of the living");
        if (Zones.SPIDER_CAVE.centres(seed).length == 0) { // a flat test world places no zones
            h.succeed();
            return;
        }
        for (Vec3 at : new Vec3[]{Vec3.ZERO, new Vec3(3000, 70, -2500), new Vec3(-6800, 70, 1200)}) {
            for (int lv : new int[]{25, 33, 80}) {
                if (EarthGod.zonesFor(lv).stream().anyMatch(z -> z.depth(seed, (int) at.x, (int) at.z) > 0)) continue; // in one: that one, below
                var a = EarthGod.answer(Level.OVERWORLD, at, lv, seed, Set.of());
                String words = a.words().getString();
                // sent to the nearest, by where its needle marks
                Zones.Zone nearest = null;
                double best = Double.MAX_VALUE;
                for (Zones.Zone z : EarthGod.zonesFor(lv)) {
                    BlockPos t = EarthGod.targetOf(z, seed, z.centres(seed)[0]);
                    double d = Math.hypot(t.getX() + 0.5 - at.x, t.getZ() + 0.5 - at.z);
                    if (d < best) { best = d; nearest = z; }
                }
                eq(h, nearest, a.zone(), "level " + lv + " at " + at + ": the nearest zone of the band");
                eq(h, EarthGod.targetOf(nearest, seed, nearest.centres(seed)[0]), a.target().pos(), "its needle to it");
                for (Zones.Zone z : EarthGod.zonesFor(lv)) {
                    String name = z == Zones.WHITE_BONE ? "White Bone" : z == Zones.BLACK_WIND ? "Black Wind" : z == Zones.FLOWER_FRUIT ? "Flower-Fruit"
                            : z == Zones.SPIDER_CAVE ? "Silk-Web Ridge" : "Mount Longevity";
                    check(h, words.contains(name), "level " + lv + " at " + at + ": " + name + " named: " + words);
                }
                check(h, words.contains("Others of your strength") && words.contains("blocks to the"), "the others' ways told: " + words);
                check(h, words.contains("Flower-Fruit") == words.contains("beyond any mortal band"), "the island's master warned of: " + words);
            }
        }
        // standing in a zone of the band: that one, whichever is nearer
        int[] bw = Zones.BLACK_WIND.centres(seed)[0];
        var inBw = EarthGod.answer(Level.OVERWORLD, new Vec3(bw[0] + 0.5, 90, bw[1] + 0.5), 25, seed, Set.of());
        eq(h, Zones.BLACK_WIND, inBw.zone(), "in Black Wind's country: Black Wind");
        check(h, inBw.words().getString().contains("You stand in that country") && inBw.words().getString().contains("White Bone Ridge"),
                "there, and the ridge named besides: " + inBw.words().getString());
        int[] sp = Zones.SPIDER_CAVE.centres(seed)[0];
        var inSp = EarthGod.answer(Level.OVERWORLD, new Vec3(sp[0] + 0.5, 70, sp[1] + 0.5), 80, seed, Set.of());
        eq(h, Zones.SPIDER_CAVE, inSp.zone(), "on Silk-Web Ridge at 80: the Spider Cave");
        eq(h, EarthGod.needle(Zones.SPIDER_CAVE, sp), inSp.target().pos(), "its needle to the cottage");
        check(h, inSp.words().getString().contains("Mount Longevity"), "and Mount Wuzhuang named besides: " + inSp.words().getString());
        int[] ff = Zones.FLOWER_FRUIT.centres(seed)[0];
        var inFf = EarthGod.answer(Level.OVERWORLD, new Vec3(ff[0] + 0.5, 80, ff[1] + 0.5), 22, seed, Set.of());
        eq(h, Zones.FLOWER_FRUIT, inFf.zone(), "on the island at 22: Flower-Fruit");
        check(h, inFf.words().getString().contains("beyond any mortal band"), "its master warned of: " + inFf.words().getString());
        h.succeed();
    }
}
