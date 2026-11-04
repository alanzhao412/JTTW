package com.jttw.engine.item;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.progression.level.PlayerLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What Star Shards forge, in the Eight Trigrams Furnace, for anyone (no gate on their recipes) and in
 * anyone's hands:
 * <ul>
 *   <li><b>Starfall Talisman</b> (星陨符): burned, it calls a falling star down on the creature its
 *       burner looks at, within {@value #STARFALL_RANGE} blocks. It strikes every foe within
 *       {@value #STARFALL_RADIUS} blocks of where it lands for half a typical character's health at
 *       the burner's level ({@link MobScaling#typicalHealth}): two kill a foe of their level.</li>
 *   <li><b>Star-Essence Elixir</b> (星华丹): eaten, it heals half its eater's health, refills half
 *       their Qi, washes off every harmful effect of vanilla's (the blindness, poison, slowness and
 *       nausea Heaven's host lays on), and lets them see by starlight for a minute; then its light
 *       takes {@value #ELIXIR_COOLDOWN} ticks to settle before another will take.</li>
 * </ul>
 * The talisman's strike is measured against the typical character of its burner's own level rather
 * than by the burner's class power: a shared craft, it reads the same for every class.
 */
public class StarCraftItem extends Item {

    public enum Kind { STARFALL, ELIXIR }

    public static final int STARFALL_RANGE = 32, STARFALL_COOLDOWN = 40;
    public static final double STARFALL_RADIUS = 4.0;
    /** The star's blow, as a share of a typical character's health at the burner's level: two kill. */
    public static final double STARFALL_SHARE = 0.5;
    /** The elixir: the share of health and of Qi it gives back, and how long until another takes. */
    public static final float ELIXIR_HEAL = 0.5f, ELIXIR_QI = 0.5f;
    public static final int ELIXIR_COOLDOWN = 600, ELIXIR_SIGHT = 1200;

    private final Kind kind;

    public StarCraftItem(Kind kind) {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
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
        if (kind == Kind.ELIXIR) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
        LivingEntity target = HeavenlyCraftItem.lookedAt(sp, STARFALL_RANGE);
        if (target == null) {
            sp.displayClientMessage(Component.literal("§7No creature under the star to call it down on."), true);
            return InteractionResultHolder.fail(stack);
        }
        starfall(sp, target);
        sp.getCooldowns().addCooldown(this, STARFALL_COOLDOWN);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    /** The star's blow for this burner: half a typical character's health at their level. */
    public static float starfallDamage(Player p) {
        int level = com.jttw.engine.character.divinebeast.BeastEvolution.poweredLevel(p, PlayerLevel.getLevel(p));
        return (float) (STARFALL_SHARE * MobScaling.typicalHealth(level));
    }

    /** Whom the star spares: its burner, their allies and pets, and players other than the one aimed at. */
    private static boolean struck(LivingEntity e, ServerPlayer p, LivingEntity target) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof OwnableEntity pet && p.getUUID().equals(pet.getOwnerUUID())) return false;
        if (com.jttw.engine.character.erlang.XiaotianQuanManager.isAllyXtq(e, p.getUUID())) return false;
        if (e instanceof Player other) return other == target && !other.isCreative();
        return true;
    }

    /**
     * The falling star: a streak of light down out of the sky onto the creature, and a burst where it
     * lands that strikes every foe within {@link #STARFALL_RADIUS} blocks once (magic, from the burner),
     * throwing them back a little. Blocks are untouched and nothing is set alight. Returns those struck.
     */
    public static List<LivingEntity> starfall(ServerPlayer p, LivingEntity target) {
        ServerLevel level = p.serverLevel();
        Vec3 at = target.position().add(0, target.getBbHeight() / 2, 0);
        // the streak: from high in the sky, slanting in from the burner's side
        Vec3 from = at.add(p.getLookAngle().multiply(-1, 0, -1).normalize().scale(10).add(0, 22, 0));
        for (int i = 0; i <= 40; i++) {
            Vec3 pt = from.lerp(at, i / 40.0);
            level.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 1, 0.05, 0.05, 0.05, 0.0);
            if (i % 4 == 0) level.sendParticles(ParticleTypes.FIREWORK, pt.x, pt.y, pt.z, 1, 0.1, 0.1, 0.1, 0.02);
        }
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 60, 0.3, 0.3, 0.3, 0.35);
        level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y, at.z, 40, STARFALL_RADIUS / 3, 0.4, STARFALL_RADIUS / 3, 0.08);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2.0f, 0.6f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.5f);

        float damage = starfallDamage(p);
        List<LivingEntity> hit = new ArrayList<>();
        AABB box = new AABB(at, at).inflate(STARFALL_RADIUS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.position().add(0, e.getBbHeight() / 2, 0).distanceTo(at) <= STARFALL_RADIUS + e.getBbWidth() / 2)) {
            if (!struck(e, p, target)) continue;
            e.hurt(p.damageSources().indirectMagic(p, p), damage);
            Vec3 away = e.position().subtract(target.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) e.knockback(0.5, -away.x, -away.z);
            hit.add(e);
        }
        return hit;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (kind != Kind.ELIXIR || !(entity instanceof ServerPlayer p)) return stack;
        drink(p);
        p.getCooldowns().addCooldown(this, ELIXIR_COOLDOWN);
        if (!p.getAbilities().instabuild) stack.shrink(1);
        return stack;
    }

    /** The elixir's work: half the health and half the Qi back, vanilla's harms washed off, starlight sight. */
    public static void drink(ServerPlayer p) {
        p.heal(p.getMaxHealth() * ELIXIR_HEAL);
        float max = com.jttw.engine.progression.qi.QiData.getMax(p);
        com.jttw.engine.progression.qi.QiData.setCurrent(p, Math.min(max, com.jttw.engine.progression.qi.QiData.getCurrent(p) + max * ELIXIR_QI));
        // vanilla's harmful effects only: the mod's own (the Fiery Gaze, the Samadhi fire...) keep their own rules
        List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> harms = new ArrayList<>();
        for (MobEffectInstance e : p.getActiveEffects()) {
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                    && e.getEffect().unwrapKey().map(k -> k.location().getNamespace().equals("minecraft")).orElse(false)) {
                harms.add(e.getEffect());
            }
        }
        harms.forEach(p::removeEffect);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ELIXIR_SIGHT, 0));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.2f);
        p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 24, 0.4, 0.6, 0.4, 0.04);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "item.jttw_engine." + (kind == Kind.STARFALL ? "starfall_talisman" : "star_essence_elixir");
        tooltip.add(Component.translatable(key + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(key + ".desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.jttw_engine.star_craft.desc").withStyle(ChatFormatting.AQUA));
    }
}
