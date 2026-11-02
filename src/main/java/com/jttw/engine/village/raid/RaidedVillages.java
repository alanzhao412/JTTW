package com.jttw.engine.village.raid;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wild.RaidChiefEntity;
import com.jttw.engine.village.TangFolkEntity;
import com.jttw.engine.village.TangVillage;
import com.jttw.engine.village.TangVillagePiece;
import com.jttw.engine.world.ZonePosts;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The raided villages' fate. Each raider and its chief carry their village (its centre:
 * {@link #markRaider}, {@link #markChief}); while the chief lives, a raider that falls comes back to
 * its post as a zone's do ({@link ZonePosts}, 20 minutes after its post's last falls, no one near).
 * When the chief falls the village is <b>freed for good</b> ({@link #free}), remembered for the world
 * ({@link State}): its raiders flee (gone, and never back: none is put back at a freed village's
 * post, and a post's dead are forgotten), its banners and skull totems come down and its embers go
 * out, its signs tell who freed it, and its folk come home, each to the bed the raid left them, a
 * Tang village again, trading as any does. Whoever struck the chief down is paid
 * {@value #FREE_KARMA} karma for it (over the 1 any monster's death pays).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class RaidedVillages {

    private RaidedVillages() {}

    /** On each raider and its chief: its village ({@code Village}, its centre); on the chief its plan too ({@code Variant}, {@code Layout}). */
    public static final String KEY = JTTWEngine.MOD_ID + ":raid";
    /** The karma for freeing a village: a third of a demon queen's (Lady White Bone's 150 is the most a deed pays). */
    public static final int FREE_KARMA = 100;
    /** No village: what {@link #villageOf} says of one of none. */
    public static final long NONE = Long.MIN_VALUE;

    public static void markRaider(LivingEntity e, BlockPos village) {
        CompoundTag t = new CompoundTag();
        t.putLong("Village", village.asLong());
        e.getPersistentData().put(KEY, t);
    }

    public static void markChief(RaidChiefEntity chief, BlockPos village, TangVillage.Variant variant, long layout) {
        CompoundTag t = new CompoundTag();
        t.putLong("Village", village.asLong());
        t.putString("Variant", variant.getSerializedName());
        t.putLong("Layout", layout);
        chief.getPersistentData().put(KEY, t);
    }

    /** The village {@code e} raids (its centre, as a long), or {@link #NONE}. */
    public static long villageOf(Entity e) {
        CompoundTag t = e.getPersistentData().getCompound(KEY);
        return t.contains("Village") ? t.getLong("Village") : NONE;
    }

    /** Whether the village centred at {@code village} has been freed. */
    public static boolean freed(ServerLevel level, BlockPos village) {
        return State.of(level).freed.containsKey(village.asLong());
    }

    // ── The chief's fall ────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level)) return;
        long village = villageOf(e);
        if (village == NONE) return;
        if (e instanceof RaidChiefEntity chief) {
            ServerPlayer by = event.getSource().getEntity() instanceof ServerPlayer p ? p
                    : chief.getKillCredit() instanceof ServerPlayer p ? p : null;
            CompoundTag t = chief.getPersistentData().getCompound(KEY);
            TangVillage.Variant variant = TangVillage.Variant.valueOf(t.getString("Variant").toUpperCase(java.util.Locale.ROOT));
            free(level, BlockPos.of(village), TangVillage.plan(variant, t.getLong("Layout")), by);
        } else if (freed(level, BlockPos.of(village))) {
            e.getPersistentData().remove(ZonePosts.KEY); // a straggler of a freed village: never put back
        }
    }

    /** None of a freed village's raiders comes back: one put back (or loaded) there is turned away. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity() instanceof RaidChiefEntity) return;
        long village = villageOf(event.getEntity());
        if (village != NONE && freed(level, BlockPos.of(village))) event.setCanceled(true);
    }

    /**
     * Frees the village centred at {@code centre}, of {@code plan}, for good: remembered; its raiders
     * gone and their posts forgotten; its marks of the raid taken down; its folk home; {@code by} (if
     * anyone) paid {@value #FREE_KARMA} karma. Returns the folk come home (none if it was already free).
     */
    public static List<TangFolkEntity> free(ServerLevel level, BlockPos centre, TangVillage.Plan plan, ServerPlayer by) {
        State state = State.of(level);
        if (state.freed.containsKey(centre.asLong())) return List.of();
        CompoundTag t = new CompoundTag();
        t.putLong("Time", level.getGameTime());
        if (by != null) t.putString("By", by.getGameProfile().getName());
        state.freed.put(centre.asLong(), t);
        state.setDirty();
        BoundingBox box = RaidedVillage.box(centre);
        AABB area = AABB.of(box).inflate(24, 16, 24);
        // the raiders flee
        for (Mob m : level.getEntitiesOfClass(Mob.class, area, m -> m.isAlive() && villageOf(m) == centre.asLong() && !(m instanceof RaidChiefEntity))) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1, m.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
            m.getPersistentData().remove(ZonePosts.KEY);
            m.discard();
        }
        // and their posts are forgotten
        ZonePosts.State posts = ZonePosts.State.of(level);
        for (ZonePosts.State.Post p : posts.posts()) {
            if (p.dimension().equals(level.dimension()) && box.isInside(p.post())) posts.forget(p.dimension(), p.post());
        }
        restore(level, centre, plan, by == null ? null : by.getGameProfile().getName());
        // the folk come home
        TangVillagePiece.settle(level, plan, centre, box);
        List<TangFolkEntity> folk = level.getEntitiesOfClass(TangFolkEntity.class, AABB.of(box));
        String[] name = plan.name;
        String place = name.length > 2 ? name[1] + " " + name[2] : name[name.length - 1];
        if (by != null) Morality.deed(by, FREE_KARMA, "you freed " + place + " from its yaoguai");
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(centre.getX(), centre.getY(), centre.getZ()) < 96 * 96) {
                p.displayClientMessage(Component.literal(place + " is free! Its folk come home.").withStyle(s -> s.withColor(0x7FD36B)), false);
            }
        }
        return folk;
    }

    /** Takes down the raid's marks in the village: its banners and totems, its embers; its signs say who freed it. */
    static void restore(ServerLevel level, BlockPos centre, TangVillage.Plan plan, String by) {
        BoundingBox box = RaidedVillage.box(centre);
        String[] name = plan.name;
        String[] lines = {name[0], "Freed from", "the yaoguai", by == null ? "" : "by " + by};
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), centre.getY() + 1, box.minZ(), box.maxX(), centre.getY() + 6, box.maxZ())) {
            var s = level.getBlockState(p);
            if (s.is(Blocks.BLACK_BANNER) || s.is(Blocks.SKELETON_SKULL) || s.is(Blocks.NETHER_BRICK_FENCE) || s.is(Blocks.SOUL_LANTERN)
                    || s.getBlock() instanceof CampfireBlock) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else if (s.getBlock() instanceof StandingSignBlock) {
                TangVillagePiece.writeSign(level, p.immutable(), DyeColor.LIME, lines);
                level.sendBlockUpdated(p, s, s, Block.UPDATE_ALL);
            }
        }
    }

    // ── What it remembers ───────────────────────────────────────────────

    /** The freed villages, for the world: each by its centre, when and by whom. */
    public static final class State extends SavedData {
        private static final String NAME = JTTWEngine.MOD_ID + "_raided_villages";
        final Map<Long, CompoundTag> freed = new HashMap<>();

        public static State of(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::load, null), NAME);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State s = new State();
            CompoundTag f = tag.getCompound("Freed");
            for (String k : f.getAllKeys()) s.freed.put(Long.parseLong(k), f.getCompound(k));
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag f = new CompoundTag();
            freed.forEach((k, v) -> f.put(Long.toString(k), v.copy()));
            tag.put("Freed", f);
            return tag;
        }

        /** Every freed village's centre. */
        public List<BlockPos> freedVillages() {
            List<BlockPos> out = new ArrayList<>();
            for (long k : freed.keySet()) out.add(BlockPos.of(k));
            return out;
        }

        /** For tests: forgets a village was freed. */
        public void unfree(BlockPos centre) {
            if (freed.remove(centre.asLong()) != null) setDirty();
        }
    }
}
