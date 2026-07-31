package com.jttw.engine.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jttw.engine.reincarnation.AlignmentData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Three-Pointed Double-Edged Spear (三尖两刃刀) — Erlang Shen's legendary weapon.
 *
 * <ul>
 *   <li>+100% damage for Erlang Shen, +20% for others</li>
 *   <li>+5 reach for Erlang Shen</li>
 *   <li>Triple-hit combo: every 3rd consecutive hit strikes 3 times</li>
 *   <li>Shift+right-click: throw (pierces, returns) — TODO</li>
 *   <li>Right-click: sweeping slash (180° arc, 3 blocks) — TODO</li>
 *   <li>Eye of Heaven synergy: bonus magic damage while active — TODO</li>
 *   <li>Wrath synergy: faster attacks, 360° slash — TODO</li>
 * </ul>
 */
public class ThreePointedSpearItem extends Item {

    private static final UUID UUID_ATK_ES = UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef0123456789");
    private static final UUID UUID_ATK_OTHER = UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef012345678a");
    private static final UUID UUID_REACH = UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef012345678b");

    // Combo tracking — stored per-entity in persistent data
    private static final String TAG_COMBO = "jttw_spear_combo";
    private static final String TAG_COMBO_TICK = "jttw_spear_combo_tick";
    private static final int COMBO_RESET_TICKS = 60; // 3 seconds to reset combo

    public ThreePointedSpearItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of();

        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();

