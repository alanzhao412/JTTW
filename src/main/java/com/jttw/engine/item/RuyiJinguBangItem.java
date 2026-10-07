package com.jttw.engine.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jttw.engine.entity.PlacedStaffEntity;
import com.jttw.engine.entity.QiWaveEntity;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.reincarnation.AlignmentData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Ruyi Jingu Bang — Sun Wukong's legendary staff.
 *
 * <ul>
 *   <li>Requires 30 base ATK to wield; dropped otherwise.</li>
 *   <li>Normal mode: 2× ATK multiplier, +5 reach (Wukong).</li>
 *   <li>Qi Empowered (Wukong only): 3× ATK, +5 reach, Qi drain, 3× visual scale.
 *       Hold right-click to charge Qi Wave, release to fire.</li>
 *   <li>Large mode (Wukong only): 3× ATK, +10 reach, 5× visual scale, explosion on hit,
 *       attacks bypass armor (magic damage).</li>
 * </ul>
 */
public class RuyiJinguBangItem extends Item {

    private static final double MIN_ATK = 30.0;
    private static final double WUKONG_REACH_BONUS = 5.0; // 3 base + 5 = 8
    private static final float LARGE_MODE_QI_PER_TICK = 0.15f;
    private static final float EXPLOSION_POWER = 2.0f;
    private static final float QI_WAVE_COST = 15.0f;

    // Charge timing (in ticks) — like a bow
    private static final int MAX_CHARGE_TICKS = 60;  // 3 seconds for full charge
    private static final int MIN_CHARGE_TICKS = 10;  // 0.5 seconds minimum to fire

    // Stable UUIDs for attribute modifiers
    private static final UUID UUID_ATK_MULT = UUID.fromString("a3d2c1e0-7f4b-4e8a-9c6d-1b5e3f7a2d4c");
    private static final UUID UUID_REACH   = UUID.fromString("b4e3d2f1-8a5c-4f9b-0d7e-2c6f4a8b3e5d");

    public static final String TAG_QI_EMPOWERED = "jttw_qi_empowered";
    public static final String TAG_LARGE_MODE = "jttw_large_mode";

