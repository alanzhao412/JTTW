package com.jttw.engine.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.entity.waters.DragonKingEntity;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.world.hell.TenCourts;
import com.jttw.engine.world.waters.Sea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * The Local Earth Gods (土地公): in the novel, wherever Sun Wukong came to a mountain whose demon he
 * did not know, he struck the ground with his staff and the place's Earth God came up out of it,
 * bowing, to tell him who lived there and where. Here they answer the Earth God's Luopan
 * ({@code EarthGodLuopanItem}): asked, the Earth God names the zone for its asker's level (the one
 * whose band holds it; under 20 White Bone Ridge, over 100 Lion-Camel Ridge), and which way and how
 * far its one region lies; for the Mystical Waters, the nearest of the Four Seas whose Dragon King
 * still lives ({@link FallenKings}). The luopan's needle then points there.
 *
 * <p>Hell (levels 57-66) is no Earth God's ground: in the overworld they send its band on to the
 * Flaming Mountains, the road west's next zone; in Hell itself they can only point to the Ten
 * Courts' gate. Anywhere else (the Nether, the End, Heaven) no Earth God hears.
 *
 * <p>Every player is given a luopan once, the first time they join a world.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class EarthGod {

    private EarthGod() {}

    /** Where the Earth God sends the asker: a place, what he says of it, and the zone (null in Hell). */
    public record Answer(@Nullable GlobalPos target, @Nullable Zones.Zone zone, Component words) {}

    /** The zone for a player of this level: the one whose band holds it; under the first, the first; past the last, the last. */
    public static Zones.Zone zoneFor(int level) {
        Zones.Zone first = null, last = null;
        for (Zones.Zone zone : Zones.ALL) {
            if (zone.minLevel() <= level && level <= zone.maxLevel()) return zone;
            if (first == null || zone.minLevel() < first.minLevel()) first = zone;
            if (last == null || zone.maxLevel() > last.maxLevel()) last = zone;
        }
        return level < first.minLevel() ? first : last;
    }

    /** The overworld zone a player of this level is sent to: the zone for the level, but Hell's band goes on to the Flaming Mountains. */
    public static Zones.Zone overworldZoneFor(int level) {
        Zones.Zone zone = zoneFor(level);
        return zone.hasFootprint() ? zone : Zones.FIRE_MOUNTAIN;
    }

    /**
     * The centre of the zone's region to send someone at (x, z) to: its one region; for the seas, the
     * nearest whose king lives (any, if all four have fallen).
     */
    @Nullable
    public static int[] regionFor(Zones.Zone zone, long seed, int x, int z, Set<Sea> fallen) {
        int[] best = null;
        double bestD = Double.MAX_VALUE;
        for (int pass = 0; pass < 2 && best == null; pass++) {
            for (int[] c : zone.centres(seed)) {
                if (pass == 0 && zone == Zones.MYSTICAL_WATERS && fallen.contains(Sea.of(c[0], c[1]))) continue;
                double d = Math.hypot(c[0] - x, c[1] - z);
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }
        }
        return best;
    }

    /** The eight winds, from east round by south (+z), as atan2(dz, dx) runs. */
    private static final String[] WINDS = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};

    /** The wind (x, z) lies on, seen from (fromX, fromZ). */
    public static String wind(double fromX, double fromZ, double x, double z) {
        double a = Math.toDegrees(Math.atan2(z - fromZ, x - fromX));
        return WINDS[Math.floorMod((int) Math.round(a / 45), 8)];
    }

    /** A distance as an Earth God gives it: to the nearest fifty blocks, or hundred past a thousand. */
    public static long roughly(double blocks) {
        long step = blocks >= 1000 ? 100 : 50;
        return Math.max(step, Math.round(blocks / step) * step);
    }

    /** Who keeps the zone's region, and what it is, in an Earth God's words (the sea's, for the waters). */
    private static String whoDwells(Zones.Zone zone, int[] c) {
        if (zone == Zones.WHITE_BONE) return "On White Bone Ridge a corpse fiend wearing borrowed faces waits for pilgrims: Lady White Bone";
        if (zone == Zones.BOTTOMLESS) return "Under the mountains the Bottomless Cave winds down to the den of Lady Earth Flow, the Gold-Nosed White Mouse";
        if (zone == Zones.MYSTICAL_WATERS) {
            return switch (Sea.of(c[0], c[1])) {
                case EAST -> "Beneath the Eastern Sea stands the crystal palace of Ao Guang, its Dragon King";
                case SOUTH -> "Beneath the Southern Sea stands the crystal palace of Ao Qin, its Dragon King";
                case WEST -> "Beneath the Western Sea stands the crystal palace of Ao Run, its Dragon King";
                case NORTH -> "Beneath the Northern Sea stands the crystal palace of Ao Shun, its Dragon King";
            };
        }
        if (zone == Zones.FIRE_MOUNTAIN) return "Past the heat of the Flaming Mountains, Red Boy keeps his Fire Cloud Cave";
        if (zone == Zones.MOUNT_WUZHUANG) return "Over the clouds floats Mount Longevity, where the Great Immortal Zhenyuan tends his ginseng tree";
        if (zone == Zones.LION_CAMEL) return "Lion-Camel Ridge: eight hundred li of demons, and the city they devoured";
        return "There the demons of your strength dwell";
    }

    /** What the Earth God answers a player at their place and level ({@code fallen}: the seas whose kings are dead). */
    public static Answer answer(ResourceKey<Level> dimension, Vec3 at, int level, long seed, Set<Sea> fallen) {
        if (dimension.equals(JTTWDimensions.HELL)) {
            BlockPos gate = new BlockPos(0, 64, TenCourts.GATE_Z);
            double d = Math.hypot(gate.getX() - at.x, gate.getZ() - at.z);
            return new Answer(GlobalPos.of(dimension, gate), null, Component.literal(
                    "No Earth God keeps ground in Diyu. Faintly, from above: \"The Ten Courts' gate lies "
                            + (d < 24 ? "before you" : "some " + roughly(d) + " blocks to the " + wind(at.x, at.z, gate.getX(), gate.getZ()))
                            + ", beyond the Naihe.\""));
        }
        if (!dimension.equals(Level.OVERWORLD)) {
            return new Answer(null, null, Component.literal("You knock, but no Earth God hears you here: their ground is the mortal world's."));
        }
        Zones.Zone zone = overworldZoneFor(level);
        int[] c = regionFor(zone, seed, (int) Math.floor(at.x), (int) Math.floor(at.z), fallen);
        if (c == null) return new Answer(null, null, Component.literal("The Earth God scratches his head: he knows of no such place."));
        StringBuilder words = new StringBuilder();
        if (!zoneFor(level).hasFootprint()) {
            words.append("\"Your trials lie in Diyu, the underworld, below any Earth God's ground. Until its gate opens to you, the road west goes on: ");
        } else {
            words.append('"');
        }
        if (zone == Zones.MYSTICAL_WATERS && fallen.size() >= 4) words.append("All four Dragon Kings have fallen, yet their seas remain. ");
        words.append(whoDwells(zone, c));
        double d = Math.hypot(c[0] - at.x, c[1] - at.z);
        if (zone.depth(seed, (int) Math.floor(at.x), (int) Math.floor(at.z)) > 0) {
            words.append(". You stand in that country now; its master is near.\"");
        } else {
            words.append(". It lies some ").append(roughly(d)).append(" blocks to the ").append(wind(at.x, at.z, c[0], c[1])).append(".\"");
        }
        return new Answer(GlobalPos.of(Level.OVERWORLD, new BlockPos(c[0], 64, c[1])), zone, Component.literal(words.toString()));
    }

    /** What the Earth God answers this player where they stand. */
    public static Answer answer(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        return answer(player.level().dimension(), player.position(), PlayerLevel.getLevel(player), overworld.getSeed(),
                FallenKings.of(overworld).seas());
    }

    /** Points the luopan where the Earth God would send its holder, quietly (its needle spins where there is nowhere). */
    public static void aim(ItemStack stack, @Nullable GlobalPos target) {
        LodestoneTracker now = stack.get(DataComponents.LODESTONE_TRACKER);
        Optional<GlobalPos> want = Optional.ofNullable(target);
        if (now != null && now.target().equals(want)) return;
        if (target == null) stack.remove(DataComponents.LODESTONE_TRACKER);
        else stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(want, false));
    }

    /**
     * Calls up the Earth God: he rises in a puff of earth, speaks, and a trail of incense smoke drifts
     * off the way he means; the luopan points there.
     */
    public static void consult(ServerPlayer player, ItemStack luopan) {
        Answer a = answer(player);
        aim(luopan, a.target());
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position();
        level.playSound(null, player.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1f, 0.8f);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.7f);
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 god = at.add(look.scale(1.6));
        level.sendParticles(ParticleTypes.POOF, god.x, at.y + 0.3, god.z, 14, 0.25, 0.2, 0.25, 0.02);
        if (a.target() != null && a.target().dimension().equals(level.dimension())) {
            Vec3 way = new Vec3(a.target().pos().getX() + 0.5 - at.x, 0, a.target().pos().getZ() + 0.5 - at.z);
            if (way.lengthSqr() > 1) {
                way = way.normalize();
                for (int i = 2; i <= 24; i++) {
                    Vec3 p = at.add(way.scale(i * 0.6));
                    level.sendParticles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, true, p.x, at.y + 1.2 + i * 0.03, p.z, 1, 0.02, 0.02, 0.02, 0.005);
                }
            }
        }
        player.sendSystemMessage(Component.literal("§6The Earth God of this place pops up out of the ground, bowing over his staff:"));
        player.sendSystemMessage(Component.literal("§e").append(a.words()));
    }

    /** The first time a player joins a world, an Earth God gives them a luopan. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag kept = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (kept.getBoolean(KEY_GIVEN)) return;
        kept.putBoolean(KEY_GIVEN, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
        ItemStack luopan = new ItemStack(ModItems.EARTH_GOD_LUOPAN.get());
        if (!player.getInventory().add(luopan)) player.drop(luopan, false);
        player.sendSystemMessage(Component.literal("§6An old man with a white beard and a crooked staff pops up out of the ground, "
                + "presses a luopan into your hands and sinks away again. §7(Use it to ask the Earth Gods where the demons of your strength dwell.)"));
    }

    /** Set (kept through deaths) once a player has been given a luopan. */
    public static final String KEY_GIVEN = JTTWEngine.MOD_ID + ":earth_god_luopan_given";

    /** A Dragon King's death is remembered: the Earth Gods send no one to an empty palace while another king lives. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof DragonKingEntity king && king.level() instanceof ServerLevel level) {
            FallenKings.of(level.getServer().overworld()).fall(king.sea());
        }
    }

    /** The seas whose Dragon Kings have died in this world. */
    public static final class FallenKings extends SavedData {

        private static final String NAME = JTTWEngine.MOD_ID + "_fallen_kings";
        private final Set<Sea> seas = EnumSet.noneOf(Sea.class);

        public static FallenKings of(ServerLevel overworld) {
            return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(FallenKings::new, FallenKings::load, null), NAME);
        }

        private static FallenKings load(CompoundTag tag, HolderLookup.Provider registries) {
            FallenKings kings = new FallenKings();
            for (Sea sea : Sea.values()) if (tag.getBoolean(sea.king)) kings.seas.add(sea);
            return kings;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            for (Sea sea : seas) tag.putBoolean(sea.king, true);
            return tag;
        }

        public Set<Sea> seas() {
            return EnumSet.copyOf(seas);
        }

        public void fall(Sea sea) {
            if (seas.add(sea)) setDirty();
        }
    }
}
