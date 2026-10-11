package com.jttw.engine.character.erlang;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.entity.XiaotianQuanEntity;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Erlang Shen's passive skills.
 *
 * <ul>
 *   <li><b>Celestial Hunt</b> I: Erlang Shen's hits mark the target for 10 s; he and Xiaotian
 *       Quan deal +15% damage to targets he has marked. II: +25%, and marked targets glow
 *       (visible through walls).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class ErlangShenPassives {

    private ErlangShenPassives() {}

    public static final String KEY_MARKED_BY = "jttw_hunt_marked_by";
    public static final String KEY_MARKED_UNTIL = "jttw_hunt_marked_until";
    public static final int MARK_TICKS = 200;

    public static float huntBonus(int tier) {
        return tier >= 2 ? 0.25f : tier == 1 ? 0.15f : 0f;
    }

    public static boolean isMarkedBy(LivingEntity target, ServerPlayer hunter) {
        CompoundTag pd = target.getPersistentData();
        return pd.hasUUID(KEY_MARKED_BY) && pd.getUUID(KEY_MARKED_BY).equals(hunter.getUUID())
                && pd.getLong(KEY_MARKED_UNTIL) > target.level().getGameTime();
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        ServerPlayer hunter;
        boolean byHound = false;
        if (attacker instanceof ServerPlayer sp) {
            hunter = sp;
        } else if (attacker instanceof XiaotianQuanEntity hound) {
            hunter = XiaotianQuanManager.resolveOwner(hound);
            byHound = true;
        } else {
            return;
        }
        if (hunter == null || Characters.of(hunter) != ErlangShenCharacter.INSTANCE) return;
        int tier = SkillTreeData.getEsHuntTier(hunter);
        LivingEntity target = event.getEntity();
        if (tier < 1 || target == hunter) return;

        if (isMarkedBy(target, hunter)) event.setAmount(event.getAmount() * (1f + huntBonus(tier)));
        if (byHound) return; // only Erlang Shen's own hits mark

        CompoundTag pd = target.getPersistentData();
        pd.putUUID(KEY_MARKED_BY, hunter.getUUID());
        pd.putLong(KEY_MARKED_UNTIL, target.level().getGameTime() + MARK_TICKS);
        if (tier >= 2) com.jttw.engine.world.PrivateGlow.show(hunter, target, MARK_TICKS); // marked for him alone
    }
}
