package com.jttw.engine.character.divinebeast;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.registry.ModEffects;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A divine beast's true form: for a while (20, 30 or 45 seconds by tier) the player is the beast
 * entire (the White Tiger, and in time the Dragon, the Black Tortoise, the Vermilion Bird), with
 * three times their health (and all of it, at once), two and a half times their strength, swifter,
 * not to be knocked aside. It costs two fifths of their Qi, and rests ten minutes from its taking.
 * When it ends (its time out, or ended early by its key) they are exhausted a minute: slowed,
 * weakened, their Qi not coming back, no ability answering.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class TrueForm {

    private TrueForm() {}

    public static final String KEY_UNTIL = "jttw_true_until", KEY_READY = "jttw_true_ready", KEY_FORM = "jttw_true_form";
    /** Ten minutes from its taking before it can be taken again. */
    public static final int COOLDOWN = 12_000;
    /** The minute of exhaustion after. */
    public static final int EXHAUSTION = 1_200;
    /** Its cost: this share of all their Qi. */
    public static final float QI_SHARE = 0.4f;
    /** What it multiplies, as MULTIPLY_TOTAL modifiers: health x3, attack x2.5, speed x1.3. */
    public static final double HEALTH = 2.0, ATTACK = 1.5, SPEED = 0.3;

    public static int seconds(int tier) {
        return tier >= 3 ? 45 : tier == 2 ? 30 : 20;
    }

    public static boolean active(Player p) {
        return p.getPersistentData().getLong(KEY_UNTIL) > p.level().getGameTime();
    }

    public static boolean exhausted(Player p) {
        return p.hasEffect(ModEffects.EXHAUSTED.get());
    }

    /**
     * Its key: takes the true form (if learned, rested, paid for, and there is room for so great a
     * body), or, in it, ends it early. Returns whether it changed anything.
     */
    public static boolean toggle(ServerPlayer p, EntityType<?> form, int tier, String abilityId) {
        if (active(p)) {
            end(p);
            return true;
        }
        if (tier < 1 || exhausted(p)) return false;
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong(KEY_READY)) return false;
        // so great a body needs room: in a narrow place, it cannot be taken
        if (!p.level().noCollision(p, form.getDimensions().makeBoundingBox(p.position()))) {
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 0.6f);
            return false;
        }
        if (!QiData.spend(p, QiData.getMax(p) * QI_SHARE)) return false;
        NetworkHandler.sendToPlayer(p, new SyncQiPacket(QiData.getCurrent(p), QiData.getMax(p)));
        CompoundTag pd = p.getPersistentData();
        pd.putLong(KEY_UNTIL, now + seconds(tier) * 20L);
        pd.putLong(KEY_READY, now + COOLDOWN);
        pd.putString(KEY_FORM, EntityType.getKey(form).toString());
        TransformationManager.transform(p, form);
        boost(p, true);
        p.setHealth(p.getMaxHealth());
        NetworkHandler.sendToPlayer(p, new SyncCooldownPacket(p.getUUID(), abilityId, COOLDOWN, COOLDOWN));
        // its coming: a roar, a flash of silver light about it
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 2.0f, 0.55f);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 1.6f);
        p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.2, p.getZ(), 60, 1.2, 1.0, 1.2, 0.12);
        p.serverLevel().sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1.2, p.getZ(), 1, 0, 0, 0, 0);
        return true;
    }

    private static void boost(ServerPlayer p, boolean on) {
        PassiveModifiers.set(p, Attributes.MAX_HEALTH, "jttw_true_health", on ? HEALTH : 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        PassiveModifiers.set(p, Attributes.ATTACK_DAMAGE, "jttw_true_attack", on ? ATTACK : 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        PassiveModifiers.set(p, Attributes.MOVEMENT_SPEED, "jttw_true_speed", on ? SPEED : 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        PassiveModifiers.add(p, Attributes.KNOCKBACK_RESISTANCE, "jttw_true_steady", on ? 1.0 : 0);
    }

    /** It ends: back to themselves, and exhausted. */
    public static void end(ServerPlayer p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(KEY_UNTIL)) return;
        String form = pd.getString(KEY_FORM);
        pd.remove(KEY_UNTIL);
        pd.remove(KEY_FORM);
        boost(p, false);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        TransformationData data = TransformationManager.getTransformation(p);
        if (data != null && data.getEntityId() != null && data.getEntityId().toString().equals(form)) TransformationManager.demorph(p);
        if (!p.isAlive()) return;
        p.addEffect(new MobEffectInstance(ModEffects.EXHAUSTED.get(), EXHAUSTION, 0, false, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EXHAUSTION, 1, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EXHAUSTION, 1, false, false, false));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(KEY_UNTIL)) return;
        TransformationData data = TransformationManager.getTransformation(p);
        boolean stillTheBeast = data != null && data.getEntityId() != null && data.getEntityId().toString().equals(pd.getString(KEY_FORM));
        if (!active(p) || !stillTheBeast) {
            end(p);
            return;
        }
        boost(p, true); // its strength is not saved with them: kept up while it lasts
        // its majesty about it: a faint silver light rising off it
        if (p.tickCount % 3 == 0) {
            float w = p.getBbWidth(), h = p.getBbHeight();
            p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + h * 0.5, p.getZ(), 1,
                    w * 0.5, h * 0.4, w * 0.5, 0.01);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && p.getPersistentData().contains(KEY_UNTIL)) {
            p.getPersistentData().remove(KEY_UNTIL);
            p.getPersistentData().remove(KEY_FORM);
            boost(p, false);
        }
    }
}
