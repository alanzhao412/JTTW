package com.jttw.engine.gametest.world;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.fengdu.AncientFrame;
import com.jttw.engine.world.fengdu.AncientGate;
import com.jttw.engine.world.fengdu.Fengdu;
import com.jttw.engine.world.fengdu.FengduCity;
import com.jttw.engine.world.fengdu.FengduGate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Comparator;
import java.util.function.BooleanSupplier;

import static com.jttw.engine.gametest.GT.onEnd;

/**
 * Places for the tests of Fengdu's gates: the city laid at its real place in the test world (a
 * flat one: no land of its own is made there), held loaded with its creatures ticking (a test
 * player keeps no chunks loaded), and Ancient City frames built far from every test's platform.
 * The tests that use the real gate each run in a batch of their own, so no two share it at once.
 */
final class GateSites {

    private GateSites() {}

    private static final TicketType<ChunkPos> HOLD = TicketType.create("jttw_fengdu_gate_test", Comparator.comparingLong(ChunkPos::toLong), 20 * 60 * 5);

    /** Fengdu's city, laid at its place at the floor the gates look for (laid again if an older city stands there); its ground. */
    static BlockPos layFengdu(ServerLevel overworld) {
        BlockPos ground = Fengdu.ground(overworld);
        if (!overworld.getBlockState(ground.offset(FengduCity.SIDE_DOOR_IN, FengduCity.TERRACE_H + 1, FengduCity.GHOST_Z)).is(ModBlocks.GHOST_GATE.get())) {
            FengduCity.layWhole(overworld, ground);
        }
        return ground;
    }

    /** Holds the chunks round {@code at} loaded, their creatures ticking, until the test ends. */
    static void hold(GameTestHelper h, ServerLevel level, BlockPos at) {
        ChunkPos c = new ChunkPos(at);
        level.getChunkSource().addRegionTicket(HOLD, c, 4, c, true);
        onEnd(h, () -> level.getChunkSource().removeRegionTicket(HOLD, c, 4, c, true));
    }

    /** Fengdu laid, held, its gate forgotten (sealed, no swarm) now and when the test ends; its ground. */
    static BlockPos freshFengdu(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        BlockPos ground = layFengdu(overworld);
        hold(h, overworld, FengduGate.arrival(ground));
        FengduGate.reset(overworld, ground);
        onEnd(h, () -> FengduGate.reset(overworld, ground));
        return ground;
    }

    /** Runs {@code then} once {@code ready} holds, trying each tick up to {@code tries} times (failing, saying {@code what}, after). */
    static void when(GameTestHelper h, BooleanSupplier ready, int tries, String what, Runnable then) {
        if (ready.getAsBoolean()) then.run();
        else if (tries <= 0) h.fail("never came to pass: " + what);
        else h.runAfterDelay(1, () -> when(h, ready, tries - 1, what, then));
    }

    /**
     * An Ancient City's frame as vanilla lays it (a ring of reinforced deepslate round a way 20 by 6),
     * its way's lowest, most negative block at {@code origin}, running along x or z, on a floor of
     * deepslate tiles reaching out either side; taken down when the test ends.
     */
    static AncientFrame.Frame buildFrame(GameTestHelper h, ServerLevel level, BlockPos origin, boolean alongX) {
        AncientFrame.Frame f = new AncientFrame.Frame(alongX, origin.getX(), origin.getY(), origin.getZ());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = -3; i <= AncientFrame.WIDTH + 2; i++) {
            for (int k = -4; k <= 4; k++) {
                for (int j = -2; j <= AncientFrame.HEIGHT + 1; j++) {
                    int x = alongX ? origin.getX() + i : origin.getX() + k, z = alongX ? origin.getZ() + k : origin.getZ() + i;
                    boolean ring = k == 0 && i >= -1 && i <= AncientFrame.WIDTH && j >= -1 && j <= AncientFrame.HEIGHT
                            && (i == -1 || i == AncientFrame.WIDTH || j == -1 || j == AncientFrame.HEIGHT);
                    Block b = ring ? Blocks.REINFORCED_DEEPSLATE : j == -2 ? Blocks.DEEPSLATE_TILES : Blocks.AIR;
                    level.setBlock(p.set(x, origin.getY() + j, z), b.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        onEnd(h, () -> {
            for (int i = -3; i <= AncientFrame.WIDTH + 2; i++) for (int k = -4; k <= 4; k++) for (int j = -2; j <= AncientFrame.HEIGHT + 1; j++) {
                int x = alongX ? origin.getX() + i : origin.getX() + k, z = alongX ? origin.getZ() + k : origin.getZ() + i;
                level.setBlock(p.set(x, origin.getY() + j, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            AncientGate.Lit.of(level).remove(f);
        });
        return f;
    }

    /** Where to build a test's frame: far out from the test platforms, high over the flat world, a place of its own for each {@code n}. */
    static BlockPos frameSite(int n) {
        return new BlockPos(-24_000 - 64 * n, 120, 24_000);
    }

    /** Lights the frame {@code f} as a player would: soul sand in its way, soul fire set on it. */
    static void lightWithSoulFire(ServerLevel level, AncientFrame.Frame f, int i) {
        int[] c = f.cell(i, 0);
        BlockPos sand = new BlockPos(c[0], c[1], c[2]);
        level.setBlockAndUpdate(sand, Blocks.SOUL_SAND.defaultBlockState());
        level.setBlockAndUpdate(sand.above(), Blocks.SOUL_FIRE.defaultBlockState());
    }

    static BlockPos at(int[] c) {
        return new BlockPos(c[0], c[1], c[2]);
    }
}
