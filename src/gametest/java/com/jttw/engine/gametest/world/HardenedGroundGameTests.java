package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.HardenedGround;
import com.jttw.engine.world.TerrainBreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/** In the mod's own places the ground stands up to destruction, unless its breaker is very strong. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class HardenedGroundGameTests {

    /** A block of stone in Heaven's open air, alone, low beneath its islands, its chunk loaded. */
    private static BlockPos stoneInHeaven(ServerLevel heaven, int x) {
        BlockPos at = new BlockPos(x, heaven.getMinBuildHeight() + 6, 0);
        heaven.getChunkAt(at);
        heaven.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
        return at;
    }

    private static void placed(GameTestHelper h, ServerLevel heaven, BlockPos at) {
        check(h, heaven.getBlockState(at).is(Blocks.STONE), "the stone is placed, at " + at);
    }

    @GameTest(template = PLATFORM, batch = "hardened_ground")
    public static void heavensStoneStandsTheWeakButNotTheMighty(GameTestHelper h) {
        ServerLevel heaven = h.getLevel().getServer().getLevel(JTTWDimensions.HEAVEN);
        check(h, heaven != null, "Heaven exists");
        ServerPlayer weak = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        ServerPlayer mighty = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(mighty, 100);
        BlockPos a = stoneInHeaven(heaven, 1000), b = stoneInHeaven(heaven, 1040);
        placed(h, heaven, a);
        placed(h, heaven, b);
        check(h, HardenedGround.hardened(heaven, a), "Heaven's ground is hardened");
        TerrainBreaker.explode(heaven, weak, a.getX() + 0.5, a.getY() + 1.5, a.getZ() + 0.5, 12f);
        TerrainBreaker.explode(heaven, mighty, b.getX() + 0.5, b.getY() + 1.5, b.getZ() + 0.5, 12f);
        TerrainBreaker.work(heaven, 100_000);
        check(h, heaven.getBlockState(a).is(Blocks.STONE), "a level-1 blast leaves it");
        check(h, heaven.getBlockState(b).isAir(), "the same blast, a level-100 character's, breaks it");
        // carving, likewise
        BlockPos c = stoneInHeaven(heaven, 1080), d = stoneInHeaven(heaven, 1120);
        placed(h, heaven, c);
        placed(h, heaven, d);
        TerrainBreaker.breakBlocks(heaven, List.of(c), weak);
        TerrainBreaker.breakBlocks(heaven, List.of(d), mighty);
        TerrainBreaker.work(heaven, 100_000);
        check(h, heaven.getBlockState(c).is(Blocks.STONE), "the weak cannot carve it");
        check(h, heaven.getBlockState(d).isAir(), "the mighty can");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void groundBeyondTheModsPlacesIsAsEver(GameTestHelper h) {
        check(h, !HardenedGround.hardened(h.getLevel(), h.absolutePos(BlockPos.ZERO)), "the test world's ground is ordinary");
        h.succeed();
    }
}
