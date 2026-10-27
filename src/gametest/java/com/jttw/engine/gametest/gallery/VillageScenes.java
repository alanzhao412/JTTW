package com.jttw.engine.gametest.gallery;

import com.jttw.engine.registry.ModVillages;
import com.jttw.engine.village.Calling;
import com.jttw.engine.village.SpiritMarkets;
import com.jttw.engine.village.TangFolkEntity;
import com.jttw.engine.village.TangVillage;
import com.jttw.engine.village.TangVillagePiece;
import com.jttw.engine.world.fengdu.FengduCity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The Tang villages in the form gallery: a village of each kind laid whole south of the stage (on
 * the gallery's flat ground, its folk put in place and still), seen from above, through its gate,
 * down its market street, at its shrine, in a courtyard, before the inn and the smithy, over its
 * paddies; every look of its folk, in rows and close; the ghost market in Fengdu's lower city (the
 * city laid as its own scenes lay it) and a merchant-immortal's stall in a pavilion of Heaven, laid
 * on a cloud. A village in a real world is the village bench ({@code -Pbench.mode=village}).
 */
final class VillageScenes {

    private VillageScenes() {}

    /** Where each village is laid: its crossroads, from the stage's origin; and the seed of its plan. */
    private static final int[][] AT = {{0, 150}, {170, 150}, {-170, 150}};
    private static final long[] SEED = {2L << 3, 1L << 3 | 1, 3L << 3};
    private static final TangVillage.Variant[] KIND = TangVillage.Variant.values();
    /** Where the folk stand for their portraits: a paved yard, from the stage's origin. */
    private static final int PX = 70, PZ = -70;
    /** The pavilion of Heaven: its floor's middle, from the stage's origin, so high over the ground. */
    private static final int HX = -60, HZ = -70, HY = 30;

    private static final boolean[] laid = new boolean[3];
    private static boolean yard, pavilion, market;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    /**
     * Sets the scene's camera ({@code cam}: as sceneCam) and takes the player up out of the picture
     * over it, flying, so the land and the folk round the camera are loaded and sent however far it
     * is from the stage.
     */
    private static void camera(ServerPlayer p, double[] cam) {
        AbilityScenes.sceneCam = cam;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(p.serverLevel(), cam[0], ground(p) + Math.max(cam[1], cam[4]) + 45, cam[2], 0f, 0f);
    }

    /** Lays village {@code k} whole, its folk still, once. */
    private static TangVillage.Plan village(ServerPlayer p, int k) {
        TangVillage.Plan plan = TangVillage.plan(KIND[k], SEED[k]);
        if (laid[k]) return plan;
        ServerLevel level = p.serverLevel();
        BlockPos centre = new BlockPos(AT[k][0], ground(p) - 1, AT[k][1]);
        BoundingBox all = new BoundingBox(centre.getX() + TangVillage.WEST - 2, level.getMinBuildHeight(), centre.getZ() + TangVillage.NORTH - 2,
                centre.getX() + TangVillage.EAST + 2, level.getMaxBuildHeight() - 1, centre.getZ() + TangVillage.SOUTH + 2);
        TangVillagePiece.lay(level, plan, centre, all, true);
        for (TangFolkEntity f : level.getEntitiesOfClass(TangFolkEntity.class, AABB.of(all))) f.setNoAi(true);
        laid[k] = true;
        return plan;
    }

    /** A scene of village {@code k}, the camera given from its crossroads (y over its ground). */
    private static AbilityScenes.Scene shot(String name, int k, double x, double y, double z, double tx, double ty, double tz) {
        return new AbilityScenes.Scene(name, p -> {
            village(p, k);
            camera(p, new double[]{AT[k][0] + x, y, AT[k][1] + z, AT[k][0] + tx, ty, AT[k][1] + tz});
        }, p -> {}, 1, false);
    }

