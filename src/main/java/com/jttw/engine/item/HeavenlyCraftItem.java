package com.jttw.engine.item;

import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.progression.stats.ProgressionClass;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * What only the Heavenly can make, and only the Heavenly can use (their crafts, gated recipes all):
 * <ul>
 *   <li><b>Five-Thunder Talisman</b> (五雷符): burned, it calls a bolt of heavenly thunder down on the
 *       creature its burner looks at, within {@value #THUNDER_RANGE} blocks: a third of a same-level
 *       character's health (it kills one in three).</li>
 *   <li><b>Heaven's Net Talisman</b> (天罗符): burned, it pins the creature its burner looks at, within
 *       {@value #NET_RANGE} blocks, for {@value #NET_TICKS} ticks (a boss shakes it off in half).</li>
 *   <li><b>Nine-Turn Golden Elixir</b> (九转金丹, forged in the Eight Trigrams Furnace): eaten, it heals
 *       fully, fills the eater's Qi, and gives 30 s of Regeneration II and a minute of Resistance I.</li>
 * </ul>
 * In any other hands a talisman is paper and the elixir does nothing.
 */
public class HeavenlyCraftItem extends Item {

    public enum Kind { THUNDER, NET, ELIXIR }

    public static final int THUNDER_RANGE = 24, NET_RANGE = 16, NET_TICKS = 100, COOLDOWN = 20;
    /** The bolt's damage at level 1, scaled by the burner's power: a third of a typical character's 20 health. */
    public static final float THUNDER_AT_LEVEL_ONE = 20f / 3f;

    private final Kind kind;

    public HeavenlyCraftItem(Kind kind) {
        super(new Item.Properties().stacksTo(16).rarity(kind == Kind.ELIXIR ? Rarity.EPIC : Rarity.RARE));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static boolean isHeavenly(Player p) {
        return PlayerLevel.getProgressionClass(p) == ProgressionClass.HEAVENLY;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.ELIXIR ? UseAnim.EAT : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return kind == Kind.ELIXIR ? 32 : 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isHeavenly(player)) {
            if (!level.isClientSide) player.displayClientMessage(Component.literal("§7Only Heaven's own can work it."), true);
            return InteractionResultHolder.fail(stack);
        }
        if (kind == Kind.ELIXIR) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
        LivingEntity target = lookedAt(sp, kind == Kind.THUNDER ? THUNDER_RANGE : NET_RANGE);
        if (target == null) {
            sp.displayClientMessage(Component.literal("§7Nothing there for it to strike."), true);
            return InteractionResultHolder.fail(stack);
        }
        if (kind == Kind.THUNDER) thunder(sp, target);
        else net(sp, target);
        sp.getCooldowns().addCooldown(this, COOLDOWN);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    /** The bolt: a heavenly thunderclap (no fire), and its damage, magic, scaled by the burner's power. */
    public static void thunder(ServerPlayer p, LivingEntity target) {
        ServerLevel level = p.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        target.hurt(p.damageSources().indirectMagic(p, p), LevelPower.scaled(p, THUNDER_AT_LEVEL_ONE));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
    }

    /** The net: golden threads close round the creature and it is pinned. */
    public static void net(ServerPlayer p, LivingEntity target) {
        com.jttw.engine.world.Stun.pin(target, NET_TICKS, p);
        p.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 40, 0.5, 0.7, 0.5, 0.1);
        p.level().playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (kind != Kind.ELIXIR || !(entity instanceof ServerPlayer p) || !isHeavenly(p)) return stack;
        p.setHealth(p.getMaxHealth());
        com.jttw.engine.progression.qi.QiData.setCurrent(p, com.jttw.engine.progression.qi.QiData.getMax(p));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
        p.serverLevel().sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.6, 0.4, 0.1);
        if (!p.getAbilities().instabuild) stack.shrink(1);
        return stack;
    }

    /** The living creature the player looks at, within {@code range} blocks and in sight, or null. */
    @Nullable
    public static LivingEntity lookedAt(Player p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1), end = eye.add(look.scale(range));
        var block = p.level().clip(new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        Vec3 reach = block.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? end : block.getLocation();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, reach, box,
                (Entity e) -> e instanceof LivingEntity l && l.isAlive() && !l.isSpectator() && e != p, range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        switch (kind) {
            case THUNDER -> {
                tooltip.add(Component.literal("Calls heavenly thunder on what you look at").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("(within " + THUNDER_RANGE + " blocks): three kill a foe of your level.").withStyle(ChatFormatting.GRAY));
            }
            case NET -> tooltip.add(Component.literal("Pins what you look at for 5 seconds (within " + NET_RANGE + " blocks).").withStyle(ChatFormatting.GRAY));
            case ELIXIR -> {
                tooltip.add(Component.literal("Heals fully and fills your Qi;").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Regeneration II 30 s, Resistance I 60 s.").withStyle(ChatFormatting.GRAY));
            }
        }
        tooltip.add(Component.literal("Heavenly craft: Heaven's own alone can make and use it.").withStyle(ChatFormatting.GOLD));
    }
}
