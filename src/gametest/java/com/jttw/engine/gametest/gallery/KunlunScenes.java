package com.jttw.engine.gametest.gallery;

import com.jttw.engine.world.kunlun.KunlunPalace;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Mount Kunlun's summit in the form gallery: the Queen Mother's palace by the Jade Pool laid whole a
 * way off the stage (on the gallery's flat ground, the summit's snow round it), seen from the
 * stair's head before its gate, from above, over the Jade Pool, before the palace, and within it,
 * the treasure chest on its dais. The peak itself, its stair and its clouds are seen in a real
 * world: the Kunlun bench ({@code -Pbench.mode=kunlun}).
 */
final class KunlunScenes {

    private KunlunScenes() {}

    /** Where the summit is laid: its centre, from the stage's origin (east of it, clear of the other works off the stage). */
    private static final int CX = 110, CZ = 45;

    private static boolean laid;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    /** The summit, its paving (local y 0) the gallery's ground level, its snow round the terrace. */
    private static void summit(ServerPlayer p) {
        if (laid) return;
        ServerLevel level = p.serverLevel();
        BlockPos g = new BlockPos(CX, ground(p) - 1, CZ);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int reach = KunlunPalace.SUMMIT + 3;
        for (int x = -reach; x <= reach; x++) for (int z = -reach; z <= reach; z++) {
            if (x * x + z * z > reach * reach) continue;
            level.setBlock(at.set(CX + x, g.getY(), CZ + z), Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 1; y <= KunlunPalace.TOP; y++) level.setBlock(at.set(CX + x, g.getY() + y, CZ + z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        KunlunPalace.layWhole(level, g);
        laid = true;
    }

    /** A scene of the summit from {@code cam} (as sceneCam: x, y, z it stands at, x, y, z it looks at; local to the summit, heights over its ground). */
    private static AbilityScenes.Scene shot(String name, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            summit(p);
            AbilityScenes.sceneCam = new double[]{CX + cam[0], cam[1], CZ + cam[2], CX + cam[3], cam[4], CZ + cam[5]};
        }, p -> {}, 1, false);
    }

    static List<AbilityScenes.Scene> all() {
        int f = KunlunPalace.PLINTH_H + 1;
        return List.of(
                shot("kunlun_gate", new double[]{4.5, 4.0, KunlunPalace.SUMMIT + 6.0, 0.5, 4.0, KunlunPalace.GATE_Z}),
                shot("kunlun_above", new double[]{0.5, 46.0, KunlunPalace.SUMMIT + 30.0, 0.5, 0.0, -2.0}),
                shot("kunlun_jade_pool", new double[]{-12.5, 6.0, 18.0, 4.0, 1.0, 6.0}),
                shot("kunlun_palace", new double[]{8.5, 6.0, 9.0, 0.5, 8.0, KunlunPalace.HALL_S - 4.0}),
                shot("kunlun_palace_inside", new double[]{2.5, f + 2.2, KunlunPalace.HALL_S - 1.5, 0.5, f + 0.8, KunlunPalace.CHEST_Z}));
    }
}
