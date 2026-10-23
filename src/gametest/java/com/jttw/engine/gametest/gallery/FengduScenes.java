package com.jttw.engine.gametest.gallery;

import com.jttw.engine.world.fengdu.FengduCity;
import com.jttw.engine.world.fengdu.FengduShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Fengdu, the Ghost City, in the form gallery: the city laid whole a way off the stage (on the
 * gallery's flat ground, its hill's top round it and the head of its road), seen from the road
 * before its archway and gate, from above, on its terrace before the Gate of Ghosts and close to
 * its veil, inside the Palace of the Son of Heaven, down the avenue, among the shrines, and into the
 * City God's temple. Its hill and its country are seen in a real world: the zone bench
 * ({@code -Pbench.mode=zones}). Hell's side of the gates (the plaza's Gate of Ghosts and its side
 * doors) is among the Hell scenes ({@code hell_gate_doors}).
 */
final class FengduScenes {

    private FengduScenes() {}

    /** Where the city is laid: its centre, from the stage's origin (west of it, clear of the other works off the stage, in sight of the camera). */
    private static final int CX = -100, CZ = -5;

    private static boolean laid;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    /** The city, its ground (local y 0) the gallery's ground level, the hill's worn top round it and the road down from its gate. */
    private static void city(ServerPlayer p) {
        if (laid) return;
        ServerLevel level = p.serverLevel();
        BlockPos g = new BlockPos(CX, ground(p) - 1, CZ);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int reach = FengduCity.OUTER + 4;
        for (int x = -reach; x <= reach; x++) for (int z = -reach; z <= FengduCity.ARCH_Z + 3; z++) {
            int r = Math.floorMod(x * 31 + z * 17, 20);
            BlockState top = FengduShape.onRoad(x, z) ? (r < 12 ? Blocks.STONE_BRICKS : r < 16 ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS).defaultBlockState()
                    : (r < 11 ? Blocks.GRASS_BLOCK : r < 15 ? Blocks.COARSE_DIRT : r < 18 ? Blocks.GRAVEL : Blocks.PODZOL).defaultBlockState();
            level.setBlock(at.set(CX + x, g.getY(), CZ + z), top, Block.UPDATE_CLIENTS);
            if (Math.abs(x) == FengduShape.ROAD_HALF + 1 && FengduShape.onRoad(0, z) && !FengduCity.covers(x, z)
                    && Math.floorMod(z, 6) == 0 && (Math.floorMod(z, 12) == 0) == (x > 0)) {
                level.setBlock(at.set(CX + x, g.getY() + 1, CZ + z), Blocks.DARK_OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(at.set(CX + x, g.getY() + 2, CZ + z), Blocks.DARK_OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(at.set(CX + x, g.getY() + 3, CZ + z), Blocks.SOUL_LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        FengduCity.layWhole(level, g);
        laid = true;
    }

    /** A scene of the city from {@code cam} (as sceneCam: x, y, z it stands at, x, y, z it looks at; local to the city, heights over its ground). */
    private static AbilityScenes.Scene shot(String name, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            city(p);
            AbilityScenes.sceneCam = new double[]{CX + cam[0], cam[1], CZ + cam[2], CX + cam[3], cam[4], CZ + cam[5]};
        }, p -> {}, 1, false);
    }

    static List<AbilityScenes.Scene> all() {
        int t = FengduCity.TERRACE_H;
        return List.of(
                shot("fengdu_gate", new double[]{9.5, 7.0, FengduCity.ARCH_Z + 22.0, 0.5, 6.0, FengduCity.WALL_OUT}),
                shot("fengdu_above", new double[]{0.5, 62.0, FengduCity.OUTER + 46.0, 0.5, 0.0, -6.0}),
                shot("fengdu_ghost_gate", new double[]{5.5, t + 3.5, FengduCity.RETURN_Z + 8.0, 0.5, t + 4.0, FengduCity.GHOST_Z}),
                shot("fengdu_ghost_gate_close", new double[]{2.0, t + 2.2, FengduCity.GHOST_Z + 4.0, 0.5, t + 3.0, FengduCity.GHOST_Z}),
                shot("fengdu_palace", new double[]{-9.5, t + 7.0, FengduCity.PALACE_S + 9.0, 0.5, t + 5.0, FengduCity.HALL_S - 4.0}),
                shot("fengdu_palace_inside", new double[]{0.5, t + 3.6, FengduCity.HALL_S - 1.5, 0.5, t + 3.0, FengduCity.THRONE_Z}),
                shot("fengdu_avenue", new double[]{0.5, 3.4, FengduCity.WALL_IN - 3.0, 0.5, t + 3.0, FengduCity.GHOST_Z}),
                shot("fengdu_shrines", new double[]{3.5, 2.6, FengduCity.PLOT_FROM_Z + FengduCity.PLOT + 1.0, 18.0, 1.5, FengduCity.PLOT_FROM_Z + 4.0}),
                shot("fengdu_temple", new double[]{-11.5, t + 2.8, (FengduCity.TEMPLE_N + FengduCity.TEMPLE_S) / 2.0 + 0.5,
                        -FengduCity.TEMPLE_OUT, t + 2.5, (FengduCity.TEMPLE_N + FengduCity.TEMPLE_S) / 2.0 + 0.5}));
    }
}
