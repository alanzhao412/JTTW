package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.hell.HellKings;
import com.jttw.engine.entity.hell.HellMonster;
import com.jttw.engine.entity.hell.ImpermanenceEntity;
import com.jttw.engine.entity.hell.JudgeClerkEntity;
import com.jttw.engine.entity.hell.KingOfHellEntity;
import com.jttw.engine.entity.hell.KingYamaEntity;
import com.jttw.engine.entity.hell.OxHeadEntity;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.hell.HellTerrain;
import com.jttw.engine.world.hell.HellWorks;
import com.jttw.engine.world.hell.Site;
import com.jttw.engine.world.hell.TenCourts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/**
 * Hell in the form gallery: its creatures and its kings on a patch of ash and black stone before
 * the stage; and pieces of its works laid a little way off the stage from their own plan, shifted
 * there ({@link Site}'s offsets): the First Court's hall with its king, King Yama's great hall,
 * the Tenth Court with the Wheel of Reincarnation, and the Bridge of Helplessness over a stretch of
 * the Naihe (its land laid from {@link HellTerrain}). The pieces are laid once and left. Hell as it
 * really is (the plaza, the road, the fortress) is seen by the zone bench's hell mode.
 */
final class HellScenes {

    private HellScenes() {}

    /** The patch: x -8..8, z 1..16. */
    private static final int X0 = -8, X1 = 8, Z0 = 1, Z1 = 16;