        // Damage is applied generically here — Erlang Shen gets full 100%,
        // others get 20%. Detection is done at runtime in hurtEnemy.
        // For attribute display, show the base (lower) value.
        // The Erlang Shen bonus is applied via ErlangShenAbilityHandler event.
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(UUID_ATK_OTHER, "jttw:spear_atk",
                0.20, AttributeModifier.Operation.MULTIPLY_TOTAL));

        return b.build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof Player player)) return true;

        // Track combo count
        var pd = player.getPersistentData();
        long now = player.level().getGameTime();
        long lastTick = pd.getLong(TAG_COMBO_TICK);
        int combo = pd.getInt(TAG_COMBO);

        // Reset combo if too much time passed
        if (now - lastTick > COMBO_RESET_TICKS) {
            combo = 0;
        }

        combo++;
        pd.putInt(TAG_COMBO, combo);
        pd.putLong(TAG_COMBO_TICK, now);

        // Every 3rd hit: triple strike (two bonus hits at reduced damage)
        if (combo >= 3) {
            pd.putInt(TAG_COMBO, 0);
            if (!player.level().isClientSide()) {
                float atk = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                // Two bonus strikes at 50% damage each
                int oldInvuln = target.invulnerableTime;
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().playerAttack(player), atk * 0.5f);
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().playerAttack(player), atk * 0.5f);
                target.invulnerableTime = oldInvuln;
            }
        }

        return true;
    }

    // ── Thrust attack (charge and release) ──────────────────────────

    private static final long THRUST_COOLDOWN_TICKS = 30L;
    private static final long THRUST_WRATH_COOLDOWN = 15L;
    private static final int MIN_CHARGE_TICKS = 5;   // 0.25s minimum
    private static final int MAX_CHARGE_TICKS = 40;  // 2s for full power
    private static final double LUNGE_BASE = 8.0;
    private static final double LUNGE_MAX = 20.0;
    private static final double WRATH_LUNGE_BONUS = 1.5;

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.SPEAR; // pointing animation
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof net.minecraft.server.level.ServerPlayer sp)) return;

        int chargeTime = getUseDuration(stack) - timeLeft;
        if (chargeTime < MIN_CHARGE_TICKS) return;

        // Cooldown check
        if (sp.getCooldowns().isOnCooldown(this)) return;

        boolean esPlayer = isErlangShen(sp);
        boolean inWrath = esPlayer && sp.getHealth() / sp.getMaxHealth() < 0.4f
                && com.jttw.engine.progression.SkillTreeData.getEsWrathTier(sp) >= 1;

        // Charge power: 0.0 → 1.0
        float chargePower = Math.min(1.0f, (float)(chargeTime - MIN_CHARGE_TICKS) / (MAX_CHARGE_TICKS - MIN_CHARGE_TICKS));

        // Lunge distance scales with charge
        double lungeBase = LUNGE_BASE + (LUNGE_MAX - LUNGE_BASE) * chargePower;
        if (inWrath) lungeBase *= WRATH_LUNGE_BONUS;

        // Damage: 150% at min charge → 250% at full charge
        float atk = (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float thrustDamage = atk * (1.5f + chargePower * 1.0f);

        // Speed scaling: +5% damage per block/second
        double speed = sp.getDeltaMovement().horizontalDistance() * 20.0;
        thrustDamage *= (float) (1.0 + speed * 0.05);

        boolean eyeActive = esPlayer && com.jttw.engine.progression.ErlangShenAbilityHandler.isEyeActive(sp.getUUID());

        // Lunge forward
        net.minecraft.world.phys.Vec3 look = sp.getLookAngle().normalize();
        sp.setDeltaMovement(sp.getDeltaMovement().add(look.scale(lungeBase * 0.25)));
        sp.hurtMarked = true;

        // Hit entities in the thrust line
        net.minecraft.world.phys.Vec3 start = sp.getEyePosition();
        net.minecraft.world.phys.AABB searchBox = sp.getBoundingBox()
                .expandTowards(look.scale(lungeBase)).inflate(1.0);

        net.minecraft.server.level.ServerLevel serverLevel = sp.serverLevel();
        java.util.List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, searchBox, e -> e != sp && e.isAlive());

        boolean piercing = esPlayer && com.jttw.engine.progression.SkillTreeData.getEsSpearTier(sp) >= 2;

        for (LivingEntity target : targets) {
            net.minecraft.world.phys.Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0).subtract(start);
            double dot = toTarget.normalize().dot(look);
            if (dot < 0.85) continue;
            if (toTarget.length() > lungeBase + 1.0) continue;

            int oldInvuln = target.invulnerableTime;
            target.invulnerableTime = 0;
            target.hurt(sp.damageSources().playerAttack(sp), thrustDamage);
            target.invulnerableTime = oldInvuln;

            if (eyeActive) {
                target.invulnerableTime = 0;
                target.hurt(sp.damageSources().magic(), thrustDamage * 0.3f);
                target.invulnerableTime = oldInvuln;
            }

            if (!piercing) break;
        }

        // T3: shockwave at full charge + sprinting
        if (esPlayer && com.jttw.engine.progression.SkillTreeData.getEsSpearTier(sp) >= 3 && chargePower >= 0.8f) {
            net.minecraft.world.phys.Vec3 shockPos = sp.position().add(look.scale(lungeBase));
            java.util.List<LivingEntity> shockTargets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    new net.minecraft.world.phys.AABB(
                            shockPos.x - 4, shockPos.y - 2, shockPos.z - 4,
                            shockPos.x + 4, shockPos.y + 2, shockPos.z + 4),
                    e -> e != sp && e.isAlive());
            for (LivingEntity target : shockTargets) {
                net.minecraft.world.phys.Vec3 pullDir = shockPos.subtract(target.position()).normalize();
                target.setDeltaMovement(target.getDeltaMovement().add(pullDir.scale(0.8)));
                target.hurt(sp.damageSources().magic(), atk * 0.5f);
            }
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    shockPos.x, shockPos.y + 1, shockPos.z, 15, 2.0, 1.0, 2.0, 0.1);
        }

        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                net.minecraft.sounds.SoundEvents.TRIDENT_THROW, net.minecraft.sounds.SoundSource.PLAYERS,
                1.0F, 1.2F);

        long cooldown = inWrath ? THRUST_WRATH_COOLDOWN : THRUST_COOLDOWN_TICKS;
        sp.getCooldowns().addCooldown(this, (int) cooldown);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Three-Pointed Double-Edged Spear").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§e+100% damage (Erlang Shen)"));
        tooltip.add(Component.literal("§e+20% damage (others)"));
        tooltip.add(Component.literal("§b+5 reach (Erlang Shen)"));
        tooltip.add(Component.literal("§aEvery 3rd hit: Triple Strike"));
        tooltip.add(Component.literal("§7Right-click: Thrust (150% ATK)"));
        tooltip.add(Component.literal("§7Sprint+right-click: Heavy Thrust (250% ATK)"));
    }

    public static boolean isErlangShen(Player player) {
        String outcome = player.getPersistentData()
                .getString(AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "eye_of_heaven".equals(outcome);
    }
}
