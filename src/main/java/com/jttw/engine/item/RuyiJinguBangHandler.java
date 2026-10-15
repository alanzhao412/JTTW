package com.jttw.engine.item;

import net.minecraft.resources.ResourceLocation;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.reincarnation.AlignmentData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Manages Wukong's reach bonus when holding the Ruyi Jingu Bang.
 * Applied/removed each tick based on mainhand + outcome + mode.
 */
public final class RuyiJinguBangHandler {
    private RuyiJinguBangHandler() {}

    private static final ResourceLocation UUID_REACH = JTTWEngine.id("ruyi_jingu_bang_reach");
    private static final double REACH_NORMAL = 5.0;       // 3 base + 5 = 8
    private static final double REACH_QI_EMPOWERED = 5.0; // 3 base + 5 = 8
    private static final double REACH_LARGE = 10.0;       // 3 base + 10 = 13
    private static final double MARTIAL_REACH_BONUS = 3.0; // Martial Arts II: +3 extra
    private static final double REACH_SPEAR_ES = 5.0;     // Three-Pointed Spear: +5 for Erlang Shen

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {

        Player player = event.getEntity();
        AttributeInstance reach = player.getAttribute(NeoForgeMod.ENTITY_REACH.get());
        if (reach == null) return;

        double desiredReach = 0.0;

        if (player.getMainHandItem().getItem() instanceof RuyiJinguBangItem) {
            boolean isWukong = "celestial_primate".equals(
                    player.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME));

            if (isWukong) {
                if (RuyiJinguBangItem.isLargeMode(player.getMainHandItem())) {
                    desiredReach = REACH_LARGE;
                } else if (RuyiJinguBangItem.isQiEmpowered(player.getMainHandItem())) {
                    desiredReach = REACH_QI_EMPOWERED;
                } else {
                    desiredReach = REACH_NORMAL;
                }
                // Martial Arts II: +3 extra reach on the staff
                if (SkillTreeData.getMartialTier(player) >= 2) {
                    desiredReach += MARTIAL_REACH_BONUS;
                }
            }
        }

        // Three-Pointed Spear: +5 reach for Erlang Shen
        if (player.getMainHandItem().getItem() instanceof ThreePointedSpearItem) {
            if (ThreePointedSpearItem.isErlangShen(player)) {
                desiredReach = REACH_SPEAR_ES;
            }
        }

        AttributeModifier existing = reach.getModifier(UUID_REACH);
        double currentReach = (existing != null) ? existing.getAmount() : 0.0;

        if (desiredReach > 0 && currentReach != desiredReach) {
            // Remove old and apply new
            if (existing != null) reach.removeModifier(UUID_REACH);
            reach.addTransientModifier(new AttributeModifier(
                    UUID_REACH,
                    desiredReach, AttributeModifier.Operation.ADD_VALUE));
        } else if (desiredReach == 0 && existing != null) {
            reach.removeModifier(UUID_REACH);
        }
    }
}
