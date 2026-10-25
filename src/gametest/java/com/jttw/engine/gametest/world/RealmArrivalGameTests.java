package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.dragon.DragonAbilityHandler;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.dimension.RealmArrival;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/**
 * Where Wukong's realm leap and the Dragon's Dimension Travel come down (RealmArrival): never on the
 * Nether's bedrock roof, but on safe ground under it near the scaled place; the End's obsidian
 * platform; the Overworld's surface, eight times out from the Nether; Hell's and Heaven's own
 * arrivals. A test player ignores the move itself (as every teleport), so these check the realm it
 * is taken to and the place worked out for it.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class RealmArrivalGameTests {

    /** As HellGameTests: a fake player taken to another world must leave its player list. */
    private static void leaveWorld(ServerPlayer p) {
        p.serverLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    /**
     * The safe-spot finder on a built scene: a floor of lava, a magma block and one netherrack
     * block in it, a bedrock roof two blocks over them; it finds the netherrack, not the lava, the
     * magma or the roof's top.
     */
    @GameTest(template = PLATFORM)
    public static void theSafeSpotFinderPassesOverLavaMagmaAndTheRoof(GameTestHelper h) {
        openAir(h); // the framework's barrier roof at y=6 is no air to stand in
        ServerLevel level = h.getLevel();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.LAVA.defaultBlockState());
                h.setBlock(new BlockPos(x, 2, z), Blocks.AIR.defaultBlockState());
                h.setBlock(new BlockPos(x, 3, z), Blocks.AIR.defaultBlockState());
                h.setBlock(new BlockPos(x, 4, z), Blocks.BEDROCK.defaultBlockState());
                h.setBlock(new BlockPos(x, 5, z), Blocks.AIR.defaultBlockState());
            }
        }
        h.setBlock(new BlockPos(3, 1, 2), Blocks.MAGMA_BLOCK.defaultBlockState());
        h.setBlock(new BlockPos(4, 1, 2), Blocks.NETHERRACK.defaultBlockState());
        BlockPos centre = h.absolutePos(new BlockPos(2, 2, 2));
        int bottom = h.absolutePos(new BlockPos(0, 1, 0)).getY(), under = h.absolutePos(new BlockPos(0, 2, 0)).getY();
        try {
            check(h, !RealmArrival.canStand(level, centre), "not on lava");
            check(h, !RealmArrival.canStand(level, h.absolutePos(new BlockPos(3, 2, 2))), "not on magma");
            eq(h, null, RealmArrival.findSafeSpot(level, centre.getX(), centre.getZ(), bottom, under, 1), "nothing within one block");
            eq(h, h.absolutePos(new BlockPos(4, 2, 2)), RealmArrival.findSafeSpot(level, centre.getX(), centre.getZ(), bottom, under, 2),
                    "the netherrack, two blocks out, under the roof");
            eq(h, h.absolutePos(new BlockPos(2, 5, 2)), RealmArrival.findSafeSpot(level, centre.getX(), centre.getZ(), bottom, under + 3, 2),
                    "without the cap, the roof's own top (the arrival of old)");
        } finally {
            for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
        }
        h.succeed();
    }

    /** Into the Nether: an eighth of the way out, on ground with room to stand, under the roof. */
    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void theNetherArrivalIsSafeGroundNearTheScaledPlace(GameTestHelper h) {
        ServerPlayer p = player(h);
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
        check(h, nether != null, "the Nether is loaded");
        p.moveTo(803.5, 70, -797.5);
        RealmArrival.Arrival a = RealmArrival.arrival(p, nether);
        eq(h, nether, a.level(), "in the Nether");
        BlockPos feet = BlockPos.containing(a.pos());
        check(h, Math.abs(feet.getX() - 100) <= RealmArrival.NETHER_SEARCH_RADIUS
                && Math.abs(feet.getZ() + 100) <= RealmArrival.NETHER_SEARCH_RADIUS, "near 100, -100: " + feet.toShortString());
        check(h, feet.getY() + 1 < RealmArrival.NETHER_ROOF, "under the bedrock roof: " + feet.toShortString());
        check(h, RealmArrival.canStand(nether, feet), "on ground, two blocks of air, no lava: " + feet.toShortString());
        h.succeed();
    }

    /** Into the End: its obsidian platform, as by the end portal, facing west. */
    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void theEndArrivalIsItsObsidianPlatform(GameTestHelper h) {
        ServerPlayer p = player(h);
        ServerLevel end = h.getLevel().getServer().getLevel(Level.END);
        RealmArrival.Arrival a = RealmArrival.arrival(p, end);
        eq(h, end, a.level(), "in the End");
        eq(h, new Vec3(100.5, 49, 0.5), a.pos(), "on the platform by the spawn point");
        near(h, 90.0, a.yRot(), "facing west");
        for (int x = 98; x <= 102; x++) {
            for (int z = -2; z <= 2; z++) {
                check(h, end.getBlockState(new BlockPos(x, 48, z)).is(Blocks.OBSIDIAN), "obsidian at " + x + ", 48, " + z);
                for (int y = 49; y <= 51; y++) check(h, end.getBlockState(new BlockPos(x, y, z)).isAir(), "air at " + x + ", " + y + ", " + z);
            }
        }
        RealmArrival.travel(p, Level.END);
        eq(h, Level.END, p.level().dimension(), "taken to the End");
        leaveWorld(p);
        h.succeed();
    }

    /** Out of the Nether: eight times as far, on the Overworld's surface. */
    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void theOverworldArrivalIsTheSurfaceEightTimesOutOfTheNether(GameTestHelper h) {
        ServerPlayer p = player(h);
        ServerLevel overworld = h.getLevel().getServer().overworld();
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
        p.teleportTo(nether, 12.5, 70, -7.5, 0f, 0f);
        eq(h, Level.NETHER, p.level().dimension(), "in the Nether");
        p.moveTo(12.5, 70, -6.5);
        RealmArrival.Arrival a = RealmArrival.arrival(p, overworld);
        eq(h, overworld, a.level(), "in the Overworld");
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 96, -56);
        eq(h, new Vec3(96.5, y, -55.5), a.pos(), "at 96, -56, on the surface");
        leaveWorld(p);
        h.succeed();
    }

    /** The Overworld's surface will not do on water or lava, nor where there is no room. */
    @GameTest(template = PLATFORM)
    public static void theSurfaceIsNoArrivalOnWater(GameTestHelper h) {
        openAir(h);
        ServerLevel level = h.getLevel();
        BlockPos dry = h.absolutePos(new BlockPos(1, 1, 2)), wet = h.absolutePos(new BlockPos(3, 1, 2));
        h.setBlock(new BlockPos(3, 1, 2), Blocks.WATER.defaultBlockState());
        try {
            BlockPos onDry = RealmArrival.surface(level, dry.getX(), dry.getZ());
            check(h, onDry != null && onDry.getY() > dry.getY(), "dry ground will do: " + onDry);
            eq(h, null, RealmArrival.surface(level, wet.getX(), wet.getZ()), "water will not");
        } finally {
            h.setBlock(new BlockPos(3, 1, 2), Blocks.STONE.defaultBlockState());
        }
        h.succeed();
    }

    /** Hell and Heaven: their own arrivals (JTTWDimensions.findSafeSpawn), as every traveller's. */
    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void hellAndHeavenArriveAtTheirOwnArrivals(GameTestHelper h) {
        ServerPlayer p = player(h);
        for (var key : java.util.List.of(JTTWDimensions.HELL, JTTWDimensions.HEAVEN)) {
            ServerLevel realm = h.getLevel().getServer().getLevel(key);
            BlockPos spawn = JTTWDimensions.findSafeSpawn(realm);
            eq(h, new Vec3(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5), RealmArrival.arrival(p, realm).pos(),
                    key.location() + ": its arrival");
        }
        RealmArrival.travel(p, JTTWDimensions.HEAVEN);
        eq(h, JTTWDimensions.HEAVEN, p.level().dimension(), "taken to Heaven");
        leaveWorld(p);
        h.succeed();
    }

    /** Wukong's Shift + V: 20 Qi, and into the Nether. */
    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void wukongsRealmLeapTakesHimIntoTheNether(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        qi(p, 100f, 100f);
        grant(p, "wu_flight_1", "wu_flight_2", "wu_flight_3");
        Characters.byOutcome(ReincarnationOutcome.CELESTIAL_PRIMATE.id).activate(p, 4, AbilityAction.USE);
        near(h, 80f, QiData.getCurrent(p), "20 Qi a leap");
        eq(h, Level.NETHER, p.level().dimension(), "into the Nether");
        leaveWorld(p);
        h.succeed();
    }

    /** The Dragon's Shift + V, the End picked: 20 Qi, and into the End. */
    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void theDragonsDimensionTravelTakesItToTheEnd(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.DRAGON);
        qi(p, 100f, 100f);
        grant(p, "dr_flight_1", "dr_flight_2", "dr_flight_3");
        DragonAbilityHandler.scrollDimension(p, 2); // Overworld, Nether, End
        Characters.byOutcome(ReincarnationOutcome.DRAGON.id).activate(p, 4, AbilityAction.USE);
        near(h, 80f, QiData.getCurrent(p), "20 Qi a journey");
        eq(h, Level.END, p.level().dimension(), "into the End");
        leaveWorld(p);
        h.succeed();
    }
}
