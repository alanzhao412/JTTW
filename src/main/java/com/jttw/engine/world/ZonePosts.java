package com.jttw.engine.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.zone.ZoneBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who holds the zones' posts, and their return: every regular, captain and miniboss the land puts at
 * a post as it is made (a camp's band, an outpost's, a household's group, a court's door guards, a
 * palace's hall guard, a floor's warden...) is {@link #keep kept} here, and once a post's holders are
 * all dead it stays empty for {@value #RESPAWN_MINUTES} minutes; then, the next time its ground is
 * awake and no one stands in it or near it, it is held again, by the same as held it before: each
 * comes back as it was first put there, its kind, rank and level, its name, its post and its reach
 * (it carries its own likeness from the day it was put there, {@link #KEY_BIRTH}). As
 * Flower-Fruit's garrison comes back ({@link com.jttw.engine.world.flowerfruit.FlowerFruitGarrison}),
 * and Black Wind's and the Fire Cloud Cave's: those three muster theirs from their plans, and keep
 * their own count; every other zone's posts come back through here.
 *
 * <p>A post is where its holders keep: the middle they are held to ({@code restrictTo}), and how far
 * they wander from it. Those put round one middle (a band round its fire) are one post: it is not
 * empty until the last of them falls, and they come back together. The dead are written down as they
 * fall ({@link #onDeath}); whether a post is empty is seen by looking for its living about it (as
 * Flower-Fruit's garrison is counted), so one that falls by some other way than dying is simply gone.
 * A zone's boss is no post's: when one falls, another takes its seat ({@link Usurpers}).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class ZonePosts {

    private ZonePosts() {}

    /** How long a cleared post stays empty: every zone's, Flower-Fruit's, Black Wind's and the Fire Cloud Cave's alike. */
    public static final int RESPAWN_MINUTES = 20;
    public static final long RESPAWN = RESPAWN_MINUTES * 60 * 20L;
    /** How often the posts are looked at, in ticks. */
    public static final int EVERY = 20;
    /** How far from its post one put without a reach of its own keeps (a palace's guard, a court's door warden). */
    public static final int REACH = 12;
    /** How far past its reach one of a post's holders is still counted at it (drawn off by a fight). */
    public static final int STRAY = 16;
    /** How far past its reach a player keeps an empty post empty: no one is put back under someone's nose. */
    public static final int NEAR = 16;

    /** On each holder: its post ({@code Post}, {@code Reach}). */
    public static final String KEY = JTTWEngine.MOD_ID + ":post";
    /** On each holder: its likeness as it was first put at its post, the whole of it, to be put back from. */
    public static final String KEY_BIRTH = JTTWEngine.MOD_ID + ":post_birth";

    // ── Keeping ─────────────────────────────────────────────────────────

    /**
     * Keeps {@code m} at its post: the middle it is held to and its reach (or, held to none, where it
     * stands, {@value #REACH} about it, and held there from now on). Called last, with it all it will
     * be (its rank, its level, its name, its post), just before it joins the world: what it is now is
     * what comes back. A zone's boss is no post's, and is left alone (its seat is {@link Usurpers}').
     */
    public static void keep(Mob m) {
        if (m instanceof ZoneBoss b && b.headsLair()) return;
        if (!m.hasRestriction()) m.restrictTo(m.blockPosition(), REACH);
        keep(m, m.getRestrictCenter(), (int) Math.ceil(m.getRestrictRadius()));
    }

    /** Keeps {@code m} at {@code post}, wandering {@code reach} from it (see {@link #keep(Mob)}). */
    public static void keep(Mob m, BlockPos post, int reach) {
        if (m instanceof ZoneBoss b && b.headsLair()) return;
        CompoundTag mark = new CompoundTag();
        mark.put("Post", NbtUtils.writeBlockPos(post));
        mark.putInt("Reach", reach);
        m.getPersistentData().put(KEY, mark);
        m.getPersistentData().remove(KEY_BIRTH);
        CompoundTag birth = m.saveWithoutId(new CompoundTag());
        birth.putString("id", EntityType.getKey(m.getType()).toString());
        birth.remove("UUID");
        m.getPersistentData().put(KEY_BIRTH, birth);
    }

    /** The post {@code e} holds, or null if it is no post's. */
    @Nullable
    public static BlockPos postOf(Entity e) {
        CompoundTag mark = e.getPersistentData().getCompound(KEY);
        return NbtUtils.readBlockPos(mark, "Post").orElse(null);
    }

    /** How far from its post {@code e} wanders (0 if it is no post's). */
    public static int reachOf(Entity e) {
        return e.getPersistentData().getCompound(KEY).getInt("Reach");
    }

    /** Whether {@code e} is kept at a post here. */
    public static boolean kept(Entity e) {
        return postOf(e) != null && e.getPersistentData().contains(KEY_BIRTH);
    }

    /** A holder is held to its post whenever it is in the world (the restriction is not saved by every kind). */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob m) || m.hasRestriction()) return;
        BlockPos post = postOf(m);
        if (post != null) m.restrictTo(post, reachOf(m));
    }

    /** One of a post's holders dies: written down, to be put back once its post has been empty long enough. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || !kept(e)) return;
        State.of(level).fall(level.dimension(), postOf(e), reachOf(e), e.getPersistentData().getCompound(KEY_BIRTH));
    }

    // ── Looking ─────────────────────────────────────────────────────────

    /** The post's living holders, about it. */
    public static List<Mob> holders(ServerLevel level, BlockPos post, int reach) {
        return level.getEntitiesOfClass(Mob.class, new AABB(post).inflate(reach + STRAY),
                m -> m.isAlive() && post.equals(postOf(m)));
    }

    /** Whether a post's ground (its middle, and its reach every way) has its creatures loaded and ticking, so a count of them is a true one. */
    public static boolean loaded(ServerLevel level, BlockPos post, int reach) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (!level.isPositionEntityTicking(post.offset(dx * reach, 0, dz * reach))) return false;
        }
        return true;
    }

    /** Whether any of {@code players} (not watching) stands in a post or near it: within its reach and {@value #NEAR} more. */
    public static boolean someoneAt(ServerLevel level, BlockPos post, int reach, List<? extends Player> players) {
        double r = reach + NEAR;
        Vec3 mid = Vec3.atBottomCenterOf(post);
        for (Player p : players) {
            if (!p.isSpectator() && p.level() == level && p.position().distanceToSqr(mid) < r * r) return true;
        }
        return false;
    }

    /**
     * One look at the post at {@code post} (one with dead to put back), with these players about: if
     * its ground is awake and none of its holders lives, it is marked cleared; one cleared
     * {@link #RESPAWN} ago is held again when no one is in it or near it. Returns those put back
     * (none, if it was not).
     */
    public static List<Mob> look(ServerLevel level, BlockPos post, List<? extends Player> players) {
        State state = State.of(level);
        CompoundTag t = state.get(level.dimension(), post);
        if (t == null) return List.of();
        int reach = t.getInt("Reach");
        if (!loaded(level, post, reach)) return List.of();
        if (!holders(level, post, reach).isEmpty()) {
            if (t.getLong("Cleared") != 0) state.cleared(level.dimension(), post, 0); // one came back to it: held still
            return List.of();
        }
        long now = level.getGameTime(), cleared = t.getLong("Cleared");
        if (cleared == 0) {
            state.cleared(level.dimension(), post, now);
            return List.of();
        }
        if (now - cleared < RESPAWN || someoneAt(level, post, reach, players)) return List.of();
        return muster(level, post);
    }

    /** Puts back a post's dead, each as it was first put there, and forgets them. Returns them. */
    public static List<Mob> muster(ServerLevel level, BlockPos post) {
        State state = State.of(level);
        CompoundTag t = state.get(level.dimension(), post);
        List<Mob> out = new ArrayList<>();
        if (t == null) return out;
        int reach = t.getInt("Reach");
        for (Tag b : t.getList("Fallen", Tag.TAG_COMPOUND)) {
            CompoundTag birth = (CompoundTag) b;
            if (!(EntityType.create(birth.copy(), level).orElse(null) instanceof Mob m)) continue;
            m.setUUID(UUID.randomUUID());
            m.getPersistentData().put(KEY_BIRTH, birth.copy());
            if (!m.hasRestriction()) m.restrictTo(post, reach);
            level.addFreshEntityWithPassengers(m);
            out.add(m);
        }
        state.forget(level.dimension(), post);
        return out;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.overworld().getGameTime() % EVERY != 0 || server.getPlayerList().getPlayerCount() == 0) return;
        State state = State.of(server.overworld());
        for (State.Post p : state.posts()) {
            ServerLevel level = server.getLevel(p.dimension());
            if (level != null && !level.players().isEmpty()) look(level, p.post(), level.players());
        }
    }

    // ── What it remembers ───────────────────────────────────────────────

    /** The posts with dead to put back: each its dimension, its middle and reach, when it was found cleared (0: not yet), its dead. */
    public static final class State extends SavedData {
        private static final String NAME = JTTWEngine.MOD_ID + "_zone_posts";
        private final Map<String, CompoundTag> posts = new LinkedHashMap<>();

        /** A post written down: where it is. */
        public record Post(ResourceKey<Level> dimension, BlockPos post) {}

        public static State of(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::load, null), NAME);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State s = new State();
            for (String k : tag.getAllKeys()) s.posts.put(k, tag.getCompound(k));
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            posts.forEach((k, v) -> tag.put(k, v.copy()));
            return tag;
        }

        private static String key(ResourceKey<Level> dim, BlockPos post) {
            return dim.location() + "|" + post.getX() + "," + post.getY() + "," + post.getZ();
        }

        /** Every post written down. */
        public List<Post> posts() {
            List<Post> out = new ArrayList<>();
            for (CompoundTag t : posts.values()) {
                BlockPos at = NbtUtils.readBlockPos(t, "Post").orElse(null);
                if (at != null) out.add(new Post(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("Dim"))), at));
            }
            return out;
        }

        @Nullable
        public CompoundTag get(ResourceKey<Level> dim, BlockPos post) {
            return posts.get(key(dim, post));
        }

        /** One of the post's holders has fallen: its likeness written down, to be put back. */
        public void fall(ResourceKey<Level> dim, BlockPos post, int reach, CompoundTag birth) {
            CompoundTag t = posts.computeIfAbsent(key(dim, post), k -> new CompoundTag());
            t.putString("Dim", dim.location().toString());
            t.put("Post", NbtUtils.writeBlockPos(post));
            t.putInt("Reach", reach);
            ListTag fallen = t.getList("Fallen", Tag.TAG_COMPOUND);
            fallen.add(birth.copy());
            t.put("Fallen", fallen);
            setDirty();
        }

        /** When the post was found cleared (0: held, or not yet found so). */
        public long clearedAt(ResourceKey<Level> dim, BlockPos post) {
            CompoundTag t = get(dim, post);
            return t == null ? 0 : t.getLong("Cleared");
        }

        /** How many of its holders wait to be put back. */
        public int fallen(ResourceKey<Level> dim, BlockPos post) {
            CompoundTag t = get(dim, post);
            return t == null ? 0 : t.getList("Fallen", Tag.TAG_COMPOUND).size();
        }

        /** Marks the post cleared at {@code when} (0: held again). */
        public void cleared(ResourceKey<Level> dim, BlockPos post, long when) {
            CompoundTag t = get(dim, post);
            if (t == null) return;
            t.putLong("Cleared", when);
            setDirty();
        }

        /** Forgets the post: its dead put back, or a test's done with. */
        public void forget(ResourceKey<Level> dim, BlockPos post) {
            if (posts.remove(key(dim, post)) != null) setDirty();
        }
    }
}
