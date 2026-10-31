package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.flowerfruit.FakePilgrimEntity;
import com.jttw.engine.entity.flowerfruit.FlowerFruitMonster;
import com.jttw.engine.entity.flowerfruit.GreatBaboonEntity;
import com.jttw.engine.entity.flowerfruit.MacaqueCloneEntity;
import com.jttw.engine.entity.flowerfruit.SixEaredMacaqueEntity;
import com.jttw.engine.entity.flowerfruit.SpearApeEntity;
import com.jttw.engine.entity.flowerfruit.StoneGibbonEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.FlowerFruitRegistry;
import com.jttw.engine.world.feature.FlowerFruitIslandFeature;
import com.jttw.engine.world.flowerfruit.CavePlan;
import com.jttw.engine.world.flowerfruit.IslandShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Flower-Fruit Mountain in the form gallery: the monkey army (each kind, its captain, their warnings:
 * the gibbon's stone raised, the ape's spear drawn back, the baboon beating its chest; kneeling to the
 * true Monkey King), the three fake pilgrims and their tricks, the Six-Eared Macaque (close, his
 * whirling staff, his crouch for the somersault, his hair clones, his Dharma image raising the staff)
 * on a patch of the mountain's grass and peach petals before a bank of its rock; and the falls and
 * the Water Curtain Cave laid a way off north of the stage, from the plan the island lays them by
 * ({@link IslandShape#falls}, {@link CavePlan}): the waterfall from the basin, the ledge behind the
 * falling water and the cave's mouth, the iron bridge, the stone gate and its tablet, the great hall
 * and its old throne, the fake pilgrims with the luggage, the dormitory, the store, and the false
 * king's sanctum with him in it. The island itself, peak, pillars and orchards: the zone bench's
 * {@code flowerfruit} mode, in a real world.
 */
final class FlowerFruitScenes {

    private FlowerFruitScenes() {}

    private static final int X0 = -8, X1 = 8, Z0 = 3, Z1 = 16;
    /** Where the falls and the cave are laid: the basin's floor this far up and north of the stage. */
    private static final int CAVE_Z = -150, CAVE_UP = 40;
    private static boolean patch, cave;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    private static void patch(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1 + 1; z++) {
            int roll = Math.floorMod(x * 31 + z * 17, 13);
            level.setBlock(at.set(x, g - 1, z), roll == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (z == Z1 + 1) {
                for (int y = 0; y < 5 + Math.floorMod(x * 7, 3); y++) {
                    level.setBlock(at.set(x, g + y, z), FlowerFruitIslandFeature.rock(y + 70, x, z), Block.UPDATE_CLIENTS);
                }
                level.setBlock(at.set(x, g + 5 + Math.floorMod(x * 7, 3), z), Blocks.MOSS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                continue;
            }
            if (roll == 3 && Math.abs(x) > 3) level.setBlock(at.set(x, g, z), Blocks.PINK_PETALS.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.PinkPetalsBlock.AMOUNT, 3), Block.UPDATE_CLIENTS);
            else if (roll == 7 && Math.abs(x) > 4) level.setBlock(at.set(x, g, z), Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        patch = true;
    }

    private static <T extends Mob> T put(ServerPlayer p, EntityType<T> type, double x, double y, double z, float yaw) {
        T m = type.create(p.serverLevel());
        if (m == null) return null;
        m.moveTo(x, y, z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(true);
        p.serverLevel().addFreshEntity(m);
        return m;
    }

    private static <T extends ZoneMonster> T monkey(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, boolean captain) {
        T m = put(p, type, x, ground(p), z, yaw);
        if (m != null) m.become(captain ? m.captainLevel() : m.regularLevel(), captain);
        return m;
    }

    private static FakePilgrimEntity pilgrim(ServerPlayer p, int variant, double x, double y, double z, float yaw) {
        FakePilgrimEntity f = put(p, FlowerFruitRegistry.FAKE_PILGRIM.get(), x, y, z, yaw);
        if (f != null) {
            f.setVariant(variant);
            f.becomeMiniboss(28 + variant, CavePlan.PILGRIM_HEALTH);
        }
        return f;
    }

    private static SixEaredMacaqueEntity macaque(ServerPlayer p, double x, double y, double z, float yaw) {
        return put(p, FlowerFruitRegistry.SIX_EARED_MACAQUE.get(), x, y, z, yaw);
    }

    private static AbilityScenes.Scene scene(String name, java.util.function.Consumer<ServerPlayer> cast, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    private static AbilityScenes.Scene cave(String name, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            lay(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    /** A camera in the laid cave: (x, y, z) from the basin's middle at its floor, looking at (lx, ly, lz). */
    private static double[] at(double x, double y, double z, double lx, double ly, double lz) {
        return new double[]{x + 0.5, CAVE_UP + y, CAVE_Z + z + 0.5, lx + 0.5, CAVE_UP + ly, CAVE_Z + lz + 0.5};
    }

    /** One creature, alone, seen from the front, the side, the back and close at its face (its height {@code tall}). */
    private static List<AbilityScenes.Scene> views(String name, double tall, java.util.function.BiConsumer<ServerPlayer, Float> put) {
        double d = 1.6 + tall * 1.25, eye = tall * 0.62, face = tall * 0.86;
        return List.of(
                scene("ff_" + name + "_front", p -> put.accept(p, 180f), new double[]{0.5, eye + 0.3, 9.5 - d, 0.5, eye, 9.5}),
                scene("ff_" + name + "_side", p -> put.accept(p, 90f), new double[]{0.5, eye + 0.3, 9.5 - d, 0.5, eye, 9.5}),
                scene("ff_" + name + "_back", p -> put.accept(p, 0f), new double[]{0.5, eye + 0.3, 9.5 - d, 0.5, eye, 9.5}),
                scene("ff_" + name + "_face", p -> put.accept(p, 160f), new double[]{0.9, face + 0.1, 9.5 - 0.9 - tall * 0.45, 0.5, face, 9.5}));
    }

    static List<AbilityScenes.Scene> all() {
        List<AbilityScenes.Scene> all = new java.util.ArrayList<>();
        all.addAll(views("gibbon", 1.25, (p, yaw) -> monkey(p, FlowerFruitRegistry.STONE_GIBBON.get(), 0.5, 9.5, yaw, false)));
        all.addAll(views("ape", 1.45, (p, yaw) -> monkey(p, FlowerFruitRegistry.SPEAR_APE.get(), 0.5, 9.5, yaw, false)));
        all.addAll(views("baboon", 1.7, (p, yaw) -> monkey(p, FlowerFruitRegistry.GREAT_BABOON.get(), 0.5, 9.5, yaw, false)));
        all.addAll(views("tang", 1.75, (p, yaw) -> pilgrim(p, FakePilgrimEntity.TANG, 0.5, ground(p), 9.5, yaw)));
        all.addAll(views("bajie", 1.65, (p, yaw) -> pilgrim(p, FakePilgrimEntity.BAJIE, 0.5, ground(p), 9.5, yaw)));
        all.addAll(views("sha", 1.85, (p, yaw) -> pilgrim(p, FakePilgrimEntity.SHA, 0.5, ground(p), 9.5, yaw)));
        all.addAll(views("macaque", 1.95, (p, yaw) -> macaque(p, 0.5, ground(p), 9.5, yaw)));
        all.addAll(List.of(
                scene("ff_monkeys", p -> {
                    monkey(p, FlowerFruitRegistry.STONE_GIBBON.get(), -2.5, 9.5, 200f, false);
                    monkey(p, FlowerFruitRegistry.SPEAR_APE.get(), 0.5, 10.0, 180f, false);
                    monkey(p, FlowerFruitRegistry.GREAT_BABOON.get(), 3.5, 10.5, 160f, false);
                }, new double[]{0.5, 1.7, 4.8, 0.5, 1.0, 10.0}),
                scene("ff_monkey_captains", p -> {
                    monkey(p, FlowerFruitRegistry.STONE_GIBBON.get(), -2.5, 9.5, 200f, true);
                    monkey(p, FlowerFruitRegistry.SPEAR_APE.get(), 0.5, 10.0, 180f, true);
                    monkey(p, FlowerFruitRegistry.GREAT_BABOON.get(), 3.5, 10.5, 160f, true);
                }, new double[]{0.5, 1.7, 4.8, 0.5, 1.0, 10.0}),
                scene("ff_monkey_warnings", p -> {
                    StoneGibbonEntity g = monkey(p, FlowerFruitRegistry.STONE_GIBBON.get(), -2.5, 9.5, 200f, false);
                    if (g != null) g.rearBack();
                    SpearApeEntity a = monkey(p, FlowerFruitRegistry.SPEAR_APE.get(), 0.5, 10.0, 150f, false);
                    if (a != null) a.drawBack();
                    GreatBaboonEntity b = monkey(p, FlowerFruitRegistry.GREAT_BABOON.get(), 3.5, 10.5, 160f, false);
                    if (b != null) b.rearUp();
                }, new double[]{0.5, 1.7, 4.8, 0.5, 1.0, 10.0}),
                scene("ff_monkeys_kneel", p -> {
                    for (int i = 0; i < 3; i++) {
                        FlowerFruitMonster m = monkey(p, List.of(FlowerFruitRegistry.STONE_GIBBON.get(), FlowerFruitRegistry.SPEAR_APE.get(),
                                FlowerFruitRegistry.GREAT_BABOON.get()).get(i), -2.5 + i * 3.0, 10.0, 180f, false);
                        if (m != null) m.kneel(p);
                    }
                }, new double[]{0.5, 1.7, 4.8, 0.5, 0.6, 10.0}),
                scene("ff_pilgrims", p -> {
                    for (int v = 0; v < 3; v++) pilgrim(p, v, 3.5 - v * 3.0, ground(p), 10.0, 180f);
                }, new double[]{0.5, 1.8, 4.6, 0.5, 1.3, 10.0}),
                scene("ff_pilgrim_tricks", p -> {
                    for (int v = 0; v < 3; v++) {
                        FakePilgrimEntity f = pilgrim(p, v, 3.5 - v * 3.0, ground(p), 10.0, 180f);
                        if (f != null) f.warn();
                    }
                }, new double[]{0.5, 1.8, 4.6, 0.5, 1.3, 10.0}),
                scene("ff_macaque_spin", p -> {
                    SixEaredMacaqueEntity m = macaque(p, 0.5, ground(p), 9.5, 180f);
                    if (m != null) m.startSpin();
                }, new double[]{0.5, 1.8, 4.6, 0.5, 1.6, 9.5}),
                scene("ff_macaque_crouch", p -> {
                    SixEaredMacaqueEntity m = macaque(p, 0.5, ground(p), 9.5, 150f);
                    if (m != null) m.startDash(p);
                }, new double[]{0.5, 1.5, 5.0, 0.5, 1.0, 9.5}),
                scene("ff_macaque_clones", p -> {
                    SixEaredMacaqueEntity m = macaque(p, 0.5, ground(p), 11.5, 180f);
                    if (m != null) {
                        m.startClones();
                        m.clones().forEach(c -> {
                            c.setNoAi(true);
                            c.setYRot(180f);
                            c.setYBodyRot(180f);
                            c.setYHeadRot(180f);
                        });
                    }
                }, new double[]{0.5, 2.2, 4.0, 0.5, 1.4, 11.5}),
                scene("ff_macaque_dharma", p -> {
                    SixEaredMacaqueEntity m = macaque(p, 0.5, ground(p), 11.5, 180f);
                    if (m != null) {
                        m.becomeGiant();
                        m.startSlam(p);
                    }
                }, new double[]{0.5, 2.8, 4.0, 0.5, 3.0, 11.5})));
        all.addAll(List.of(
                cave("ff_falls", at(0, 18, IslandShape.BASIN_END - 20, 0, 34, IslandShape.CLIFF_Z)),
                cave("ff_falls_low", at(-14, 4, IslandShape.POOL_Z + 18, 0, 10, IslandShape.CLIFF_Z)),
                cave("ff_behind_the_curtain", at(-14, 3, IslandShape.CLIFF_Z + 2, 0, 5, IslandShape.CLIFF_Z + 1)),
                cave("ff_cave_mouth", at(5, 3, IslandShape.CLIFF_Z + 2, -1, 4, CavePlan.MOUTH_Z)),
                cave("ff_iron_bridge", at(-12, 6, CavePlan.BRIDGE_Z1, 0, 0, (CavePlan.CHASM_Z0 + CavePlan.CHASM_Z1) / 2.0)),
                cave("ff_stone_gate", at(0, 6, CavePlan.BRIDGE_Z0 + 2, 0, 11, CavePlan.GATE_Z)),
                cave("ff_tablet", at(0, 12, CavePlan.GATE_Z + 6, 0, 13, CavePlan.GATE_Z)),
                cave("ff_great_hall", at(0, 12, CavePlan.COURT_Z0 - 2, 0, 4, -20)),
                cave("ff_old_throne", at(5, 4, -10, 0, 3, CavePlan.DAIS_Z0 + 2)),
                cave("ff_pilgrims_in_hall", at(-4, 4, -2, 0, 2, -14)),
                cave("ff_dormitory", at(-CavePlan.SIDE_X + 8, 5, CavePlan.SIDE_Z + 14, -CavePlan.SIDE_X, 1, CavePlan.SIDE_Z)),
                cave("ff_store", at(CavePlan.SIDE_X - 8, 5, CavePlan.SIDE_Z + 14, CavePlan.SIDE_X, 1, CavePlan.SIDE_Z)),
                cave("ff_sanctum_warning", at(CavePlan.TUNNEL_X - 3, 3, -5, CavePlan.TUNNEL_X + 4, 3, -12)),
                cave("ff_sanctum", at(CavePlan.SANCTUM_X + 12, 9, CavePlan.SANCTUM_Z + 18, CavePlan.SANCTUM_X, 3, CavePlan.SANCTUM_Z - 6)),
                cave("ff_sanctum_macaque", at(CavePlan.SANCTUM_X + 2, 3, CavePlan.SANCTUM_Z + 7, CavePlan.SANCTUM_X, 2, CavePlan.SANCTUM_Z))));
        return all;
    }

    /**
     * Lays the falls country and the Water Curtain Cave north of the stage, from the plan the island
     * lays them by (the land round them at the mountain's height, the cliff and its gorge, the basin,
     * the pool, the ledge, the river's lip and the falling sheet), and puts its people in it: once.
     */
    private static void lay(ServerPlayer p) {
        if (cave) return;
        ServerLevel level = p.serverLevel();
        int base = ground(p) + CAVE_UP - IslandShape.BASIN_Y; // world y of a plan's absolute y
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int g = ground(p);
        for (int lx = -80; lx <= 80; lx++) {
            for (int lz = CavePlan.Z_MIN - 6; lz <= IslandShape.BASIN_END - 20; lz++) {
                int x = lx, z = CAVE_Z + lz;
                int top = (int) Math.round(IslandShape.falls(lx, lz, IslandShape.BASIN_Y + 42));
                boolean pool = IslandShape.pool(lx, lz);
                if (pool) top = IslandShape.BASIN_Y - IslandShape.poolDepth(lx, lz);
                boolean river = IslandShape.river(lx, lz) && !IslandShape.overhang(lx, lz);
                for (int y = Math.max(g, base + IslandShape.BASIN_Y - 12); y <= base + top; y++) {
                    BlockState st = y == base + top ? (y - base >= IslandShape.BASIN_Y + 6 && !river ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : IslandShape.ledge(lx, lz) ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState())
                            : FlowerFruitIslandFeature.rock(y - base, x, z);
                    level.setBlock(at.set(x, y, z), st, Block.UPDATE_CLIENTS);
                }
                if (pool) for (int y = base + top + 1; y <= base + IslandShape.BASIN_Y; y++) level.setBlock(at.set(x, y, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                if (river) level.setBlock(at.set(x, base + IslandShape.LIP_Y - 1, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                if (IslandShape.overhang(lx, lz)) {
                    for (int y = IslandShape.LIP_Y - 4; y <= IslandShape.LIP_Y - 2; y++) level.setBlock(at.set(x, base + y, z), FlowerFruitIslandFeature.rock(y, x, z), Block.UPDATE_CLIENTS);
                    level.setBlock(at.set(x, base + IslandShape.LIP_Y - 1, z), IslandShape.river(lx, lz) ? Blocks.WATER.defaultBlockState()
                            : Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                if (IslandShape.curtain(lx, lz)) {
                    for (int y = IslandShape.BASIN_Y + 1; y <= IslandShape.LIP_Y - 1; y++) {
                        level.setBlock(at.set(x, base + y, z), Fluids.WATER.getFlowing(8, true).createLegacyBlock(), Block.UPDATE_CLIENTS);
                    }
                }
                if (CavePlan.covers(lx, lz)) {
                    for (int ly = CavePlan.Y_MIN; ly <= CavePlan.Y_MAX; ly++) {
                        BlockState st = CavePlan.at(lx, ly, lz);
                        if (st != null) level.setBlock(at.set(x, base + IslandShape.BASIN_Y + ly, z), st, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        int floor = base + IslandShape.BASIN_Y + 1;
        for (CavePlan.Post post : CavePlan.POSTS) {
            double x = post.x() + 0.5, z = CAVE_Z + post.z() + 0.5, y = floor + CavePlan.floor(post.x(), post.z());
            if (post.kind() == CavePlan.PILGRIM) {
                pilgrim(p, post.variant(), x, y, z, 0f);
                continue;
            }
            EntityType<? extends FlowerFruitMonster> type = switch (post.kind()) {
                case CavePlan.GIBBON -> FlowerFruitRegistry.STONE_GIBBON.get();
                case CavePlan.APE -> FlowerFruitRegistry.SPEAR_APE.get();
                default -> FlowerFruitRegistry.GREAT_BABOON.get();
            };
            FlowerFruitMonster m = put(p, type, x, y, z, (post.x() * 37 + post.z() * 11) % 360);
            if (m != null) m.become(post.level(), post.captain());
        }
        for (int[] l : CavePlan.LUGGAGE) {
            level.setBlock(at.set(l[0], floor, CAVE_Z + l[1]), Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (CavePlan.Sign sign : CavePlan.SIGNS) {
            BlockPos s = new BlockPos(sign.x(), base + IslandShape.BASIN_Y + sign.y(), CAVE_Z + sign.z());
            level.setBlock(s, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING, sign.facing()), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(s) instanceof net.minecraft.world.level.block.entity.SignBlockEntity be) {
                var text = new net.minecraft.world.level.block.entity.SignText();
                for (int i = 0; i < 4; i++) text = text.setMessage(i, net.minecraft.network.chat.Component.literal(sign.lines()[i]));
                be.setText(text.setColor(net.minecraft.world.item.DyeColor.ORANGE).setHasGlowingText(true), true);
            }
        }
        macaque(p, CavePlan.BOSS_X + 0.5, floor, CAVE_Z + CavePlan.BOSS_Z + 0.5, 0f);
        cave = true;
    }

    /** Clears the patch's creatures (the cave stays, its people with it). */
    static void clear(ServerPlayer p) {
        if (!patch) return;
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0 - 1; x <= X1 + 1; x++) for (int z = Z0 - 1; z <= Z1 + 2; z++) {
            level.setBlock(at.set(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 0; y <= 10; y++) level.setBlock(at.set(x, g + y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.getEntities((Entity) null, new AABB(X0 - 6, g - 5, Z0 - 6, X1 + 6, g + 14, Z1 + 6),
                e -> e instanceof FlowerFruitMonster || e instanceof SixEaredMacaqueEntity || e instanceof MacaqueCloneEntity).forEach(Entity::discard);
        patch = false;
    }
}
