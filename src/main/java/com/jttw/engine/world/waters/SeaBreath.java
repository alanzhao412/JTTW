package com.jttw.engine.world.waters;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.DragonRegaliaItem;
import com.jttw.engine.item.WaterRepellingPearlItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * How a player lives some 120 blocks under the sea, in the Mystical Waters' trenches: by the
 * Water-Repelling Pearl (避水珠) carried anywhere about them, or by the Dragon Kings' regalia worn
 * whole. Either way the sea parts round them: they never run out of air, and while in water they
 * have Conduit Power (sight in the dark water, and hands as quick there as in air). Those who
 * breathe water already (the Azure Dragon and the sea forms) need neither.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class SeaBreath {

    private SeaBreath() {}

    /** How long each grant of Conduit Power lasts, and how often it is renewed, in ticks. */
    public static final int POWER = 260, RENEW = 40;

    /** Whether the sea parts round {@code p}: they carry the pearl or wear the regalia whole. */
    public static boolean protects(Player p) {
        return WaterRepellingPearlItem.carries(p) || DragonRegaliaItem.wearsFullSet(p);
    }

    /** They breathe, however deep. */
    @SubscribeEvent
    public static void onBreathe(LivingBreatheEvent event) {
        if (!event.canBreathe() && event.getEntity() instanceof Player p && protects(p)) {
            event.setCanBreathe(true);
            event.setRefillAirAmount(Math.max(event.getRefillAirAmount(), 4));
        }
    }

    /** In water, they see and work as in air. Returns whether it granted it. */
    public static boolean grant(Player p) {
        if (!p.isInWater() || !protects(p)) return false;
        MobEffectInstance now = p.getEffect(MobEffects.CONDUIT_POWER);
        if (now != null && now.getDuration() > POWER - RENEW) return true;
        p.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, POWER, 0, true, false, true));
        return true;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide && p.tickCount % RENEW == 0) grant(p);
    }
}
