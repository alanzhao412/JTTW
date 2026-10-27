package com.jttw.engine.village;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Mortals run from demons. A player of the Yaoguai class (the Bull Demon King, the Saurian Demon
 * King, the Grand Saint of Nine Spirits) who comes within {@value #RADIUS} blocks of villagers sets
 * them fleeing, as vanilla villagers flee zombies: the Tang villages' mortals (their own avoidance)
 * and vanilla's villagers too (they take the demon for a hostile and panic:
 * {@code VillagerHostilesSensorMixin}). A mortal of the Tang villages will not trade with one
 * either. The merchants of the supernatural world, the ghost market and Heaven's, deal with demons
 * as with anyone.
 *
 * <p>The novel's demons go among men in disguise (Lady White Bone as a village girl, the Yellow
 * Robe as a son-in-law): a Yaoguai wearing a mortal or animal shape through the transformation
 * arts ({@link #disguised}: another player's likeness, a block, or any of vanilla's creatures that
 * is not a monster) frightens no one, and may trade. A demon's own true shape (the mod's creatures)
 * and the shape of a monster fool nobody; and a disguised demon who strikes a villager is
 * <b>revealed</b> for {@value #REVEALED_TICKS} ticks, the disguise no help till it passes.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class YaoguaiFear {

    private YaoguaiFear() {}

    /** How near a demon must come for mortals to run. */
    public static final float RADIUS = 12;
    /** How long a demon who strikes a villager stays known for what it is, in ticks (a minute). */
    public static final int REVEALED_TICKS = 1200;
    public static final String KEY_REVEALED = JTTWEngine.MOD_ID + ":yaoguai_revealed_until";

    public static boolean isYaoguai(Player p) {
        return PlayerLevel.getProgressionClass(p) == ProgressionClass.YAOGUAI;
    }

    /** Whether {@code p} wears a shape that passes among mortals: another player's, a thing's, or a vanilla creature's that is no monster. */
    public static boolean disguised(Player p) {
        TransformationData form = TransformationManager.getTransformation(p);
        if (form == null) return false;
        if (form.isPlayerForm() || form.isBlockForm()) return true;
        var type = form.getEntityType();
        return type != null && "minecraft".equals(form.getEntityId().getNamespace()) && type.getCategory() != MobCategory.MONSTER;
    }

    /** Whether {@code p} has lately struck a villager, and so is known for what they are. */
    public static boolean revealed(Player p) {
        return p.getPersistentData().getLong(KEY_REVEALED) > p.level().getGameTime();
    }

    /** Whether mortals take {@code p} for a demon: a Yaoguai, undisguised or revealed (a spectator is unseen). */
    public static boolean dreaded(Player p) {
        return isYaoguai(p) && !p.isSpectator() && (!disguised(p) || revealed(p));
    }

    /** Whether {@code mortal} runs from {@code other}: a dreaded demon within {@link #RADIUS}. */
    public static boolean frightens(LivingEntity mortal, LivingEntity other) {
        return other instanceof Player p && dreaded(p) && p.distanceToSqr(mortal) <= RADIUS * RADIUS;
    }

    /** A demon who strikes a villager or a village's guardian is revealed. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p) || !isYaoguai(p)) return;
        if (event.getEntity() instanceof AbstractVillager || event.getEntity() instanceof IronGolem) {
            p.getPersistentData().putLong(KEY_REVEALED, p.level().getGameTime() + REVEALED_TICKS);
        }
    }
}
