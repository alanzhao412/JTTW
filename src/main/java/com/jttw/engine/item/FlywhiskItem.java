package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Great Immortal Zhenyuan's horsetail whisk (拂尘), the Daoist's emblem, with which he brushed
 * the Monkey King's blows aside. Wielded, it strikes for +{@value #DAMAGE_PERCENT}% damage. Used, it
 * <b>sweeps</b>: whatever stands within {@value #REACH} blocks before its bearer is swept away, and
 * whatever flies at them (arrows, bolts, fireballs) is brushed out of the air; then it rests
 * {@value #REST} ticks. Zhenyuan leaves it when he is struck down.
 */
public class FlywhiskItem extends Item {

    public static final int DAMAGE_PERCENT = 35, REST = 100;
    public static final double REACH = 6, SWEEP = 1.8;

    public FlywhiskItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(JTTWEngine.id("flywhisk_atk"),
                        DAMAGE_PERCENT / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(JTTWEngine.id("flywhisk_speed"),
                        -1.6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            sweep(player);
            player.getCooldowns().addCooldown(this, REST);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The whisk's sweep before {@code bearer}: returns how many creatures were swept away (projectiles brushed aside not counted). */
    public static int sweep(LivingEntity bearer) {
        Level level = bearer.level();
        Vec3 look = bearer.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, bearer.getBoundingBox().inflate(REACH), e -> e != bearer && e.isAlive())) {
            Vec3 to = e.position().subtract(bearer.position()).multiply(1, 0, 1);
            if (to.length() > REACH || (to.lengthSqr() > 0.01 && to.normalize().dot(look) < 0.4)) continue;
            if (bearer.isAlliedTo(e)) continue;
            Vec3 away = to.lengthSqr() < 0.01 ? look : to.normalize();
            e.push(away.x * SWEEP, 0.5, away.z * SWEEP);
            e.hurtMarked = true;
            n++;
        }
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, bearer.getBoundingBox().inflate(REACH), p -> p.getOwner() != bearer)) {
            if (level instanceof ServerLevel server) server.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
            p.discard();
        }
        if (level instanceof ServerLevel server) {
            for (int i = 0; i < 12; i++) {
                double a = (i - 6) * 0.13;
                Vec3 d = look.yRot((float) a);
                server.sendParticles(ParticleTypes.CLOUD, bearer.getX() + d.x * 2.5, bearer.getY(0.6), bearer.getZ() + d.z * 2.5, 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
        level.playSound(null, bearer.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.7f);
        return n;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.zhenyuan_flywhisk.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("+" + DAMAGE_PERCENT + "% damage").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Use: sweep away what stands before you and brush arrows from the air").withStyle(ChatFormatting.AQUA));
    }
}
