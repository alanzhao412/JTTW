package com.jttw.engine.village;

import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What a rumour scroll tells (the market gossip a fortune teller writes down): which demons lie
 * ahead on its reader's road. The zone for the reader's level (as the Earth Gods name it), who rules
 * there and the levels of its demons, which way and how far it lies; and the one after it, the
 * road's next trial. In Hell's band, the way down through Fengdu. Read from the zones' own data.
 */
public final class Rumours {

    private Rumours() {}

    /** The master of a zone, as the market tells it. */
    public static String master(Zones.Zone zone) {
        if (zone == Zones.WHITE_BONE) return "Lady White Bone, a corpse fiend who wears the faces of the dead";
        if (zone == Zones.BOTTOMLESS) return "Lady Earth Flow, the Gold-Nosed White Mouse, deep in the Bottomless Cave";
        if (zone == Zones.MYSTICAL_WATERS) return "the Dragon Kings of the Four Seas, in their crystal palaces";
        if (zone == Zones.HELL) return "the Ten Kings of Hell, King Yama at their head";
        if (zone == Zones.FIRE_MOUNTAIN) return "Red Boy, the Holy Child King, and his Six Strong Generals";
        if (zone == Zones.MOUNT_WUZHUANG) return "the Great Immortal Zhenyuan's disciples, guarding the ginseng tree";
        if (zone == Zones.LION_CAMEL) return "the Azure Lion and the White Elephant, who ate a whole kingdom";
        return "demons no one has lived to name";
    }

    /** A zone's name, as the market says it. */
    public static String name(Zones.Zone zone) {
        if (zone == Zones.WHITE_BONE) return "White Bone Ridge";
        if (zone == Zones.BOTTOMLESS) return "the Bottomless Cave";
        if (zone == Zones.MYSTICAL_WATERS) return "the Mystical Waters";
        if (zone == Zones.HELL) return "Diyu, the underworld";
        if (zone == Zones.FIRE_MOUNTAIN) return "the Flaming Mountains";
        if (zone == Zones.MOUNT_WUZHUANG) return "Mount Wuzhuang";
        if (zone == Zones.LION_CAMEL) return "Lion-Camel Ridge";
        if (zone == Zones.FENGDU) return "Fengdu, the Ghost City";
        if (zone == Zones.KUNLUN) return "Mount Kunlun";
        return zone.biome().location().getPath();
    }

    /** The zone after {@code zone} on the road (by level band), or null after the last. */
    public static Zones.Zone next(Zones.Zone zone) {
        Zones.Zone best = null;
        for (Zones.Zone z : Zones.ALL) {
            if (!z.hasLevelBand() || z.minLevel() <= zone.maxLevel()) continue;
            if (best == null || z.minLevel() < best.minLevel()) best = z;
        }
        return best;
    }

    /** What a rumour scroll tells a reader of {@code level} standing at (x, z) in the world of this seed. */
    public static List<String> lines(int level, double x, double z, long seed, boolean overworld) {
        List<String> out = new ArrayList<>();
        Zones.Zone here = EarthGod.zoneFor(level);
        out.add("The scroll reads, in a fortune teller's hasty hand:");
        out.add(" \"" + about(here, x, z, seed, overworld, "Your road leads to ") + "\"");
        Zones.Zone after = next(here);
        if (after != null) out.add(" \"" + about(after, x, z, seed, overworld, "And beyond it, they say, ") + "\"");
        else out.add(" \"Beyond that no traveller has gone and come back to tell of it.\"");
        return out;
    }

    private static String about(Zones.Zone zone, double x, double z, long seed, boolean overworld, String lead) {
        StringBuilder s = new StringBuilder(lead).append(name(zone)).append(" (levels ").append(zone.minLevel()).append('-')
                .append(zone.maxLevel()).append("), where ").append(master(zone)).append(" hold").append(zone == Zones.MYSTICAL_WATERS
                        || zone == Zones.HELL || zone == Zones.MOUNT_WUZHUANG || zone == Zones.LION_CAMEL || zone == Zones.FIRE_MOUNTAIN ? "" : "s")
                .append(" sway");
        Zones.Zone place = zone.hasFootprint() ? zone : Zones.FENGDU;
        int[] c = overworld ? EarthGod.regionFor(place, seed, (int) Math.floor(x), (int) Math.floor(z), Set.of()) : null;
        if (c != null) {
            s.append(zone.hasFootprint() ? "; it lies some " : "; the way down is through Fengdu, some ").append(EarthGod.roughly(Math.hypot(c[0] - x, c[1] - z)))
                    .append(" blocks to the ").append(EarthGod.wind(x, z, c[0], c[1]));
        }
        return s.append('.').toString();
    }

    /** The rumour, for {@code p} where they stand. */
    public static List<Component> tell(ServerPlayer p) {
        boolean overworld = p.level().dimension().equals(Level.OVERWORLD);
        return lines(PlayerLevel.getLevel(p), p.getX(), p.getZ(), p.serverLevel().getSeed(), overworld).stream()
                .map(l -> (Component) Component.literal(l.startsWith(" ") ? "§7" + l : "§6" + l)).toList();
    }
}
