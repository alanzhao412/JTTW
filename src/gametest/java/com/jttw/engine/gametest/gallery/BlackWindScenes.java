package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.blackwind.BearSoldierEntity;
import com.jttw.engine.entity.blackwind.BlackBearDemonEntity;
import com.jttw.engine.entity.blackwind.BlackWhirlwindEntity;
import com.jttw.engine.entity.blackwind.BlackWindMonster;
import com.jttw.engine.entity.blackwind.BoarDemonEntity;
import com.jttw.engine.entity.blackwind.LingxuziEntity;
import com.jttw.engine.entity.blackwind.WhiteRobedScholarEntity;
import com.jttw.engine.entity.blackwind.WindImpEntity;
import com.jttw.engine.registry.BlackWindRegistry;
import com.jttw.engine.world.blackwind.BlackWindBlocks;
import com.jttw.engine.world.blackwind.BlackWindCave;
import com.jttw.engine.world.blackwind.BlackWindPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Black Wind Mountain in the form gallery: its creatures (each kind, a captain beside a regular),
 * Lingxuzi and the White-Robed Scholar, and the Black Bear Demon, front, side, back and close, each in
 * its attacks' poses, on a patch of dark pine-forest floor laid before the stage (podzol and coarse earth,
 * dark spruce trunks behind, a smouldering stump); the king among his whirlwinds; and pieces of the Black
 * Wind Cave laid off the stage, high: its great door with its two keepers before it, the Black Wind Court
 * with its garrison, the throne cavern with the king before his dais; and the burnt Guanyin Chan Temple.
 * The mountain itself, its pines and its black winds, are seen in a real world: the bench's blackwind mode.
 */
final class BlackWindScenes {

    private BlackWindScenes() {}

    private static final int X0 = -8, X1 = 8, Z0 = 3, Z1 = 18;
    /** Where the cave's pieces and the temple are laid, from the stage, and how high. */
    private static final int DOOR_X = 60, DOOR_Z = -100, DOOR_UP = 40, COURT_X = -80, COURT_Z = -30, COURT_UP = 40,
            THRONE_X = -80, THRONE_Z = 160, THRONE_UP = 60, TEMPLE_X = 64, TEMPLE_Z = 72;

