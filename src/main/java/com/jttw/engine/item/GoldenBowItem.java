package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.erlang.ErlangShenCharacter;
import com.jttw.engine.character.erlang.XiaotianQuanManager;
import com.jttw.engine.progression.stats.LevelPower;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Erlang Shen's Golden Bow. In anyone else's hands it is a bow. In his, it needs no arrows: it
 * looses golden arrows of light that strike by his power (as his own blows do, so a level-80
 * Erlang's arrow is a level-80 blow), vanish when they land, and send Xiaotian Quan after
 * whatever they hit. Like his other hits, they mark it for the Celestial Hunt.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public class GoldenBowItem extends BowItem {

    /** A golden arrow's damage at level 1 before its speed multiplies it (a vanilla arrow's is 2). */
    public static final double ARROW_DAMAGE = 2.4;
    /** On an arrow: loosed from the Golden Bow by Erlang Shen. */
    public static final String KEY_GOLDEN = "jttw_golden_arrow";

    public GoldenBowItem() {
        super(new Properties().durability(1200).rarity(Rarity.EPIC));
    }

    private static boolean erlang(LivingEntity user) {
        return user instanceof Player p && com.jttw.engine.character.Characters.of(p) == ErlangShenCharacter.INSTANCE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        if (!erlang(p)) return super.use(level, p, hand);
        p.startUsingItem(hand); // no arrows needed: he draws them of light
        return InteractionResultHolder.consume(p.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!erlang(user)) {
            super.releaseUsing(stack, level, user, timeLeft);
            return;
        }
        Player p = (Player) user;
        float power = getPowerForTime(getUseDuration(stack) - timeLeft);
        if (power < 0.1f) return;
        if (!level.isClientSide) {
            loose(p, power);
            stack.hurtAndBreak(1, p, e -> e.broadcastBreakEvent(p.getUsedItemHand()));
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1f, 1.4f + power * 0.3f);
        p.awardStat(Stats.ITEM_USED.get(this));
    }

    /** Looses one golden arrow at {@code power} (0 to 1) of a full draw. Public for tests. */
    public static Arrow loose(Player p, float power) {
        Arrow arrow = new Arrow(p.level(), p);
        arrow.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, power * 3.4f, 0.4f);
        if (power >= 1f) arrow.setCritArrow(true);
        arrow.setBaseDamage(ARROW_DAMAGE * LevelPower.of(p));
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY; // light, not wood: nothing to pick up
        arrow.getPersistentData().putBoolean(KEY_GOLDEN, true);
        p.level().addFreshEntity(arrow);
        return arrow;
    }

    /** Where a golden arrow strikes, the hound follows. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow) || !arrow.getPersistentData().getBoolean(KEY_GOLDEN)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer erlang)) return;
        var hound = XiaotianQuanManager.houndOf(erlang);
        LivingEntity target = event.getEntity();
        if (hound != null && target != hound && target.isAlive()) hound.setTarget(target);
    }

    /** A golden arrow that strikes the ground or a wall fades there in a scatter of light. */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof AbstractArrow arrow && arrow.getPersistentData().getBoolean(KEY_GOLDEN)
                && event.getRayTraceResult() instanceof BlockHitResult hit && arrow.level() instanceof ServerLevel level) {
            var at = hit.getLocation();
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.03);
            arrow.discard();
        }
    }
}
