package com.jttw.engine.world.flowerfruit;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.flowerfruit.FakePilgrimEntity;
import com.jttw.engine.entity.flowerfruit.FlowerFruitMonster;
import com.jttw.engine.registry.FlowerFruitRegistry;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Who holds Flower-Fruit Mountain, and their return: the Water Curtain Cave's garrison
 * ({@link CavePlan#POSTS}, the three fake pilgrims among them) and each camp's band
 * ({@link IslandShape#camps}) are put there as the land is made, each marked with the part it holds
 * (0 the cave, 1 + n the n-th camp). As Black Wind's and the Fire Cloud Cave's garrisons do, a part
 * whose holders are all dead stays empty for {@value #RESPAWN_MINUTES} minutes, then is mustered again
 * the next time someone comes near while no one is in it ({@link #look}). The Six-Eared Macaque is no
 * part of it: he is the lair's, once; fallen, his seat is a usurper's ({@link com.jttw.engine.world.Usurpers}).
 * (Every other zone's posts come back through {@link com.jttw.engine.world.ZonePosts}, on the same 20 minutes.)
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class FlowerFruitGarrison {

    private FlowerFruitGarrison() {}

    public static final int RESPAWN_MINUTES = com.jttw.engine.world.ZonePosts.RESPAWN_MINUTES; // every zone's posts alike
    public static final long RESPAWN = RESPAWN_MINUTES * 60 * 20L;
    public static final int EVERY = 20;
    /** The part of the cave's garrison, and the first camp's (the n-th camp's is {@code CAMP + n}). */
    public static final int CAVE = 0, CAMP = 1;
    /** How near a part someone must come for it to be mustered again; how near a camp counts as in it. */
    public static final int CAVE_NEAR = 80, CAMP_NEAR = 56, IN_CAMP = 24;
    /** How far a cave post's holder roams; a camp band's, a patrol's. */
    public static final int POST_REACH = 10, CAMP_REACH = 12, PATROL_REACH = 40;
    private static final String KEY = JTTWEngine.MOD_ID + ":ff_post";

    // ── The marks ───────────────────────────────────────────────────────

    /** Marks {@code e} as one of part {@code part} of the region centred at {@code c}. */
    public static void mark(Entity e, int[] c, int part) {
        CompoundTag t = new CompoundTag();
        t.putInt("Part", part);
        t.putInt("Cx", c[0]);
        t.putInt("Cz", c[1]);
        e.getPersistentData().put(KEY, t);
    }

    /** The part {@code e} holds, or -1 if it is no garrison's. */
    public static int partOf(Entity e) {
        CompoundTag t = e.getPersistentData().getCompound(KEY);
        return t.contains("Part") ? t.getInt("Part") : -1;
    }

    private static boolean of(Entity e, int[] c, int part) {
        CompoundTag t = e.getPersistentData().getCompound(KEY);
        return t.contains("Part") && t.getInt("Part") == part && t.getInt("Cx") == c[0] && t.getInt("Cz") == c[1];
    }

    // ── Where each part is ──────────────────────────────────────────────

    /** The number of parts: the cave and each camp. */
    public static int parts(long seed) {
        return CAMP + IslandShape.camps(seed).size();
    }

    /** A camp's middle (world; y the ground its band stands on). */
    public static BlockPos campMiddle(long seed, int[] c, int camp) {
        IslandShape.Camp k = IslandShape.camps(seed).get(camp);
        return new BlockPos(c[0] + k.x(), IslandShape.campBase(seed, k) + 1, c[1] + k.z());
    }

    /** Where a part's holders keep (with their reach about their posts). */
    public static AABB bounds(long seed, int[] c, int part) {
        if (part == CAVE) {
            return new AABB(c[0] + CavePlan.X_MIN - 4, IslandShape.BASIN_Y + CavePlan.Y_MIN - 4, c[1] + CavePlan.Z_MIN - 4,
                    c[0] + CavePlan.X_MAX + 5, IslandShape.BASIN_Y + CavePlan.Y_MAX + 5, c[1] + CavePlan.Z_MAX + 24);
        }
        BlockPos m = campMiddle(seed, c, part - CAMP);
        int r = (IslandShape.camps(seed).get(part - CAMP).patrol() ? PATROL_REACH : CAMP_REACH) + 8;
        return new AABB(m).inflate(r, 32, r);
    }

    /** Whether all of a part's ground has its creatures loaded and ticking, so a count of them is a true one. */
    public static boolean loaded(ServerLevel level, long seed, int[] c, int part) {
        AABB b = bounds(seed, c, part);
        if (part == CAVE) { // the posts' chunks, and round them their holders' reach
            for (CavePlan.Post post : CavePlan.POSTS) {
                for (int dx = -POST_REACH; dx <= POST_REACH; dx += POST_REACH) for (int dz = -POST_REACH; dz <= POST_REACH; dz += POST_REACH) {
                    if (!level.isPositionEntityTicking(new BlockPos(c[0] + post.x() + dx, IslandShape.BASIN_Y, c[1] + post.z() + dz))) return false;
                }
            }
            return true;
        }
        for (int x = (int) b.minX >> 4; x <= (int) b.maxX >> 4; x++) for (int z = (int) b.minZ >> 4; z <= (int) b.maxZ >> 4; z++) {
            if (!level.isPositionEntityTicking(new BlockPos(x * 16 + 8, 64, z * 16 + 8))) return false;
        }
        return true;
    }

    /** A part's holders still living. */
    public static List<Mob> members(ServerLevel level, long seed, int[] c, int part) {
        return level.getEntitiesOfClass(Mob.class, bounds(seed, c, part).inflate(16), m -> m.isAlive() && of(m, c, part));
    }

    // ── Putting them there ──────────────────────────────────────────────

    private static <T extends FlowerFruitMonster> T put(WorldGenLevel level, EntityType<T> type, int x, int y, int z, boolean captain, int lv,
                                                        BlockPos post, int reach, RandomSource random, int[] c, int part) {
        T m = type.create(level.getLevel());
        if (m == null) return null;
        BlockPos at = com.jttw.engine.entity.zone.ZoneMonster.roomNear(level, type, new BlockPos(x, y, z), 2); // clear of the stools, the pots, the beds
        m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.STRUCTURE, null);
        m.become(lv > 0 ? lv : captain ? m.rollCaptainLevel(random) : m.rollRegularLevel(random), captain); // 0: the road's, where it stands
        m.setPersistenceRequired();
        m.restrictTo(post, reach);
        mark(m, c, part);
        level.addFreshEntityWithPassengers(m);
        return m;
    }

    /** Puts the holder of one of the cave's posts there (the land being made, or the garrison mustered again). */
    public static Mob post(WorldGenLevel level, int[] c, CavePlan.Post post, RandomSource random) {
        int x = c[0] + post.x(), z = c[1] + post.z();
        BlockPos at = new BlockPos(x, IslandShape.BASIN_Y + 1 + CavePlan.floor(post.x(), post.z()), z);
        return switch (post.kind()) {
            case CavePlan.PILGRIM -> {
                FakePilgrimEntity f = FlowerFruitRegistry.FAKE_PILGRIM.get().create(level.getLevel());
                if (f == null) yield null;
                f.moveTo(x + 0.5, at.getY(), z + 0.5, 0f, 0f);
                f.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
                f.setVariant(post.variant());
                f.becomeMiniboss(post.level(), CavePlan.PILGRIM_HEALTH);
                f.setPersistenceRequired();
                f.restrictTo(at, FakePilgrimEntity.POST);
                mark(f, c, CAVE);
                level.addFreshEntityWithPassengers(f);
                yield f;
            }
            case CavePlan.GIBBON -> put(level, FlowerFruitRegistry.STONE_GIBBON.get(), x, at.getY(), z, post.captain(), post.level(), at, POST_REACH, random, c, CAVE);
            case CavePlan.APE -> put(level, FlowerFruitRegistry.SPEAR_APE.get(), x, at.getY(), z, post.captain(), post.level(), at, POST_REACH, random, c, CAVE);
            default -> put(level, FlowerFruitRegistry.GREAT_BABOON.get(), x, at.getY(), z, post.captain(), post.level(), at, POST_REACH, random, c, CAVE);
        };
    }

    /** Puts the {@code n}-th camp's band round its fire: a captain and three or four, of the three kinds; a patrol roams. */
    public static List<Mob> band(WorldGenLevel level, long seed, int[] c, int n, RandomSource random) {
        IslandShape.Camp camp = IslandShape.camps(seed).get(n);
        BlockPos mid = campMiddle(seed, c, n);
        int x = mid.getX(), y = mid.getY(), z = mid.getZ();
        long h = Mth.getSeed(x, 43, z);
        int count = 4 + (int) Math.floorMod(h >> 8, 2L);
        List<Mob> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            boolean captain = i == 0;
            int roll = (int) Math.floorMod(h >> (4 + i * 3), 7L);
            EntityType<? extends FlowerFruitMonster> type = roll < 3 ? FlowerFruitRegistry.STONE_GIBBON.get()
                    : roll < 6 ? FlowerFruitRegistry.SPEAR_APE.get() : FlowerFruitRegistry.GREAT_BABOON.get();
            double a = i * Math.PI * 2 / count + 0.4;
            Mob m = put(level, type, x + (int) Math.round(Math.cos(a) * 3.5), y, z + (int) Math.round(Math.sin(a) * 3.5), captain,
                    0, mid, camp.patrol() ? PATROL_REACH : CAMP_REACH, random, c, CAMP + n);
            if (m != null) out.add(m);
        }
        return out;
    }

    /** Musters a part again: the cave's whole garrison, or a camp's band. Returns them. */
    public static List<Mob> muster(ServerLevel level, long seed, int[] c, int part) {
        List<Mob> out = new ArrayList<>();
        if (part == CAVE) {
            for (CavePlan.Post post : CavePlan.POSTS) {
                Mob m = post(level, c, post, level.random);
                if (m != null) out.add(m);
            }
        } else {
            out.addAll(band(level, seed, c, part - CAMP, level.random));
        }
        State.of(level).cleared(c, part, 0);
        return out;
    }

    // ── Their return ────────────────────────────────────────────────────

    /** Whether {@code p} is in a part (the cave: under its roof; a camp: about its fire). */
    public static boolean in(long seed, int[] c, int part, ServerPlayer p) {
        if (part == CAVE) {
            int lx = (int) Math.floor(p.getX()) - c[0], lz = (int) Math.floor(p.getZ()) - c[1];
            return CavePlan.covers(lx, lz) && p.getY() < IslandShape.BASIN_Y + CavePlan.Y_MAX && p.getY() > IslandShape.BASIN_Y + CavePlan.Y_MIN - 2;
        }
        return p.position().distanceToSqr(campMiddle(seed, c, part - CAMP).getCenter()) < IN_CAMP * IN_CAMP;
    }

    /** Whether {@code p} is near enough a part for it to be mustered again. */
    public static boolean near(long seed, int[] c, int part, ServerPlayer p) {
        if (part == CAVE) {
            double dx = p.getX() - c[0], dz = p.getZ() - (c[1] + IslandShape.CLIFF_Z);
            return in(seed, c, part, p) || dx * dx + dz * dz < CAVE_NEAR * CAVE_NEAR;
        }
        return p.position().distanceToSqr(campMiddle(seed, c, part - CAMP).getCenter()) < CAMP_NEAR * CAMP_NEAR;
    }

    /**
     * One look at the island centred at {@code c}, the players near it: each part whose ground is
     * loaded and whose holders are all dead is marked cleared; one cleared {@link #RESPAWN} ago is
     * mustered again when someone is near it and no one in it. Returns the parts mustered.
     */
    public static List<Integer> look(ServerLevel level, long seed, int[] c, List<ServerPlayer> players) {
        State state = State.of(level);
        long now = level.getGameTime();
        List<Integer> mustered = new ArrayList<>();
        for (int part = 0; part < parts(seed); part++) {
            boolean wanted = false, on = false;
            for (ServerPlayer p : players) {
                if (p.isSpectator()) continue;
                wanted |= near(seed, c, part, p);
                on |= in(seed, c, part, p);
            }
            if (!wanted || !loaded(level, seed, c, part)) continue;
            if (!members(level, seed, c, part).isEmpty()) continue;
            long cleared = state.clearedAt(c, part);
            if (cleared == 0) {
                state.cleared(c, part, now);
                continue;
            }
            if (now - cleared >= RESPAWN && !on) {
                muster(level, seed, c, part);
                mustered.add(part);
            }
        }
        return mustered;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % EVERY != 0 || level.players().isEmpty()) return;
        long seed = level.getSeed();
        int[][] cs = Zones.FLOWER_FRUIT.centres(seed);
        if (cs.length == 0) return;
        int[] c = cs[0];
        int reach = IslandShape.COAST + IslandShape.COAST_WANDER + CAMP_NEAR;
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && Math.abs(p.getX() - c[0]) < reach && Math.abs(p.getZ() - c[1]) < reach) near.add(p);
        }
        if (!near.isEmpty()) look(level, seed, c, near);
    }

    // ── What it remembers ───────────────────────────────────────────────

    /** When each part of each island was last found cleared (0: held, or not yet found so). */
    public static final class State extends SavedData {
        private static final String NAME = JTTWEngine.MOD_ID + "_flower_fruit_garrison";
        private final CompoundTag islands;

        private State(CompoundTag islands) {
            this.islands = islands;
        }

        public static State of(ServerLevel level) {
            return level.getServer().overworld().getDataStorage()
                    .computeIfAbsent(new SavedData.Factory<>(() -> new State(new CompoundTag()), State::load, null), NAME);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            return new State(tag.copy());
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            for (String k : islands.getAllKeys()) tag.put(k, islands.get(k).copy());
            return tag;
        }

        private static String key(int[] c) {
            return c[0] + "," + c[1];
        }

        public long clearedAt(int[] c, int part) {
            return islands.getCompound(key(c)).getLong("Cleared" + part);
        }

        /** Marks part {@code part} cleared at {@code when} (0: held again). */
        public void cleared(int[] c, int part, long when) {
            CompoundTag t = islands.getCompound(key(c));
            t.putLong("Cleared" + part, when);
            islands.put(key(c), t);
            setDirty();
        }

        /** Forgets everything of the island centred at {@code c} (the tests' islands). */
        public void forget(int[] c) {
            islands.remove(key(c));
            setDirty();
        }
    }
}
