package com.jttw.engine.world;

import com.jttw.engine.character.peng.PengCharacter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/**
 * Which players a zone's creatures are allied to ({@link Zones.Zone#allies()}): those they never
 * target, never fight back, and whom the characters' area attacks pass over (they skip whatever
 * {@code isAlliedTo} them). A zone switches one on with {@link Zones.Zone#alliedTo}.
 */
public final class ZoneAllies {

    private ZoneAllies() {}

    /** No one: the usual. */
    public static final Predicate<Player> NONE = p -> false;

    /** Whoever plays the Gold-Winged Peng (Lion-Camel Ridge's creatures are his kin's). */
    public static final Predicate<Player> PENG = p -> PengCharacter.is(p);

    /** Whether {@code other} is a player these allies take in. */
    public static boolean allied(Predicate<Player> allies, Entity other) {
        return other instanceof Player p && allies.test(p);
    }
}
