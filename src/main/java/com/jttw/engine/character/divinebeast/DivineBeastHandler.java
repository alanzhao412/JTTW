package com.jttw.engine.character.divinebeast;

import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.FormStage;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.JTTWEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Handles Divine Beast passive abilities:
 * <ul>
 *   <li>Bare-handed mining at tool tier based on {@link FormStage#harvestLevel}</li>
 *   <li>Domain-based stat buffs when in the player's elemental domain</li>
 * </ul>
 */
public final class DivineBeastHandler {
    private DivineBeastHandler() {}

    // -----------------------------------------------------------------------
    // Domain buff attribute modifiers
    // -----------------------------------------------------------------------

    private static final UUID UUID_DOMAIN_HP  = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678900");
    private static final UUID UUID_DOMAIN_STR = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID UUID_DOMAIN_DEF = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678902");

    private static final String MOD_DOMAIN_HP  = "jttw:domain_hp";
    private static final String MOD_DOMAIN_STR = "jttw:domain_str";
    private static final String MOD_DOMAIN_DEF = "jttw:domain_def";
    private static final String MOD_DOMAIN_MRES = "jttw:domain_mres";

    /** Domain buff: +20% to HP, ATK, physical and magic resistance when in the beast's domain. */
    private static final double DOMAIN_BUFF_MULTIPLIER = 0.20;

    /** Only re-check domain every N ticks to avoid per-tick biome lookups. */
    private static final int DOMAIN_CHECK_INTERVAL = 20;

    // -----------------------------------------------------------------------
    // Mining — HarvestCheck and BreakSpeed
    // -----------------------------------------------------------------------

    /**
     * Allow Divine Beasts to harvest blocks bare-handed up to their form stage's tier.
     */
    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (PlayerLevel.getProgressionClass(sp) != ProgressionClass.DIVINE_BEAST) return;
        // Only applies when mining bare-handed (empty main hand)
        if (!sp.getMainHandItem().isEmpty()) return;

        FormStage stage = FormStage.fromIndex(PlayerLevel.getFormStage(sp));
        BlockState state = event.getTargetBlock();

        if (canHarvestAtLevel(state, stage.harvestLevel)) {
            event.setCanHarvest(true);
        }
    }

    /**
     * Give Divine Beasts reasonable break speed when mining bare-handed.
     * Scales with form stage to feel like using the equivalent tool tier.
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (PlayerLevel.getProgressionClass(sp) != ProgressionClass.DIVINE_BEAST) return;
        if (!sp.getMainHandItem().isEmpty()) return;

        FormStage stage = FormStage.fromIndex(PlayerLevel.getFormStage(sp));
        BlockState state = event.getState();

        if (!canHarvestAtLevel(state, stage.harvestLevel)) return;

        // Base fist speed is 1.0. Scale it to match the equivalent tool tier.
        // Wood=2, Stone=4, Iron=6, Diamond=8, Netherite=9
        float tierSpeed = switch (stage.harvestLevel) {
            case 0 -> 2.0f;   // wood
            case 1 -> 4.0f;   // stone
            case 2 -> 6.0f;   // iron
            case 3 -> 8.0f;   // diamond
            case 4 -> 9.0f;   // netherite
            default -> 1.0f;
        };

        // Only boost if our tier speed is faster than what they already have
        if (tierSpeed > event.getNewSpeed()) {
            event.setNewSpeed(tierSpeed);
        }
    }

    /**
     * Check if a block can be harvested at the given harvest level.
     * Mirrors vanilla tool tier tag checks.
     */
    private static boolean canHarvestAtLevel(BlockState state, int harvestLevel) {
        // Blocks with hardness < 0 (bedrock) are never harvestable
        // Blocks that don't require correct tool can always be harvested
        if (!state.requiresCorrectToolForDrops()) return true;

        // Check each tier tag — if the block needs a higher tier than we have, deny
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL))  return harvestLevel >= 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL))     return harvestLevel >= 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL))    return harvestLevel >= 1;

        // Forge adds needs_netherite_tool for modded blocks
        // Check via string tag since it may not exist in all environments
        try {
            TagKey<net.minecraft.world.level.block.Block> netheriteTag =
                    BlockTags.create(new net.minecraft.resources.ResourceLocation("forge", "needs_netherite_tool"));
            if (state.is(netheriteTag)) return harvestLevel >= 4;
        } catch (Exception ignored) {}

        // Block requires correct tool but isn't tagged for any specific tier — wood suffices
        return true;
    }

    // -----------------------------------------------------------------------
    // Domain buff — tick-based biome/weather check
    // -----------------------------------------------------------------------

    /**
     * Every second, check if the Divine Beast player is in their domain
     * and apply or remove the stat buff accordingly.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % DOMAIN_CHECK_INTERVAL != 0) return;

        ProgressionClass cls = PlayerLevel.getProgressionClass(sp);
        if (cls != ProgressionClass.DIVINE_BEAST) {
            // Remove domain buffs if they somehow still exist
            removeDomainBuffs(sp);
            return;
        }

        DivineBeastLine line = PlayerLevel.getBeastLine(sp);
        if (line == null) {
            removeDomainBuffs(sp);
            return;
        }

        if (line.isInDomain(sp)) {
            applyDomainBuffs(sp);
        } else {
            removeDomainBuffs(sp);
        }
    }

    private static void applyDomainBuffs(ServerPlayer player) {
        applyDomainMod(player, Attributes.MAX_HEALTH, UUID_DOMAIN_HP, MOD_DOMAIN_HP, DOMAIN_BUFF_MULTIPLIER);
        applyDomainMod(player, Attributes.ATTACK_DAMAGE, UUID_DOMAIN_STR, MOD_DOMAIN_STR, DOMAIN_BUFF_MULTIPLIER);
        applyDomainMod(player, com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get(), UUID_DOMAIN_DEF, MOD_DOMAIN_DEF, DOMAIN_BUFF_MULTIPLIER);
        applyDomainMod(player, com.jttw.engine.registry.ModAttributes.MAGIC_RESISTANCE.get(), UUID_DOMAIN_DEF, MOD_DOMAIN_MRES, DOMAIN_BUFF_MULTIPLIER);
        removeDomainMod(player, Attributes.ARMOR, UUID_DOMAIN_DEF); // the old armor version
    }

    private static void removeDomainBuffs(ServerPlayer player) {
        removeDomainMod(player, Attributes.MAX_HEALTH, UUID_DOMAIN_HP);
        removeDomainMod(player, Attributes.ATTACK_DAMAGE, UUID_DOMAIN_STR);
        removeDomainMod(player, com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get(), UUID_DOMAIN_DEF);
        removeDomainMod(player, com.jttw.engine.registry.ModAttributes.MAGIC_RESISTANCE.get(), UUID_DOMAIN_DEF);
        removeDomainMod(player, Attributes.ARMOR, UUID_DOMAIN_DEF); // the old armor version

        // Clamp HP if we just lost the health buff
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth((float) player.getMaxHealth());
        }
    }

    private static void applyDomainMod(ServerPlayer player,
                                       net.minecraft.world.entity.ai.attributes.Attribute attr,
                                       UUID uuid, String name, double multiplier) {
        var inst = player.getAttribute(attr);
        if (inst == null) return;

        // Don't re-apply if already present
        if (inst.getModifier(uuid) != null) return;

        inst.addPermanentModifier(new AttributeModifier(uuid, name, multiplier,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeDomainMod(ServerPlayer player,
                                        net.minecraft.world.entity.ai.attributes.Attribute attr,
                                        UUID uuid) {
        var inst = player.getAttribute(attr);
        if (inst != null) inst.removeModifier(uuid);
    }
}
