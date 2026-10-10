package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QiankunRingItem extends Item {

    private static final UUID UUID_ATK = UUID.fromString("d2b3c4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e");
    public static final String TAG_SIZE = "jttw_qk_size";
    /** On the thrown ring: the damage it deals on hitting a living target. */
    private static final String TAG_HIT_DAMAGE = "jttw_qk_damage";

    public QiankunRingItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    public static int getSize(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_SIZE)) return 1;
        return Math.max(1, Math.min(5, stack.getTag().getInt(TAG_SIZE)));
    }

    public static void setSize(ItemStack stack, int size) {
        stack.getOrCreateTag().putInt(TAG_SIZE, Math.max(1, Math.min(5, size)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of();
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(UUID_ATK, "jttw:ring_atk",
                0.25 * Armaments.refineMultiplier(stack), AttributeModifier.Operation.MULTIPLY_TOTAL));
        return b.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
        if (!WieldRules.canThrowRing(sp)) {
            sp.displayClientMessage(Component.literal("§cThe Qiankun Ring answers only to Nezha."), true);
            return InteractionResultHolder.fail(stack);
        }

        int size = getSize(stack);
        Snowball proj = new Snowball(level, sp);
        proj.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 1.5F, 1.0F);
        // The ring hits for size x the thrower's attack (onRingHit)
        proj.getPersistentData().putInt("jttw_qk_size", size);
        proj.getPersistentData().putFloat(TAG_HIT_DAMAGE, size * (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE));
        proj.getPersistentData().putUUID("jttw_qk_owner", sp.getUUID());
        level.addFreshEntity(proj);

        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.2F);

        // Don't remove item — ring returns conceptually
        sp.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int size = getSize(stack);
        tooltip.add(Component.literal("§6Qiankun Ring").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§eSize: " + size + "/5"));
        tooltip.add(Component.literal("§eDamage: " + (size * 100) + "% STR"));
        tooltip.add(Component.literal("§e+25% melee bonus"));
        tooltip.add(Component.literal("§7Right-click: Throw"));
    }

    /** A thrown ring hits a living target for the damage it was thrown with. */
    @SubscribeEvent
    public static void onRingHit(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)) return;
        var proj = event.getProjectile();
        if (proj.level().isClientSide() || !proj.getPersistentData().contains(TAG_HIT_DAMAGE)) return;
        if (!(hit.getEntity() instanceof LivingEntity target)) return;
        float damage = proj.getPersistentData().getFloat(TAG_HIT_DAMAGE);
        var source = proj.getOwner() instanceof Player owner
                ? proj.damageSources().thrown(proj, owner)
                : proj.damageSources().thrown(proj, null);
        target.hurt(source, damage);
    }
}
