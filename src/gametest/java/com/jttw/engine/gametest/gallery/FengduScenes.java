package com.jttw.engine.gametest.gallery;

import com.jttw.engine.world.fengdu.AncientFrame;
import com.jttw.engine.world.fengdu.FengduCity;
import com.jttw.engine.world.fengdu.FengduGate;
import com.jttw.engine.world.fengdu.GateRules;
import com.jttw.engine.world.fengdu.FengduShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Fengdu, the Ghost City, in the form gallery: the city laid whole a way off the stage (on the
 * gallery's flat ground, its hill's top round it and the head of its road), seen from the road
 * before its archway and gate, from above, on its terrace before the Gate of Ghosts and close to
 * its veil, inside the Palace of the Son of Heaven, down the avenue, among the shrines, and into the
 * City God's temple; its Gate of Ghosts sealed, its guard risen on the terrace, the gate igniting
 * when the last falls, and burning; a side door back to the Ancient City; and the way in, an
 * Ancient City's frame, built as vanilla lays it a way off the stage, and lit with soul fire. Its
 * hill and its country are seen in a real world: the zone bench ({@code -Pbench.mode=zones}).
 * Hell's side of the gates (the plaza's Gate of Ghosts and its side doors) is among the Hell scenes
 * ({@code hell_gate_doors}); the Yin soldiers close, there too ({@code hell_yin_soldier}).
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

    /** The city's ground, as its gate knows it. */
    private static BlockPos cityGround(ServerPlayer p) {
        return new BlockPos(CX, ground(p) - 1, CZ);
    }

    /** The gate's guard raised: as many vanguards as {@code arrivals} (the gallery's player standing for each pilgrim), held where they rose, facing south. */
    private static void raise(ServerPlayer p, int arrivals) {
        ServerLevel level = p.serverLevel();
        BlockPos g = cityGround(p);
        for (int i = 0; i < arrivals; i++) FengduGate.arriveByCity(p, g);
        FengduGate.step(level, g, List.of(p));
        for (UUID id : FengduGate.swarm(level, g)) {
            if (!(level.getEntity(id) instanceof Mob m)) continue;
            m.setNoAi(true);
            float yaw = 150f + level.random.nextFloat() * 60f;
            m.setYRot(yaw);
            m.setYHeadRot(yaw);
            m.setYBodyRot(yaw);
        }
    }

    /** The swarm beaten at once (its soldiers gone, wave after wave): the gate ignites. */
    private static void beat(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos g = cityGround(p);
        FengduGate.setTrip(p, GateRules.Trip.FIGHTING); // the gallery's player, standing for the pilgrim (off on the stage, their trip lapsed)
        for (int i = 0; i < 8 && FengduGate.swarmUp(level, g); i++) {
            FengduGate.step(level, g, List.of(p));
            for (UUID id : FengduGate.swarm(level, g)) {
                Entity e = level.getEntity(id);
                if (e != null) e.discard();
            }
        }
        FengduGate.step(level, g, List.of(p));
    }

    /** A scene of the gate: the city laid, its gate forgotten (sealed, no guard), then {@code setup}; the camera at {@code cam}. */
    private static AbilityScenes.Scene gate(String name, Consumer<ServerPlayer> setup, Consumer<ServerPlayer> trigger, int before, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            city(p);
            FengduGate.reset(p.serverLevel(), cityGround(p));
            setup.accept(p);
            AbilityScenes.sceneCam = new double[]{CX + cam[0], cam[1], CZ + cam[2], CX + cam[3], cam[4], CZ + cam[5]};
        }, trigger, before, false);
    }

    // ── The Ancient City's frame ────────────────────────────────────────

    /** Where the frame is built: its way's lowest, most negative block (x, z), from the stage's origin; it runs along x. */
    private static final int FX = 50, FZ = -40;

    /**
     * An Ancient City's frame as vanilla lays it, on a floor of deepslate tiles, a border of tiles
     * round its ring and a plinth under it, sculk and candles about (lit afresh each time: its way
     * emptied first).
     */
    private static AncientFrame.Frame frame(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        AncientFrame.Frame f = new AncientFrame.Frame(true, FX, g + 1, FZ);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = -8; i <= AncientFrame.WIDTH + 7; i++) for (int k = -7; k <= 7; k++) {
            int r = Math.floorMod(i * 31 + k * 17, 23);
            BlockState floor = (r < 3 ? Blocks.SCULK : r < 13 ? Blocks.DEEPSLATE_TILES : r < 19 ? Blocks.DEEPSLATE_BRICKS : Blocks.CRACKED_DEEPSLATE_TILES).defaultBlockState();
            level.setBlock(at.set(FX + i, g - 1, FZ + k), floor, Block.UPDATE_CLIENTS);
            if (r == 5 && Math.abs(k) >= 3) level.setBlock(at.set(FX + i, g, FZ + k), Blocks.CANDLE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3).setValue(net.minecraft.world.level.block.CandleBlock.LIT, true), Block.UPDATE_CLIENTS);
        }
        for (int i = -3; i <= AncientFrame.WIDTH + 2; i++) for (int j = -1; j <= AncientFrame.HEIGHT + 2; j++) for (int k = -1; k <= 1; k++) {
            boolean ring = k == 0 && i >= -1 && i <= AncientFrame.WIDTH && j >= -1 && j <= AncientFrame.HEIGHT
                    && (i == -1 || i == AncientFrame.WIDTH || j == -1 || j == AncientFrame.HEIGHT);
            boolean way = i >= 0 && i < AncientFrame.WIDTH && j >= 0 && j < AncientFrame.HEIGHT;
            boolean border = !way && !ring && (i <= -2 || i >= AncientFrame.WIDTH + 1 || j >= AncientFrame.HEIGHT + 1 || j == -1 && k != 0);
            BlockState s = ring ? Blocks.REINFORCED_DEEPSLATE.defaultBlockState() : border ? (Math.floorMod(i + j, 5) == 0 ? Blocks.DEEPSLATE_BRICKS
                    : Blocks.DEEPSLATE_TILES).defaultBlockState() : Blocks.AIR.defaultBlockState();
            level.setBlock(at.set(FX + i, g + j, FZ + k), s, Block.UPDATE_CLIENTS);
        }
        return f;
    }

    /** Lights the frame as a player would: soul sand in its way, soul fire set on it. */
    private static void lightFrame(ServerPlayer p, AncientFrame.Frame f) {
        int[] c = f.cell(AncientFrame.WIDTH / 2, 0);
        BlockPos sand = new BlockPos(c[0], c[1], c[2]);
        p.serverLevel().setBlockAndUpdate(sand, Blocks.SOUL_SAND.defaultBlockState());
        p.serverLevel().setBlockAndUpdate(sand.above(), Blocks.SOUL_FIRE.defaultBlockState());
    }

    private static AbilityScenes.Scene frameShot(String name, boolean lit, int before, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            AncientFrame.Frame f = frame(p);
            if (!lit) {
                int[] c = f.cell(AncientFrame.WIDTH / 2, 0);
                p.serverLevel().setBlock(new BlockPos(c[0], c[1], c[2]), Blocks.SOUL_SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            AbilityScenes.sceneCam = new double[]{FX + cam[0], cam[1], FZ + cam[2], FX + cam[3], cam[4], FZ + cam[5]};
        }, p -> {
            if (lit) lightFrame(p, frame(p));
        }, before, false);
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
        double w = AncientFrame.WIDTH / 2.0;
        double[] beforeGate = {6.5, t + 4.0, FengduCity.RETURN_Z + 9.0, 0.5, t + 3.0, FengduCity.GHOST_Z};
        return List.of(
                gate("fengdu_gate_sealed", p -> {}, p -> {}, 1, beforeGate),
                gate("fengdu_gate_sealed_close", p -> {}, p -> {}, 1, new double[]{1.5, t + 2.2, FengduCity.GHOST_Z + 4.5, 0.5, t + 3.0, FengduCity.GHOST_Z}),
                gate("fengdu_swarm", p -> {}, p -> raise(p, 3), 10, new double[]{-7.5, t + 7.0, FengduCity.RETURN_Z + 13.0, 0.5, t + 1.0, FengduCity.GHOST_Z + 1}),
                gate("fengdu_swarm_close", p -> {}, p -> raise(p, 2), 10, new double[]{3.5, t + 2.4, FengduCity.RETURN_Z + 2.0, -1.5, t + 1.6, FengduCity.GHOST_Z + 1}),
                gate("fengdu_gate_igniting", p -> FengduGate.arriveByCity(p, cityGround(p)), FengduScenes::beat, 6, beforeGate),
                gate("fengdu_gate_burning", p -> FengduGate.arriveFromHell(p, cityGround(p)), p -> FengduGate.arriveFromHell(p, cityGround(p)), 30, beforeGate),
                gate("fengdu_side_door", p -> {}, p -> {}, 1, new double[]{-FengduCity.SIDE_DOOR_IN - 0.5, t + 2.2, FengduCity.GHOST_Z + 4.0,
                        -FengduCity.SIDE_DOOR_IN + 0.0, t + 2.0, FengduCity.GHOST_Z}),
                frameShot("ancient_frame", false, 1, new double[]{w, 4.0, 17.0, w, 4.0, 0.0}),
                frameShot("ancient_frame_lighting", true, 4, new double[]{w, 4.0, 17.0, w, 4.0, 0.0}),
                frameShot("ancient_frame_lit", true, 40, new double[]{w - 6, 3.0, 9.0, w, 4.0, 0.0}),
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