    private static boolean patch, court, yama, wheel, bridge;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    private static void patch(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            double n = HellTerrain.grain(x, 0, z);
            BlockState s = n < 0.55 ? ModBlocks.SOUL_ASH.get().defaultBlockState() : n < 0.8 ? Blocks.SOUL_SOIL.defaultBlockState()
                    : n < 0.93 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
            level.setBlock(at.set(x, g - 1, z), s, Block.UPDATE_CLIENTS);
        }
        patch = true;
    }

    private static <T extends Mob> T put(ServerPlayer p, EntityType<T> type, double x, double z, float yaw) {
        T m = type.create(p.serverLevel());
        if (m == null) return null;
        m.moveTo(x, ground(p), z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(true);
        m.setPersistenceRequired();
        p.serverLevel().addFreshEntity(m);
        return m;
    }

    private static <T extends HellMonster> T creature(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, boolean captain) {
        T m = put(p, type, x, z, yaw);
        if (m != null) m.become(captain ? m.captainLevel() : m.regularLevel(), captain);
        return m;
    }

    private static AbilityScenes.Scene scene(String name, Consumer<ServerPlayer> cast, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    private static AbilityScenes.Scene scene(String name, Consumer<ServerPlayer> cast, Consumer<ServerPlayer> trigger, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, trigger, 5, false);
    }

    private static final double[] PAIR = {0.5, 3.4, -0.5, 0.5, 2.0, 8.0}, CLOSE = {0.5, 3.0, 2.5, 0.5, 2.3, 8.0},
            LINEUP = {0.5, 5.0, -0.5, 0.5, 1.8, 10.5};

    static List<AbilityScenes.Scene> all() {
        return List.of(
                scene("hell_ox", p -> {
                    creature(p, ModEntities.OX_HEAD.get(), -2.0, 8.0, 200f, false);
                    creature(p, ModEntities.OX_HEAD.get(), 2.5, 10.0, 160f, true);
                }, PAIR),
                scene("hell_ox_close", p -> creature(p, ModEntities.OX_HEAD.get(), 0.5, 7.5, 160f, false), CLOSE),
                scene("hell_ox_side", p -> creature(p, ModEntities.OX_HEAD.get(), 0.5, 7.5, 90f, false), CLOSE),
                scene("hell_ox_charging", p -> {
                    OxHeadEntity ox = creature(p, ModEntities.OX_HEAD.get(), 0.5, 7.5, 160f, false);
                    if (ox != null) ox.showCharging(true);
                }, CLOSE),
                scene("hell_horse", p -> {
                    creature(p, ModEntities.HORSE_FACE.get(), -2.0, 8.0, 200f, false);
                    creature(p, ModEntities.HORSE_FACE.get(), 2.5, 10.0, 160f, true);
                }, PAIR),
                scene("hell_horse_close", p -> creature(p, ModEntities.HORSE_FACE.get(), 0.5, 7.5, 160f, false), CLOSE),
                scene("hell_horse_side", p -> creature(p, ModEntities.HORSE_FACE.get(), 0.5, 7.5, 90f, false), CLOSE),
                scene("hell_guards", p -> {
                    creature(p, ModEntities.OX_HEAD.get(), -1.5, 8.0, 180f, false);
                    creature(p, ModEntities.HORSE_FACE.get(), 2.5, 8.0, 180f, false);
                }, PAIR),
                scene("hell_clerk", p -> {
                    creature(p, ModEntities.JUDGE_CLERK.get(), -1.0, 7.0, 190f, false);
                    creature(p, ModEntities.JUDGE_CLERK.get(), 2.5, 9.0, 160f, true);
                }, PAIR),
                scene("hell_clerk_writing", p -> creature(p, ModEntities.JUDGE_CLERK.get(), 0.5, 6.5, 160f, false), p -> {
                    p.serverLevel().getEntitiesOfClass(JudgeClerkEntity.class, new AABB(p.blockPosition()).inflate(12)).forEach(JudgeClerkEntity::showWriting);
                }, CLOSE),
                scene("hell_impermanence", p -> {
                    creature(p, ModEntities.BLACK_IMPERMANENCE.get(), -1.0, 7.5, 190f, false);
                    creature(p, ModEntities.WHITE_IMPERMANENCE.get(), 2.0, 7.5, 170f, false);
                }, PAIR),
                scene("hell_white_close", p -> creature(p, ModEntities.WHITE_IMPERMANENCE.get(), 0.5, 7.0, 170f, false), CLOSE),
                scene("hell_black_close", p -> creature(p, ModEntities.BLACK_IMPERMANENCE.get(), 0.5, 7.0, 190f, false), CLOSE),
                scene("hell_impermanence_side", p -> {
                    creature(p, ModEntities.BLACK_IMPERMANENCE.get(), -1.0, 7.5, 90f, false);
                    creature(p, ModEntities.WHITE_IMPERMANENCE.get(), 2.0, 7.5, 90f, false);
                }, PAIR),
                scene("hell_ghosts", p -> {
                    creature(p, ModEntities.WANDERING_GHOST.get(), -2.5, 8.0, 200f, false);
                    creature(p, ModEntities.WANDERING_GHOST.get(), 0.5, 10.0, 180f, false);
                    creature(p, ModEntities.WANDERING_GHOST.get(), 3.5, 8.5, 150f, true);
                }, PAIR),
                scene("hell_ghost_close", p -> creature(p, ModEntities.WANDERING_GHOST.get(), 0.5, 6.5, 170f, false), CLOSE),
                // the Ten Kings, in two rows of five: the First to the Fifth behind, the Sixth to the Tenth before
                scene("hell_kings", p -> {
                    for (int n = 1; n <= 10; n++) {
                        int col = (n - 1) % 5;
                        put(p, HellKings.type(n), -5.5 + col * 3, n <= 5 ? 12.5 : 8.5, 180f);
                    }
                }, LINEUP),
                scene("hell_king_close", p -> put(p, ModEntities.KING_QINGUANG.get(), 0.5, 7.0, 170f), CLOSE),
                scene("hell_king_side", p -> put(p, ModEntities.KING_CHUJIANG.get(), 0.5, 7.0, 90f), CLOSE),
                scene("hell_king_judging", p -> put(p, ModEntities.KING_TAISHAN.get(), 0.5, 7.0, 170f), p -> {
                    p.serverLevel().getEntitiesOfClass(KingOfHellEntity.class, new AABB(p.blockPosition()).inflate(12)).forEach(KingOfHellEntity::showJudging);
                }, CLOSE),
                scene("hell_yama", p -> {
                    put(p, ModEntities.KING_YAMA.get(), 0.5, 9.0, 180f);
                    creature(p, ModEntities.OX_HEAD.get(), -3.0, 8.0, 190f, false);
                    creature(p, ModEntities.HORSE_FACE.get(), 4.0, 8.0, 170f, false);
                }, PAIR),
                scene("hell_yama_close", p -> put(p, ModEntities.KING_YAMA.get(), 0.5, 7.5, 175f), new double[]{0.5, 3.4, 2.0, 0.5, 2.6, 8.0}),
                scene("hell_yama_wrath", p -> {
                    KingYamaEntity y = put(p, ModEntities.KING_YAMA.get(), 0.5, 8.5, 175f);
                    if (y != null) y.wrath();
                }, new double[]{0.5, 3.8, 1.0, 0.5, 2.8, 8.5}),
                // the works, laid off the stage
                new AbilityScenes.Scene("hell_court", p -> {
                    court(p);
                    // from the avenue's side, before the door, over the yard
                    AbilityScenes.sceneCam = cam(-4, -414, HellTerrain.GROUND + 10, COURT_O, -38, -400, HellTerrain.GROUND + 5);
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("hell_court_inside", p -> {
                    court(p);
                    AbilityScenes.sceneCam = cam(-27, -400, HellTerrain.GROUND + 4.2, COURT_O, -47, -400, HellTerrain.GROUND + 4.0);
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("hell_yama_hall", p -> {
                    yama(p);
                    AbilityScenes.sceneCam = cam(16, -522, HellTerrain.GROUND + 14, YAMA_O, 0, -556, HellTerrain.GROUND + 6);
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("hell_yama_hall_inside", p -> {
                    yama(p);
                    AbilityScenes.sceneCam = cam(0, -545, HellTerrain.GROUND + 5, YAMA_O, 0, -567, HellTerrain.GROUND + 5);
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("hell_wheel", p -> {
                    wheel(p);
                    int[] w = TenCourts.wheel();
                    AbilityScenes.sceneCam = cam(w[0], w[1] + 14, HellTerrain.GROUND + 7, WHEEL_O, w[0], w[1], HellTerrain.GROUND + 7);
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("hell_court_ten", p -> {
                    wheel(p);
                    AbilityScenes.sceneCam = cam(8, -380, HellTerrain.GROUND + 11, WHEEL_O, 38, -360, HellTerrain.GROUND + 4);
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("hell_bridge", p -> {
                    bridge(p);
                    AbilityScenes.sceneCam = cam(26, -146, HellTerrain.GROUND + 7, BRIDGE_O, 0, -160, HellTerrain.GROUND + 1);
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("hell_bridge_walk", p -> {
                    bridge(p);
                    int[] b = HellTerrain.bridgeEnds();
                    AbilityScenes.sceneCam = cam(0, b[1] + 6, HellTerrain.GROUND + 3, BRIDGE_O, 0, b[0], HellTerrain.GROUND + 3);
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("hell_meng_po", p -> {
                    bridge(p);
                    int[] b = HellTerrain.bridgeEnds();
                    AbilityScenes.sceneCam = cam(0, b[0] - 14, HellTerrain.GROUND + 5, BRIDGE_O, -9, b[0] - 6, HellTerrain.GROUND + 2);
                }, p -> {}, 1, false));
    }

    // ── The works, laid off the stage ───────────────────────────────────

    /** Where each piece is laid: {ox, oz, plan height laid on the gallery's ground}. */
    private static final int[] COURT_O = {38, 470, HellTerrain.GROUND + 1}, YAMA_O = {100, 631, HellTerrain.GROUND + 1},
            WHEEL_O = {-138, 285, HellTerrain.GROUND + 1}, BRIDGE_O = {-90, 240, HellTerrain.SEA_LEVEL - 6};

    /** A camera at plan (x, y, z) looking at plan (tx, ty, tz), for a piece laid at offset {@code o}: as sceneCam (heights over the ground). */
    private static double[] cam(double x, double z, double y, int[] o, double tx, double tz, double ty) {
        return new double[]{x + o[0] + 0.5, y - o[2], z + o[1] + 0.5, tx + o[0] + 0.5, ty - o[2], tz + o[1] + 0.5};
    }

    /**
     * Lays the plan's box (x0, z0)..(x1, z1) of Hell at offset {@code o}: its land, from the
     * gallery's ground up (ash over underworld stone, soul soil under water; the rivers' water), then
     * the works there; and stills whatever the works put there (its guards, its king).
     */
    private static void lay(ServerPlayer p, int x0, int z0, int x1, int z1, int[] o) {
        ServerLevel level = p.serverLevel();
        int g = ground(p), dy = g - o[2];
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int top = HellTerrain.surface(x, z);
            int px = x + o[0], pz = z + o[1];
            for (int y = o[2]; y <= top; y++) {
                level.setBlock(at.set(px, y + dy, pz), (y == top ? (top < HellTerrain.SEA_LEVEL - 1 ? Blocks.SOUL_SOIL.defaultBlockState()
                        : ModBlocks.SOUL_ASH.get().defaultBlockState()) : ModBlocks.UNDERWORLD_STONE.get().defaultBlockState()), Block.UPDATE_CLIENTS);
            }
            for (int y = top + 1; y < HellTerrain.SEA_LEVEL; y++) level.setBlock(at.set(px, y + dy, pz), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        HellWorks.build(new Site(level, x0, z0, x1, z1, o[0], dy, o[1]), level.getSeed());
        level.getEntitiesOfClass(Mob.class, new AABB(x0 + o[0], g - 10, z0 + o[1], x1 + o[0] + 1, g + 60, z1 + o[1] + 1))
                .forEach(m -> {
                    m.setNoAi(true);
                    m.setPersistenceRequired();
                });
    }

    private static void court(ServerPlayer p) {
        if (court) return;
        lay(p, -64, -416, -8, -384, COURT_O);
        court = true;
    }

    private static void yama(ServerPlayer p) {
        if (yama) return;
        lay(p, -28, -580, 28, -520, YAMA_O);
        yama = true;
    }

    private static void wheel(ServerPlayer p) {
        if (wheel) return;
        lay(p, 4, -380, 62, -338, WHEEL_O);
        wheel = true;
    }

    private static void bridge(ServerPlayer p) {
        if (bridge) return;
        int[] b = HellTerrain.bridgeEnds();
        lay(p, -24, b[0] - 16, 24, b[1] + 12, BRIDGE_O);
        bridge = true;
    }

    /** Clears the patch's creatures before the next shot (the pieces of the works stay). */
    static void clear(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        if (patch) {
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) level.setBlock(at.set(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            patch = false;
        }
        level.getEntities((Entity) null, new AABB(X0 - 4, g - 3, Z0 - 4, X1 + 4, g + 8, Z1 + 4),
                e -> e instanceof HellMonster || e instanceof KingOfHellEntity).forEach(Entity::discard);
    }
}
