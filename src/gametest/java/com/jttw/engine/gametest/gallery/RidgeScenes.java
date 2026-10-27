package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.ridge.LadyWhiteBoneEntity;
import com.jttw.engine.entity.ridge.RidgeMonster;
import com.jttw.engine.world.feature.WhiteBoneCaveFeature;
import com.jttw.engine.world.ridge.CaveGarrison;
import com.jttw.engine.world.ridge.WhiteBoneCave;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The White Bone Cave in the form gallery: its inside laid whole just south of the stage, hung in
 * the air over the flat world (its rock a great block, its halls hollowed out of it), each floor
 * shot with its own garrison standing at their posts, and Lady White Bone on her throne room's
 * floor among her court, found out and herself. The mountain over it, its forecourt and its great
 * door are seen in a real world: the ridge bench ({@code -Pbench.mode=ridge}).
 */
final class RidgeScenes {

    private RidgeScenes() {}

    /** Where the cave is laid: its centre, from the stage's origin, and its forecourt floor over the gallery's ground. */
    private static final int CX = -30, CZ = 85, RISE = 200;

    private static boolean laid;

    private static WhiteBoneCave.Site site(ServerPlayer p) {
        return new WhiteBoneCave.Site(CX, p.serverLevel().getMinBuildHeight() + 4 + RISE, CZ);
    }

    private static WhiteBoneCave cave(ServerPlayer p) {
        return WhiteBoneCave.of(p.serverLevel().getSeed());
    }

    /** The cave's inside, laid once, its seals and the well's grates shut as the land makes them. */
    private static void lay(ServerPlayer p) {
        if (laid) return;
        ServerLevel level = p.serverLevel();
        WhiteBoneCave.Site site = site(p);
        int r = WhiteBoneCave.REACH + 1;
        for (int x0 = (CX - r) & ~15; x0 <= CX + r; x0 += 16) {
            for (int z0 = (CZ - r) & ~15; z0 <= CZ + r; z0 += 16) WhiteBoneCaveFeature.buildInside(level, cave(p), site, x0, z0);
        }
        laid = true;
    }

    /** Sends away whoever an earlier scene put in the cave. */
    private static void clear(ServerPlayer p) {
        WhiteBoneCave.Site site = site(p);
        AABB all = new AABB(site.at(-70, WhiteBoneCave.Y_MIN, -70)).minmax(new AABB(site.at(70, WhiteBoneCave.Y_MAX, 70)));
        p.serverLevel().getEntitiesOfClass(Entity.class, all, e -> e instanceof RidgeMonster || e instanceof LadyWhiteBoneEntity).forEach(Entity::discard);
    }

    /** Puts floor k's garrison at its posts (8: her court), standing still as if on watch. */
    private static void man(ServerPlayer p, int k) {
        ServerLevel level = p.serverLevel();
        WhiteBoneCave.Site site = site(p);
        List<WhiteBoneCave.Post> posts = cave(p).posts(k);
        for (int i = 0; i < posts.size(); i++) {
            BlockPos at = CaveGarrison.where(level, site, posts.get(i));
            RidgeMonster m = CaveGarrison.spawn(level, posts.get(i), i, at, level.random, MobSpawnType.COMMAND);
            if (m == null) continue;
            m.setNoAi(true);
            float yaw = level.random.nextFloat() * 360f;
            m.setYRot(yaw);
            m.setYHeadRot(yaw);
            m.setYBodyRot(yaw);
        }
    }

    /** A scene of floor k from {@code cam} (local to the cave: x, y, z it stands at, x, y, z it looks at). */
    private static AbilityScenes.Scene shot(String name, int k, double[] cam, boolean lady) {
        return new AbilityScenes.Scene(name, p -> {
            lay(p);
            clear(p);
            // as the bench sees it: the halls' own light is dim, for those who walk them
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20 * 60, 0, false, false));
            man(p, k);
            WhiteBoneCave.Site site = site(p);
            if (lady) {
                LadyWhiteBoneEntity her = CaveGarrison.placeLady(p.serverLevel(), site, p.serverLevel().random);
                if (her != null) {
                    her.reveal(null);
                    her.setNoAi(true);
                    her.setYRot(0f);
                    her.setYHeadRot(0f);
                    her.setYBodyRot(0f);
                }
            }
            int ground = p.serverLevel().getMinBuildHeight() + 4, y0 = site.y0() - ground;
            AbilityScenes.sceneCam = new double[]{CX + cam[0], y0 + cam[1], CZ + cam[2], CX + cam[3], y0 + cam[4], CZ + cam[5]};
        }, p -> {}, 1, false);
    }

    static List<AbilityScenes.Scene> all() {
        int t = WhiteBoneCave.THRONE_Y;
        return List.of(
                shot("ridge_cave_ossuary", 0, new double[]{-30.5, 9, 40.5, 10, 2, -20}, false),
                shot("ridge_cave_bone_forge", 2, new double[]{-36.5, WhiteBoneCave.floorY(2) + 8, 40.5, 0, WhiteBoneCave.floorY(2) + 1, 0}, false),
                shot("ridge_cave_borrowed_faces", 4, new double[]{-27.5, WhiteBoneCave.floorY(4) + 6, 27.5, 27, WhiteBoneCave.floorY(4) + 2, 27}, false),
                shot("ridge_cave_seal", 6, new double[]{-2.5, WhiteBoneCave.floorY(6) + 4, -30.5, -12, WhiteBoneCave.floorY(6) + 2, -46}, false),
                shot("ridge_cave_throne_lady", WhiteBoneCave.FLOORS, new double[]{4.5, t + 3.5, 3.5, 0, t + 2, -6}, true),
                shot("ridge_cave_throne_hall", WhiteBoneCave.FLOORS, new double[]{0.5, t + 14, 33.5, 0, t + 6, -34}, true));
    }
}
