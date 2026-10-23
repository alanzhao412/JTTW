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
        eq(h, Zones.FIRE_MOUNTAIN, EarthGod.overworldZoneFor(60), "Hell's band is sent on to the Flaming Mountains");
        eq(h, Zones.FIRE_MOUNTAIN, EarthGod.zoneFor(70), "70");
        eq(h, Zones.MOUNT_WUZHUANG, EarthGod.zoneFor(80), "80");
        eq(h, Zones.LION_CAMEL, EarthGod.zoneFor(100), "100");
        eq(h, Zones.LION_CAMEL, EarthGod.zoneFor(150), "past the last band, the last zone");
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
        int[] ridge = Zones.WHITE_BONE.centres(seed)[0];
        eq(h, new BlockPos(ridge[0], 64, ridge[1]), t.target().get().pos(), "to White Bone Ridge at 25");
        eq(h, Level.OVERWORLD, t.target().get().dimension(), "in the overworld");
        check(h, p.getCooldowns().isOnCooldown(ModItems.EARTH_GOD_LUOPAN.get()), "a pause before the next calling");
        // held at another level, its needle turns as it ticks
        PlayerLevel.setLevel(p, 95);
        p.tickCount = 0;
        luopan.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        int[] lion = Zones.LION_CAMEL.centres(seed)[0];
        eq(h, new BlockPos(lion[0], 64, lion[1]), luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos(), "to Lion-Camel Ridge at 95");
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
        eq(h, Zones.FIRE_MOUNTAIN, diyu.zone(), "Hell's band in the overworld: the Flaming Mountains");
        check(h, diyu.words().getString().contains("Diyu"), "told of Diyu");
        h.succeed();
    }
}
