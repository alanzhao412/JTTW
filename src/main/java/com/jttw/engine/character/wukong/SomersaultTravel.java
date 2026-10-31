package com.jttw.engine.character.wukong;

import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.dimension.RealmArrival;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.OpenSomersaultMenuPacket;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Somersault Anywhere (Shift + M), the Fighting Buddha's: one somersault of the cloud (筋斗云),
 * 108,000 li at a bound, to anyone or anyplace. The key asks the server for the menu
 * ({@link #openMenu}); the menu lists every online player (in whatever realm), every zone's region
 * and its boss's lair, Fengdu, Mount Kunlun, Hell's arrival and its Ten Courts, Heaven's Jade
 * Palace, the world's spawn, his own respawn point and each realm, and takes typed coordinates
 * (x y z and a realm). The server works out where the choice is ({@link #resolve}), finds a safe
 * place to stand nearest it ({@link #landing}: never inside a block) and takes him there in a
 * golden streak. A {@value #COOLDOWN_TICKS}-tick cooldown between somersaults.
 */
public final class SomersaultTravel {

    private SomersaultTravel() {}

    public static final int COOLDOWN_TICKS = 60;
    public static final String KEY_READY_AT = "jttw_somersault_ready_at";
    /** How far round the place asked for a spot to stand is looked for: across, and up and down. */
    public static final int SEARCH_ACROSS = 12, SEARCH_UP = 16;

    /** One entry of the menu: its id (sent back when chosen), its heading, its label. */
    public record Destination(String id, String group, String label) {}

    /** Where a choice is: the world, and the block one would stand in there (before {@link #landing}). */
    public record Target(ServerLevel level, BlockPos pos, @Nullable Float yRot) {}

    public static final String COORDS = "coords";

    // ── The menu ────────────────────────────────────────────────────────

    /** Shift + M: sends him the menu, of what is there now. */
    public static void openMenu(ServerPlayer p) {
        if (!FightingBuddha.is(p)) return;
        List<String> realms = new ArrayList<>();
        for (ServerLevel l : p.server.getAllLevels()) realms.add(l.dimension().location().toString());
        NetworkHandler.sendToPlayer(p, new OpenSomersaultMenuPacket(menu(p), realms));
    }

    /** Everything the menu offers him now. */
    public static List<Destination> menu(ServerPlayer p) {
        List<Destination> out = new ArrayList<>();
        for (ServerPlayer other : p.server.getPlayerList().getPlayers()) {
            if (other == p) continue;
            out.add(new Destination("player:" + other.getUUID(), "Players",
                    other.getGameProfile().getName() + " §7(" + realmName(other.level().dimension()) + ")"));
        }
        out.add(new Destination("spawn", "Home", "The world's spawn"));
        if (p.getRespawnPosition() != null) out.add(new Destination("respawn", "Home", "Your respawn point"));
        for (ServerLevel l : p.server.getAllLevels()) {
            out.add(new Destination("realm:" + l.dimension().location(), "Realms", realmName(l.dimension())));
        }
        out.add(new Destination("zone:white_bone", "Zones", "White Bone Ridge"));
        out.add(new Destination("zone:bottomless", "Zones", "The Bottomless Cave"));
        for (int i = 0; i < SEAS.length; i++) out.add(new Destination("zone:sea:" + i, "Zones", "The Mystical Waters: " + SEAS[i]));
        out.add(new Destination("zone:fire", "Zones", "The Flaming Mountains"));
        out.add(new Destination("zone:wuzhuang", "Zones", "Mount Wuzhuang"));
        out.add(new Destination("zone:lion", "Zones", "Lion-Camel Ridge (the city)"));
        out.add(new Destination("lair:white_bone", "Lairs", "Lady White Bone's throne room"));
        out.add(new Destination("lair:bottomless", "Lairs", "Lady Earth Flow's palace"));
        for (int i = 0; i < SEAS.length; i++) out.add(new Destination("lair:sea:" + i, "Lairs", KINGS[i] + "'s palace (" + SEAS[i] + ")"));
        out.add(new Destination("lair:fire", "Lairs", "Red Boy's throne, the Fire Cloud Cave"));
        out.add(new Destination("lair:wuzhuang", "Lairs", "Zhenyuan's temple hall"));
        out.add(new Destination("lair:lion", "Lairs", "The demon kings' throne room"));
        out.add(new Destination("place:fengdu", "Places", "Fengdu, the Ghost City (Gate of Ghosts)"));
        out.add(new Destination("place:kunlun", "Places", "Mount Kunlun: the Queen Mother's palace"));
        out.add(new Destination("place:kunlun_pool", "Places", "Mount Kunlun: the Jade Pool"));
        out.add(new Destination("hell:arrival", "Hell", "Hell: the Book of Life and Death"));
        for (int n = 1; n <= 10; n++) out.add(new Destination("hell:court:" + n, "Hell", "The " + ORDINALS[n - 1] + " Court (" + COURT_KINGS[n - 1] + ")"));
        out.add(new Destination("heaven:arrival", "Heaven", "Heaven: the arrival"));
        out.add(new Destination("heaven:palace", "Heaven", "Heaven: the Jade Palace"));
        return out;
    }

    static final String[] SEAS = {"the Eastern Sea", "the Southern Sea", "the Western Sea", "the Northern Sea"};
    static final String[] KINGS = {"Ao Guang", "Ao Qin", "Ao Run", "Ao Shun"};
    static final String[] ORDINALS = {"First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth"};
    static final String[] COURT_KINGS = {"King Qinguang", "King Chujiang", "King Songdi", "King Wuguan", "King Yama",
            "King Biancheng", "King Taishan", "King Dushi", "King Pingdeng", "King Zhuanlun"};

    /** A realm's name for the menu. */
    public static String realmName(ResourceKey<Level> dim) {
        if (dim.equals(Level.OVERWORLD)) return "The Overworld";
        if (dim.equals(Level.NETHER)) return "The Nether";
        if (dim.equals(Level.END)) return "The End";
        if (dim.equals(JTTWDimensions.HELL)) return "Hell (Diyu)";
        if (dim.equals(JTTWDimensions.HEAVEN)) return "Heaven";
        return dim.location().toString();
    }

    // ── Travelling ──────────────────────────────────────────────────────

    /** The menu's choice: {@code id}, or for {@link #COORDS} the typed x, y, z in realm {@code dim}. */
    public static boolean travel(ServerPlayer p, String id, int x, int y, int z, String dim) {
        if (!FightingBuddha.is(p)) return false;
        long now = p.server.overworld().getGameTime();
        if (now < p.getPersistentData().getLong(KEY_READY_AT)) {
            p.displayClientMessage(Component.literal("§cThe cloud gathers itself... §7(a moment)"), true);
            return false;
        }
        Target t;
        try {
            t = COORDS.equals(id) ? coords(p, x, y, z, dim) : resolve(p, id);
        } catch (RuntimeException e) {
            com.jttw.engine.JTTWEngine.LOGGER.warn("Somersault to {} failed", id, e);
            t = null;
        }
        if (t == null) {
            p.displayClientMessage(Component.literal("§cNo cloud can find that place in this world."), true);
            return false;
        }
        Vec3 at = landing(t.level(), t.pos());
        go(p, t.level(), at, t.yRot() == null ? p.getYRot() : t.yRot());
        p.getPersistentData().putLong(KEY_READY_AT, now + COOLDOWN_TICKS);
        NetworkHandler.sendToPlayer(p, new SyncCooldownPacket(p.getUUID(), FightingBuddha.TRAVEL, COOLDOWN_TICKS, COOLDOWN_TICKS));
        return true;
    }

    /** Takes him to {@code at} in {@code level} in a golden streak (unseen, if he is vanished). */
    public static void go(ServerPlayer p, ServerLevel level, Vec3 at, float yRot) {
        boolean seen = !BuddhaVanish.isVanished(p);
        if (seen) streak(p.serverLevel(), p.position(), true);
        p.stopRiding();
        p.teleportTo(level, at.x, at.y, at.z, yRot, p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        if (seen) streak(level, at, false);
        else p.playNotifySound(SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /** The somersault's golden trail: a streak up into the sky where he leaves, down out of it where he lands. */
    public static void streak(ServerLevel level, Vec3 at, boolean leaving) {
        DustParticleOptions gold = new DustParticleOptions(new org.joml.Vector3f(1.0f, 0.78f, 0.2f), 1.6f);
        for (int i = 0; i < 24; i++) {
            double y = at.y + 0.5 + i * 0.75;
            level.sendParticles(gold, at.x, y, at.z, 4, 0.12, 0.2, 0.12, 0.0);
            if (i % 3 == 0) level.sendParticles(ParticleTypes.END_ROD, at.x, y, at.z, 1, 0.05, 0.2, 0.05, 0.01);
        }
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.3, at.z, 24, 0.8, 0.2, 0.8, 0.03);
        if (leaving) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2f, 0.8f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.6f);
        } else {
            level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.2f, 1.0f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.9f);
        }
    }

    /** Typed coordinates in a realm (its id, or the one he is in if blank). */
    @Nullable
    public static Target coords(ServerPlayer p, int x, int y, int z, String dim) {
        ServerLevel level = p.serverLevel();
        if (dim != null && !dim.isBlank()) {
            ResourceLocation rl = ResourceLocation.tryParse(dim.trim());
            if (rl == null) return null;
            level = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
            if (level == null) return null;
        }
        return new Target(level, new BlockPos(x, y, z), null);
    }

    /** Where the menu's entry {@code id} is, now; null if it is nowhere in this world. */
    @Nullable
    public static Target resolve(ServerPlayer p, String id) {
        MinecraftServer server = p.server;
        ServerLevel ow = server.overworld();
        long seed = ow.getSeed();
        String[] a = id.split(":");
        switch (a[0]) {
            case "player" -> {
                ServerPlayer other = server.getPlayerList().getPlayer(UUID.fromString(a[1]));
                if (other == null) return null;
                return new Target(other.serverLevel(), other.blockPosition(), other.getYRot());
            }
            case "spawn" -> {
                return new Target(ow, ow.getSharedSpawnPos(), null);
            }
            case "respawn" -> {
                BlockPos at = p.getRespawnPosition();
                ServerLevel level = server.getLevel(p.getRespawnDimension());
                if (at == null || level == null) return null;
                return new Target(level, at, p.getRespawnAngle());
            }
            case "realm" -> {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(id.substring("realm:".length()))));
                if (level == null) return null;
                if (level.dimension().equals(JTTWDimensions.HELL) || level.dimension().equals(JTTWDimensions.HEAVEN)) {
                    return new Target(level, JTTWDimensions.findSafeSpawn(level), null);
                }
                RealmArrival.Arrival arr = RealmArrival.arrival(p, level);
                return new Target(level, BlockPos.containing(arr.pos()), arr.yRot());
            }
            case "zone" -> {
                return zone(ow, seed, a);
            }
            case "lair" -> {
                return lair(ow, seed, a);
            }
            case "place" -> {
                return switch (a[1]) {
                    case "fengdu" -> new Target(ow, com.jttw.engine.world.fengdu.Fengdu.returnSpot(ow), 180f);
                    case "kunlun" -> new Target(ow, com.jttw.engine.world.kunlun.Kunlun.summit(ow).offset(0, 1, 38), 180f);
                    case "kunlun_pool" -> new Target(ow, com.jttw.engine.world.kunlun.Kunlun.summit(ow).offset(0, 1, 27), 180f);
                    default -> null;
                };
            }
            case "hell" -> {
                ServerLevel hell = server.getLevel(JTTWDimensions.HELL);
                if (hell == null) return null;
                if (a[1].equals("arrival")) return new Target(hell, JTTWDimensions.findSafeSpawn(hell), null);
                var court = com.jttw.engine.world.hell.TenCourts.court(Mth.clamp(Integer.parseInt(a[2]), 1, 10));
                int[] door = court.door();
                // just outside its door, facing in
                float face = Direction.fromDelta(-court.doorX(), 0, -court.doorZ()) == null ? 0f
                        : Direction.fromDelta(-court.doorX(), 0, -court.doorZ()).toYRot();
                return new Target(hell, new BlockPos(door[0] + court.doorX() * 2, com.jttw.engine.world.hell.HellTerrain.GROUND + 1,
                        door[1] + court.doorZ() * 2), face);
            }
            case "heaven" -> {
                ServerLevel heaven = server.getLevel(JTTWDimensions.HEAVEN);
                if (heaven == null) return null;
                if (a[1].equals("arrival")) return new Target(heaven, JTTWDimensions.findSafeSpawn(heaven), null);
                return new Target(heaven, com.jttw.engine.world.heaven.HeavenFeatures.palace(heaven.getSeed()).above(), null);
            }
            default -> {
                return null;
            }
        }
    }

    private static int[] centre(Zones.Zone zone, long seed, int i) {
        int[][] cs = zone.centres(seed);
        return cs.length == 0 ? null : cs[Mth.clamp(i, 0, cs.length - 1)];
    }

    @Nullable
    private static Target zone(ServerLevel ow, long seed, String[] a) {
        switch (a[1]) {
            case "white_bone" -> {
                var site = com.jttw.engine.world.ridge.CaveSites.site(ow);
                if (site != null) return new Target(ow, site.at(0, 1, 64), 0f);
                int[] c = centre(Zones.WHITE_BONE, seed, 0);
                return c == null ? null : new Target(ow, surface(ow, c[0], c[1]), null);
            }
            case "bottomless" -> {
                int[] c = centre(Zones.BOTTOMLESS, seed, 0);
                if (c == null) return null;
                var lab = com.jttw.engine.world.cave.BottomlessCave.blueprint(ow, c[0], c[1]);
                return new Target(ow, com.jttw.engine.world.cave.BottomlessCave.landings(lab).get(0).at().above(), null);
            }
            case "sea" -> {
                int[] c = centre(Zones.MYSTICAL_WATERS, seed, Integer.parseInt(a[2]));
                return c == null ? null : new Target(ow, new BlockPos(c[0], ow.getSeaLevel() + 1, c[1]), null);
            }
            case "fire" -> {
                BlockPos o = com.jttw.engine.world.fire.FireFortress.origin(ow);
                return o == null ? null : new Target(ow, o.offset(0, 1, 56), 180f);
            }
            case "wuzhuang" -> {
                int[] c = centre(Zones.MOUNT_WUZHUANG, seed, 0);
                if (c == null) return null;
                if (com.jttw.engine.world.wuzhuang.MountWuzhuangFeature.islandFor(ow, c[0], c[1]) == null) {
                    return new Target(ow, surface(ow, c[0], c[1]), null);
                }
                return new Target(ow, new BlockPos(c[0], com.jttw.engine.world.wuzhuang.IslandShape.TERRACE + 1, c[1] + 38), 180f);
            }
            case "lion" -> {
                int[] c = centre(Zones.LION_CAMEL, seed, 0);
                if (c == null) return null;
                var f = com.jttw.engine.world.lion.LionGates.of(ow, c[0], c[1]);
                return f == null ? null : new Target(ow, f.world(0, 1, 0), null);
            }
            default -> {
                return null;
            }
        }
    }

    @Nullable
    private static Target lair(ServerLevel ow, long seed, String[] a) {
        switch (a[1]) {
            case "white_bone" -> {
                var site = com.jttw.engine.world.ridge.CaveSites.site(ow);
                if (site != null) return new Target(ow, site.throne(), null);
                int[] c = centre(Zones.WHITE_BONE, seed, 0);
                return c == null ? null : new Target(ow, surface(ow, c[0], c[1]), null);
            }
            case "bottomless" -> {
                int[] c = centre(Zones.BOTTOMLESS, seed, 0);
                if (c == null) return null;
                var lab = com.jttw.engine.world.cave.BottomlessCave.blueprint(ow, c[0], c[1]);
                var landings = com.jttw.engine.world.cave.BottomlessCave.landings(lab);
                return new Target(ow, landings.get(landings.size() - 1).at().above(), null);
            }
            case "sea" -> {
                int[] c = centre(Zones.MYSTICAL_WATERS, seed, Integer.parseInt(a[2]));
                if (c == null) return null;
                BlockPos throne = com.jttw.engine.world.waters.TrenchShape.throne(seed, c[0], c[1], ow.getMinBuildHeight());
                return throne == null ? null : new Target(ow, com.jttw.engine.world.waters.DragonPalace.kingAt(throne), null);
            }
            case "fire" -> {
                BlockPos o = com.jttw.engine.world.fire.FireFortress.origin(ow);
                return o == null ? null : new Target(ow, o.offset(0, com.jttw.engine.world.fire.FortressPlan.THRONE_Y + 1, 0), null);
            }
            case "wuzhuang" -> {
                int[] c = centre(Zones.MOUNT_WUZHUANG, seed, 0);
                if (c == null) return null;
                return new Target(ow, new BlockPos(c[0], com.jttw.engine.world.wuzhuang.IslandShape.TERRACE + 2, c[1]), null);
            }
            case "lion" -> {
                int[] c = centre(Zones.LION_CAMEL, seed, 0);
                if (c == null) return null;
                var f = com.jttw.engine.world.lion.LionGates.of(ow, c[0], c[1]);
                return f == null ? null : new Target(ow, f.world(0, com.jttw.engine.world.lion.LionFortress.THRONE_Y + 1,
                        com.jttw.engine.world.lion.LionFortress.FZ - 54), null);
            }
            default -> {
                return null;
            }
        }
    }

    /** The surface at x, z: the block one stands in on top of what is there. */
    private static BlockPos surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    // ── A safe place to stand ───────────────────────────────────────────

    /**
     * Where he comes down for {@code want}: the feet position nearest it where he stands on solid
     * ground with room for his body (no block, no lava, no fire) at his feet and head; failing that,
     * the nearest room for his body at all (in water, in the air); failing that, the top of the
     * column there; failing all (solid rock all round), room is made for him where he asked, as
     * nothing stands against him. Never inside a block.
     */
    public static Vec3 landing(ServerLevel level, BlockPos want) {
        int minFeet = level.getMinBuildHeight(), maxFeet = level.getMaxBuildHeight() - 2;
        BlockPos clamped = level.getWorldBorder().clampToBounds(want.getX(), Mth.clamp(want.getY(), minFeet, maxFeet), want.getZ());
        BlockPos at = new BlockPos(clamped.getX(), Mth.clamp(want.getY(), minFeet, maxFeet), clamped.getZ());
        for (int cx = (at.getX() - SEARCH_ACROSS) >> 4; cx <= (at.getX() + SEARCH_ACROSS) >> 4; cx++) {
            for (int cz = (at.getZ() - SEARCH_ACROSS) >> 4; cz <= (at.getZ() + SEARCH_ACROSS) >> 4; cz++) level.getChunk(cx, cz);
        }
        BlockPos found = nearest(level, at, true, minFeet, maxFeet);
        if (found == null) found = nearest(level, at, false, minFeet, maxFeet);
        if (found == null) {
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
            if (top > level.getMinBuildHeight() && top <= maxFeet && room(level, new BlockPos(at.getX(), top, at.getZ()))) {
                found = new BlockPos(at.getX(), top, at.getZ());
            }
        }
        if (found == null) {
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(at.above(), Blocks.AIR.defaultBlockState(), 3);
            found = at;
        }
        return Vec3.atBottomCenterOf(found);
    }

    /** The nearest feet position to {@code at} (ring by ring across, nearest height first) that stands ({@code ground}) or has room. */
    @Nullable
    private static BlockPos nearest(ServerLevel level, BlockPos at, boolean ground, int minFeet, int maxFeet) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int r = 0; r <= SEARCH_ACROSS; r++) {
            BlockPos best = null;
            int bestScore = Integer.MAX_VALUE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (int k = 0; k <= 2 * SEARCH_UP; k++) {
                        int dy = (k + 1) / 2 * (k % 2 == 0 ? -1 : 1);
                        int y = at.getY() + dy;
                        if (y < minFeet || y > maxFeet) continue;
                        int score = dx * dx + dz * dz + dy * dy;
                        if (score >= bestScore) break;
                        m.set(at.getX() + dx, y, at.getZ() + dz);
                        if (ground ? stands(level, m) : room(level, m)) {
                            best = m.immutable();
                            bestScore = score;
                            break;
                        }
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    /** Room for his body at {@code feet}: nothing solid, no lava, no fire, at his feet and head. */
    public static boolean room(Level level, BlockPos feet) {
        return clear(level, feet) && clear(level, feet.above());
    }

    private static boolean clear(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (!s.getCollisionShape(level, p).isEmpty()) return false;
        if (s.getFluidState().is(FluidTags.LAVA)) return false;
        return !s.is(Blocks.FIRE) && !s.is(Blocks.SOUL_FIRE) && !s.is(Blocks.END_PORTAL) && !s.is(Blocks.NETHER_PORTAL)
                && !s.is(Blocks.END_GATEWAY) && !s.is(Blocks.POWDER_SNOW) && !s.is(Blocks.SWEET_BERRY_BUSH) && !s.is(Blocks.WITHER_ROSE);
    }

    /** Solid, safe ground under {@code feet} (not lava nor magma) and room for his body. */
    public static boolean stands(Level level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState floor = level.getBlockState(below);
        if (!floor.getFluidState().isEmpty() || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CAMPFIRE) || floor.is(Blocks.SOUL_CAMPFIRE)) return false;
        if (!floor.isFaceSturdy(level, below, Direction.UP)) return false;
        return room(level, feet) && level.getFluidState(feet).isEmpty();
    }
}
