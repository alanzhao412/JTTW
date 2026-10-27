package com.jttw.engine.village;

import com.jttw.engine.registry.ModVillages;
import com.jttw.engine.world.fengdu.FengduCity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/**
 * The markets of the supernatural world, where talismans, charms and incense are sold (no mortal
 * sells them): the <b>ghost market</b> (鬼市) in the lower city of Fengdu, two stalls either side of
 * the avenue just inside the gate, each kept by a dead shopkeeper ({@link FengduCity#MARKET_STALLS});
 * and <b>Heaven's merchant-immortals</b>, one keeping a stall in every third pavilion of the
 * Immortals on the celestial islands. They are put there as the land is made, as a village's folk
 * are; Fengdu spawns nothing of itself, and its ghosts' stalls are no exception.
 */
public final class SpiritMarkets {

    private SpiritMarkets() {}

    /** One pavilion in this many has a merchant-immortal's stall in it. */
    public static final int HEAVEN_STALL_EVERY = 3;

    /** Puts a spirit merchant of this calling standing at {@code at}, facing {@code yaw}, keeping to its stall. */
    @Nullable
    public static TangFolkEntity put(ServerLevelAccessor level, Calling calling, BlockPos at, float yaw) {
        TangFolkEntity f = ModVillages.SPIRIT_MERCHANT.get().create(level.getLevel());
        if (f == null) return null;
        f.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0f);
        f.setYHeadRot(yaw);
        f.setYBodyRot(yaw);
        f.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        f.settle(calling, false, null, at);
        level.addFreshEntityWithPassengers(f);
        return f;
    }

    /** The ghost market's shopkeepers whose stalls are in the chunk at (x0, z0), Fengdu's ground at {@code ground} (its centre). */
    public static void ghostMerchants(ServerLevelAccessor level, int x0, int z0, BlockPos ground) {
        for (int[] s : FengduCity.MARKET_STALLS) {
            int x = ground.getX() + s[0], z = ground.getZ() + s[1];
            if (x < x0 || x >= x0 + 16 || z < z0 || z >= z0 + 16) continue;
            // behind the counter, facing the avenue
            put(level, Calling.GHOST_MERCHANT, new BlockPos(x, ground.getY() + 1, z), s[0] < 0 ? 270f : 90f);
        }
    }

    /** Whether the pavilion on the island of cell (cx, cz) has a merchant-immortal's stall. */
    public static boolean heavenlyStall(long seed, int cx, int cz) {
        return RandomSource.create(seed ^ 0x57A11L ^ cx * 341873128712L ^ cz * 132897987541L).nextInt(HEAVEN_STALL_EVERY) == 0;
    }

    /** A merchant-immortal's stall in the pavilion whose floor's middle is {@code at}: a jade counter, a lantern, the merchant behind it. */
    public static void celestialStall(ServerLevelAccessor level, BlockPos at) {
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlock(at.offset(dx, 0, -1), dx == 0 ? Blocks.GOLD_BLOCK.defaultBlockState()
                    : com.jttw.engine.registry.ModBlocks.JADE_BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(at.offset(-1, 1, -1), Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(at.offset(1, 1, -1), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true),
                Block.UPDATE_CLIENTS);
        put(level, Calling.CELESTIAL_MERCHANT, at.offset(0, 0, -2), 0f);
    }
}
