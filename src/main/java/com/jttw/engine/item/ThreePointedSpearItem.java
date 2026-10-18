package com.jttw.engine.item;

import net.minecraft.resources.ResourceLocation;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.reincarnation.AlignmentData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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
 *   <li>Sneak + hold right-click, release: throw (Erlang Shen with Spear Mastery I). Tears
 *       through terrain in a straight line, returns after 20 s — see {@link com.jttw.engine.entity.ThrownSpearEntity}</li>
 *   <li>Hold right-click {@value #MIN_CHARGE_TICKS}+ ticks, release: charged thrust (anyone)</li>
 *   <li>Right-click, let go before the thrust is ready: sweeping slash, {@value #SLASH_DAMAGE_MULTIPLIER}×
 *       ATK to every foe in the half circle ahead within {@value #SLASH_REACH} blocks;
 *       {@value #SLASH_COOLDOWN_TICKS} ticks' cooldown (anyone). Whoever can throw never slashes
 *       while sneaking.</li>
 *   <li>Eye of Heaven synergy (Erlang Shen, Eye open): each melee hit and slash adds
 *       {@value #EYE_MAGIC_FRACTION} of itself as magic damage, which magic resistance reduces
 *       (the thrust keeps its own 30% magic)</li>
 *   <li>Wrath synergy (Erlang Shen, Zhàn Shén I, below 40% health): +15/25/35% attack speed by
 *       Zhàn Shén tier while the spear is in the main hand, and the slash sweeps all round on half
 *       the cooldown</li>
 * </ul>
 */
public class ThreePointedSpearItem extends Item implements Charged {

    private static final ResourceLocation UUID_ATK_ES = JTTWEngine.id("three_pointed_spear_atk_es");
    private static final ResourceLocation UUID_ATK_OTHER = JTTWEngine.id("three_pointed_spear_atk");
    private static final ResourceLocation UUID_REACH = JTTWEngine.id("three_pointed_spear_reach");

    // Combo tracking — stored per-entity in persistent data
    private static final String TAG_COMBO = "jttw_spear_combo";
    private static final String TAG_COMBO_TICK = "jttw_spear_combo_tick";
    private static final int COMBO_RESET_TICKS = 60; // 3 seconds to reset combo

    public ThreePointedSpearItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();

        // Damage is applied generically here — Erlang Shen gets full 100%,
        // others get 20%. Detection is done at runtime in hurtEnemy.
        // For attribute display, show the base (lower) value.
        // The Erlang Shen bonus is applied via ErlangShenAbilityHandler event.
        b.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(UUID_ATK_OTHER,
                0.20 * Armaments.refineMultiplier(stack), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.MAINHAND);

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

        // Eye of Heaven: a share of the blow as magic
        if (!player.level().isClientSide() && eyeOpen(player)) {
            eyeStrike(player, target, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }

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
    public static final int MIN_CHARGE_TICKS = 5;   // 0.25s minimum
    private static final int MAX_CHARGE_TICKS = 40;  // 2s for full power
    private static final double LUNGE_BASE = 8.0;

    @Override
    @Nullable
    public Charge charge(ItemStack stack, Player user, int ticks, java.util.Set<String> nodes) {
        boolean throwing = user.isShiftKeyDown()
                && com.jttw.engine.character.erlang.ErlangShenSkillTree.TREE.highestTier(nodes, "es_spear") >= 1;
        // the throw is ready or not; the thrust grows from its least to its greatest
        return throwing ? new Charge(Math.min(1f, ticks / (float) THROW_MIN_CHARGE_TICKS), ticks >= THROW_MIN_CHARGE_TICKS)
                : Charge.between(ticks, MIN_CHARGE_TICKS, MAX_CHARGE_TICKS);
    }

    private static final double LUNGE_MAX = 20.0;
    private static final double WRATH_LUNGE_BONUS = 1.5;

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
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

        int chargeTime = getUseDuration(stack, entity) - timeLeft;

        // Cooldown check
        if (sp.getCooldowns().isOnCooldown(this)) return;

        // Spear Mastery I: sneaking turns the release into a throw
        if (sp.isShiftKeyDown() && isErlangShen(sp)
                && com.jttw.engine.progression.skilltree.SkillTreeData.getEsSpearTier(sp) >= 1) {
            if (chargeTime >= THROW_MIN_CHARGE_TICKS) throwSpear(sp, stack);
            return;
        }

        // Let go before the thrust is ready: a sweeping slash
        if (chargeTime < MIN_CHARGE_TICKS) {
            slash(sp, sp.getUsedItemHand());
            sp.getCooldowns().addCooldown(this, inWrath(sp) ? SLASH_WRATH_COOLDOWN_TICKS : SLASH_COOLDOWN_TICKS);
            return;
        }

        boolean esPlayer = isErlangShen(sp);
        boolean inWrath = inWrath(sp);

        // Charge power: 0.0 → 1.0
        float chargePower = Charge.between(chargeTime, MIN_CHARGE_TICKS, MAX_CHARGE_TICKS).fraction();

        // Lunge distance scales with charge
        double lungeBase = LUNGE_BASE + (LUNGE_MAX - LUNGE_BASE) * chargePower;
        if (inWrath) lungeBase *= WRATH_LUNGE_BONUS;

        // Damage: 150% at min charge → 250% at full charge
        float atk = (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float thrustDamage = atk * (1.5f + chargePower * 1.0f);

        // Speed scaling: +5% damage per block/second
        double speed = sp.getDeltaMovement().horizontalDistance() * 20.0;
        thrustDamage *= (float) (1.0 + speed * 0.05);

        boolean eyeActive = esPlayer && com.jttw.engine.character.erlang.ErlangShenAbilityHandler.isEyeActive(sp.getUUID());

        // Lunge forward
        net.minecraft.world.phys.Vec3 look = sp.getLookAngle().normalize();
        sp.setDeltaMovement(sp.getDeltaMovement().add(look.scale(lungeBase * 0.25)));
        sp.hurtMarked = true;

        // Hit everything in the thrust's area: a cone ahead, as long as the lunge
        net.minecraft.world.phys.Vec3 start = sp.getEyePosition();
        net.minecraft.world.phys.AABB searchBox = sp.getBoundingBox()
                .expandTowards(look.scale(lungeBase)).inflate(1.0);

        net.minecraft.server.level.ServerLevel serverLevel = sp.serverLevel();
        java.util.List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, searchBox, e -> e != sp && e.isAlive());

        // Spear Mastery II: +25% thrust damage
        if (esPlayer && com.jttw.engine.progression.skilltree.SkillTreeData.getEsSpearTier(sp) >= 2) thrustDamage *= 1.25f;

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

        }

        // T3: shockwave at full charge + sprinting
        if (esPlayer && com.jttw.engine.progression.skilltree.SkillTreeData.getEsSpearTier(sp) >= 3 && chargePower >= 0.8f) {
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

    // ── Sweeping slash (right-click) ────────────────────────────────

    /** How far the slash reaches from the wielder, in blocks. */
    public static final double SLASH_REACH = 3.0;
    /** What the slash deals to each foe it sweeps, as a multiple of ATK. */
    public static final float SLASH_DAMAGE_MULTIPLIER = 1.0f;
    public static final int SLASH_COOLDOWN_TICKS = 20;
    public static final int SLASH_WRATH_COOLDOWN_TICKS = 10;
    /** The open Eye of Heaven adds this share of a spear blow as magic damage. */
    public static final float EYE_MAGIC_FRACTION = 0.3f;

    /**
     * Sweeps the foes around the wielder: the half circle ahead, or all round in Wrath.
     * Returns how many it struck.
     */
    public static int slash(net.minecraft.server.level.ServerPlayer sp, net.minecraft.world.InteractionHand hand) {
        boolean wrath = inWrath(sp);
        boolean eye = eyeOpen(sp);
        float damage = (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE) * SLASH_DAMAGE_MULTIPLIER;
        // the arc lies flat, wherever he looks up or down
        net.minecraft.world.phys.Vec3 facing = net.minecraft.world.phys.Vec3.directionFromRotation(0f, sp.getYRot());
        net.minecraft.world.phys.AABB box = sp.getBoundingBox().inflate(SLASH_REACH + 1.0, 1.0, SLASH_REACH + 1.0);
        int struck = 0;
        for (LivingEntity e : sp.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(e, sp))) {
            net.minecraft.world.phys.Vec3 to = new net.minecraft.world.phys.Vec3(e.getX() - sp.getX(), 0, e.getZ() - sp.getZ());
            if (to.length() > SLASH_REACH + e.getBbWidth() / 2) continue;
            if (!wrath && to.dot(facing) < 0) continue; // behind him: only Wrath's full circle reaches
            int oldInvuln = e.invulnerableTime;
            e.invulnerableTime = 0;
            boolean hit = e.hurt(sp.damageSources().playerAttack(sp), damage);
            e.invulnerableTime = oldInvuln;
            if (!hit) continue;
            if (eye) eyeStrike(sp, e, damage);
            e.knockback(0.4, -to.x, -to.z);
            struck++;
        }

        // the sweep's trail: a half circle of sweeps ahead, or a full ring
        for (int deg = wrath ? -180 : -90; deg <= (wrath ? 150 : 90); deg += 30) {
            net.minecraft.world.phys.Vec3 d = net.minecraft.world.phys.Vec3.directionFromRotation(0f, sp.getYRot() + deg);
            sp.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                    sp.getX() + d.x * 2.0, sp.getY() + sp.getBbHeight() * 0.5, sp.getZ() + d.z * 2.0, 1, 0, 0, 0, 0);
        }
        sp.swing(hand, true);
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, wrath ? 0.7F : 0.9F);
        return struck;
    }

    private static boolean isFoe(LivingEntity e, Player p) {
        return e != p && e.isAlive() && !e.isAlliedTo(p)
                && !(e instanceof Player other && (other.isCreative() || other.isSpectator()))
                && !com.jttw.engine.character.erlang.XiaotianQuanManager.isAllyXtq(e, p.getUUID());
    }

    /** Erlang Shen with his Eye of Heaven open. */
    public static boolean eyeOpen(Player p) {
        return isErlangShen(p) && com.jttw.engine.character.erlang.ErlangShenAbilityHandler.isEyeActive(p.getUUID());
    }

    /**
     * The Eye's share of a blow of {@code blow}, as magic from the wielder: magic resistance
     * reduces it, and Erlang Shen's spear bonus raises it as it does the blow.
     */
    static void eyeStrike(Player p, LivingEntity target, float blow) {
        if (!target.isAlive()) return;
        int oldInvuln = target.invulnerableTime;
        target.invulnerableTime = 0;
        target.hurt(p.damageSources().indirectMagic(p, p), blow * EYE_MAGIC_FRACTION);
        target.invulnerableTime = oldInvuln;
    }

    // ── Wrath (Zhàn Shén) ────────────────────────────────────────────

    private static final ResourceLocation WRATH_SPEED = JTTWEngine.id("three_pointed_spear_wrath_speed");

    /** Erlang Shen with Zhàn Shén I, below 40% health. */
    public static boolean inWrath(Player p) {
        return isErlangShen(p) && p.getHealth() / p.getMaxHealth() < 0.4f
                && com.jttw.engine.progression.skilltree.SkillTreeData.getEsWrathTier(p) >= 1;
    }

    /** Wrath's extra attack speed with the spear: +15/25/35% by Zhàn Shén tier. */
    public static double wrathSpeed(int tier) {
        return tier >= 3 ? 0.35 : tier >= 2 ? 0.25 : tier >= 1 ? 0.15 : 0.0;
    }

    /** Gives or takes Wrath's attack speed, as he is in Wrath with the spear in hand or not. */
    public static void updateWrathSpeed(Player p) {
        var speed = p.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null) return;
        double want = p.getMainHandItem().getItem() instanceof ThreePointedSpearItem && inWrath(p)
                ? wrathSpeed(com.jttw.engine.progression.skilltree.SkillTreeData.getEsWrathTier(p)) : 0.0;
        AttributeModifier has = speed.getModifier(WRATH_SPEED);
        if (has != null && has.amount() == want) return;
        if (has != null) speed.removeModifier(WRATH_SPEED);
        if (want > 0) {
            speed.addTransientModifier(new AttributeModifier(WRATH_SPEED, want, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Keeps Wrath's attack speed in step, each tick on the server. */
    @net.neoforged.fml.common.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
    public static final class WrathSpeed {
        private WrathSpeed() {}

        @net.neoforged.bus.api.SubscribeEvent
        public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
            if (!event.getEntity().level().isClientSide()) updateWrathSpeed(event.getEntity());
        }
    }

    /** How long the throw must be drawn, like a trident. */
    public static final int THROW_MIN_CHARGE_TICKS = 10;
    /** Hardness the throw can tear through per point of the thrower's ATK. */
    public static final double THROW_BUDGET_PER_ATK = 4.0;
    /** What the thrown spear deals to everything it passes through, as a multiple of ATK. */
    public static final float THROW_DAMAGE_MULTIPLIER = 2.5f;

    private void throwSpear(net.minecraft.server.level.ServerPlayer sp, ItemStack stack) {
        if (SpearReturn.isOut(sp)) {
            sp.sendSystemMessage(Component.literal("§7[JTTW] Your other spear has not returned yet."));
            return;
        }
        float atk = (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack thrownStack = stack.copy();
        com.jttw.engine.entity.ThrownSpearEntity thrown = new com.jttw.engine.entity.ThrownSpearEntity(
                sp.level(), sp, thrownStack, sp.getLookAngle(),
                atk * THROW_BUDGET_PER_ATK, atk * THROW_DAMAGE_MULTIPLIER);
        sp.level().addFreshEntity(thrown);
        SpearReturn.recordThrow(sp, thrownStack, thrown);
        stack.setCount(0); // it is in flight now
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                net.minecraft.sounds.SoundEvents.TRIDENT_THROW, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Three-Pointed Double-Edged Spear").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§e+100% damage (Erlang Shen)"));
        tooltip.add(Component.literal("§e+20% damage (others)"));
        tooltip.add(Component.literal("§b+5 reach (Erlang Shen)"));
        tooltip.add(Component.literal("§aEvery 3rd hit: Triple Strike"));
        tooltip.add(Component.literal("§7Right-click: Sweeping Slash, 100% ATK to all in a half circle ahead"));
        tooltip.add(Component.literal("§7Hold right-click: Thrust, 150% to 250% ATK by charge"));
        tooltip.add(Component.literal("§7Hits everything in front of you"));
        tooltip.add(Component.literal("§7Sneak + hold, release: Throw (Erlang Shen, Spear Mastery I)"));
        tooltip.add(Component.literal("§dEye of Heaven open: +30% of each blow as magic (Erlang Shen)"));
        tooltip.add(Component.literal("§cWrath: faster attacks, the slash sweeps all round (Erlang Shen)"));
    }

    public static boolean isErlangShen(Player player) {
        String outcome = player.getPersistentData()
                .getString(AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "eye_of_heaven".equals(outcome);
    }
}
