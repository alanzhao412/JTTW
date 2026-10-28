package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.wild.GoblinEntity;
import com.jttw.engine.entity.wild.KoboldEntity;
import com.jttw.engine.entity.wild.OgreEntity;
import com.jttw.engine.entity.wild.WildYaoguai;
import com.jttw.engine.entity.wild.WolfmanEntity;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/**
 * The open world's yaoguai in the form gallery, on a patch of wild ground (grass, coarse dirt,
 * podzol and moss) before the stage: for each of the goblin, the kobold, the wolfman and the ogre,
 * a group, a close look at one, one side-on, and one in action (a goblin cowering as another stabs,
 * a kobold swinging its pick, a wolfman howling, an ogre heaving its club up to slam).
 */
final class WildScenes {

    private WildScenes() {}

    /** The patch: x -13..13, z 1..16. */
    private static final int X0 = -13, X1 = 13, Z0 = 1, Z1 = 16;

    private static boolean patch;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    private static void patch(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            int h = Math.floorMod(x * 73856093 ^ z * 19349663, 100);
            BlockState s = h < 60 ? Blocks.GRASS_BLOCK.defaultBlockState() : h < 75 ? Blocks.COARSE_DIRT.defaultBlockState()
                    : h < 90 ? Blocks.PODZOL.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState();
            level.setBlock(at.set(x, g - 1, z), s, Block.UPDATE_CLIENTS);
        }
        patch = true;
    }

    private static <T extends WildYaoguai> T put(ServerPlayer p, EntityType<T> type, double x, double z, float yaw) {
        T m = type.create(p.serverLevel());
        if (m == null) return null;
        m.moveTo(x, ground(p), z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(true);
        m.setPersistenceRequired();
        m.become(m.minLevel() + 1);
        p.serverLevel().addFreshEntity(m);
        return m;
    }

    private static AbilityScenes.Scene scene(String name, Consumer<ServerPlayer> cast, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    /** A scene whose creatures swing their weapons ({@code swingBefore} ticks before the picture: mid-blow at 3). */
    private static AbilityScenes.Scene swinging(String name, Consumer<ServerPlayer> cast, double[] cam, Class<? extends WildYaoguai> who) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> p.serverLevel().getEntitiesOfClass(who, new AABB(p.blockPosition()).inflate(20)).forEach(m -> m.swing(InteractionHand.MAIN_HAND, true)),
                3, false);
    }

    /** Cameras: {camera x, height over the ground, z, then what it looks at}. */
    private static double[] cam(double x, double y, double z, double tx, double ty, double tz) {
        return new double[]{x, y, z, tx, ty, tz};
    }

    static List<AbilityScenes.Scene> all() {
        return List.of(
                // ── goblins: a gang of five, one close, one side-on, one cowering as another stabs
                scene("wild_goblins", p -> {
                    put(p, ModEntities.GOBLIN.get(), -2.2, 7.6, 205f);
                    put(p, ModEntities.GOBLIN.get(), -0.4, 9.2, 185f);
                    put(p, ModEntities.GOBLIN.get(), 1.4, 7.2, 165f);
                    put(p, ModEntities.GOBLIN.get(), 3.0, 9.0, 150f);
                    put(p, ModEntities.GOBLIN.get(), 0.6, 6.0, 175f);
                }, cam(0.5, 2.4, 1.2, 0.5, 0.7, 7.8)),
                scene("wild_goblin_close", p -> put(p, ModEntities.GOBLIN.get(), 0.5, 5.5, 165f), cam(0.1, 1.2, 3.85, 0.5, 0.8, 5.5)),
                scene("wild_goblin_face", p -> put(p, ModEntities.GOBLIN.get(), 0.5, 5.5, 175f), cam(0.4, 1.15, 4.4, 0.5, 1.05, 5.5)),
                scene("wild_goblin_side", p -> put(p, ModEntities.GOBLIN.get(), 0.5, 5.5, 90f), cam(0.5, 1.1, 3.4, 0.5, 0.7, 5.5)),
                scene("wild_goblin_back", p -> put(p, ModEntities.GOBLIN.get(), 0.5, 5.5, 20f), cam(0.5, 1.3, 3.5, 0.5, 0.7, 5.5)),
                swinging("wild_goblin_action", p -> {
                    GoblinEntity a = put(p, ModEntities.GOBLIN.get(), -0.6, 6.0, 150f);
                    GoblinEntity b = put(p, ModEntities.GOBLIN.get(), 1.8, 6.6, 215f);
                    if (b != null) b.showFleeing(true);
                }, cam(0.5, 1.5, 2.6, 0.5, 0.7, 6.2), GoblinEntity.class),

                // ── kobolds: a swarm of six, one close, one side-on, a pair swinging their picks
                scene("wild_kobolds", p -> {
                    put(p, ModEntities.KOBOLD.get(), -2.6, 7.4, 210f);
                    put(p, ModEntities.KOBOLD.get(), -1.0, 9.0, 190f);
                    put(p, ModEntities.KOBOLD.get(), 0.8, 7.0, 175f);
                    put(p, ModEntities.KOBOLD.get(), 2.4, 8.6, 160f);
                    put(p, ModEntities.KOBOLD.get(), 3.6, 6.8, 145f);
                    put(p, ModEntities.KOBOLD.get(), 0.4, 5.6, 180f);
                }, cam(0.5, 2.3, 1.2, 0.5, 0.6, 7.6)),
                scene("wild_kobold_close", p -> put(p, ModEntities.KOBOLD.get(), 0.5, 5.0, 165f), cam(0.3, 1.2, 3.4, 0.5, 0.65, 5.0)),
                scene("wild_kobold_side", p -> put(p, ModEntities.KOBOLD.get(), 0.5, 5.0, 90f), cam(0.5, 1.1, 2.9, 0.5, 0.6, 5.0)),
                scene("wild_kobold_back", p -> put(p, ModEntities.KOBOLD.get(), 0.5, 5.0, 20f), cam(0.5, 1.3, 3.0, 0.5, 0.6, 5.0)),
                swinging("wild_kobold_action", p -> {
                    put(p, ModEntities.KOBOLD.get(), -0.5, 5.4, 140f);
                    put(p, ModEntities.KOBOLD.get(), 1.6, 5.8, 215f);
                }, cam(0.5, 1.4, 2.6, 0.5, 0.7, 5.6), KoboldEntity.class),

                // ── wolfmen: a pack of four, one close, one side-on, one howling while another strikes
                scene("wild_wolfmen", p -> {
                    put(p, ModEntities.WOLFMAN.get(), -3.0, 9.0, 200f);
                    put(p, ModEntities.WOLFMAN.get(), -0.6, 10.6, 185f);
                    put(p, ModEntities.WOLFMAN.get(), 1.8, 8.4, 170f);
                    put(p, ModEntities.WOLFMAN.get(), 4.2, 10.0, 155f);
                }, cam(0.5, 3.0, 0.6, 0.5, 1.6, 9.4)),
                scene("wild_wolfman_close", p -> put(p, ModEntities.WOLFMAN.get(), 0.5, 7.0, 165f), cam(-0.2, 2.2, 3.4, 0.5, 1.55, 7.0)),
                scene("wild_wolfman_face", p -> put(p, ModEntities.WOLFMAN.get(), 0.5, 7.0, 175f), cam(0.1, 2.45, 4.9, 0.5, 2.25, 7.0)),
                scene("wild_wolfman_side", p -> put(p, ModEntities.WOLFMAN.get(), 0.5, 7.0, 90f), cam(0.5, 2.0, 2.8, 0.5, 1.5, 7.0)),
                scene("wild_wolfman_back", p -> put(p, ModEntities.WOLFMAN.get(), 0.5, 7.0, 20f), cam(0.5, 2.2, 3.0, 0.5, 1.5, 7.0)),
                swinging("wild_wolfman_action", p -> {
                    WolfmanEntity howler = put(p, ModEntities.WOLFMAN.get(), -1.4, 8.4, 160f);
                    if (howler != null) howler.showHowling();
                    put(p, ModEntities.WOLFMAN.get(), 2.2, 7.4, 200f);
                }, cam(0.5, 2.4, 1.6, 0.5, 1.6, 7.8), WolfmanEntity.class),

                // ── ogres: one with a wolfman pack for scale, one close, one side-on, one heaving its club up to slam
                scene("wild_ogre", p -> {
                    put(p, ModEntities.OGRE.get(), 0.5, 10.0, 180f);
                    put(p, ModEntities.WOLFMAN.get(), -3.2, 8.6, 200f);
                    put(p, ModEntities.GOBLIN.get(), 3.4, 8.0, 160f);
                    put(p, ModEntities.KOBOLD.get(), 2.0, 7.0, 175f);
                }, cam(0.5, 3.2, 0.4, 0.5, 2.0, 10.0)),
                scene("wild_ogre_close", p -> put(p, ModEntities.OGRE.get(), 0.5, 8.0, 165f), cam(-0.6, 2.8, 2.6, 0.5, 2.1, 8.0)),
                scene("wild_ogre_face", p -> put(p, ModEntities.OGRE.get(), 0.5, 8.0, 175f), cam(0.2, 3.0, 4.8, 0.5, 2.75, 8.0)),
                scene("wild_ogre_side", p -> put(p, ModEntities.OGRE.get(), 0.5, 8.0, 90f), cam(0.5, 2.4, 1.6, 0.5, 2.0, 8.0)),
                scene("wild_ogre_back", p -> put(p, ModEntities.OGRE.get(), 0.5, 8.0, 20f), cam(0.5, 2.6, 1.8, 0.5, 2.0, 8.0)),
                scene("wild_ogre_action", p -> {
                    OgreEntity o = put(p, ModEntities.OGRE.get(), 0.5, 8.5, 170f);
                    if (o != null) o.showSlam();
                }, cam(-2.6, 1.8, 3.8, 0.5, 3.1, 8.5)),
                // a wolfman alone, howling, three-quarters on
                scene("wild_wolfman_howl", p -> {
                    WolfmanEntity w = put(p, ModEntities.WOLFMAN.get(), 0.5, 7.0, 150f);
                    if (w != null) w.showHowling();
                }, cam(-1.6, 2.0, 3.6, 0.5, 1.9, 7.0)),
                // a goblin cowering, arms over its head
                scene("wild_goblin_cower", p -> {
                    GoblinEntity g = put(p, ModEntities.GOBLIN.get(), 0.5, 5.5, 160f);
                    if (g != null) g.showFleeing(true);
                }, cam(-0.4, 1.2, 3.7, 0.5, 0.75, 5.5))
        );
    }

    static void clear(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        if (patch) {
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) level.setBlock(at.set(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            patch = false;
        }
        level.getEntities((Entity) null, new AABB(X0 - 4, g - 3, Z0 - 4, X1 + 4, g + 8, Z1 + 4),
                e -> e instanceof WildYaoguai).forEach(Entity::discard);
    }
}
