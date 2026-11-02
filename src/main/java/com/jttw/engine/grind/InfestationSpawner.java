package com.jttw.engine.grind;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wild.WildYaoguai;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.world.ZonePosts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the infested wilds crawling ({@link Infestation}). Every {@value #EVERY} ticks, round each
 * player standing in a patch:
 * <ul>
 *   <li><b>packs</b>: while fewer than {@value #CAP} of the patch's creatures are within
 *       {@value #COUNT_REACH} blocks of them, one time in two a pack of {@value #PACK_LEAST}-{@value #PACK_MOST}
 *       comes, {@value #NEAREST}-{@value #FARTHEST} blocks off, on open ground in the patch, by day or
 *       night: regulars of the patch's band, one pack in {@value #CAPTAIN_ONE_IN} led by a captain.
 *       They come and go as monsters do (they despawn when no one is near);</li>
 *   <li><b>its heart</b>: within {@value #HEART_NEAR} blocks of its middle, if none of its heart's
 *       holders lives and {@value ZonePosts#RESPAWN_MINUTES} minutes have passed since the last of
 *       them fell (or none ever did), its heart is held again: its minibosses (one or two, the theme's
 *       heaviest kind, one over the band's top) and two regulars, kept there
 *       ({@value #HEART_REACH} blocks about it), never despawning.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class InfestationSpawner {

    private InfestationSpawner() {}

    public static final int EVERY = 40, CAP = 9, COUNT_REACH = 48, PACK_LEAST = 2, PACK_MOST = 4, NEAREST = 22, FARTHEST = 40;
    public static final int CAPTAIN_ONE_IN = 4, HEART_NEAR = 72, HEART_REACH = 16, HEART_GUARD = 2;
    /** On each of a patch's creatures: its patch ({@code Patch}), and whether it holds the heart ({@code Heart}). */
    public static final String KEY = JTTWEngine.MOD_ID + ":infest";

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % EVERY != 0 || level.players().isEmpty()) return;
        Infestation.Ground ground = Infestation.Ground.of(level);
        for (ServerPlayer p : List.copyOf(level.players())) {
            if (p.isSpectator() || p.isCreative()) continue;
            Infestation.Patch patch = Infestation.at(ground, p.getX(), p.getZ());
            if (patch == null) continue;
            look(level, patch, p, level.random);
        }
    }

    /** One look round {@code p}, in {@code patch}: its heart held if it is due, a pack come if there is room. Returns those put there. */
    public static List<WildYaoguai> look(ServerLevel level, Infestation.Patch patch, ServerPlayer p, RandomSource r) {
        List<WildYaoguai> out = new ArrayList<>(heart(level, patch, false));
        int near = level.getEntitiesOfClass(WildYaoguai.class, p.getBoundingBox().inflate(COUNT_REACH), m -> m.isAlive() && patchOf(m) == patch.key()).size();
        if (near < CAP && r.nextInt(2) == 0) out.addAll(pack(level, patch, p, r));
        return out;
    }

    /** The patch {@code e} is of (its key), or {@link Long#MIN_VALUE}. */
    public static long patchOf(LivingEntity e) {
        CompoundTag t = e.getPersistentData().getCompound(KEY);
        return t.contains("Patch") ? t.getLong("Patch") : Long.MIN_VALUE;
    }

    public static boolean holdsHeart(LivingEntity e) {
        return e.getPersistentData().getCompound(KEY).getBoolean("Heart");
    }

    /** Where a patch's heart is: its middle, on the ground. */
    public static BlockPos heartOf(ServerLevel level, Infestation.Patch patch) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, patch.x(), patch.z());
        return new BlockPos(patch.x(), y, patch.z());
    }

    /**
     * Holds the patch's heart if it is due (or, {@code force}, whenever none holds it): a player within
     * {@value #HEART_NEAR}, none within {@value #HEART_REACH} and {@value ZonePosts#NEAR} of it (once it
     * has fallen), its ground awake, none of its holders alive, its time come. Returns those put there.
     */
    public static List<WildYaoguai> heart(ServerLevel level, Infestation.Patch patch, boolean force) {
        BlockPos at = heartOf(level, patch);
        boolean near = level.getNearestPlayer(at.getX(), at.getY(), at.getZ(), HEART_NEAR, false) != null;
        boolean atIt = level.getNearestPlayer(at.getX(), at.getY(), at.getZ(), HEART_REACH + ZonePosts.NEAR, false) != null;
        return heart(level, patch, near || force, atIt && !force, force);
    }

    /** As {@link #heart(ServerLevel, Infestation.Patch, boolean)}, told whether someone is {@code near} it and whether someone is {@code atIt}. */
    public static List<WildYaoguai> heart(ServerLevel level, Infestation.Patch patch, boolean near, boolean atIt, boolean force) {
        BlockPos at = heartOf(level, patch);
        if (!level.isPositionEntityTicking(at) || !near) return List.of();
        if (!level.getEntitiesOfClass(WildYaoguai.class, new AABB(at).inflate(HEART_REACH + ZonePosts.STRAY),
                m -> m.isAlive() && patchOf(m) == patch.key() && holdsHeart(m)).isEmpty()) return List.of();
        State state = State.of(level);
        long fell = state.heartFell(patch.key());
        if (!force && fell != 0 && (level.getGameTime() - fell < ZonePosts.RESPAWN || atIt)) return List.of();
        List<WildYaoguai> out = new ArrayList<>();
        RandomSource r = level.random;
        RingLevels.Band band = patch.band();
        for (int i = 0; i < patch.at().minibosses(); i++) {
            WildYaoguai m = put(level, patch, patch.theme().heart(), at.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2), band.miniboss(),
                    WildYaoguai.Rank.MINIBOSS, true);
            if (m != null) out.add(m);
        }
        for (int i = 0; i < HEART_GUARD; i++) {
            var kinds = patch.theme().packs();
            WildYaoguai m = put(level, patch, kinds.get(r.nextInt(kinds.size())), at.offset(r.nextInt(7) - 3, 0, r.nextInt(7) - 3),
                    band.lo() + r.nextInt(band.hi() - band.lo() + 1), WildYaoguai.Rank.REGULAR, true);
            if (m != null) out.add(m);
        }
        state.held(patch.key());
        return out;
    }

    /** A pack round {@code p}: on open ground of the patch, out of their sight's reach. Returns it (empty if no ground was found). */
    public static List<WildYaoguai> pack(ServerLevel level, Infestation.Patch patch, ServerPlayer p, RandomSource r) {
        BlockPos at = null;
        for (int tries = 0; tries < 8 && at == null; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = NEAREST + r.nextDouble() * (FARTHEST - NEAREST);
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            if (!patch.contains(x, z)) continue;
            BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!level.isPositionEntityTicking(ground) || !level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.below()).isEmpty()) continue;
            if (level.getNearestPlayer(x, ground.getY(), z, NEAREST - 4, false) != null) continue;
            at = ground;
        }
        if (at == null) return List.of();
        List<WildYaoguai> out = new ArrayList<>();
        var kinds = patch.theme().packs();
        RingLevels.Band band = patch.band();
        int n = PACK_LEAST + r.nextInt(PACK_MOST - PACK_LEAST + 1);
        boolean captain = r.nextInt(CAPTAIN_ONE_IN) == 0;
        for (int i = 0; i < n; i++) {
            int lv = band.lo() + r.nextInt(band.hi() - band.lo() + 1);
            boolean leads = captain && i == 0;
            WildYaoguai m = put(level, patch, kinds.get(r.nextInt(kinds.size())), at.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2),
                    leads ? band.captainOver(lv, r.nextInt(3)) : lv, leads ? WildYaoguai.Rank.CAPTAIN : WildYaoguai.Rank.REGULAR, false);
            if (m != null) out.add(m);
        }
        return out;
    }

    /** One of a patch's creatures, of {@code kind}, level and rank, at {@code at} (on the ground there); kept at the heart if {@code heart}. */
    @Nullable
    static WildYaoguai put(ServerLevel level, Infestation.Patch patch, EntityType<? extends WildYaoguai> kind, BlockPos at, int lv,
                           WildYaoguai.Rank rank, boolean heart) {
        WildYaoguai m = kind.create(level);
        if (m == null) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        BlockPos stand = new BlockPos(at.getX(), y, at.getZ());
        m.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), MobSpawnType.EVENT, null);
        m.become(lv, rank);
        GrindXp.mark(m, patch.band(), rank == WildYaoguai.Rank.REGULAR ? EnemyTier.GRUNT : EnemyTier.GENERAL);
        CompoundTag t = new CompoundTag();
        t.putLong("Patch", patch.key());
        t.putBoolean("Heart", heart);
        m.getPersistentData().put(KEY, t);
        if (heart) {
            m.setPersistenceRequired();
            m.restrictTo(heartOf(level, patch), HEART_REACH);
        }
        level.addFreshEntityWithPassengers(m);
        return m;
    }

    /** The last of a heart's holders falls: written down, its heart held again {@value ZonePosts#RESPAWN_MINUTES} minutes after. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || !holdsHeart(e)) return;
        long key = patchOf(e);
        boolean last = level.getEntitiesOfClass(WildYaoguai.class, e.getBoundingBox().inflate(HEART_REACH * 2 + ZonePosts.STRAY),
                m -> m != e && m.isAlive() && patchOf(m) == key && holdsHeart(m)).isEmpty();
        if (last) State.of(level).fell(key, level.getGameTime());
    }

    // ── What it remembers ───────────────────────────────────────────────

    /** When each patch's heart last fell (0: held, or never fallen). */
    public static final class State extends SavedData {
        private static final String NAME = JTTWEngine.MOD_ID + "_infestations";
        private final Map<Long, Long> fell = new HashMap<>();

        public static State of(ServerLevel level) {
            return level.getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::load, null), NAME);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State s = new State();
            for (String k : tag.getAllKeys()) s.fell.put(Long.parseLong(k), tag.getLong(k));
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            fell.forEach((k, v) -> tag.putLong(Long.toString(k), v));
            return tag;
        }

        public long heartFell(long patch) {
            return fell.getOrDefault(patch, 0L);
        }

        public void fell(long patch, long when) {
            fell.put(patch, when);
            setDirty();
        }

        public void held(long patch) {
            if (fell.remove(patch) != null) setDirty();
        }
    }
}
