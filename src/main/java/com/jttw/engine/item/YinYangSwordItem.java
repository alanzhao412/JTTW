package com.jttw.engine.item;

import net.minecraft.resources.ResourceLocation;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModItems;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class YinYangSwordItem extends Item {

    private static final ResourceLocation UUID_ATK = JTTWEngine.id("yin_yang_sword_atk");
    /** The off-hand sword needs its own id, or the two hands overwrite each other's bonus. */
    private static final ResourceLocation UUID_ATK_OFFHAND = JTTWEngine.id("atk_offhand");
    public static final String TAG_SIDE = "jttw_yy_side";

    public YinYangSwordItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    public static boolean isYin(ItemStack stack) {
        return stack.hasTag() && "yin".equals(stack.getTag().getString(TAG_SIDE));
    }

    public static boolean isYang(ItemStack stack) {
        return stack.hasTag() && "yang".equals(stack.getTag().getString(TAG_SIDE));
    }

    public static ItemStack createYin() {
        ItemStack stack = new ItemStack(ModItems.YIN_YANG_SWORD.get());
        stack.getOrCreateTag().putString(TAG_SIDE, "yin");
        return stack;
    }

    public static ItemStack createYang() {
        ItemStack stack = new ItemStack(ModItems.YIN_YANG_SWORD.get());
        stack.getOrCreateTag().putString(TAG_SIDE, "yang");
        return stack;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) return ImmutableMultimap.of();
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        UUID id = slot == EquipmentSlot.MAINHAND ? UUID_ATK : UUID_ATK_OFFHAND;
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(id,
                0.5 * Armaments.refineMultiplier(stack), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return b.build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (isYin(stack)) {
            // Yin sword: Weakness I, lowering the target's attack
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Yang sword offhand right-click: magic defense debuff attack
        if (!isYang(stack)) return InteractionResultHolder.pass(stack);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // Find entity in look direction within 4 blocks
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Vec3 end = eye.add(look.scale(4.0));
        AABB searchBox = sp.getBoundingBox().expandTowards(look.scale(4.0)).inflate(1.0);

        LivingEntity target = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : sp.level().getEntities(sp, searchBox)) {
            if (!(e instanceof LivingEntity le) || le == sp) continue;
            AABB ebb = e.getBoundingBox().inflate(0.3);
            var hit = ebb.clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceToSqr(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    target = le;
                }
            }
        }

        if (target != null) {
            // Deal damage + Mining Fatigue II
            float atk = (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
            target.hurt(sp.damageSources().playerAttack(sp), atk);
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1)); // Mining Fatigue II
            // Magic-vulnerable for 5 s: +25% magic damage taken (ResistanceHandler)
            target.getPersistentData().putLong(com.jttw.engine.progression.stats.ResistanceHandler.KEY_MAGIC_VULN, sp.level().getGameTime() + 100);
        }

        sp.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isYin(stack)) return Component.literal("Yin Sword");
        if (isYang(stack)) return Component.literal("Yang Sword");
        return Component.literal("Yin-Yang Sword");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Yin-Yang Swords of Nezha").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§e+50% damage"));
        if (isYin(stack)) {
            tooltip.add(Component.literal("§b[Yin] Left-click: Lowers target's defense"));
        } else if (isYang(stack)) {
            tooltip.add(Component.literal("§d[Yang] Right-click: Lowers target's magic defense"));
        }
    }
}
