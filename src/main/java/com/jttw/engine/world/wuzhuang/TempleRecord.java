package com.jttw.engine.world.wuzhuang;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wuzhuang.TempleFolk;
import com.jttw.engine.entity.wuzhuang.ZhenyuanEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * What has become of each Temple of the Five Villages' household, kept with the world (saved data
 * of the overworld, so it holds whether or not a temple's chunks are loaded, and through restarts).
 *
 * <ul>
 *   <li>A temple's household is <b>finite</b>: {@link #household()} of them (its disciples, Clear
 *       Wind and Bright Moon), put in place as the land is made ({@code MountWuzhuangFeature}), each
 *       marked with its temple ({@link #KEY_TEMPLE}: the region's centre). None spawn of themselves,
 *       and none come back. Each one's death is written down ({@link #fall}); how many live is the
 *       household less the fallen.</li>
 *   <li>When the <b>last</b> of them falls at a player's hand (their blow, or the last a player
 *       struck it, as kill credit goes), that player is marked, and a deep bell tolls for them; a
 *       full day after ({@value #DAY} ticks of the world's time), the Great Immortal Zhenyuan comes
 *       back for them, <b>wherever they are</b> ({@link ZhenyuanEntity#arrive}), and fights them to
 *       the death. A minute before, they hear the bell again, far off. If they are not in the world
 *       when the time comes, he comes when they are next. Once for each temple.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class TempleRecord extends SavedData {

    public static final String NAME = "jttw_wuzhuang_temples";
    /** On one of a temple's household: its temple (the region's centre, packed by {@link #key}). */
    public static final String KEY_TEMPLE = "jttw_temple";
    /** How long after the last disciple falls Zhenyuan comes, how long before that the bell warns, how often the record is checked. */
    public static final int DAY = 24000, WARN_BEFORE = 1200, CHECK_EVERY = 20;

    /** A temple's household: how many are put there, all told. */
    public static int household() {
        return Temple.RESIDENTS.size();
    }

    /** What has become of one temple's household. */
    private static final class Entry {
        final Set<UUID> fallen = new HashSet<>();
        /** Who killed the last of them, and when Zhenyuan comes for them. */
        @Nullable UUID marked;
        long due;
        boolean warned, sent;
    }

    private final Map<Long, Entry> temples = new HashMap<>();

    public TempleRecord() {}

    /** The world's record. */
    public static TempleRecord get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TempleRecord::new, TempleRecord::load), NAME);
    }

    /** The world's clock the record keeps: the overworld's game time (it runs on as long as the world does, and sleep does not hasten it). */
    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    /** A temple's key: its region's centre. */
    public static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** Marks {@code e} one of the household of the temple at ({@code cx}, {@code cz}). */
    public static void belongs(Entity e, int cx, int cz) {
        e.getPersistentData().putLong(KEY_TEMPLE, key(cx, cz));
    }

    /** The temple {@code e} belongs to, if it is one of a temple's household (one put there as the land was made). */
    public static OptionalLong templeOf(Entity e) {
        return e.getPersistentData().contains(KEY_TEMPLE, Tag.TAG_LONG) ? OptionalLong.of(e.getPersistentData().getLong(KEY_TEMPLE)) : OptionalLong.empty();
    }

    private Entry entry(long temple) {
        return temples.computeIfAbsent(temple, k -> new Entry());
    }

    // ── What the record says ────────────────────────────────────────────

    /** How many of a temple's household still live. */
    public int living(long temple) {
        Entry e = temples.get(temple);
        return Math.max(0, household() - (e == null ? 0 : e.fallen.size()));
    }

    /** Who killed the last of a temple's household, if anyone has. */
    @Nullable
    public UUID marked(long temple) {
        Entry e = temples.get(temple);
        return e == null ? null : e.marked;
    }

    /** When Zhenyuan comes for them (meaningless if no one is marked). */
    public long due(long temple) {
        Entry e = temples.get(temple);
        return e == null ? 0 : e.due;
    }

    /** Whether Zhenyuan has come for them. */
    public boolean sent(long temple) {
        Entry e = temples.get(temple);
        return e != null && e.sent;
    }

    /** For tests: forgets a temple. */
    public void forget(long temple) {
        if (temples.remove(temple) != null) setDirty();
    }

    // ── What happens ────────────────────────────────────────────────────

    /**
     * One of a temple's household, {@code who}, has fallen ({@code by} a player, or none). Returns
     * whether it was the last and Zhenyuan is now coming for them (once only).
     */
    public boolean fall(long temple, UUID who, @Nullable ServerPlayer by, long now) {
        Entry e = entry(temple);
        if (!e.fallen.add(who)) return false;
        setDirty();
        if (e.marked != null || e.fallen.size() < household() || by == null) return false;
        e.marked = by.getUUID();
        e.due = now + DAY;
        // a deep bell, far off: no word, but they will know something has begun
        by.playNotifySound(SoundEvents.BELL_RESONATE, SoundSource.AMBIENT, 1f, 0.5f);
        by.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, 1f, 0.5f);
        return true;
    }

    /**
     * Sends Zhenyuan to whoever's day is up and who is in the world ({@code online}: a player by
     * their UUID, or null if not here), and rings the warning bell for whoever's day is nearly up.
     * Returns the Zhenyuans sent.
     */
    public List<ZhenyuanEntity> sendDue(long now, Function<UUID, ServerPlayer> online) {
        List<ZhenyuanEntity> sent = new ArrayList<>();
        for (Entry e : temples.values()) {
            if (e.marked == null || e.sent) continue;
            if (now < e.due - WARN_BEFORE) continue;
            ServerPlayer p = online.apply(e.marked);
            if (p == null || !p.isAlive() || p.isSpectator()) continue; // he comes when they are here
            if (now < e.due) {
                if (!e.warned) {
                    e.warned = true;
                    setDirty();
                    p.playNotifySound(SoundEvents.BELL_RESONATE, SoundSource.AMBIENT, 0.7f, 0.5f);
                }
                continue;
            }
            ZhenyuanEntity z = ZhenyuanEntity.arrive(p);
            if (z == null) continue;
            e.sent = true;
            setDirty();
            sent.add(z);
        }
        return sent;
    }

    /** Who a fallen one's death is laid at: the player who struck it down, or the last who struck it (as kill credit goes). */
    @Nullable
    public static ServerPlayer killer(LivingEntity dead, DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer p) return p;
        return dead.getKillCredit() instanceof ServerPlayer p ? p : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead instanceof TempleFolk) || !(dead.level() instanceof ServerLevel level)) return;
        OptionalLong temple = templeOf(dead);
        if (temple.isEmpty()) return;
        MinecraftServer server = level.getServer();
        get(server).fall(temple.getAsLong(), dead.getUUID(), killer(dead, event.getSource()), now(server));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_EVERY != 0) return;
        TempleRecord record = get(server);
        if (record.temples.isEmpty()) return;
        record.sendDue(now(server), server.getPlayerList()::getPlayer);
    }

    // ── Saving ──────────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Entry> t : temples.entrySet()) {
            Entry e = t.getValue();
            CompoundTag c = new CompoundTag();
            c.putLong("Temple", t.getKey());
            ListTag fallen = new ListTag();
            for (UUID u : e.fallen) fallen.add(NbtUtils.createUUID(u));
            c.put("Fallen", fallen);
            if (e.marked != null) c.putUUID("Marked", e.marked);
            c.putLong("Due", e.due);
            c.putBoolean("Warned", e.warned);
            c.putBoolean("Sent", e.sent);
            list.add(c);
        }
        tag.put("Temples", list);
        return tag;
    }

    public static TempleRecord load(CompoundTag tag, HolderLookup.Provider registries) {
        TempleRecord r = new TempleRecord();
        for (Tag t : tag.getList("Temples", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Entry e = r.entry(c.getLong("Temple"));
            for (Tag u : c.getList("Fallen", Tag.TAG_INT_ARRAY)) e.fallen.add(NbtUtils.loadUUID(u));
            e.marked = c.hasUUID("Marked") ? c.getUUID("Marked") : null;
            e.due = c.getLong("Due");
            e.warned = c.getBoolean("Warned");
            e.sent = c.getBoolean("Sent");
        }
        return r;
    }
}