    private static boolean patch, door, court, throne, temple;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    private static void patch(ServerPlayer p) {
        if (patch) return;
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            int roll = Math.floorMod(x * 31 + z * 17, 11);
            BlockState floor = roll < 5 ? Blocks.PODZOL.defaultBlockState() : roll < 8 ? Blocks.COARSE_DIRT.defaultBlockState()
                    : roll < 10 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            level.setBlock(at.set(x, g - 1, z), floor, Block.UPDATE_CLIENTS);
        }
        // dark spruce trunks behind, their boughs over the back
        for (int x = X0; x <= X1; x += 3) {
            int high = 9 + Math.floorMod(x * 7, 4);
            for (int y = 0; y < high; y++) level.setBlock(at.set(x, g + y, Z1 + 1), Blocks.SPRUCE_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int dx = -2; dx <= 2; dx++) for (int dy = 4; dy < high + 1; dy++) {
                if (Math.abs(dx) + (dy % 3) > 3) continue;
                level.setBlock(at.set(x + dx, g + dy, Z1 + 2), Blocks.SPRUCE_LEAVES.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int x = X0; x <= X1; x++) for (int y = 0; y < 6; y++) level.setBlock(at.set(x, g + y, Z1 + 3), Blocks.DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
        // a smouldering stump and a fern or two
        level.setBlock(at.set(X1 - 1, g, Z0 + 3), Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y), Block.UPDATE_CLIENTS);
        level.setBlock(at.set(X1 - 1, g + 1, Z0 + 3), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true), Block.UPDATE_CLIENTS);
        level.setBlock(at.set(X0 + 1, g, Z0 + 6), Blocks.FERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(at.set(X0 + 2, g, Z1 - 1), Blocks.FERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        patch = true;
    }

    private static <T extends Mob> T put(ServerPlayer p, EntityType<T> type, double x, double y, double z, float yaw) {
        T m = type.create(p.serverLevel());
        if (m == null) return null;
        m.moveTo(x, ground(p) + y, z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(true);
        p.serverLevel().addFreshEntity(m);
        return m;
    }

    private static <T extends BlackWindMonster> T creature(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, boolean captain) {
        T m = put(p, type, x, 0, z, yaw);
        if (m != null) m.become(captain ? m.captainLevel() : m.regularLevel(), captain);
        return m;
    }

    private static BlackBearDemonEntity king(ServerPlayer p, double x, double z, float yaw) {
        return put(p, BlackWindRegistry.BLACK_BEAR_DEMON.get(), x, 0, z, yaw);
    }

    private static AbilityScenes.Scene scene(String name, java.util.function.Consumer<ServerPlayer> cast, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    static List<AbilityScenes.Scene> all() {
        return List.of(
                // the Black Bear Demon
                scene("bw_king_front", p -> king(p, 0.5, 11.5, 180f), new double[]{0.5, 2.6, 5.0, 0.5, 2.3, 11.5}),
                scene("bw_king_quarter", p -> king(p, 0.5, 11.5, 210f), new double[]{-3.5, 3.0, 6.2, 0.5, 2.2, 11.5}),
                scene("bw_king_side", p -> king(p, 0.5, 11.5, 90f), new double[]{0.5, 2.5, 5.0, 0.5, 2.2, 11.5}),
                scene("bw_king_back", p -> king(p, 0.5, 9.5, 0f), new double[]{0.5, 2.8, 3.2, 0.5, 2.3, 9.5}),
                scene("bw_king_face", p -> king(p, 0.5, 9.5, 180f), new double[]{1.1, 4.3, 6.4, 0.5, 4.2, 9.5}),
                scene("bw_king_kasaya", p -> king(p, 0.5, 9.5, 180f), new double[]{1.4, 3.0, 5.6, 0.5, 2.7, 9.5}),
                scene("bw_king_black_wind", p -> {
                    BlackBearDemonEntity k = king(p, 0.5, 11.5, 180f);
                    if (k != null) k.pose(BlackBearDemonEntity.GATHERING);
                }, new double[]{0.5, 2.9, 5.0, 0.5, 2.5, 11.5}),
                scene("bw_king_maul", p -> {
                    BlackBearDemonEntity k = king(p, 0.5, 11.5, 160f);
                    if (k != null) k.pose(BlackBearDemonEntity.REARING);
                }, new double[]{-3.0, 2.8, 5.5, 0.5, 2.8, 11.5}),
                scene("bw_king_gale", p -> {
                    BlackBearDemonEntity k = king(p, 0.5, 11.5, 200f);
                    if (k != null) k.pose(BlackBearDemonEntity.DRAWING);
                }, new double[]{4.0, 2.8, 5.8, 0.5, 2.4, 11.5}),
                new AbilityScenes.Scene("bw_king_whirlwinds", p -> {
                    patch(p);
                    BlackBearDemonEntity k = king(p, 0.5, 12.5, 180f);
                    if (k != null) {
                        k.confine(6);
                        k.pose(BlackBearDemonEntity.GATHERING);
                        k.raiseWhirlwinds(3);
                    }
                    AbilityScenes.sceneCam = new double[]{0.5, 5.0, -2.0, 0.5, 2.5, 12.5};
                }, p -> {}, 1, false),
                // his soldiers, a captain among them
                scene("bw_soldiers", p -> {
                    creature(p, BlackWindRegistry.BEAR_SOLDIER.get(), -2.5, 11.5, 200f, false);
                    creature(p, BlackWindRegistry.BEAR_SOLDIER.get(), 2.5, 12.5, 160f, true);
                }, new double[]{0.5, 2.3, 5.8, 0.5, 1.8, 12.0}),
                scene("bw_soldier_back", p -> creature(p, BlackWindRegistry.BEAR_SOLDIER.get(), 0.5, 9.5, 20f, false),
                        new double[]{0.5, 2.2, 4.6, 0.5, 1.7, 9.5}),
                scene("bw_soldier_slam", p -> {
                    BearSoldierEntity s = creature(p, BlackWindRegistry.BEAR_SOLDIER.get(), 0.5, 9.5, 150f, false);
                    if (s != null) s.rear();
                }, new double[]{-2.6, 2.3, 4.6, 0.5, 2.2, 9.5}),
                // the boars
                scene("bw_boars", p -> {
                    creature(p, BlackWindRegistry.BOAR_DEMON.get(), -2.0, 9.5, 200f, false);
                    creature(p, BlackWindRegistry.BOAR_DEMON.get(), 2.0, 10.5, 160f, true);
                }, new double[]{0.5, 1.9, 5.4, 0.5, 1.3, 10.0}),
                scene("bw_boar_close", p -> creature(p, BlackWindRegistry.BOAR_DEMON.get(), 0.5, 8.5, 200f, false),
                        new double[]{1.3, 1.9, 5.7, 0.5, 1.7, 8.5}),
                scene("bw_boar_side", p -> creature(p, BlackWindRegistry.BOAR_DEMON.get(), 0.5, 8.5, 90f, false),
                        new double[]{0.5, 1.6, 5.2, 0.5, 1.3, 8.5}),
                scene("bw_boar_charge", p -> {
                    BoarDemonEntity b = creature(p, BlackWindRegistry.BOAR_DEMON.get(), 0.5, 9.5, 120f, false);
                    if (b != null) b.pose(BoarDemonEntity.CHARGING);
                    BoarDemonEntity c = creature(p, BlackWindRegistry.BOAR_DEMON.get(), -3.5, 12.5, 200f, false);
                    if (c != null) c.pose(BoarDemonEntity.PAWING);
                }, new double[]{3.0, 1.9, 5.4, -0.5, 1.2, 10.5}),
                // the imps
                scene("bw_imps", p -> {
                    creature(p, BlackWindRegistry.WIND_IMP.get(), -1.5, 7.5, 200f, false);
                    creature(p, BlackWindRegistry.WIND_IMP.get(), 1.5, 8.0, 160f, true);
                    creature(p, BlackWindRegistry.WIND_IMP.get(), -0.2, 9.5, 180f, false);
                }, new double[]{0.5, 1.3, 5.3, 0.5, 0.8, 8.0}),
                scene("bw_imp_close", p -> creature(p, BlackWindRegistry.WIND_IMP.get(), 0.5, 7.0, 200f, false),
                        new double[]{0.9, 1.2, 5.0, 0.5, 0.9, 7.0}),
                scene("bw_imp_back", p -> creature(p, BlackWindRegistry.WIND_IMP.get(), 0.5, 7.0, 20f, false),
                        new double[]{0.5, 1.2, 5.0, 0.5, 0.8, 7.0}),
                scene("bw_imp_ride", p -> {
                    WindImpEntity i = creature(p, BlackWindRegistry.WIND_IMP.get(), 0.5, 7.5, 100f, false);
                    if (i != null) i.pose(WindImpEntity.RIDING);
                }, new double[]{0.5, 1.2, 5.2, 0.5, 0.8, 7.5}),
                // Lingxuzi
                scene("bw_lingxuzi", p -> put(p, BlackWindRegistry.LINGXUZI.get(), 0.5, 0, 8.5, 190f), new double[]{0.9, 1.7, 5.5, 0.5, 1.4, 8.5}),
                scene("bw_lingxuzi_side", p -> put(p, BlackWindRegistry.LINGXUZI.get(), 0.5, 0, 8.5, 90f), new double[]{0.5, 1.6, 5.5, 0.5, 1.3, 8.5}),
                scene("bw_lingxuzi_back", p -> put(p, BlackWindRegistry.LINGXUZI.get(), 0.5, 0, 8.5, 10f), new double[]{0.5, 1.7, 5.5, 0.5, 1.3, 8.5}),
                scene("bw_lingxuzi_face", p -> put(p, BlackWindRegistry.LINGXUZI.get(), 0.5, 0, 8.5, 200f), new double[]{0.8, 2.2, 6.8, 0.5, 2.1, 8.5}),
                scene("bw_lingxuzi_pounce", p -> {
                    LingxuziEntity l = put(p, BlackWindRegistry.LINGXUZI.get(), 0.5, 0, 8.5, 140f);
                    if (l != null) l.pose(LingxuziEntity.CROUCHING);
                }, new double[]{-1.8, 1.4, 5.5, 0.5, 0.9, 8.5}),
                scene("bw_lingxuzi_elixir", p -> {
                    LingxuziEntity l = put(p, BlackWindRegistry.LINGXUZI.get(), 0.5, 0, 8.5, 200f);
                    if (l != null) l.pose(LingxuziEntity.DRINKING);
                }, new double[]{1.1, 1.9, 6.0, 0.5, 1.7, 8.5}),
                // the White-Robed Scholar
                scene("bw_scholar", p -> put(p, BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(), 0.5, 0, 9.5, 190f), new double[]{0.9, 1.7, 6.3, 0.5, 1.4, 9.5}),
                scene("bw_scholar_side", p -> put(p, BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(), 0.5, 0, 9.5, 90f), new double[]{0.5, 1.6, 6.0, 0.5, 1.1, 9.5}),
                scene("bw_scholar_back", p -> put(p, BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(), 0.5, 0, 8.5, 20f), new double[]{0.5, 1.8, 4.5, 0.5, 0.9, 8.5}),
                scene("bw_scholar_face", p -> put(p, BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(), 0.5, 0, 8.5, 190f), new double[]{0.8, 2.3, 6.9, 0.5, 2.2, 8.5}),
                scene("bw_scholar_spit", p -> {
                    WhiteRobedScholarEntity s = put(p, BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(), 0.5, 0, 9.0, 160f);
                    if (s != null) s.pose(WhiteRobedScholarEntity.REARING);
                }, new double[]{-1.6, 1.9, 6.0, 0.5, 1.7, 9.0}),
                scene("bw_scholar_coil", p -> {
                    WhiteRobedScholarEntity s = put(p, BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(), 0.5, 0, 9.0, 200f);
                    if (s != null) s.pose(WhiteRobedScholarEntity.COILING);
                }, new double[]{2.6, 2.6, 5.6, 0.5, 0.8, 10.0}),
                // the three friends together
                scene("bw_three_friends", p -> {
                    put(p, BlackWindRegistry.LINGXUZI.get(), -3.0, 0, 9.5, 200f);
                    king(p, 0.5, 12.5, 180f);
                    put(p, BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(), 4.0, 0, 9.5, 160f);
                }, new double[]{0.5, 2.6, 3.5, 0.5, 2.0, 11.0}),
                // the cave and the temple, laid off the stage
                new AbilityScenes.Scene("bw_cave_door", p -> {
                    door(p);
                    AbilityScenes.sceneCam = new double[]{DOOR_X + 9.5, DOOR_UP + 9.0, DOOR_Z + BlackWindPlan.FACE_Z + 40.0, DOOR_X + 0.5, DOOR_UP + 12.0, DOOR_Z + BlackWindPlan.FACE_Z};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("bw_cave_door_close", p -> {
                    door(p);
                    AbilityScenes.sceneCam = new double[]{DOOR_X - 5.5, DOOR_UP + 4.0, DOOR_Z + BlackWindPlan.FACE_Z + 18.0, DOOR_X + 0.5, DOOR_UP + 9.0, DOOR_Z + BlackWindPlan.FACE_Z};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("bw_cave_keepers", p -> {
                    door(p);
                    AbilityScenes.sceneCam = new double[]{DOOR_X + 0.5, DOOR_UP + 3.5, DOOR_Z + 76.0, DOOR_X + 0.5, DOOR_UP + 2.0, DOOR_Z + 66.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("bw_cave_court", p -> {
                    court(p);
                    see(p);
                    AbilityScenes.sceneCam = new double[]{COURT_X + 0.5, COURT_UP + 14.0, COURT_Z + 24.0, COURT_X + 0.5, COURT_UP + 4.0, COURT_Z - 30.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("bw_cave_feast", p -> {
                    court(p);
                    see(p);
                    AbilityScenes.sceneCam = new double[]{COURT_X + 12.5, COURT_UP + 7.0, COURT_Z - 20.0, COURT_X + 0.5, COURT_UP + 3.0, COURT_Z - 42.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("bw_cave_throne", p -> {
                    throne(p);
                    see(p);
                    AbilityScenes.sceneCam = new double[]{THRONE_X + 0.5, THRONE_UP + 9.0, THRONE_Z - 60.0, THRONE_X + 0.5, THRONE_UP + 3.0, THRONE_Z - 100.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("bw_cave_king_on_his_floor", p -> {
                    throne(p);
                    see(p);
                    AbilityScenes.sceneCam = new double[]{THRONE_X + 5.5, THRONE_UP + 3.0, THRONE_Z - 72.0, THRONE_X + 0.5, THRONE_UP + 2.5, THRONE_Z - 82.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("bw_temple", p -> {
                    temple(p);
                    AbilityScenes.sceneCam = new double[]{TEMPLE_X + 18.5, 16.0, TEMPLE_Z + 46.0, TEMPLE_X + 0.5, 2.0, TEMPLE_Z - 4.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("bw_temple_hall", p -> {
                    temple(p);
                    AbilityScenes.sceneCam = new double[]{TEMPLE_X + 6.5, 5.0, TEMPLE_Z + 2.0, TEMPLE_X - 2.5, 3.0, TEMPLE_Z - 28.0};
                }, p -> {}, 1, false));
    }

    private static void see(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
    }

    /** The cave's great door and forecourt, its keepers before it: laid once. */
    private static void door(ServerPlayer p) {
        if (door) return;
        ServerLevel level = p.serverLevel();
        BlockPos o = new BlockPos(DOOR_X, ground(p) - 1 + DOOR_UP, DOOR_Z);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        // the forecourt's floor, the valley's walls of dark rock either side
        for (int x = -40; x <= 40; x++) for (int z = BlackWindPlan.FACE_Z + 1; z <= BlackWindPlan.Z_MAX + 6; z++) {
            int ax = Math.abs(x);
            level.setBlock(at.set(o.getX() + x, o.getY(), o.getZ() + z), (ax <= 3 ? Blocks.DEEPSLATE_TILES : (ax + z) % 5 == 0 ? Blocks.GRAVEL : Blocks.TUFF)
                    .defaultBlockState(), Block.UPDATE_CLIENTS);
            int wall = ax <= 24 ? 0 : (ax - 24) * 3;
            for (int y = 1; y <= Math.min(44, wall); y++) level.setBlock(at.set(o.getX() + x, o.getY() + y, o.getZ() + z), BlackWindBlocks.rock(o.getY() + y, o.getX() + x, o.getZ() + z), Block.UPDATE_CLIENTS);
        }
        BlackWindBlocks.layBox(level, o, -40, -2, 36, 40, BlackWindPlan.Y_MAX, BlackWindPlan.Z_MAX);
        for (Mob m : BlackWindCave.muster(level, o, 0)) {
            m.setNoAi(true);
            m.setYRot(0f);
            m.setYHeadRot(0f);
            m.setYBodyRot(0f);
        }
        door = true;
    }

    /** The Black Wind Court and its garrison: laid once. */
    private static void court(ServerPlayer p) {
        if (court) return;
        ServerLevel level = p.serverLevel();
        BlockPos o = new BlockPos(COURT_X, ground(p) - 1 + COURT_UP, COURT_Z);
        BlackWindBlocks.layBox(level, o, BlackWindPlan.X_MIN, -2, -50, BlackWindPlan.X_MAX, BlackWindPlan.Y_MAX, 30);
        // its back opened a little so the light gets in from the passage's end
        for (Mob m : BlackWindCave.muster(level, o, 1)) m.setNoAi(true);
        court = true;
    }

    /** The throne cavern, the king before his dais: laid once. */
    private static void throne(ServerPlayer p) {
        if (throne) return;
        ServerLevel level = p.serverLevel();
        BlockPos o = new BlockPos(THRONE_X, ground(p) - 1 + THRONE_UP - BlackWindPlan.THRONE_Y, THRONE_Z);
        BlackWindBlocks.layBox(level, o, -BlackWindPlan.THRONE_RX - 3, BlackWindPlan.THRONE_Y - 3, BlackWindPlan.Z_MIN,
                BlackWindPlan.THRONE_RX + 3, BlackWindPlan.THRONE_Y + BlackWindPlan.THRONE_H + 4, -52);
        put(p, BlackWindRegistry.BLACK_BEAR_DEMON.get(), THRONE_X + 0.5, THRONE_UP - 1, THRONE_Z + BlackWindPlan.THRONE_Z + 0.5, 0f);
        throne = true;
    }

    /** The burnt Guanyin Chan Temple on its terrace: laid once. */
    private static void temple(ServerPlayer p) {
        if (temple) return;
        BlackWindBlocks.layTemple(p.serverLevel(), new BlockPos(TEMPLE_X, ground(p) - 1, TEMPLE_Z));
        temple = true;
    }

    /** Clears what the last of these scenes put on the patch (the cave's pieces and the temple stay). */
    static void clear(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        if (patch) {
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int x = X0 - 3; x <= X1 + 3; x++) for (int z = Z0 - 1; z <= Z1 + 4; z++) {
                level.setBlock(at.set(x, g - 2, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(at.set(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 0; y <= 14; y++) level.setBlock(at.set(x, g + y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            patch = false;
        }
        level.getEntities((Entity) null, new AABB(X0 - 6, g - 5, Z0 - 8, X1 + 6, g + 14, Z1 + 6),
                e -> e instanceof BlackWindMonster || e instanceof BlackBearDemonEntity || e instanceof BlackWhirlwindEntity).forEach(Entity::discard);
    }
}