    public RuyiJinguBangItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    // ── STR check: drop if under 30 ATK ──────────────────────────────

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) return;

        // Wukong can always wield the staff
        if (isWukong(sp)) {
            // Still drain Qi in empowered mode
            // Only while in the main hand: `selected` is also true for the off hand when hotbar slot 0 is selected
            if (isQiEmpowered(stack) && sp.getMainHandItem() == stack) {
                if (!QiData.spend(sp, LARGE_MODE_QI_PER_TICK)) {
                    stack.getOrCreateTag().putBoolean(TAG_QI_EMPOWERED, false);
                    sp.sendSystemMessage(Component.literal(
                            "§6[JTTW] §eThe Qi empowerment fades from the staff."));
                }
                QiManager.syncNow(sp);
            }
            return;
        }

        double baseAtk = getBaseAtk(sp);
        if (baseAtk < MIN_ATK) {
            // Drop it from wherever it is. `slot` indexes the list the stack is in (the off hand
            // and armour lists start at 0 too), so clear the stack itself, not a main-inventory slot.
            ItemStack dropped = stack.copy();
            dropped.getOrCreateTag().putBoolean(TAG_QI_EMPOWERED, false);
            dropped.getOrCreateTag().putBoolean(TAG_LARGE_MODE, false);
            stack.setCount(0);
            sp.drop(dropped, false);
            sp.sendSystemMessage(Component.literal(
                    "§4[JTTW] §cYou lack the strength to wield the Ruyi Jingu Bang! (Need 30 ATK)"));
            return;
        }

        // Non-Wukong can't use empowered/large modes — clear if somehow set
        if (isQiEmpowered(stack) || isLargeMode(stack)) {
            stack.getOrCreateTag().putBoolean(TAG_QI_EMPOWERED, false);
            stack.getOrCreateTag().putBoolean(TAG_LARGE_MODE, false);
        }
    }

    // ── Right-click actions ─────────────────────────────────────────
    // Normal mode:
    //   Right-click: toggle to Qi Empowered
    //   Sneak + right-click: place staff in world
    // Qi Empowered mode:
    //   Hold right-click: charge Qi wave (bow-style), release to fire
    //   Sneak + right-click: switch to Large mode
    // Large mode:
    //   Sneak + right-click: back to Normal

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!isWukong(player)) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal(
                        "§4[JTTW] §cOnly Sun Wukong can command the Ruyi Jingu Bang."));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (isQiEmpowered(stack)) {
            if (player.isShiftKeyDown()) {
                // Qi Empowered + sneak: switch to Large mode
                stack.getOrCreateTag().putBoolean(TAG_QI_EMPOWERED, false);
                stack.getOrCreateTag().putBoolean(TAG_LARGE_MODE, true);
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal(
                            "§6[JTTW] §eThe Ruyi Jingu Bang grows to immense size!"));
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            } else {
                // Qi Empowered + right-click: start charging Qi wave (bow-style)
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            }
        } else if (isLargeMode(stack)) {
            if (player.isShiftKeyDown()) {
                // Large mode + sneak: back to normal
                stack.getOrCreateTag().putBoolean(TAG_LARGE_MODE, false);
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal(
                            "§6[JTTW] §eRuyi Jingu Bang shrinks back to normal size."));
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            return InteractionResultHolder.pass(stack);
        } else {
            // Normal mode
            if (player.isShiftKeyDown()) {
                // Normal mode + sneak: place staff where player is looking (server only)
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    double placeX, placeY, placeZ;
                    Vec3 eyePos = sp.getEyePosition();
                    Vec3 lookVec = sp.getLookAngle();
                    Vec3 endPos = eyePos.add(lookVec.scale(5.0));
                    BlockHitResult hitResult = level.clip(new ClipContext(
                            eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, sp));

                    if (hitResult.getType() == HitResult.Type.BLOCK) {
                        var abovePos = hitResult.getBlockPos().above();
                        placeX = abovePos.getX() + 0.5;
                        placeY = abovePos.getY();
                        placeZ = abovePos.getZ() + 0.5;
                    } else {
                        placeX = sp.getX();
                        placeY = sp.getY();
                        placeZ = sp.getZ();
                    }

                    PlacedStaffEntity placed = new PlacedStaffEntity(
                            level, placeX, placeY, placeZ, sp.getUUID());
                    level.addFreshEntity(placed);
                    sp.setItemInHand(hand, ItemStack.EMPTY);
                    sp.sendSystemMessage(Component.literal(
                            "§6[JTTW] §eYou plant the Ruyi Jingu Bang into the ground. Scroll to resize, punch to topple!"));
                }
            } else {
                // Normal mode + right-click: enter Qi Empowered
                stack.getOrCreateTag().putBoolean(TAG_QI_EMPOWERED, true);
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal(
                            "§6[JTTW] §eQi surges through the Ruyi Jingu Bang!"));
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
    }

    // ── Bow-style charge mechanics ───────────────────────────────────

    @Override
    public int getUseDuration(ItemStack stack) {
        // Max time you can hold it — 72000 is vanilla bow standard
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // BOW gives the pulling-back animation (only Qi Empowered charges Qi Wave)
        return isQiEmpowered(stack) ? UseAnim.BOW : UseAnim.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (level.isClientSide() || !(livingEntity instanceof ServerPlayer player)) return;
        if (!isQiEmpowered(stack) || !isWukong(player)) return;

        int chargeTime = getUseDuration(stack) - timeLeft;

        if (chargeTime < MIN_CHARGE_TICKS) {
            // Not charged enough — fizzle
            player.sendSystemMessage(Component.literal(
                    "§7[JTTW] §8The Qi dissipates... hold longer to charge."));
            return;
        }

        // Charge power: 0.0 at MIN_CHARGE → 1.0 at MAX_CHARGE
        float chargePower = Math.min(1.0f,
                (float)(chargeTime - MIN_CHARGE_TICKS) / (MAX_CHARGE_TICKS - MIN_CHARGE_TICKS));

        // Qi cost scales with charge
        float qiCost = QI_WAVE_COST * (0.5f + chargePower * 0.5f);
        if (!QiData.spend(player, qiCost)) {
            player.sendSystemMessage(Component.literal(
                    "§4[JTTW] §cNot enough Qi to unleash the wave!"));
            return;
        }
        QiManager.syncNow(player);

        // ATK at time of firing
        float atk = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        // Scale wave properties by charge power
        Vec3 look = player.getLookAngle();
        QiWaveEntity wave = new QiWaveEntity(player.level(), player, look, atk);
        wave.setChargePower(chargePower);
        player.level().addFreshEntity(wave);

        if (chargePower >= 1.0f) {
            player.sendSystemMessage(Component.literal(
                    "§6[JTTW] §e§lFULL POWER! §eA devastating wave of Qi erupts from the staff!"));
        } else {
            int pct = (int)(chargePower * 100);
            player.sendSystemMessage(Component.literal(
                    "§6[JTTW] §eA wave of Qi erupts from the staff! §7(" + pct + "% charge)"));
        }
    }

    // ── Explosion on hit (Qi Empowered & Large mode) ─────────────────

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            if (isQiEmpowered(stack)) {
                target.level().explode(
                        attacker, target.getX(), target.getY(), target.getZ(),
                        EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
            } else if (isLargeMode(stack)) {
                // Large mode: destructive explosion (breaks blocks)
                target.level().explode(
                        attacker, target.getX(), target.getY(), target.getZ(),
                        EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
            }
            // Large mode: extra hit that bypasses armor (magic damage)
            if (isLargeMode(stack) && attacker instanceof Player player) {
                float atk = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                // Reset invulnerability so this bonus damage isn't ignored
                int oldInvuln = target.invulnerableTime;
                target.invulnerableTime = 0;
                target.hurt(target.level().damageSources().indirectMagic(player, player), atk * 0.5f);
                target.invulnerableTime = oldInvuln;
            }
        }
        return true;
    }

    // ── Attribute modifiers ──────────────────────────────────────────

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return ImmutableMultimap.of();
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        // Both Qi Empowered and Large get 3× ATK (2.0 MULTIPLY_TOTAL = ×3)
        double atkMult = (isQiEmpowered(stack) || isLargeMode(stack)) ? 2.0 : 1.0;
        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(UUID_ATK_MULT, "jttw:ruyi_atk",
                        atkMult, AttributeModifier.Operation.MULTIPLY_TOTAL));

        return builder.build();
    }

    // ── Tooltip ──────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Requires 30 ATK to wield").withStyle(ChatFormatting.RED));

        if (isQiEmpowered(stack)) {
            tooltip.add(Component.literal("§dQI EMPOWERED §7— 3× ATK"));
            tooltip.add(Component.literal("§bWukong: +5 reach (8 blocks)"));
            tooltip.add(Component.literal("§e  Hold right-click to charge Qi Wave"));
            tooltip.add(Component.literal("§c  Drains Qi rapidly"));
            tooltip.add(Component.literal("§7  Sneak + right-click: Large mode"));
        } else if (isLargeMode(stack)) {
            tooltip.add(Component.literal("§6LARGE MODE §7— 3× ATK"));
            tooltip.add(Component.literal("§bWukong: +10 reach (13 blocks)"));
            tooltip.add(Component.literal("§e  Explosion on hit"));
            tooltip.add(Component.literal("§c  Attacks bypass armor"));
            tooltip.add(Component.literal("§7  Sneak + right-click: Normal"));
        } else {
            tooltip.add(Component.literal("§e2× ATK multiplier"));
            tooltip.add(Component.literal("§bWukong: +5 reach (8 blocks)"));
            tooltip.add(Component.literal("§7  Right-click: Qi Empowered"));
        }
    }

    // ── Enchant glow in Qi Empowered only ────────────────────────────

    @Override
    public boolean isFoil(ItemStack stack) {
        return isQiEmpowered(stack);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    public static boolean isQiEmpowered(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_QI_EMPOWERED);
    }

    public static boolean isLargeMode(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_LARGE_MODE);
    }

    /** Returns true if the staff is in any powered-up mode (Qi Empowered or Large). */
    public static boolean isEmpoweredOrLarge(ItemStack stack) {
        return isQiEmpowered(stack) || isLargeMode(stack);
    }

    private static boolean isWukong(Player player) {
        String outcome = player.getPersistentData()
                .getString(AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "celestial_primate".equals(outcome);
    }

    private static double getBaseAtk(Player player) {
        double total = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof RuyiJinguBangItem) {
            double mult = isEmpoweredOrLarge(mainHand) ? 2.0 : 1.0;
            total = total / (1.0 + mult);
        }
        return total;
    }
}
