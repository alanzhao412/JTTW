package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.wild.RaidChiefEntity;
import com.jttw.engine.entity.wild.WildYaoguai;
import com.jttw.engine.grind.Infestation;
import com.jttw.engine.grind.InfestationMarks;
import com.jttw.engine.grind.RingLevels;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModGrinds;
import com.jttw.engine.village.TangVillage;
import com.jttw.engine.village.TangVillagePiece;
import com.jttw.engine.village.raid.RaidedVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The open world's grinding grounds in the form gallery: the Yaksha King, a raided village's chief
 * (alone, close, his face, with an ogre for scale, heaving his club up for the stomp over its ring of
 * fire); a raided village laid whole east of the Tang villages (from above, through its gate, on its
 * square, before the shrine where the chief holds court); and the marks at an infested wild's edge.
 * In a real world: {@code ./gradlew runWorldgenBench -Pbench.mode=raided}.
 */
final class RaidScenes {

    private RaidScenes() {}

    /** Where the raided village is laid: its crossroads, from the stage's origin. */
    private static final int VX = 340, VZ = 150;
    private static final long LAYOUT = 5L << 3;
    /** The chief's stage: east of the wild patch. */
    private static final int CX = 30, CZ = 8;

    private static boolean village;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    private static void camera(ServerPlayer p, double[] cam) {
        AbilityScenes.sceneCam = cam;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(p.serverLevel(), cam[0], ground(p) + Math.max(cam[1], cam[4]) + 45, cam[2], 0f, 0f);
    }

    private static void village(ServerPlayer p) {
        if (village) return;
        ServerLevel level = p.serverLevel();
        BlockPos centre = new BlockPos(VX, ground(p) - 1, VZ);
        BoundingBox all = new BoundingBox(centre.getX() + TangVillage.WEST - 2, level.getMinBuildHeight(), centre.getZ() + TangVillage.NORTH - 2,
                centre.getX() + TangVillage.EAST + 2, level.getMaxBuildHeight() - 1, centre.getZ() + TangVillage.SOUTH + 2);
        RaidedVillage.lay(level, TangVillage.plan(TangVillage.Variant.PLAINS, LAYOUT), centre, all, LAYOUT, new RingLevels.Band(31, 37), true);
        for (Mob m : level.getEntitiesOfClass(Mob.class, AABB.of(all))) m.setNoAi(true);
        village = true;
    }

    private static AbilityScenes.Scene shot(String name, double x, double y, double z, double tx, double ty, double tz) {
        return new AbilityScenes.Scene(name, p -> {
            village(p);
            camera(p, new double[]{VX + x, y, VZ + z, VX + tx, ty, VZ + tz});
        }, p -> {}, 1, false);
    }

