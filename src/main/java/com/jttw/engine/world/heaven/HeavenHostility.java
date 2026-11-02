package com.jttw.engine.world.heaven;

import com.jttw.engine.entity.heaven.HeavenFolk;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Whom Heaven's host fights ({@link HeavenFolk}). It depends on who:
 * <ul>
 *   <li><b>A demon</b>: a Yaoguai ({@link ProgressionClass#YAOGUAI}) is attacked on sight. That is what
 *       Havoc in Heaven means for one.</li>
 *   <li><b>Bad karma</b>: anyone Heaven holds a suspect ({@link #ON_SIGHT}, karma −100 and under, the
 *       "Suspect" of {@link Morality#standing}) is attacked on sight too.</li>
 *   <li><b>An outlaw</b> (karma −250 and under, {@link Morality#OUTLAW}) besides has the host sent
 *       down round them while they are in Heaven ({@link com.jttw.engine.world.morality.HeavenlyHost}).</li>
 *   <li><b>Anyone else</b> is left alone until they strike; then the one struck and its allies within
 *       {@value #RALLY} blocks fight back ({@link #rally}).</li>
 * </ul>
 *
 * <p>Heaven judges who struck first ({@link #killDeed}): killing one of its host that came at you (on
 * sight, rallying to an ally, hunting you) is self-defence, no deed at all; killing one you struck first
 * is a crime against Heaven, by its rank ({@link HeavenFolk#crime()}): a soldier −2, a star general (a
 * captain) −5, a miniboss −15, a Heavenly King −30, Nezha or Erlang Shen −40, Li Jing −60.
 */
public final class HeavenHostility {

    private HeavenHostility() {}

    /** At or under this karma Heaven's host attacks on sight: a "Suspect" (Morality.standing). */
    public static final int ON_SIGHT = -100;
    /** How far a struck one's allies come to its aid. */
    public static final double RALLY = 16;

    /** The karma a kill of Heaven's host one struck first costs, by its rank. */
    public static final int SOLDIER = -2, STAR_GENERAL = -5, GENERAL = -15, HEAVENLY_KING = -30, CHAMPION = -40, LI_JING = -60;

    /** Why Heaven's host attacks someone on sight, if it does. */
    public enum Why { NOT, DEMON, BAD_KARMA }

    public static Why why(Player p) {
        if (p.isCreative() || p.isSpectator()) return Why.NOT;
        if (PlayerLevel.getProgressionClass(p) == ProgressionClass.YAOGUAI) return Why.DEMON;
        if (Morality.karma(p) <= ON_SIGHT) return Why.BAD_KARMA;
        return Why.NOT;
    }

    /** Whether Heaven's host attacks {@code p} on sight: a demon, or of bad karma (an outlaw is that and more). */
    public static boolean attacksOnSight(Player p) {
        return why(p) != Why.NOT;
    }

    /** As a predicate on any living thing, for the targeting goals: a player it attacks on sight. */
    public static boolean attacksOnSight(LivingEntity e) {
        return e instanceof Player p && attacksOnSight(p);
    }

    /**
     * {@code struck} was struck by {@code attacker}: its allies of Heaven's host within {@value #RALLY}
     * blocks who have no foe yet take the attacker for theirs (and so came at them: self-defence to
     * kill). Returns how many rallied.
     */
    public static int rally(LivingEntity struck, LivingEntity attacker) {
        if (!(attacker instanceof Player p) || p.isCreative() || p.isSpectator()) return 0;
        int n = 0;
        for (Mob m : struck.level().getEntitiesOfClass(Mob.class, struck.getBoundingBox().inflate(RALLY),
                m -> m != struck && m instanceof HeavenFolk && m.isAlive() && m.getTarget() == null)) {
            if (!m.canAttack(p)) continue;
            m.setTarget(p);
            if (m.getTarget() == p) n++;
        }
        return n;
    }

    /** What killing {@code victim} says of {@code killer}: nothing in self-defence, else its rank's crime. */
    public static int killDeed(Player killer, HeavenFolk victim) {
        if (victim.grudge().selfDefence(killer)) return 0;
        return victim.crime();
    }
}
