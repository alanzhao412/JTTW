package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.lion.AzureLionEntity;
import com.jttw.engine.entity.lion.DemonKingEntity;
import com.jttw.engine.entity.lion.LionCamelMonster;
import com.jttw.engine.entity.lion.LittleWindDrillerEntity;
import com.jttw.engine.entity.lion.PengDemonEntity;
import com.jttw.engine.entity.lion.WhiteElephantEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.feature.LionCamelCityFeature;
import com.jttw.engine.world.lion.LionCamelCity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Lion-Camel Ridge in the form gallery: its creatures and the two demon kings on a patch of the
 * ridge's grim ground laid in front of the stage (coarse earth and gravel strewn with bones, a wall
 * of dark crag behind with skulls on stakes), seen from inside it; the Little Wind-Driller raising
 * the alarm; the Azure Lion's jaws gaping before a victim, and after; his roar; the White Elephant's
 * trunk holding a foe aloft; a Peng player walking among the demons, who bow to him; and Lion-Camel
 * City laid whole a way off, seen from over its main gate, inside the throne hall, in the elephant
 * court and down a street. The patch is cleared before the next shot ({@link #clear}); the city,
 * off the stage behind the usual cameras, is laid once and left, its shots among the gallery's last.
 * The ridge itself is seen in a real world: the zone bench's lion mode.
 */
final class LionScenes {

    private LionScenes() {}

    /** The patch: x -9..9, z 3..17; the crag behind it at z 18. */
    private static final int X0 = -9, X1 = 9, Z0 = 3, Z1 = 17;
    /** Where the city is laid: its centre, from the stage's origin. */
    private static final int CITY_X = 40, CITY_Z = -100;

    private static boolean patch, city;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    private static void patch(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1 + 1; z++) {
            int r = Math.floorMod(x * 31 + z * 17, 20);
            BlockState top = r < 8 ? Blocks.COARSE_DIRT.defaultBlockState() : r < 12 ? Blocks.GRAVEL.defaultBlockState()
                    : r < 15 ? Blocks.PODZOL.defaultBlockState() : r < 17 ? Blocks.STONE.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
            level.setBlock(at.set(x, g - 1, z), top, Block.UPDATE_CLIENTS);
            if (z == Z1 + 1) {
                // the crag behind, dark and banded
                int high = 7 + (int) Math.round(2.5 * Math.sin(x * 0.45) + 1.5 * Math.sin(x * 1.3 + 1) + Math.floorMod(x * 7, 2));
                for (int y = 0; y < high; y++) {
                    BlockState rock = switch (Math.floorMod(y + (x >> 2), 6)) {
                        case 0 -> Blocks.TUFF.defaultBlockState();
                        case 2 -> Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                        case 4 -> Blocks.ANDESITE.defaultBlockState();
                        default -> Blocks.STONE.defaultBlockState();
                    };
                    level.setBlock(at.set(x, g + y, z), rock, Block.UPDATE_CLIENTS);
                }
                continue;
            }
            int roll = Math.floorMod(x * 13 + z * 29, 61);
            if (roll == 0 && Math.abs(x) > 3 && z > Z0 + 4) {
                level.setBlock(at.set(x, g, z), Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), Block.UPDATE_CLIENTS);
            } else if (roll == 1 && Math.abs(x) > 2) {
                level.setBlock(at.set(x, g, z), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, Math.floorMod(x * 5 + z, 16)), Block.UPDATE_CLIENTS);
            }
        }
        // skulls on stakes before the crag
        for (int x : new int[]{-7, -2, 4, 8}) {
            level.setBlock(at.set(x, g, Z1), Blocks.DARK_OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(at.set(x, g + 1, Z1), Blocks.DARK_OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(at.set(x, g + 2, Z1), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 0), Block.UPDATE_CLIENTS);
        }
        patch = true;
    }

    private static <T extends Mob> T put(ServerPlayer p, EntityType<T> type, double x, double y, double z, float yaw, boolean ai) {
        T m = type.create(p.serverLevel());
        if (m == null) return null;
        m.moveTo(x, ground(p) + y, z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(!ai);
        p.serverLevel().addFreshEntity(m);
        return m;
    }

    private static <T extends ZoneMonster> T creature(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, boolean captain) {
        T m = put(p, type, x, 0, z, yaw, false);
        if (m != null) m.become(captain ? m.captainLevel() : m.regularLevel(), captain);
        return m;
    }

    private static LivingEntity victim(ServerPlayer p, double x, double z, float yaw) {
        LivingEntity g = put(p, EntityType.VILLAGER, x, 0, z, yaw, false);
        if (g != null) {
            g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);
            g.setHealth(g.getMaxHealth());
        }
        return g;
    }

    private static AbilityScenes.Scene scene(String name, java.util.function.Consumer<ServerPlayer> cast, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    private static <T extends Entity> T nearest(ServerPlayer p, Class<T> type) {
        return p.serverLevel().getEntitiesOfClass(type, new AABB(p.blockPosition()).inflate(40)).stream().findFirst().orElse(null);
    }

    static List<AbilityScenes.Scene> all() {
        return List.of(
                scene("lion_lion_demons", p -> {
                    creature(p, ModEntities.LION_DEMON.get(), -2.0, 9.5, 200f, false);
                    creature(p, ModEntities.LION_DEMON.get(), 1.5, 10.5, 160f, true);
                    creature(p, ModEntities.LION_DEMON.get(), 4.5, 12.5, 190f, false);
                }, new double[]{0.5, 2.6, 3.5, 0.5, 1.4, 10.5}),
                scene("lion_lion_demon_close", p -> creature(p, ModEntities.LION_DEMON.get(), 0.5, 8.0, 180f, false),
                        new double[]{1.8, 2.0, 4.0, 0.5, 1.4, 8.0}),
                scene("lion_elephant_demons", p -> {
                    creature(p, ModEntities.ELEPHANT_DEMON.get(), -2.5, 10.5, 200f, false);
                    creature(p, ModEntities.ELEPHANT_DEMON.get(), 2.5, 11.5, 160f, true);
                }, new double[]{0.5, 3.2, 3.0, 0.5, 2.0, 11.0}),
                scene("lion_peng_demons", p -> {
                    creature(p, ModEntities.PENG_DEMON.get(), -2.0, 9.5, 200f, false);
                    PengDemonEntity aloft = creature(p, ModEntities.PENG_DEMON.get(), 2.0, 10.5, 160f, false);
                    if (aloft != null) {
                        aloft.moveTo(aloft.getX(), aloft.getY() + 2.5, aloft.getZ(), 160f, 0f);
                        aloft.spreadWings(true);
                    }
                }, new double[]{0.5, 3.0, 3.0, 0.5, 2.0, 10.0}),
                scene("lion_wind_driller", p -> creature(p, ModEntities.LITTLE_WIND_DRILLER.get(), 0.5, 7.0, 180f, false),
                        new double[]{1.4, 1.5, 4.0, 0.5, 0.9, 7.0}),
                new AbilityScenes.Scene("lion_wind_driller_alarm", p -> {
                    patch(p);
                    creature(p, ModEntities.LITTLE_WIND_DRILLER.get(), 0.5, 11.5, 180f, false);
                    victim(p, -2.5, 9.0, 270f);
                    AbilityScenes.sceneCam = new double[]{0.5, 5.5, 1.5, 0.5, 1.0, 11.0};
                }, p -> {
                    LittleWindDrillerEntity d = nearest(p, LittleWindDrillerEntity.class);
                    LivingEntity v = nearest(p, net.minecraft.world.entity.npc.Villager.class);
                    if (d != null && v != null) d.raiseAlarm(v);
                }, 12, false),
                scene("lion_azure_lion", p -> put(p, ModEntities.AZURE_LION.get(), 0.5, 0, 10.5, 180f, false),
                        new double[]{2.5, 3.0, 3.5, 0.5, 2.6, 10.5}),
                scene("lion_azure_lion_side", p -> put(p, ModEntities.AZURE_LION.get(), 0.5, 0, 10.5, 90f, false),
                        new double[]{0.5, 3.0, 3.0, 0.5, 2.4, 10.5}),
                new AbilityScenes.Scene("lion_swallow_gape", p -> {
                    patch(p);
                    put(p, ModEntities.AZURE_LION.get(), -2.0, 0, 11.0, 270f, false);
                    victim(p, 0.6, 11.0, 90f);
                    AbilityScenes.sceneCam = new double[]{0.0, 3.2, 4.0, -0.5, 2.4, 11.0};
                }, p -> {
                    AzureLionEntity l = nearest(p, AzureLionEntity.class);
                    if (l != null) l.gape();
                }, 4, false),
                new AbilityScenes.Scene("lion_swallowed", p -> {
                    patch(p);
                    put(p, ModEntities.AZURE_LION.get(), 0.5, 0, 10.5, 200f, false);
                    victim(p, 0.5, 7.5, 0f);
                    AbilityScenes.sceneCam = new double[]{3.0, 3.0, 4.0, 0.5, 2.2, 10.5};
                }, p -> {
                    AzureLionEntity l = nearest(p, AzureLionEntity.class);
                    LivingEntity v = nearest(p, net.minecraft.world.entity.npc.Villager.class);
                    if (l != null && v != null) l.swallow(v);
                }, 10, false),
                new AbilityScenes.Scene("lion_roar", p -> {
                    patch(p);
                    put(p, ModEntities.AZURE_LION.get(), -3.5, 0, 11.0, 270f, false);
                    victim(p, 3.5, 11.0, 90f);
                    AbilityScenes.sceneCam = new double[]{0.5, 3.5, 3.0, 0.0, 2.2, 11.0};
                }, p -> {
                    AzureLionEntity l = nearest(p, AzureLionEntity.class);
                    LivingEntity v = nearest(p, net.minecraft.world.entity.npc.Villager.class);
                    if (l != null && v != null) {
                        l.throwBackHead();
                        l.roar(v);
                    }
                }, 3, false),
                scene("lion_white_elephant", p -> put(p, ModEntities.WHITE_ELEPHANT.get(), 0.5, 0, 11.0, 180f, false),
                        new double[]{2.5, 3.0, 3.5, 0.5, 2.7, 11.0}),
                scene("lion_white_elephant_side", p -> put(p, ModEntities.WHITE_ELEPHANT.get(), 0.5, 0, 11.0, 90f, false),
                        new double[]{0.5, 3.0, 3.0, 0.5, 2.5, 11.0}),
                new AbilityScenes.Scene("lion_trunk_grab", p -> {
                    patch(p);
                    put(p, ModEntities.WHITE_ELEPHANT.get(), -2.5, 0, 11.0, 270f, false);
                    victim(p, 0.0, 11.0, 90f);
                    AbilityScenes.sceneCam = new double[]{1.0, 3.2, 2.5, -0.5, 3.0, 11.0};
                }, p -> {
                    WhiteElephantEntity e = nearest(p, WhiteElephantEntity.class);
                    LivingEntity v = nearest(p, net.minecraft.world.entity.npc.Villager.class);
                    if (e != null && v != null) e.grab(v);
                }, 10, false),
                new AbilityScenes.Scene("lion_peng_among_demons", p -> {
                    patch(p);
                    AlignmentData.markReincarnated(p, ReincarnationOutcome.GOLD_WINGED_PENG);
                    LevelManager.initClass(p, ReincarnationOutcome.GOLD_WINGED_PENG);
                    p.setGameMode(GameType.SURVIVAL);
                    p.teleportTo(0.5, ground(p), 8.5);
                    // his brothers' host round him, awake: none raises a hand to him
                    float[][] ring = {{-2.5f, 7.0f, 270f}, {3.0f, 7.5f, 90f}, {-1.5f, 11.0f, 200f}, {2.0f, 11.0f, 160f}, {0.5f, 12.5f, 180f}};
                    EntityType<?>[] kinds = {ModEntities.LION_DEMON.get(), ModEntities.ELEPHANT_DEMON.get(), ModEntities.PENG_DEMON.get(),
                            ModEntities.LION_DEMON.get(), ModEntities.LITTLE_WIND_DRILLER.get()};
                    for (int i = 0; i < ring.length; i++) {
                        @SuppressWarnings("unchecked")
                        Mob m = put(p, (EntityType<? extends Mob>) kinds[i], ring[i][0], 0, ring[i][1], ring[i][2], true);
                        if (m instanceof ZoneMonster z) z.become(z.regularLevel(), false);
                    }
                    AbilityScenes.facing = 0f;
                    AbilityScenes.sceneCam = new double[]{5.5, 3.6, 3.0, 0.5, 1.0, 9.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("lion_city_gate", p -> {
                    city(p);
                    AbilityScenes.sceneCam = new double[]{CITY_X + 22.5, 20.0, CITY_Z + LionCamelCity.OUTER + 30.0, CITY_X + 0.5, 6.0, CITY_Z + LionCamelCity.WALL_OUT};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("lion_city_above", p -> {
                    city(p);
                    AbilityScenes.sceneCam = new double[]{CITY_X + 0.5, 70.0, CITY_Z + LionCamelCity.OUTER + 50.0, CITY_X + 0.5, 0.0, CITY_Z - 10.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("lion_city_hall", p -> {
                    city(p);
                    int h = LionCamelCity.TERRACE_H;
                    AbilityScenes.sceneCam = new double[]{CITY_X + 0.5, h + 4.0, CITY_Z + 8.0, CITY_X + 0.5, h + 2.0, CITY_Z + LionCamelCity.THRONE_Z};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("lion_city_court", p -> {
                    city(p);
                    AbilityScenes.sceneCam = new double[]{CITY_X + 9.0, 8.0, CITY_Z + LionCamelCity.COURT_S - 1.0, CITY_X + 0.5, 2.0, CITY_Z + LionCamelCity.COURT_Z};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("lion_city_street", p -> {
                    city(p);
                    AbilityScenes.sceneCam = new double[]{CITY_X + 25.5, 3.2, CITY_Z + 50.5, CITY_X + 25.5, 1.5, CITY_Z + 8.0};
                }, p -> {}, 1, false));
    }

    /** Lion-Camel City, its ground at the stage's ground level, its court and garrison at their posts (still, for the pictures). */
    private static void city(ServerPlayer p) {
        if (city) return; // laid once
        ServerLevel level = p.serverLevel();
        BlockPos ground = new BlockPos(CITY_X, ground(p) - 1, CITY_Z);
        // the apron of trodden earth round it
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int reach = LionCamelCity.OUTER + 8;
        for (int x = -reach; x <= reach; x++) for (int z = -reach; z <= reach; z++) {
            int r = Math.floorMod(x * 31 + z * 17, 10);
            level.setBlock(at.set(CITY_X + x, ground.getY(), CITY_Z + z), (r < 5 ? Blocks.COARSE_DIRT : r < 8 ? Blocks.GRAVEL : Blocks.PACKED_MUD).defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        LionCamelCity.layWhole(level, ground);
        put(p, ModEntities.AZURE_LION.get(), CITY_X + 0.5, LionCamelCity.TERRACE_H, CITY_Z + 0.5, 0f, false);
        for (var post : LionCamelCityFeature.POSTS) {
            Mob m = put(p, post.who().get(), CITY_X + post.x() + 0.5, post.y() - 1, CITY_Z + post.z() + 0.5, 0f, false);
            if (m instanceof ZoneMonster z) z.become(post.captain() ? z.captainLevel() : z.regularLevel(), post.captain());
        }
        city = true;
    }

    /** Clears what the last of these scenes built on the patch (the city stays), and makes the player himself again. */
    static void clear(ServerPlayer p) {
        if (!patch) return;
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0 - 1; x <= X1 + 1; x++) for (int z = Z0 - 1; z <= Z1 + 2; z++) {
            level.setBlock(at.set(x, g - 2, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(at.set(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 0; y <= 12; y++) level.setBlock(at.set(x, g + y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.getEntities((Entity) null, new AABB(X0 - 12, g - 5, Z0 - 12, X1 + 12, g + 14, Z1 + 12),
                e -> e instanceof LionCamelMonster || e instanceof DemonKingEntity || e instanceof net.minecraft.world.entity.npc.Villager)
                .forEach(e -> {
                    e.ejectPassengers();
                    e.discard();
                });
        patch = false;
    }
}
