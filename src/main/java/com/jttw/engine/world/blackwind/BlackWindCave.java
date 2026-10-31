package com.jttw.engine.world.blackwind;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.blackwind.BlackBearDemonEntity;
import com.jttw.engine.entity.blackwind.BlackWindChief;
import com.jttw.engine.entity.blackwind.BlackWindMonster;
import com.jttw.engine.registry.BlackWindRegistry;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The Black Wind Cave as the living find it: its door, its grate, its garrisons.
 * <ul>
 *   <li><b>The great door</b> is barred from within while its two keepers live: Lingxuzi and the
 *       White-Robed Scholar, the Black Bear's friends, who stand before it in the forecourt. Knock on it
 *       (use it) while they live and they turn on you; once both are dead it is blown open by the black
 *       wind as soon as someone comes near it, or knocks, and stays open ({@link #knock}).</li>
 *   <li><b>The garrisons</b> ({@link #muster}): the forecourt's (the keepers and their escort) when anyone
 *       comes near the region's heart, the Black Wind Court's when someone is in the forecourt or the court;
 *       each member kept to its part of the cave ({@link #leash}); none of the world's own creatures are
 *       born inside it. A part whose garrison is all dead stays empty for {@value #RESPAWN_MINUTES}
 *       minutes, then is mustered again the next time someone comes near while no one is in it.</li>
 *   <li><b>The grate</b> over the grand stair down to the throne cavern is held by its warden, a Black Bear
 *       Soldier miniboss; his death throws it open for good ({@link #openSeal}).</li>
 *   <li><b>The Black Bear Demon</b> keeps to his throne cavern: should anything bring him off it he is
 *       taken back to his post.</li>
 * </ul>
 * Everything is worked out from the cave's origin (the forecourt floor's level at the region's centre), so
 * the tests' caves work as the world's does.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class BlackWindCave {

    private BlackWindCave() {}

    public static final int RESPAWN_MINUTES = 20;
    public static final long RESPAWN = RESPAWN_MINUTES * 60 * 20L;
    public static final int EVERY = 20;
    /** How near the region's centre someone must be for its cave to be looked after. */
    public static final int NEAR = 200;
    /** How near the door someone must come, its keepers dead, for the black wind to blow it open. */
    public static final int DOOR_NEAR = 14;
    /** How far a member of a garrison wanders from its post; a keeper or the warden keeps closer. */
    public static final int ROAM = 24, GUARD_ROAM = 8;

    public static final String KEY = JTTWEngine.MOD_ID + ":black_wind_post";

    // ── Where ───────────────────────────────────────────────────────────

    /** The cave's origin in this world: the region's centre at the forecourt floor's level (its local (0, 0, 0)). */
    @Nullable
    public static BlockPos origin(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD)) return null;
        int[][] cs = Zones.BLACK_WIND.centres(level.getSeed());
        if (cs.length == 0) return null;
        return new BlockPos(cs[0][0], BlackWindRangeFeature.floors(level, cs[0][0], cs[0][1]).court(), cs[0][1]);
    }

    /** Where the Earth Gods' luopan points for Black Wind Mountain: the cave's great door in the region centred at {@code c}. */
    public static BlockPos doorTarget(int[] c) {
        return new BlockPos(c[0], 64, c[1] + BlackWindPlan.FACE_Z + 4);
    }

    public static BlockPos at(BlockPos origin, int x, int y, int z) {
        return origin.offset(x, y, z);
    }

    /** The whole cave's bounds, the forecourt with it. */
    public static AABB bounds(BlockPos origin) {
        return new AABB(origin.getX() + BlackWindPlan.X_MIN, origin.getY() + BlackWindPlan.Y_MIN, origin.getZ() + BlackWindPlan.Z_MIN,
                origin.getX() + BlackWindPlan.X_MAX + 1, origin.getY() + BlackWindPlan.Y_MAX + 1, origin.getZ() + BlackWindPlan.Z_MAX + 30);
    }

    /** Whether world {@code p} is inside the cave's rock (where nothing is born of itself). */
    public static boolean inside(BlockPos origin, BlockPos p) {
        int x = p.getX() - origin.getX(), y = p.getY() - origin.getY(), z = p.getZ() - origin.getZ();
        return Math.abs(x) <= BlackWindPlan.X_MAX && z >= BlackWindPlan.Z_MIN && z <= BlackWindPlan.FACE_Z && y >= BlackWindPlan.Y_MIN && y <= BlackWindPlan.Y_MAX;
    }

    // ── What it remembers ───────────────────────────────────────────────

    public static final class State extends SavedData {
        private static final String NAME = JTTWEngine.MOD_ID + "_black_wind_cave";
        private final Map<Long, CompoundTag> caves = new HashMap<>();

        public static State of(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::load, null), NAME);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State s = new State();
            for (String k : tag.getAllKeys()) s.caves.put(Long.parseLong(k), tag.getCompound(k));
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            caves.forEach((k, v) -> tag.put(Long.toString(k), v));
            return tag;
        }

        CompoundTag of(BlockPos origin) {
            return caves.computeIfAbsent(origin.asLong(), k -> new CompoundTag());
        }

        public boolean mustered(BlockPos origin, int floor) {
            return (of(origin).getInt("Mustered") & (1 << floor)) != 0;
        }

        public long clearedAt(BlockPos origin, int floor) {
            return of(origin).getLong("Cleared" + floor);
        }

        void markMustered(BlockPos origin, int floor) {
            CompoundTag t = of(origin);
            t.putInt("Mustered", t.getInt("Mustered") | (1 << floor));
            t.putLong("Cleared" + floor, 0);
            setDirty();
        }

        void cleared(BlockPos origin, int floor, long now) {
            of(origin).putLong("Cleared" + floor, now);
            setDirty();
        }

        /** Forgets everything of the cave round {@code origin} (the tests' caves, laid anew). */
        public void forget(BlockPos origin) {
            caves.remove(origin.asLong());
            setDirty();
        }
    }

    // ── The door ────────────────────────────────────────────────────────

    /** Whether the door is open: its middle gone. */
    public static boolean doorOpen(ServerLevel level, BlockPos origin) {
        return !BlackWindBlocks.doorish(level.getBlockState(at(origin, 0, 7, BlackWindPlan.DOOR[5])));
    }

    /** The door's keepers still standing, near it. */
    public static List<BlackWindChief> keepers(ServerLevel level, BlockPos origin) {
        BlockPos door = at(origin, 0, 1, BlackWindPlan.FACE_Z);
        return level.getEntitiesOfClass(BlackWindChief.class, new AABB(door).inflate(72),
                m -> m.isAlive() && floorOf(m) == 0 && guards(m) && origin.equals(originOf(m)));
    }

    private static final List<Opening> OPENING = new ArrayList<>();

    private record Opening(ServerLevel level, BlockPos origin, int[] step) {}

    /**
     * Someone knocks on the door (or comes near it): barred while its keepers live (they turn on the
     * knocker, who is told so); else the black wind blows it open from the middle out, and it stays open.
     * A player in creative opens it regardless. Returns whether it opens.
     */
    public static boolean knock(ServerLevel level, BlockPos origin, @Nullable Player who) {
        if (doorOpen(level, origin) || opening(origin)) return false;
        boolean free = who != null && who.isCreative();
        List<BlackWindChief> keepers = keepers(level, origin);
        if (!keepers.isEmpty() && !free) {
            level.playSound(null, at(origin, 0, 7, BlackWindPlan.DOOR[5]), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 2f, 0.5f);
            if (who != null) {
                who.displayClientMessage(Component.literal("§cThe great door of the Black Wind Cave is barred from within. Its two keepers, "
                        + "the Black Bear's friends, stand before it: strike them down."), true);
                for (BlackWindChief k : keepers) k.setTarget(who);
            }
            return false;
        }
        OPENING.add(new Opening(level, origin, new int[]{0}));
        BlockPos mid = at(origin, 0, 7, BlackWindPlan.DOOR[5]);
        level.playSound(null, mid, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 3f, 0.4f);
        level.playSound(null, mid, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.BLOCKS, 3f, 0.5f);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(Vec3.atCenterOf(mid)) < 64 * 64) {
                p.displayClientMessage(Component.literal("§8A black wind howls out of the mountain: the great door of the Black Wind Cave is blown open."), true);
            }
        }
        return true;
    }

    private static boolean opening(BlockPos origin) {
        for (Opening o : OPENING) if (o.origin().equals(origin)) return true;
        return false;
    }

    private static void openDoors() {
        for (Iterator<Opening> it = OPENING.iterator(); it.hasNext(); ) {
            Opening o = it.next();
            int s = o.step()[0]++;
            if (s % 3 != 0) continue;
            int reach = s / 3;
            int[] d = BlackWindPlan.DOOR;
            for (int x = d[0]; x <= d[3]; x++) {
                if (Math.abs(x) != reach) continue;
                for (int y = d[1]; y <= d[4]; y++) for (int z = d[2]; z <= d[5]; z++) {
                    if (BlackWindPlan.door(BlackWindPlan.code(x, y, z))) o.level().setBlock(at(o.origin(), x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                BlockPos mid = at(o.origin(), x, 7, d[5]);
                o.level().sendParticles(ParticleTypes.LARGE_SMOKE, mid.getX() + 0.5, mid.getY(), mid.getZ() + 1, 14, 0.3, 4, 0.6, 0.08);
            }
            if (reach >= Math.max(-d[0], d[3])) it.remove();
        }
    }

    // ── The grate ───────────────────────────────────────────────────────

    /** Opens the grate over the grand stair. Returns how many bars it took away. */
    public static int openSeal(ServerLevel level, BlockPos origin) {
        int[] b = BlackWindPlan.SEAL_BOX;
        int gone = 0;
        for (int x = b[0]; x <= b[3]; x++) for (int y = b[1]; y <= b[4]; y++) for (int z = b[2]; z <= b[5]; z++) {
            BlockPos p = at(origin, x, y, z);
            if (!level.getBlockState(p).is(Blocks.IRON_BARS)) continue;
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.02);
            gone++;
        }
        if (gone > 0) {
            BlockPos mid = at(origin, 0, 2, b[2]);
            level.playSound(null, mid, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 2f, 0.5f);
            level.playSound(null, mid, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2f, 0.6f);
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(Vec3.atCenterOf(mid)) < 80 * 80) {
                    p.displayClientMessage(Component.literal("§6The grate falls open: the grand stair goes down to the Black Wind King's throne."), true);
                }
            }
        }
        return gone;
    }

    public static boolean sealed(ServerLevel level, BlockPos origin) {
        int[] b = BlackWindPlan.SEAL_BOX;
        return level.getBlockState(at(origin, 0, b[1] + 1, b[2])).is(Blocks.IRON_BARS);
    }

    /** The warden's death opens the grate; a keeper's is told to those near. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob m) || !(m.level() instanceof ServerLevel level) || !guards(m)) return;
        BlockPos origin = originOf(m);
        if (origin == null) return;
        if (floorOf(m) == 1) openSeal(level, origin);
        else if (floorOf(m) == 0) {
            int left = keepers(level, origin).size() - 1;
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(m) < 64 * 64) p.displayClientMessage(Component.literal(left > 0
                        ? "§7One of the door's keepers falls; the other still bars it."
                        : "§7The door's keepers are dead: nothing bars the Black Wind Cave now."), true);
            }
        }
    }

    // ── The garrisons ───────────────────────────────────────────────────

    private static CompoundTag post(Entity e) {
        return e.getPersistentData().getCompound(KEY);
    }

    /** The part of the cave a garrison's member keeps to (0 the forecourt, 1 the court); -1 if it is none of the cave's. */
    public static int floorOf(Entity e) {
        CompoundTag t = post(e);
        return t.contains("Floor") ? t.getInt("Floor") : -1;
    }

    /** Whether it guards (a keeper of the door, the stair's warden). */
    public static boolean guards(Entity e) {
        return post(e).getBoolean("Guard");
    }

    @Nullable
    public static BlockPos originOf(Entity e) {
        CompoundTag t = post(e);
        return t.contains("Ox") ? new BlockPos(t.getInt("Ox"), t.getInt("Oy"), t.getInt("Oz")) : null;
    }

    /** The part of the cave {@code e} stands in (0 forecourt, 1 court, 2 throne cavern), or -1. */
    public static int floorOf(BlockPos origin, Entity e) {
        BlockPos p = e.blockPosition();
        return BlackWindPlan.floorAt(p.getX() - origin.getX(), p.getY() - origin.getY(), p.getZ() - origin.getZ());
    }

    /** Puts one of a garrison at its spot. */
    @Nullable
    public static Mob spawn(ServerLevel level, BlockPos origin, int floor, BlackWindPlan.Spot s, boolean guard) {
        EntityType<? extends BlackWindMonster> type = switch (s.kind()) {
            case BlackWindPlan.BOAR, BlackWindPlan.BOAR_CAPTAIN -> BlackWindRegistry.BOAR_DEMON.get();
            case BlackWindPlan.BEAR, BlackWindPlan.BEAR_CAPTAIN, BlackWindPlan.WARDEN -> BlackWindRegistry.BEAR_SOLDIER.get();
            case BlackWindPlan.LINGXUZI -> BlackWindRegistry.LINGXUZI.get();
            case BlackWindPlan.SCHOLAR -> BlackWindRegistry.WHITE_ROBED_SCHOLAR.get();
            default -> BlackWindRegistry.WIND_IMP.get();
        };
        BlackWindMonster m = type.create(level);
        if (m == null) return null;
        BlockPos p = at(origin, s.x(), s.y(), s.z());
        var shape = level.getBlockState(p).getCollisionShape(level, p);
        double over = shape.isEmpty() ? 0 : shape.max(net.minecraft.core.Direction.Axis.Y);
        m.moveTo(p.getX() + 0.5, p.getY() + over, p.getZ() + 0.5, 180f + (level.random.nextFloat() - 0.5f) * 90f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        if (s.kind() == BlackWindPlan.WARDEN) m.becomeMiniboss(BlackWindPlan.guardLevel(floor), BlackWindChief.HEALTH_OVER);
        else if (guard) m.become(BlackWindPlan.guardLevel(floor), true);
        else m.become(s.captain() ? BlackWindPlan.captainLevel(floor) : BlackWindPlan.regularLevel(floor) + level.random.nextInt(3) - 1, s.captain());
        m.setPersistenceRequired();
        m.restrictTo(p, guard ? GUARD_ROAM : ROAM);
        CompoundTag t = new CompoundTag();
        t.putInt("Floor", floor);
        t.putBoolean("Guard", guard);
        t.putInt("Ox", origin.getX());
        t.putInt("Oy", origin.getY());
        t.putInt("Oz", origin.getZ());
        m.getPersistentData().put(KEY, t);
        level.addFreshEntity(m);
        return m;
    }

    /** Musters part {@code n} of the cave's garrison (0 the forecourt, its keepers and escort; 1 the court, its warden). Returns them. */
    public static List<Mob> muster(ServerLevel level, BlockPos origin, int n) {
        BlackWindPlan.Floor f = BlackWindPlan.get().floor(n);
        List<Mob> out = new ArrayList<>();
        for (BlackWindPlan.Spot s : f.garrison()) {
            Mob m = spawn(level, origin, n, s, false);
            if (m != null) out.add(m);
        }
        for (BlackWindPlan.Spot s : f.guards()) {
            Mob m = spawn(level, origin, n, s, true);
            if (m != null) out.add(m);
        }
        State.of(level).markMustered(origin, n);
        return out;
    }

    public static List<Mob> garrison(ServerLevel level, BlockPos origin, int n) {
        return level.getEntitiesOfClass(Mob.class, bounds(origin), m -> m.isAlive() && floorOf(m) == n && origin.equals(originOf(m)));
    }

    /**
     * Whether the parts of the cave that hold garrisons (the forecourt and the court) have their creatures
     * loaded and ticking, so a count of them is a true one. The throne cavern, farther in, need not be:
     * someone at the door would not have it loaded.
     */
    public static boolean loaded(ServerLevel level, BlockPos origin) {
        for (int cx = (origin.getX() - BlackWindPlan.COURT_RX - 2) >> 4; cx <= (origin.getX() + BlackWindPlan.COURT_RX + 2) >> 4; cx++) {
            for (int cz = (origin.getZ() + BlackWindPlan.STAIR_HEAD_Z - 2) >> 4; cz <= (origin.getZ() + BlackWindPlan.Z_MAX) >> 4; cz++) {
                if (!level.isPositionEntityTicking(new BlockPos(cx * 16 + 8, origin.getY(), cz * 16 + 8))) return false;
            }
        }
        return true;
    }

    /** Musters the parts someone is near, and marks the cleared ones. */
    public static void garrisons(ServerLevel level, BlockPos origin, List<ServerPlayer> near) {
        if (!loaded(level, origin)) return;
        State state = State.of(level);
        long now = level.getGameTime();
        boolean[] want = new boolean[3], on = new boolean[3];
        for (ServerPlayer p : near) {
            int f = floorOf(origin, p);
            want[0] = true;
            if (f >= 0) on[f] = true;
            if (f == 0 || f == 1) want[1] = true;
        }
        for (int n = 0; n <= 1; n++) {
            boolean alive = !garrison(level, origin, n).isEmpty();
            boolean mustered = state.mustered(origin, n);
            long cleared = state.clearedAt(origin, n);
            if (!alive && mustered && cleared == 0) {
                state.cleared(origin, n, now);
                cleared = now;
            }
            if (alive || !want[n]) continue;
            if (!mustered || (now - cleared >= RESPAWN && !on[n])) muster(level, origin, n);
        }
    }

    /**
     * Keeps each garrison to its part of the cave, and the Black Bear to his throne cavern: one found off it
     * is taken back to its post, its quarry forgotten. Returns how many were.
     */
    public static int leash(ServerLevel level, BlockPos origin) {
        int n = 0;
        BlockPos post = at(origin, 0, BlackWindPlan.THRONE_Y, BlackWindPlan.THRONE_Z);
        for (Mob m : level.getEntitiesOfClass(Mob.class, bounds(origin).inflate(24), m -> m.isAlive()
                && (origin.equals(originOf(m)) || m instanceof BlackBearDemonEntity))) {
            int home;
            if (m instanceof BlackBearDemonEntity bear) {
                if (!bear.hasRestriction() || !bear.getRestrictCenter().equals(post)) continue;
                home = 2;
            } else home = floorOf(m);
            BlockPos p = m.blockPosition();
            int x = p.getX() - origin.getX(), y = p.getY() - origin.getY(), z = p.getZ() - origin.getZ();
            boolean stray = home == 0 ? Math.abs(y) > 12 || Math.abs(x) > 44 || z < BlackWindPlan.DOOR[2] - 1 || z > BlackWindPlan.Z_MAX + 24
                    : BlackWindPlan.floorAt(x, y, z) != home;
            if (!stray || !m.hasRestriction()) continue;
            BlockPos back = m.getRestrictCenter();
            m.setTarget(null);
            m.getNavigation().stop();
            m.teleportTo(back.getX() + 0.5, back.getY(), back.getZ() + 0.5);
            m.setDeltaMovement(Vec3.ZERO);
            n++;
        }
        return n;
    }

    // ── Looking after it ────────────────────────────────────────────────

    private static BlockPos worldOrigin;
    private static long worldSeed = Long.MIN_VALUE;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        openDoors();
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % EVERY != 0 || level.players().isEmpty()) return;
        if (worldSeed != level.getSeed()) {
            worldSeed = level.getSeed();
            worldOrigin = null;
        }
        int[][] cs = Zones.BLACK_WIND.centres(level.getSeed());
        if (cs.length == 0) return;
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && Math.abs(p.getX() - cs[0][0]) < NEAR && Math.abs(p.getZ() - cs[0][1]) < NEAR + 60) near.add(p);
        }
        if (near.isEmpty()) return;
        if (!level.hasChunkAt(new BlockPos(cs[0][0], 64, cs[0][1]))) return;
        if (worldOrigin == null) worldOrigin = origin(level);
        look(level, worldOrigin, near);
    }

    /** One look at the cave round {@code origin}, the players near it: its garrisons, its strays, its door. */
    public static void look(ServerLevel level, BlockPos origin, List<ServerPlayer> near) {
        garrisons(level, origin, near);
        leash(level, origin);
        if (!doorOpen(level, origin) && State.of(level).mustered(origin, 0) && keepers(level, origin).isEmpty() && loaded(level, origin)) {
            BlockPos door = at(origin, 0, 1, BlackWindPlan.FACE_Z + 1);
            for (ServerPlayer p : near) {
                if (!p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(door)) < DOOR_NEAR * DOOR_NEAR) {
                    knock(level, origin, p);
                    break;
                }
            }
        }
    }

    /** The door, knocked on by using it. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer p)) return;
        if (!BlackWindBlocks.doorish(level.getBlockState(event.getPos()))) return;
        BlockPos origin = level.dimension().equals(Level.OVERWORLD) ? origin(level) : null;
        if (origin == null || Math.abs(event.getPos().getX() - origin.getX()) > 64 || Math.abs(event.getPos().getZ() - origin.getZ()) > 120) return;
        use(level, origin, p, event.getPos(), event);
    }

    /** Using a block of the cave round {@code origin}: knocking on its door. Returns whether it was the door. */
    public static boolean use(ServerLevel level, BlockPos origin, ServerPlayer p, BlockPos pos, @Nullable PlayerInteractEvent.RightClickBlock event) {
        int x = pos.getX() - origin.getX(), y = pos.getY() - origin.getY(), z = pos.getZ() - origin.getZ();
        if (!BlackWindPlan.door(BlackWindPlan.code(x, y, z))) return false;
        knock(level, origin, p);
        if (event != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
        return true;
    }

    /** The door and the grate do not yield to picks or axes; a player in creative breaks them as anything else. */
    @SubscribeEvent
    public static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getPlayer().isCreative()) return;
        BlockState st = event.getState();
        if (!st.is(Blocks.IRON_BARS) && !BlackWindBlocks.doorish(st)) return;
        BlockPos origin = level.dimension().equals(Level.OVERWORLD) ? origin(level) : null;
        if (origin != null && guarded(level, origin, event.getPos())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.literal("§7It will not yield: the Black Bear's own hold it."), true);
        }
    }

    /** Whether world {@code p} is part of the door or the grate of the cave round {@code origin}, still standing. */
    public static boolean guarded(ServerLevel level, BlockPos origin, BlockPos p) {
        byte c = BlackWindPlan.code(p.getX() - origin.getX(), p.getY() - origin.getY(), p.getZ() - origin.getZ());
        BlockState st = level.getBlockState(p);
        return (BlackWindPlan.door(c) && BlackWindBlocks.doorish(st)) || (BlackWindPlan.seal(c) && st.is(Blocks.IRON_BARS));
    }

    /** Nothing of the world's own is born inside the cave. */
    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        ServerLevel level = event.getLevel().getLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        int[][] cs = Zones.BLACK_WIND.centres(level.getSeed());
        if (cs.length == 0) return;
        BlockPos pos = event.getPos();
        if (Math.abs(pos.getX() - cs[0][0]) > BlackWindPlan.X_MAX + 2 || pos.getZ() - cs[0][1] > BlackWindPlan.FACE_Z || pos.getZ() - cs[0][1] < BlackWindPlan.Z_MIN) return;
        BlockPos origin = worldOrigin != null && worldSeed == level.getSeed() ? worldOrigin : origin(level);
        if (origin != null && inside(origin, pos)) event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }
}
