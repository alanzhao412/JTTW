package com.jttw.engine.item;

import com.jttw.engine.character.wukong.WukongCheckpoints;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.registry.ModAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Immortality Peach — a divine consumable from the Heavenly Peach Garden.
 *
 * <p>Everyone: restores 3 hunger bars + refills 4700 Qi.
 * <p>Wukong (first eat only): +4700 max Qi, projectile immunity,
 * +48 physical resistance, +40 magic resistance — permanently.
 */
public class ImmortalPeachItem extends Item {

    public static final String KEY_PEACH_CONSUMED = "jttw_peach_consumed";
    private static final float QI_REFILL = 4700f;
    public static final float QI_POOL_BONUS = 4700f;
    private static final double PRES_BONUS = 48.0;
    private static final double MRES_BONUS = 40.0;

    // Ids kept from the armor/toughness version so it can be removed from old saves
    private static final UUID MOD_ARMOR_UUID =
            UUID.nameUUIDFromBytes("jttw_peach_armor".getBytes());
    private static final UUID MOD_TOUGHNESS_UUID =
            UUID.nameUUIDFromBytes("jttw_peach_toughness".getBytes());

    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(6) // 3 hunger bars
            .saturationMod(1.2F)
            .alwaysEat()
            .build();

    public ImmortalPeachItem() {
        super(new Item.Properties()
                .rarity(Rarity.EPIC)
                .food(FOOD)
                .stacksTo(16));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return result;

        // Universal effect: refill 4700 Qi
        QiData.restore(player, QI_REFILL);
        NetworkHandler.sendToPlayer(player,
                new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        // Wukong first-eat bonus (Wukong only, not the other Primordials)
        if (com.jttw.engine.character.wukong.WukongCharacter.is(player)) {
            CompoundTag pd = player.getPersistentData();
            if (!pd.getBoolean(KEY_PEACH_CONSUMED)) {
                pd.putBoolean(KEY_PEACH_CONSUMED, true);
                applyWukongBonus(player);
            }
        }

        return result;
    }

    private void applyWukongBonus(ServerPlayer player) {
        // +4700 max Qi
        float newMax = QiData.getMax(player) + QI_POOL_BONUS;
        QiData.setMax(player, newMax);
        QiData.restore(player, QI_POOL_BONUS); // top up with the bonus
        NetworkHandler.sendToPlayer(player,
                new SyncQiPacket(QiData.getCurrent(player), newMax));

        applyResistances(player);

        // Projectile immunity flag (checked in damage handler)
        player.getPersistentData().putBoolean("jttw_projectile_immune", true);

        player.sendSystemMessage(Component.literal(
                "§6[JTTW] §eThe Immortality Peach floods your body with celestial energy!"
                + "\n§b+4700 Max Qi §r| §7+48 Physical Resistance §r| §7+40 Magic Resistance §r| §dProjectile Immunity"));

        // Set immortal peaches progression flag (for full immortality system)
        ImmortalItems.onPeachConsumedByWukong(player);

        // Mark checkpoint as complete
        WukongCheckpoints.complete(player, WukongCheckpoints.Checkpoint.IMMORTAL_PEACHES);
    }

    /**
     * Re-apply the peach's resistance modifiers after death (new player entity).
     * Called from LevelManager.onPlayerClone.
     */
    public static void reapplyPeachModifiers(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(KEY_PEACH_CONSUMED)) return;
        applyResistances(player);
    }

    private static void applyResistances(ServerPlayer player) {
        setMod(player.getAttribute(ModAttributes.PHYSICAL_RESISTANCE.get()), MOD_ARMOR_UUID,
                "JTTW Peach Physical Resistance", PRES_BONUS);
        setMod(player.getAttribute(ModAttributes.MAGIC_RESISTANCE.get()), MOD_TOUGHNESS_UUID,
                "JTTW Peach Magic Resistance", MRES_BONUS);
        // The armor/toughness version of this bonus, on players who still carry it
        removeMod(player.getAttribute(Attributes.ARMOR), MOD_ARMOR_UUID);
        removeMod(player.getAttribute(Attributes.ARMOR_TOUGHNESS), MOD_TOUGHNESS_UUID);
    }

    private static void setMod(AttributeInstance inst, UUID id, String name, double amount) {
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    private static void removeMod(AttributeInstance inst, UUID id) {
        if (inst != null) inst.removeModifier(id);
    }
}