    /** The portrait yard: smooth stone, a white wall behind. */
    private static void yard(ServerPlayer p) {
        if (yard) return;
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = -12; x <= 12; x++) for (int z = -6; z <= 12; z++) {
            level.setBlock(at.set(PX + x, g - 1, PZ + z), (Math.floorMod(x + z, 5) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS).defaultBlockState(),
                    Block.UPDATE_CLIENTS);
            if (z == 12) for (int y = 0; y < 5; y++) level.setBlock(at.set(PX + x, g + y, PZ + z), (y == 0 ? Blocks.STONE_BRICKS : Blocks.CALCITE)
                    .defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        yard = true;
    }

    private static final List<Entity> sitters = new ArrayList<>();

    /** Puts folk of these looks ({@code "farmer"}, {@code "farmer_woman"}...) in a row across the yard, facing the camera. */
    private static void sit(ServerPlayer p, String... looks) {
        yard(p);
        ServerLevel level = p.serverLevel();
        for (int i = 0; i < looks.length; i++) {
            String look = looks[i];
            boolean woman = look.endsWith("_woman");
            Calling c = Calling.byId(woman ? look.substring(0, look.length() - "_woman".length()) : look);
            TangFolkEntity f = (c.mortal() ? ModVillages.TANG_VILLAGER : ModVillages.SPIRIT_MERCHANT).get().create(level);
            if (f == null) continue;
            double x = PX + (i - (looks.length - 1) / 2.0) * 1.6 + 0.5;
            f.moveTo(x, ground(p), PZ + 6.5, 180f + (i - (looks.length - 1) / 2.0f) * 8f, 0f);
            f.setYHeadRot(f.getYRot());
            f.setYBodyRot(f.getYRot());
            f.setCalling(c, woman);
            f.setNoAi(true);
            f.setPersistenceRequired();
            level.addFreshEntity(f);
            sitters.add(f);
        }
    }

    private static AbilityScenes.Scene portrait(String name, double dist, double height, String... looks) {
        return new AbilityScenes.Scene(name, p -> {
            sit(p, looks);
            camera(p, new double[]{PX + 0.5, height, PZ + 6.5 - dist, PX + 0.5, 1.15, PZ + 6.5});
        }, p -> {}, 1, false);
    }

    /** The ghost market in Fengdu's lower city: the city laid as its scenes lay it, a shopkeeper at each stall. */
    private static void ghostMarket(ServerPlayer p) {
        FengduScenes.city(p);
        if (market) return;
        BlockPos g = FengduScenes.cityGround(p);
        for (int[] s : FengduCity.MARKET_STALLS) {
            TangFolkEntity f = SpiritMarkets.put(p.serverLevel(), Calling.GHOST_MERCHANT, g.offset(s[0], 1, s[1]), s[0] < 0 ? 270f : 90f);
            if (f != null) f.setNoAi(true);
        }
        market = true;
    }

    /** A pavilion of the Immortals on a bank of cloud, a merchant-immortal's stall in it. */
    private static void pavilion(ServerPlayer p) {
        if (pavilion) return;
        ServerLevel level = p.serverLevel();
        int y = ground(p) + HY;
        BlockPos at = new BlockPos(HX, y, HZ);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) {
            if (dx * dx + dz * dz > 85) continue;
            for (int dy = 1; dy <= 2 + (dx * dx + dz * dz < 30 ? 1 : 0); dy++) {
                level.setBlock(m.set(HX + dx, y - dy, HZ + dz), com.jttw.engine.registry.ModBlocks.CELESTIAL_CLOUD.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        com.jttw.engine.world.heaven.HeavenFeatures.IslandBuilding.pavilion(level, at, level.random);
        SpiritMarkets.celestialStall(level, at);
        for (TangFolkEntity f : level.getEntitiesOfClass(TangFolkEntity.class, new AABB(at).inflate(8))) f.setNoAi(true);
        pavilion = true;
    }

    static List<AbilityScenes.Scene> all() {
        List<AbilityScenes.Scene> all = new ArrayList<>();
        String[] names = {"plains", "mountain", "oasis"};
        for (int k = 0; k < 3; k++) {
            String n = "tang_village_" + names[k];
            // through the gate first: by the time the camera rises over it, the village is all loaded
            all.add(shot(n + "_approach", k, 4.5, 3.2, TangVillage.GATE_Z + 9, 0, 3, 0));
            all.add(shot(n + "_view", k, -38, 52, 78, 0, 0, 0));
            if (k == 0) {
                all.add(shot(n + "_market", k, 0.5, 2.6, 30, 0.5, 2.2, 4));
                all.add(shot(n + "_shrine", k, 2.5, 2.4, -17, 0, 2.5, -30));
                all.add(shot(n + "_square", k, 8, 4.5, 9, -2, 1, -3));
                all.add(shot(n + "_courtyard", k, -9, 15, -28, -16, 0, -10));
                all.add(shot(n + "_inn", k, 10, 2.8, 2, 14, 3.5, -12));
                all.add(shot(n + "_smithy", k, 14, 3, -1.5, 14, 1.5, 8));
                all.add(shot(n + "_fields", k, -26, 7, -2, -42, 0, 18));
            }
        }
        all.add(portrait("tang_folk_men", 7.5, 1.7, "farmer", "scholar", "merchant", "innkeeper", "smith"));
        all.add(portrait("tang_folk_women", 6.5, 1.7, "farmer_woman", "merchant_woman", "innkeeper_woman", "herbalist_woman"));
        all.add(portrait("tang_folk_learned", 5.5, 1.7, "monk", "herbalist", "fortune_teller"));
        all.add(portrait("tang_folk_spirits", 4.5, 1.7, "ghost_merchant", "celestial_merchant"));
        for (String look : new String[]{"farmer", "farmer_woman", "monk", "scholar", "merchant", "merchant_woman", "innkeeper", "innkeeper_woman",
                "herbalist", "herbalist_woman", "fortune_teller", "smith", "ghost_merchant", "celestial_merchant"}) {
            all.add(portrait("tang_folk_" + look, 2.6, 1.6, look));
        }
        all.add(new AbilityScenes.Scene("tang_ghost_market", p -> {
            ghostMarket(p);
            BlockPos g = FengduScenes.cityGround(p);
            int z = FengduCity.MARKET_STALLS.get(0)[1];
            camera(p, new double[]{g.getX() + 0.5, 3.0, g.getZ() + z + 7.5, g.getX() - 7.0, 1.6, g.getZ() + z});
        }, p -> {}, 1, false));
        all.add(new AbilityScenes.Scene("tang_ghost_market_close", p -> {
            ghostMarket(p);
            BlockPos g = FengduScenes.cityGround(p);
            int[] s = FengduCity.MARKET_STALLS.get(1);
            camera(p, new double[]{g.getX() + 1.5, 2.6, g.getZ() + s[1] + 1.5, g.getX() + s[0] + 0.5, 1.8, g.getZ() + s[1] + 0.5});
        }, p -> {}, 1, false));
        all.add(new AbilityScenes.Scene("tang_heaven_merchant", p -> {
            pavilion(p);
            camera(p, new double[]{HX + 3.5, HY + 2.6, HZ + 7.5, HX + 0.5, HY + 1.4, HZ - 1.5});
        }, p -> {}, 1, false));
        return all;
    }

    /** Clears the portrait row before the next shot (the villages, the market and the pavilion stay). */
    static void clear(ServerPlayer p) {
        sitters.forEach(Entity::discard);
        sitters.clear();
    }
}
