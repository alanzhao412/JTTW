package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.peng.PengSkillTree;
import com.jttw.engine.dimension.GreatVoid;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/**
 * The Great Void: an empty realm one to one over every other, Wukong's and the Peng's at their
 * top flight. In at where one is, out where one has got to, in the realm one came from.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class GreatVoidGameTests {

    /** As HellGameTests: a fake player taken to another world must leave its player list. */
    private static void leaveWorld(ServerPlayer p) {
        p.serverLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static ServerLevel voidLevel(GameTestHelper h) {
        ServerLevel v = h.getLevel().getServer().getLevel(GreatVoid.KEY);
        check(h, v != null, "the Great Void is loaded");
        return v;
    }

    /** Nothing is made there: a chunk far out is air from its floor to its top, and one to one. */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void theVoidIsEmptyAndOneToOne(GameTestHelper h) {
        ServerLevel v = voidLevel(h);
        eq(h, 1.0, v.dimensionType().coordinateScale(), "one to one");
        check(h, v.getMinBuildHeight() <= -64 && v.getMaxBuildHeight() >= 640, "as deep and high as the Overworld can be");
        LevelChunk chunk = v.getChunk(31_250, -12_500);
        for (int y = v.getMinBuildHeight(); y < v.getMaxBuildHeight(); y += 7) {
            for (int x = 0; x < 16; x += 5) for (int z = 0; z < 16; z += 5) {
                if (!chunk.getBlockState(new BlockPos(x, y, z)).isAir()) {
                    h.fail("a block in the Void at " + x + ", " + y + ", " + z);
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void onlyTheTopFliersMayEnter(GameTestHelper h) {
        ServerPlayer wukong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        grant(wukong, "wu_flight_1", "wu_flight_2");
        check(h, !GreatVoid.canEnter(wukong), "not Wukong before Somersault Cloud");
        grant(wukong, "wu_flight_3");
        check(h, GreatVoid.canEnter(wukong), "Wukong at Somersault Cloud");

        ServerPlayer peng = as(h, ReincarnationOutcome.GOLD_WINGED_PENG);
        grant(peng, PengSkillTree.SOAR_1, PengSkillTree.SOAR_2);
        check(h, !GreatVoid.canEnter(peng), "not the Peng before Ninety-Thousand-Li III");
        grant(peng, PengSkillTree.SOAR_3);
        check(h, GreatVoid.canEnter(peng), "the Peng at III");

        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        check(h, !GreatVoid.canEnter(erlang), "no one else");
        GreatVoid.toggle(erlang);
        eq(h, Level.OVERWORLD, erlang.level().dimension(), "the key does nothing for him");
        h.succeed();
    }

    /** In, flying; out where one has got to, in the realm one came from. */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void inAndOutAtTheSamePlaceOrWhereOneHasGot(GameTestHelper h) {
        ServerLevel v = voidLevel(h);
        ServerLevel home = h.getLevel();
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        grant(p, "wu_flight_1", "wu_flight_2", "wu_flight_3");
        openAir(h);
        Vec3 start = h.absoluteVec(new Vec3(2.5, 4, 2.5));
        p.moveTo(start.x, start.y, start.z);

        GreatVoid.toggle(p);
        eq(h, GreatVoid.KEY, p.level().dimension(), "in the Void");
        check(h, p.getAbilities().flying, "flying");
        eq(h, home, GreatVoid.origin(p), "the way home remembered");

        // Across the Void and back out, over the platform's floor
        Vec3 there = start.add(0, -2.5, 0); // the feet inside the platform's stone
        p.moveTo(there.x, there.y, there.z);
        // (a test player ignores where a teleport puts it, as RealmArrivalGameTests says: so the
        // place worked out for it, and the realm it is taken to)
        Vec3 out = GreatVoid.exit(p, home);
        GreatVoid.toggle(p);
        eq(h, home.dimension(), p.level().dimension(), "back home");
        eq(h, start.x, out.x, "same x");
        eq(h, start.z, out.z, "same z");
        BlockPos floor = h.absolutePos(new BlockPos(2, 1, 2));
        eq(h, floor.getY() + 1.0, out.y, "inside the stone: the nearest room, just on top of it");
        check(h, !p.getPersistentData().contains(GreatVoid.ORIGIN_TAG), "the way home forgotten");
        leaveWorld(p);
        h.succeed();
    }

    /** Out of the Void into the realm one came from: the Nether stays the Nether, one to one. */
    @GameTest(template = PLATFORM)
    public static void theWayHomeIsTheRealmOneCameFrom(GameTestHelper h) {
        ServerPlayer p = player(h);
        eq(h, Level.OVERWORLD, GreatVoid.origin(p).dimension(), "unknown: the Overworld");
        p.getPersistentData().putString(GreatVoid.ORIGIN_TAG, "minecraft:the_nether");
        eq(h, Level.NETHER, GreatVoid.origin(p).dimension(), "from the Nether, the Nether");
        p.getPersistentData().putString(GreatVoid.ORIGIN_TAG, "jttw_engine:no_such_realm");
        eq(h, Level.OVERWORLD, GreatVoid.origin(p).dimension(), "a realm that is gone: the Overworld");
        h.succeed();
    }

    /** Coming out in open air keeps one's height; inside a wall, the nearest room above or below. */
    @GameTest(template = PLATFORM)
    public static void theExitIsTheNearestRoom(GameTestHelper h) {
        ServerPlayer p = player(h);
        openAir(h);
        Vec3 air = h.absoluteVec(new Vec3(2.5, 3.25, 2.5));
        p.moveTo(air.x, air.y, air.z);
        eq(h, air, GreatVoid.exit(p, h.getLevel()), "in the open: exactly there");
        // a pillar from the floor up through where he is: out on its top
        for (int y = 2; y <= 4; y++) h.setBlock(new BlockPos(2, y, 2), Blocks.STONE.defaultBlockState());
        try {
            Vec3 out = GreatVoid.exit(p, h.getLevel());
            eq(h, air.x, out.x, "same x");
            eq(h, air.z, out.z, "same z");
            eq(h, h.absolutePos(new BlockPos(2, 5, 2)).getY() * 1.0, out.y, "on the pillar's top");
        } finally {
            for (int y = 2; y <= 4; y++) h.setBlock(new BlockPos(2, y, 2), Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }
}
