package com.jttw.engine.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.hell.HellKings;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.registry.BlackWindRegistry;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.SpiderCaveRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The usurpers: a fallen boss's seat is not left empty. When the master of a lair dies (a boss the
 * world put there, {@link ZoneBoss#seat()}; or a usurper before him), the seat stays empty for
 * {@value ZonePosts#RESPAWN_MINUTES} minutes, as the zones' posts do; then, the next time its ground
 * is awake and no one is in the lair, another demon king has heard of it and taken it for his own: a
 * boss of another kind, at random, made a usurper of that lair ({@link ZoneBoss#becomeUsurper}: of its
 * zone, at its band's top ({@link ZoneBoss#USURPER_LEVELS_ABOVE_BAND} over it, where its master stood
 * three over), {@link ZoneBoss#USURPER_HITS} of his kind's hits, half a boss's XP: easier than the gym's
 * leader he follows). When he falls, another follows him, and so on for ever.
 *
 * <p>Who may usurp ({@link #HOUSES}): every boss whose kit is his own to carry anywhere, a house at a
 * time (one house as likely as another, then one of it): Lady Earth Flow (who, a usurper, has no
 * shrine to flee to and fights it out), the Black Bear Demon, Red Boy, a King of Hell (any of the
 * Ten, King Yama among them), the Hundred-Eyed Demon Lord, and a king of Lion-Camel (the White
 * Elephant or the Azure Lion). Never the Six-Eared Macaque (the one superboss, met once), nor the
 * temple's youths (minibosses), nor Zhenyuan (righteous: he fights only the temple's enemies, and
 * keeps no lair), nor Lady White Bone (her kit is her ridge's: she passes for its dead, calls them up,
 * lays its curse). The Dragon Kings are for the drowned palaces only, and only they: a palace's seat is
 * under the sea, where no other can swim, and a king out of the sea has no sea's wrath to raise.
 *
 * <p>A lair that holds two (the White Elephant's and the Azure Lion's throne room) has two seats: each
 * is taken by its own usurper, never of a kind already sat in the lair. Each of the Ten Courts is a
 * seat. The Six-Eared Macaque's sanctum is a seat like any other once he has fallen: a usurper of
 * Flower-Fruit's band takes it (he himself never comes back).
 *
 * <p>Every hit a usurper deals is scaled here to his level ({@link ZoneBoss#strikes}: the generic hook
 * on all damage he is the striker of, his blows, his specials, his projectiles and summoned blasts).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Usurpers {

    private Usurpers() {}

    /** How far past its lair's ground a player keeps an empty seat empty: no one takes a lair under someone's nose. */
    public static final int NEAR = 16;
    /** How far off those who hear of a lair taken are told. */
    public static final int HEARD = 160;

    /** The houses that may usurp a lair on land: one house as likely as another, then one of its kinds. */
    public static List<List<EntityType<? extends ZoneBoss>>> houses() {
        List<EntityType<? extends ZoneBoss>> hell = new ArrayList<>();
        for (int court = 1; court <= 10; court++) hell.add(HellKings.type(court));
        return List.of(
                List.of(ModEntities.LADY_EARTH_FLOW.get()),
                List.of(BlackWindRegistry.BLACK_BEAR_DEMON.get()),
                List.of(ModEntities.RED_BOY.get()),
                hell,
                List.of(SpiderCaveRegistry.HUNDRED_EYED_DEMON.get()),
                List.of(ModEntities.WHITE_ELEPHANT.get(), ModEntities.AZURE_LION.get()));
    }

    /** The house that may usurp a drowned palace: the Dragon Kings, each of another sea. */
    public static List<EntityType<? extends ZoneBoss>> seaKings() {
        return List.of(ModEntities.AO_GUANG.get(), ModEntities.AO_QIN.get(), ModEntities.AO_RUN.get(), ModEntities.AO_SHUN.get());
    }

    /** Every kind that may ever usurp a seat. */
    public static Set<EntityType<?>> candidates() {
        Set<EntityType<?>> out = new HashSet<>(seaKings());
        for (List<EntityType<? extends ZoneBoss>> house : houses()) out.addAll(house);
        return out;
    }

    // ── A seat falls ────────────────────────────────────────────────────

    /** {@code boss}, a lair's master, has died: his seat is written down, to be taken in time. */
    public static void fallen(ServerLevel level, ZoneBoss boss) {
        BlockPos seat = boss.seat();
        if (seat == null) return;
        State.of(level).fall(level.dimension(), seat, boss.lairZone(), boss.homeReach(), boss.getType(), level.getGameTime());
    }

    /** Whether a seat lies under water (a drowned palace's): only a sea king can hold it. */
    public static boolean drowned(ServerLevel level, BlockPos seat) {
        return level.getFluidState(seat).is(FluidTags.WATER) || level.getFluidState(seat.above()).is(FluidTags.WATER);
    }

    /** The kinds already sat in the lair round {@code seat}: a usurper is never one of them (the other king of a pair). */
    public static Set<EntityType<?>> sitting(ServerLevel level, BlockPos seat, int reach) {
        Set<EntityType<?>> out = new HashSet<>();
        for (ZoneBoss b : level.getEntitiesOfClass(ZoneBoss.class, new AABB(seat).inflate(reach * 2 + 8), b -> b.isAlive() && b.headsLair())) {
            out.add(b.getType());
        }
        return out;
    }

    /**
     * The kind that takes a seat: a random one of a random house (for a drowned seat, a random sea
     * king), never the fallen one's kind ({@code fallen}) nor any of {@code barred} (those already in
     * the lair). Null if none is left (it cannot be, but for a sea of four kings all sat at once).
     */
    @Nullable
    public static EntityType<? extends ZoneBoss> choose(boolean drowned, @Nullable EntityType<?> fallen, Set<EntityType<?>> barred, RandomSource random) {
        List<List<EntityType<? extends ZoneBoss>>> houses = new ArrayList<>();
        for (List<EntityType<? extends ZoneBoss>> house : drowned ? List.of(seaKings()) : houses()) {
            List<EntityType<? extends ZoneBoss>> left = new ArrayList<>();
            for (EntityType<? extends ZoneBoss> kind : house) if (kind != fallen && !barred.contains(kind)) left.add(kind);
            if (!left.isEmpty()) houses.add(left);
        }
        if (houses.isEmpty()) return null;
        List<EntityType<? extends ZoneBoss>> house = houses.get(random.nextInt(houses.size()));
        return house.get(random.nextInt(house.size()));
    }

    /**
     * Sits a usurper on the seat at {@code seat}, of {@code zone}'s lair, keeping within {@code reach}:
     * of a kind {@link #choose chosen} against {@code fallen} and those sat there. Returns him, or null.
     */
    @Nullable
    public static ZoneBoss seat(ServerLevel level, BlockPos seat, Zones.Zone zone, int reach, @Nullable EntityType<?> fallen, RandomSource random) {
        EntityType<? extends ZoneBoss> kind = choose(drowned(level, seat), fallen, sitting(level, seat, reach), random);
        return kind == null ? null : seatAs(level, kind, seat, zone, reach);
    }

    /** Sits a usurper of {@code kind} on the seat at {@code seat}, of {@code zone}'s lair. Returns him, or null. */
    @Nullable
    public static ZoneBoss seatAs(ServerLevel level, EntityType<? extends ZoneBoss> kind, BlockPos seat, Zones.Zone zone, int reach) {
        ZoneBoss boss = kind.create(level);
        if (boss == null) return null;
        boss.moveTo(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(seat), MobSpawnType.EVENT, null);
        boss.becomeUsurper(zone, seat, reach);
        level.addFreshEntityWithPassengers(boss);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY(1), boss.getZ(), 80, 1, 1.5, 1, 0.03);
        level.playSound(null, seat, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 0.6f);
        return boss;
    }

    // ── Looking ─────────────────────────────────────────────────────────

    /** Whether any of {@code players} (not watching) is in the lair round {@code seat}, or near it. */
    public static boolean someoneIn(ServerLevel level, BlockPos seat, int reach, List<? extends Player> players) {
        double r = reach + NEAR;
        Vec3 mid = Vec3.atBottomCenterOf(seat);
        for (Player p : players) {
            if (!p.isSpectator() && p.level() == level && p.position().distanceToSqr(mid) < r * r) return true;
        }
        return false;
    }

    /**
     * One look at the empty seat at {@code seat} with these players about: fallen
     * {@link ZonePosts#RESPAWN} ago, its ground awake and no one in its lair, it is taken. Returns the
     * usurper (null, if it was not taken).
     */
    @Nullable
    public static ZoneBoss look(ServerLevel level, BlockPos seat, List<? extends Player> players) {
        State state = State.of(level);
        CompoundTag t = state.get(level.dimension(), seat);
        if (t == null) return null;
        int reach = t.getInt("Reach");
        if (level.getGameTime() - t.getLong("Fell") < ZonePosts.RESPAWN) return null;
        if (!level.isPositionEntityTicking(seat) || someoneIn(level, seat, reach, players)) return null;
        Zones.Zone zone = Zones.byId(t.getString("Zone"));
        EntityType<?> fallen = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(t.getString("Fallen"))).orElse(null);
        state.forget(level.dimension(), seat);
        if (zone == null) return null;
        ZoneBoss boss = seat(level, seat, zone, reach, fallen, level.random);
        if (boss != null) {
            Component word = Component.literal("§7Word comes on the wind: ").append(boss.getDisplayName())
                    .append(Component.literal("§7 has taken an empty lair."));
            for (ServerPlayer p : level.players()) {
                if (p.position().distanceToSqr(Vec3.atCenterOf(seat)) < HEARD * HEARD) p.sendSystemMessage(word);
            }
        }
        return boss;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.overworld().getGameTime() % ZonePosts.EVERY != 10 || server.getPlayerList().getPlayerCount() == 0) return;
        State state = State.of(server.overworld());
        for (State.Seat s : state.seats()) {
            ServerLevel level = server.getLevel(s.dimension());
            if (level != null && !level.players().isEmpty()) look(level, s.seat(), level.players());
        }
    }

    /** Every hit a usurper strikes lands scaled to his level (ZoneBoss#strikes); a blow turned back on its striker (thorns) is the striker's own, and is not. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ZoneBoss boss) || !boss.isUsurper()) return;
        if (event.getSource().is(DamageTypes.THORNS)) return;
        event.setAmount(boss.strikes(event.getAmount()));
    }

    // ── What it remembers ───────────────────────────────────────────────

    /** The empty seats: each its dimension and place, its lair's zone and ground, who fell there and when. */
    public static final class State extends SavedData {
        private static final String NAME = JTTWEngine.MOD_ID + "_usurpers";
        private final Map<String, CompoundTag> seats = new LinkedHashMap<>();

        /** A seat written down: where it is. */
        public record Seat(ResourceKey<Level> dimension, BlockPos seat) {}

        public static State of(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::load, null), NAME);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State s = new State();
            for (String k : tag.getAllKeys()) s.seats.put(k, tag.getCompound(k));
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            seats.forEach((k, v) -> tag.put(k, v.copy()));
            return tag;
        }

        private static String key(ResourceKey<Level> dim, BlockPos seat) {
            return dim.location() + "|" + seat.getX() + "," + seat.getY() + "," + seat.getZ();
        }

        /** Every empty seat. */
        public List<Seat> seats() {
            List<Seat> out = new ArrayList<>();
            for (CompoundTag t : seats.values()) {
                BlockPos at = NbtUtils.readBlockPos(t, "Seat").orElse(null);
                if (at != null) out.add(new Seat(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("Dim"))), at));
            }
            return out;
        }

        @Nullable
        public CompoundTag get(ResourceKey<Level> dim, BlockPos seat) {
            return seats.get(key(dim, seat));
        }

        /** The seat's master, of {@code kind}, has fallen at {@code when}. */
        public void fall(ResourceKey<Level> dim, BlockPos seat, Zones.Zone zone, int reach, EntityType<?> kind, long when) {
            CompoundTag t = new CompoundTag();
            t.putString("Dim", dim.location().toString());
            t.put("Seat", NbtUtils.writeBlockPos(seat));
            t.putString("Zone", zone.id());
            t.putInt("Reach", reach);
            t.putString("Fallen", EntityType.getKey(kind).toString());
            t.putLong("Fell", when);
            seats.put(key(dim, seat), t);
            setDirty();
        }

        /** Moves when the seat's master fell (the tests' clock). */
        public void fellAt(ResourceKey<Level> dim, BlockPos seat, long when) {
            CompoundTag t = get(dim, seat);
            if (t == null) return;
            t.putLong("Fell", when);
            setDirty();
        }

        /** Forgets the seat: taken, or a test's done with. */
        public void forget(ResourceKey<Level> dim, BlockPos seat) {
            if (seats.remove(key(dim, seat)) != null) setDirty();
        }
    }
}
