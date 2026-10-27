package com.jttw.engine.village;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModVillages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * What the supernatural world's talismans and charms do, against the <b>unclean</b>
 * ({@link #isUnclean}): the walking dead, ghosts, and demons (the zones' demons and their kings; not
 * Heaven's officials, the Dragon Kings, the Kings of Hell and their officers, nor Mount Wuzhuang's
 * Daoists, who are righteous).
 * <ul>
 *   <li><b>Demon-Quelling Talisman</b> (镇妖符), burned: for 3 minutes its user's blows on the
 *       unclean strike {@value #QUELL_PERCENT}% harder;</li>
 *   <li><b>Ghost-Warding Talisman</b> (驱鬼符), burned: for 3 minutes the unclean's blows on its user
 *       land {@value #WARD_PERCENT}% softer;</li>
 *   <li><b>Incense</b> (香), burned: for 90 seconds the lesser unclean (not a captain, not a zone's
 *       master) will not take its burner for a quarry, and those already hunting them lose interest;</li>
 *   <li><b>Peace Charm</b> (平安符), carried anywhere about one: the unclean's blows land
 *       {@value #CHARM_PERCENT}% softer (it does not add up with a second);</li>
 *   <li><b>House-Warding Talisman</b> (镇宅符): see {@link Wards}.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Talismans {

    private Talismans() {}

    public static final int QUELL_PERCENT = 25, WARD_PERCENT = 25, CHARM_PERCENT = 15;

    /** Whether talismans work on {@code e}: the walking dead, a ghost, a demon. */
    public static boolean isUnclean(LivingEntity e) {
        if (e.getType().is(EntityTypeTags.UNDEAD)) return true;
        if (e instanceof com.jttw.engine.entity.wuzhuang.WuzhuangMonster || e instanceof com.jttw.engine.entity.wuzhuang.TempleImmortal) return false;
        if (e instanceof com.jttw.engine.entity.hell.HellMonster m) return !m.isOfficial();
        if (e instanceof com.jttw.engine.entity.hell.KingOfHellEntity || e instanceof com.jttw.engine.entity.waters.DragonKingEntity) return false;
        return e instanceof ZoneMonster || e instanceof ZoneBoss;
    }

    /** What a blow of {@code amount} from {@code attacker} on {@code target} comes to, after their talismans and charms. */
    public static float afterTalismans(LivingEntity attacker, LivingEntity target, float amount) {
        boolean uncleanAttacker = isUnclean(attacker);
        return amount * scale(attacker.hasEffect(ModVillages.DEMON_QUELLING), isUnclean(target), uncleanAttacker,
                uncleanAttacker && target.hasEffect(ModVillages.GHOST_WARD), uncleanAttacker && target instanceof Player p && carriesCharm(p));
    }

    /**
     * What a blow is multiplied by: struck by one burning a demon-quelling talisman at an unclean
     * target; landing on one warded by a ghost-warding talisman, or carrying a peace charm, from an
     * unclean attacker.
     */
    public static float scale(boolean quelling, boolean targetUnclean, boolean attackerUnclean, boolean warded, boolean charmed) {
        float k = 1;
        if (quelling && targetUnclean) k *= 1 + QUELL_PERCENT / 100f;
        if (attackerUnclean && warded) k *= 1 - WARD_PERCENT / 100f;
        if (attackerUnclean && charmed) k *= 1 - CHARM_PERCENT / 100f;
        return k;
    }

    public static boolean carriesCharm(Player p) {
        return p.getInventory().contains(s -> s.is(ModVillages.PEACE_CHARM.get()));
    }

    /** Whether incense keeps {@code mob} from hunting {@code quarry}: the quarry burns it, and the mob is one of the lesser unclean. */
    public static boolean appeased(LivingEntity mob, LivingEntity quarry) {
        if (quarry == null || !quarry.hasEffect(ModVillages.INCENSE_CALM) || !isUnclean(mob)) return false;
        if (mob instanceof ZoneBoss) return false;
        return !(mob instanceof ZoneMonster m && m.isCaptain());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == event.getEntity()) return;
        float before = event.getAmount();
        float after = afterTalismans(attacker, event.getEntity(), before);
        if (after != before) event.setAmount(after);
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (appeased(event.getEntity(), event.getNewAboutToBeSetTarget())) event.setCanceled(true);
    }

    /** Once a second, those already hunting a burner of incense lose interest. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !p.hasEffect(ModVillages.INCENSE_CALM)) return;
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, new AABB(p.blockPosition()).inflate(32), m -> m.getTarget() == p)) {
            if (appeased(m, p)) m.setTarget(null);
        }
    }
}
