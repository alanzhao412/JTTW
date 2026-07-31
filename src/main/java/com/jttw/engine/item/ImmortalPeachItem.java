package com.jttw.engine.item;

import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.SyncQiPacket;
import com.jttw.engine.progression.PlayerLevel;
import com.jttw.engine.progression.ProgressionClass;
import com.jttw.engine.progression.QiData;
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
 * +12 armor, +10 armor toughness — permanently.
 */
public class ImmortalPeachItem extends Item {

    private static final String KEY_PEACH_CONSUMED = "jttw_peach_consumed";
    private static final float QI_REFILL = 4700f;
    private static final float QI_POOL_BONUS = 4700f;
    private static final double ARMOR_BONUS = 12.0;
    private static final double TOUGHNESS_BONUS = 10.0;

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

        // Wukong first-eat bonus
        ProgressionClass cls = PlayerLevel.getProgressionClass(player);
        if (cls == ProgressionClass.PRIMORDIAL) {
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

        // +12 Armor
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(MOD_ARMOR_UUID);
            armor.addPermanentModifier(new AttributeModifier(
                    MOD_ARMOR_UUID, "JTTW Peach Armor",
                    ARMOR_BONUS, AttributeModifier.Operation.ADDITION));
        }

        // +10 Armor Toughness
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null) {
            toughness.removeModifier(MOD_TOUGHNESS_UUID);
            toughness.addPermanentModifier(new AttributeModifier(
                    MOD_TOUGHNESS_UUID, "JTTW Peach Toughness",
                    TOUGHNESS_BONUS, AttributeModifier.Operation.ADDITION));
        }

        // Projectile immunity flag (checked in damage handler)
        player.getPersistentData().putBoolean("jttw_projectile_immune", true);

        player.sendSystemMessage(Component.literal(
                "§6[JTTW] §eThe Immortality Peach floods your body with celestial energy!"
                + "\n§b+4700 Max Qi §r| §7+12 Armor §r| §7+10 Toughness §r| §dProjectile Immunity"));

        // Set immortal peaches progression flag (for full immortality system)
        ImmortalItems.onPeachConsumedByWukong(player);

        // Mark checkpoint as complete
        WukongCheckpoints.complete(player, WukongCheckpoints.Checkpoint.IMMORTAL_PEACHES);
    }

    /**
     * Re-apply peach armor/toughness modifiers after death (new player entity).
     * Called from LevelManager.onPlayerClone.
     */
    public static void reapplyPeachModifiers(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean("jttw_peach_consumed")) return;

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null && armor.getModifier(MOD_ARMOR_UUID) == null) {
            armor.addPermanentModifier(new AttributeModifier(
                    MOD_ARMOR_UUID, "JTTW Peach Armor",
                    ARMOR_BONUS, AttributeModifier.Operation.ADDITION));
        }

        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null && toughness.getModifier(MOD_TOUGHNESS_UUID) == null) {
            toughness.addPermanentModifier(new AttributeModifier(
                    MOD_TOUGHNESS_UUID, "JTTW Peach Toughness",
                    TOUGHNESS_BONUS, AttributeModifier.Operation.ADDITION));
        }
    }
}
