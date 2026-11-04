package com.jttw.engine.gametest.gallery;

import com.jttw.engine.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Heaven's host in the form gallery ({@code -Pgallery.only=heaven_}): a patch of Heaven's cloud and jade
 * paving before a bank of cloud, and on it each creature alone (front, side, back, face), its ranks and its
 * wind-ups. The scenes of each part of the host are kept in their own file: {@link HeavenHostScenes} (the
 * rank and file, the Lunar Mansions), {@link HeavenGodScenes} (the gods and star lords),
 * {@link HeavenGreatScenes} (the Heavenly Kings, Li Jing, Nezha, Erlang Shen); the stage and the helpers
 * are here.
 */
final class HeavenScenes {

    private HeavenScenes() {}

    static final int X0 = -10, X1 = 10, Z0 = 2, Z1 = 18;
    private static boolean stage;

    static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    /** Heaven's paving: celestial marble with jade bands over cloud, a bank of cloud behind. */
    static void stage(ServerPlayer p) {
        if (stage) return;
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1 + 2; z++) {
            boolean band = Math.floorMod(x, 6) == 0 || Math.floorMod(z, 6) == 0;
            level.setBlock(at.set(x, g - 1, z), (band ? ModBlocks.JADE_BLOCK.get() : ModBlocks.CELESTIAL_MARBLE.get()).defaultBlockState(), Block.UPDATE_CLIENTS);
            if (z >= Z1 + 1) {
                int h = 3 + Math.floorMod(x * 7 + z * 3, 3);
                for (int y = 0; y < h; y++) level.setBlock(at.set(x, g + y, z), ModBlocks.CELESTIAL_CLOUD.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        stage = true;
    }

    /** One of {@code type} put at (x, y, z), facing {@code yaw}, still. */
    static <T extends Mob> T put(ServerPlayer p, EntityType<T> type, double x, double y, double z, float yaw) {
        T m = type.create(p.serverLevel());
        if (m == null) return null;
        m.moveTo(x, y, z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(true);
        m.setPersistenceRequired();
        p.serverLevel().addFreshEntity(m);
        return m;
    }

    static AbilityScenes.Scene scene(String name, Consumer<ServerPlayer> cast, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            stage(p);
            p.serverLevel().setDayTime(6000);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    /** One creature, alone, seen from the front, the side, the back and close at its face (its height {@code tall}); named heaven_{name}_*. */
    static List<AbilityScenes.Scene> views(String name, double tall, BiConsumer<ServerPlayer, Float> put) {
        double d = 1.6 + tall * 1.25, eye = tall * 0.62, face = tall * 0.86;
        return List.of(
                scene("heaven_" + name + "_front", p -> put.accept(p, 180f), new double[]{0.5, eye + 0.3, 9.5 - d, 0.5, eye, 9.5}),
                scene("heaven_" + name + "_side", p -> put.accept(p, 90f), new double[]{0.5, eye + 0.3, 9.5 - d, 0.5, eye, 9.5}),
                scene("heaven_" + name + "_back", p -> put.accept(p, 0f), new double[]{0.5, eye + 0.3, 9.5 - d, 0.5, eye, 9.5}),
                scene("heaven_" + name + "_face", p -> put.accept(p, 180f), new double[]{0.5 - tall * 0.12, face, 9.5 - 0.75 - tall * 0.42,
                        0.5, face - 0.02, 9.5 - 0.1 * tall}));
    }

    /** Close at the face of one standing at (0.5, 9.5) facing the camera's line, its face {@code face} high: from
     *  the front, three-quarters and the side; named heaven_{name}_face, _face_quarter, _face_side. */
    static List<AbilityScenes.Scene> faces(String name, double face, Consumer<ServerPlayer> put) {
        double r = 1.0, tz = 9.3;
        return List.of(
                scene("heaven_" + name + "_face", put, new double[]{0.5, face + 0.05, tz - r, 0.5, face, tz}),
                scene("heaven_" + name + "_face_quarter", put, new double[]{0.5 + r * 0.7, face + 0.05, tz - r * 0.7, 0.5, face, tz}),
                scene("heaven_" + name + "_face_side", put, new double[]{0.5 + r, face + 0.05, tz, 0.5, face, tz}));
    }

    static List<AbilityScenes.Scene> all() {
        List<AbilityScenes.Scene> all = new java.util.ArrayList<>();
        all.addAll(HeavenHostScenes.all());
        all.addAll(HeavenGodScenes.all());
        all.addAll(HeavenGreatScenes.all());
        return all;
    }
}
