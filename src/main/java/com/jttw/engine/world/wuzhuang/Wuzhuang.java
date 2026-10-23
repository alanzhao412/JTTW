package com.jttw.engine.world.wuzhuang;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wuzhuang.TempleFolk;
import com.jttw.engine.item.SilkLinedTrayItem;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Mount Wuzhuang's rules: who is the temple's guest and who its thief, what the ginseng fruit does
 * once off its tree and once eaten.
 *
 * <ul>
 *   <li><b>Guests</b>: whoever has passed Zhenyuan's trial (he yielded to them) has his leave: the
 *       fruit is theirs to knock down, and his cranes no longer harry them. Kept by the soul (through
 *       death and into a new life), as karma is;</li>
 *   <li><b>thieves</b>: whoever brings a ripe fruit down without leave has stolen it (karma
 *       {@link Morality#STEAL_GINSENG}) and is the temple's enemy for {@value #THIEF_TICKS} ticks:
 *       every disciple, both youths and the Great Immortal near them turn on them, as Clear Wind and
 *       Bright Moon turned on the pilgrims;</li>
 *   <li><b>the fallen fruit</b> ({@link #fruitTick}): it dissolves in water; it sinks into the earth
 *       wherever it comes to rest, unless on something lined (wool, a carpet, a bed); falling from its
 *       tree it cannot be caught by hand, only in a Silk-Lined Tray held under it;</li>
 *   <li><b>eaten</b> ({@link #eat}): whoever eats one is made whole (healed, Regeneration II for 30 s,
 *       Absorption III for 2 min); the first a soul ever eats adds forty-seven thousand years to its
 *       life, as the novel has it: <b>+{@value #LONGEVITY_PERCENT}% maximum health</b>, for good.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Wuzhuang {

    private Wuzhuang() {}

    public static final String KEY_GUEST = "jttw_wuzhuang_guest", KEY_THIEF = "jttw_wuzhuang_thief_until",
            KEY_EATEN = "jttw_ginseng_eaten", KEY_KNOCKED = "jttw_ginseng_knocked";
    /** How long a thief is the temple's enemy. */
    public static final int THIEF_TICKS = 6000;
    /** How far the alarm carries. */
    public static final double ALARM = 48;
    public static final int LONGEVITY_PERCENT = 20;
    public static final ResourceLocation LONGEVITY = JTTWEngine.id("ginseng_longevity");

    // ── Guests and thieves ──────────────────────────────────────────────

    public static boolean isGuest(Player p) {
        return p.getPersistentData().getBoolean(KEY_GUEST);
    }

    /** Zhenyuan's leave: the fruit is theirs to take, his cranes leave them be. */
    public static void makeGuest(Player p) {
        p.getPersistentData().putBoolean(KEY_GUEST, true);
        p.getPersistentData().remove(KEY_THIEF);
    }

    public static boolean isThief(Player p) {
        return p.getPersistentData().getLong(KEY_THIEF) > p.level().getGameTime();
    }

    /** {@code p} has stolen a ginseng fruit: the deed, and the temple roused against them. */
    public static void stole(ServerPlayer p) {
        Morality.deed(p, Morality.STEAL_GINSENG, "you stole a ginseng fruit from the Temple of the Five Villages");
        markThief(p);
    }

    /** Marks {@code p} the temple's thief; its folk near them turn on them. Returns how many. */
    public static int markThief(Player p) {
        p.getPersistentData().putLong(KEY_THIEF, p.level().getGameTime() + THIEF_TICKS);
        int roused = 0;
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(ALARM), m -> m instanceof TempleFolk && m.isAlive())) {
            m.setTarget(p);
            if (m.getTarget() == p) roused++;
        }
        if (roused > 0) p.displayClientMessage(Component.translatable("zone.jttw_engine.mount_wuzhuang.thief"), false);
        return roused;
    }

    // ── The fallen fruit ────────────────────────────────────────────────

    /** Whether a block is lined, soft enough for the fruit to rest on: wool, a carpet, a bed. */
    public static boolean lined(BlockState s) {
        return s.is(BlockTags.WOOL) || s.is(BlockTags.WOOL_CARPETS) || s.getBlock() instanceof BedBlock;
    }

    /** Whether {@code p} holds a Silk-Lined Tray, in either hand. */
    public static boolean holdsTray(Player p) {
        return p.getMainHandItem().getItem() instanceof SilkLinedTrayItem || p.getOffhandItem().getItem() instanceof SilkLinedTrayItem;
    }

    /**
     * A ginseng fruit lying or falling in the world, each tick: dissolved in water, caught in a tray
     * held under it, at rest on something lined, or sunk into the earth. Returns true when it is gone
     * (its tick then goes no further).
     */
    public static boolean fruitTick(ItemEntity e) {
        Level level = e.level();
        if (level.isClientSide || !e.isAlive()) return false;
        if (e.isInWater()) {
            if (level instanceof ServerLevel server) server.sendParticles(ParticleTypes.BUBBLE_POP, e.getX(), e.getY() + 0.2, e.getZ(), 20, 0.2, 0.2, 0.2, 0.02);
            level.playSound(null, e.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.BLOCKS, 1f, 1.2f);
            e.discard();
            return true;
        }
        boolean knocked = e.getPersistentData().getBoolean(KEY_KNOCKED);
        if (knocked) {
            for (Player p : level.getEntitiesOfClass(Player.class, e.getBoundingBox().inflate(2, 4, 2), Wuzhuang::holdsTray)) {
                if (Math.hypot(p.getX() - e.getX(), p.getZ() - e.getZ()) > 1.4) continue;
                if (e.getY() < p.getY() - 0.2 || e.getY() > p.getY() + p.getBbHeight() + 2.5) continue;
                if (!p.getInventory().add(e.getItem().copy())) continue;
                level.playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
                p.displayClientMessage(Component.translatable("item.jttw_engine.silk_lined_tray.caught"), true);
                e.discard();
                return true;
            }
        }
        if (!e.onGround()) return false;
        BlockPos at = e.blockPosition(), below = BlockPos.containing(e.getX(), e.getY() - 0.05, e.getZ());
        if (lined(level.getBlockState(at)) || lined(level.getBlockState(below))) {
            if (knocked) {
                e.getPersistentData().remove(KEY_KNOCKED);
                e.setPickUpDelay(10);
            }
            return false;
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(below)), e.getX(), e.getY() + 0.1, e.getZ(),
                    20, 0.15, 0.05, 0.15, 0.05);
        }
        level.playSound(null, at, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 1f, 0.7f);
        e.discard();
        return true;
    }

    // ── Eating it ───────────────────────────────────────────────────────

    /** {@code p} eats a ginseng fruit. Returns whether it was the first their soul ever ate (and so lengthened their life). */
    public static boolean eat(ServerPlayer p) {
        boolean first = !p.getPersistentData().getBoolean(KEY_EATEN);
        if (first) {
            p.getPersistentData().putBoolean(KEY_EATEN, true);
            applyLongevity(p);
            p.sendSystemMessage(Component.translatable("item.jttw_engine.ginseng_fruit.first"));
        } else {
            p.displayClientMessage(Component.translatable("item.jttw_engine.ginseng_fruit.again"), true);
        }
        p.setHealth(p.getMaxHealth());
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 2));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.6f);
        return first;
    }

    /** The fruit's lengthening of a soul's life: its maximum health, as long as it has eaten one. */
    public static void applyLongevity(Player p) {
        AttributeInstance health = p.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        health.removeModifier(LONGEVITY);
        if (p.getPersistentData().getBoolean(KEY_EATEN)) {
            health.addPermanentModifier(new AttributeModifier(LONGEVITY, LONGEVITY_PERCENT / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** The soul keeps its leave and its long life through death. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        var from = event.getOriginal().getPersistentData();
        var to = event.getEntity().getPersistentData();
        for (String key : new String[]{KEY_GUEST, KEY_EATEN}) if (from.getBoolean(key)) to.putBoolean(key, true);
        applyLongevity(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        applyLongevity(event.getEntity());
    }
}