    private static <T extends WildYaoguai> T put(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, int level) {
        T m = type.create(p.serverLevel());
        if (m == null) return null;
        m.moveTo(x, ground(p), z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(true);
        m.setPersistenceRequired();
        m.become(level);
        p.serverLevel().addFreshEntity(m);
        return m;
    }

    private static AbilityScenes.Scene chief(String name, java.util.function.Consumer<ServerPlayer> cast, double x, double y, double z, double tx, double ty, double tz) {
        return new AbilityScenes.Scene(name, p -> {
            cast.accept(p);
            AbilityScenes.sceneCam = new double[]{CX + x, y, CZ + z, CX + tx, ty, CZ + tz};
        }, p -> {}, 1, false);
    }

    static List<AbilityScenes.Scene> all() {
        return List.of(
                chief("raid_chief", p -> put(p, ModGrinds.RAID_CHIEF.get(), CX + 0.5, CZ + 0.5, 170f, 43), 0.0, 3.4, -7.5, 0.5, 2.3, 0.5),
                chief("raid_chief_face", p -> put(p, ModGrinds.RAID_CHIEF.get(), CX + 0.5, CZ + 0.5, 178f, 43), 0.3, 3.7, -3.4, 0.5, 3.3, 0.5),
                chief("raid_chief_side", p -> put(p, ModGrinds.RAID_CHIEF.get(), CX + 0.5, CZ + 0.5, 90f, 43), -6.5, 3.0, 0.5, 0.5, 2.3, 0.5),
                chief("raid_chief_scale", p -> {
                    put(p, ModGrinds.RAID_CHIEF.get(), CX + 0.5, CZ + 0.5, 180f, 43);
                    put(p, ModEntities.OGRE.get(), CX - 3.5, CZ - 0.5, 200f, 30);
                    put(p, ModEntities.WOLFMAN.get(), CX + 4.0, CZ - 1.0, 160f, 30);
                }, 0.5, 3.6, -10.5, 0.5, 2.2, 0.0),
                chief("raid_chief_stomp", p -> {
                    RaidChiefEntity c = put(p, ModGrinds.RAID_CHIEF.get(), CX + 0.5, CZ + 0.5, 170f, 43);
                    if (c != null) c.showSlam();
                    // the ring of fire his stomp will strike within
                    ServerLevel level = p.serverLevel();
                    for (int i = 0; i < 48; i++) {
                        double a = 2 * Math.PI * i / 48;
                        BlockPos at = BlockPos.containing(CX + 0.5 + RaidChiefEntity.STOMP_REACH * Math.cos(a), ground(p), CZ + 0.5 + RaidChiefEntity.STOMP_REACH * Math.sin(a));
                        if (level.getBlockState(at).isAir()) level.setBlock(at, Blocks.FIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }, -3.5, 4.5, -9.5, 0.5, 2.0, 0.5),
                shot("raid_village_above", -30, 42, 62, 0, 0, 0),
                shot("raid_village_gate", 3, 4.5, 50, 0, 3, 20),
                shot("raid_village_square", 0.5, 9, 14, 0, 1, -4),
                shot("raid_village_shrine", 2.5, 4, -11, 0, 2.5, -27),
                shot("raid_village_street", -2, 7, 24, 2, 1, -10),
                new AbilityScenes.Scene("infested_edge", p -> {
                    ServerLevel level = p.serverLevel();
                    int g = ground(p);
                    BlockPos at = new BlockPos(CX - 10, g, CZ + 18);
                    RaidedVillage.totem(level, at, 8);
                    RaidedVillage.totem(level, at.offset(6, 0, 2), 8);
                    BlockPos sign = at.offset(2, 0, 0);
                    level.setBlock(sign, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8), Block.UPDATE_CLIENTS);
                    Infestation.Patch patch = new Infestation.Patch(new Infestation.Candidate(0, 0, 0, 0, 120, 1), Infestation.Theme.FOREST, new RingLevels.Band(44, 50));
                    TangVillagePiece.writeSign(level, sign, DyeColor.RED, InfestationMarks.signLines(patch));
                    level.setBlock(at.offset(4, -1, 3), Blocks.BONE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                    put(p, ModEntities.WOLFMAN.get(), at.getX() + 2.5, at.getZ() + 7.5, 170f, 46);
                    put(p, ModEntities.WOLFMAN.get(), at.getX() + 5.0, at.getZ() + 9.0, 190f, 47).become(48, WildYaoguai.Rank.CAPTAIN);
                    AbilityScenes.sceneCam = new double[]{at.getX() + 2.5, 2.4, at.getZ() - 6.5, at.getX() + 2.5, 1.6, at.getZ() + 4};
                }, p -> {}, 1, false)
        );
    }

    static void clear(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        level.getEntities((Entity) null, new AABB(CX - 20, g - 3, CZ - 12, CX + 20, g + 12, CZ + 34),
                e -> e instanceof WildYaoguai).forEach(Entity::discard);
        for (BlockPos q : BlockPos.betweenClosed(CX - 8, g, CZ - 8, CX + 8, g, CZ + 8)) {
            if (level.getBlockState(q).is(Blocks.FIRE)) level.setBlock(q, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos q : BlockPos.betweenClosed(CX - 12, g - 1, CZ + 16, CX + 2, g + 6, CZ + 24)) {
            var s = level.getBlockState(q);
            if (s.is(Blocks.SKELETON_SKULL) || s.is(Blocks.NETHER_BRICK_FENCE) || s.is(Blocks.DARK_OAK_SIGN) || s.is(Blocks.BONE_BLOCK) || s.is(Blocks.SOUL_LANTERN)) {
                level.setBlock(q, q.getY() < g ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }
}
