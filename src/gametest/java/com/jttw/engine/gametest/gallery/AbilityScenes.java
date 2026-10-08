package com.jttw.engine.gametest.gallery;

import com.jttw.engine.character.marshal.MarshalAbilityHandler;
import com.jttw.engine.character.ninespirits.NineSpiritsAbilityHandler;
import com.jttw.engine.character.peng.PengAbilityHandler;
import com.jttw.engine.item.YinYangVaseItem;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.TruffleScentPacket;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * Ability scenes for the form gallery: each sets a character up, fires an ability a few ticks
 * before the picture, and is photographed from a three-quarter camera, or in first person with
 * the HUD showing. The player stands at the origin facing south (+z); targets stand in front.
 */
final class AbilityScenes {

    private AbilityScenes() {}

    /**
     * @param setup     on the server, after the stage is reset: character, nodes, items, targets
     * @param trigger   on the server, {@code triggerBefore} ticks before the picture
     * @param firstPerson photograph through the player's eyes with the HUD on
     * @param pitch     where the player looks: 0 ahead, positive down
     */
    record Scene(String name, Consumer<ServerPlayer> setup, Consumer<ServerPlayer> trigger,
                 int triggerBefore, boolean firstPerson, float pitch) {
        Scene(String name, Consumer<ServerPlayer> setup, Consumer<ServerPlayer> trigger,
              int triggerBefore, boolean firstPerson) {
            this(name, setup, trigger, triggerBefore, firstPerson, 0f);
        }
    }

    static List<Scene> all() {
        return List.of(
                // ── Grand Saint of Nine Spirits ──
                new Scene("ns_fire_head", p -> saint(p, NineSpiritsAbilityHandler.FIRE, "ns_heads_1"),
                        NineSpiritsAbilityHandler::fireHead, 4, false),
                new Scene("ns_frost_head", p -> saint(p, NineSpiritsAbilityHandler.FROST, "ns_heads_1", "ns_heads_2", "ns_heads_3"),
                        NineSpiritsAbilityHandler::fireHead, 4, false),
                new Scene("ns_thunder_head", p -> {
                    saint(p, NineSpiritsAbilityHandler.THUNDER, "ns_heads_1", "ns_heads_2", "ns_heads_3");
                    targets(p, 0, 5, 2, 6, -2, 6);
                }, NineSpiritsAbilityHandler::fireHead, 6, false),
                new Scene("ns_roar", p -> {
                    saint(p, NineSpiritsAbilityHandler.FIRE, "ns_roar_1", "ns_roar_2", "ns_roar_3");
                    targets(p, 0, 4, 1, 5, -1, 5);
                }, NineSpiritsAbilityHandler::roar, 6, false),
                new Scene("ns_mane_flare", p -> saint(p, NineSpiritsAbilityHandler.FIRE, "ns_flare_1", "ns_flare_2"),
                        NineSpiritsAbilityHandler::flare, 6, false),
                new Scene("ns_true_form", p -> {
                    saint(p, NineSpiritsAbilityHandler.FIRE, "ns_form_1", "ns_form_2", "ns_form_3");
                    NineSpiritsAbilityHandler.toggleTrueForm(p);
                }, p -> {}, 1, false),
                new Scene("ns_head_tint_thunder", p -> {
                    saint(p, NineSpiritsAbilityHandler.THUNDER, "ns_heads_1", "ns_heads_2");
                    NineSpiritsAbilityHandler.syncHead(p);
                }, p -> {}, 1, true),
                new Scene("bell_of_taiyi_in_hand", p -> p.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.BELL_OF_TAIYI.get())), p -> {}, 1, true),

                // ── Marshal of the Heavenly Canopy ──
                new Scene("mc_belly_slam", p -> {
                    marshal(p, "mc_slam_1", "mc_slam_2", "mc_slam_3", "mc_stomach_1");
                    p.getFoodData().setFoodLevel(20);
                    MarshalAbilityHandler.setReserve(p, 20);
                    targets(p, 0, 3, 3, 2, -3, 2, 2, 5, -2, 5);
                }, p -> MarshalAbilityHandler.slamImpact(p), 6, false),
                new Scene("mc_truffle_snout", p -> {
                    marshal(p, "mc_snout_1", "mc_snout_2", "mc_snout_3");
                    bury(p, 1, 4, Blocks.DIAMOND_ORE);
                    bury(p, -2, 6, Blocks.GOLD_ORE);
                    bury(p, 3, 7, Blocks.IRON_ORE);
                    bury(p, 0, 9, Blocks.CHEST);
                    targets(p, 4, 10, -5, 12);
                }, p -> MarshalAbilityHandler.sniff(p), 4, true, 35f),
                new Scene("mc_second_stomach_hud", p -> {
                    marshal(p, "mc_stomach_1");
                    p.setGameMode(GameType.SURVIVAL); // the food bar only shows in survival
                    p.getFoodData().setFoodLevel(20);
                    MarshalAbilityHandler.setReserve(p, 13);
                }, p -> {}, 4, true),
                new Scene("mc_rake_burst", p -> {
                    marshal(p);
                    p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NINE_TOOTH_RAKE.get()));
                    targets(p, 0, 2, 1, 3, -1, 3);
                }, p -> p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(3)).stream()
                        .min(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(p))).ifPresent(p::attack), 6, false),
                new Scene("nine_tooth_rake_in_hand", p -> p.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.NINE_TOOTH_RAKE.get())), p -> {}, 1, true),

                // ── Gold-Winged Peng ──
                new Scene("gp_feather_storm", p -> {
                    peng(p, "gp_feathers_1", "gp_feathers_2", "gp_feathers_3");
                    targets(p, 0, 9, 3, 9, -3, 9);
                }, p -> PengAbilityHandler.featherStorm(p), 3, false),
                new Scene("gp_eclipse_wings", p -> {
                    peng(p, "gp_eclipse_1", "gp_eclipse_2");
                    targets(p, 0, 4, 3, 2, -3, 2, 2, -2);
                }, p -> PengAbilityHandler.eclipse(p), 8, false),
                new Scene("gp_vase_burst", p -> {
                    peng(p);
                    targets(p, 1, 4, -1, 5, 2, 6);
                    ItemStack vase = new ItemStack(ModItems.YIN_YANG_VASE.get());
                    p.setItemInHand(InteractionHand.MAIN_HAND, vase);
                    Mob husk = EntityType.HUSK.create(p.serverLevel());
                    husk.moveTo(p.getX(), p.getY(), p.getZ() + 4);
                    p.serverLevel().addFreshEntity(husk);
                    vase.interactLivingEntity(p, husk, InteractionHand.MAIN_HAND);
                }, p -> YinYangVaseItem.release(p.getMainHandItem(), p.serverLevel(),
                        p.position().add(0, 0, 4), p), 6, false),
                new Scene("yin_yang_vase_in_hand", p -> p.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.YIN_YANG_VASE.get())), p -> {}, 1, true));
    }

    /** Makes the player the Grand Saint with these nodes, this head leading, and Qi to spend. */
    private static void saint(ServerPlayer p, int head, String... nodes) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS);
        LevelManager.initClass(p, ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        p.getPersistentData().putInt(NineSpiritsAbilityHandler.KEY_HEAD, head);
        for (String k : List.of("jttw_ns_heads_ready_at", "jttw_ns_roar_ready_at", "jttw_ns_flare_ready_at")) {
            p.getPersistentData().remove(k);
        }
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    /** Makes the player the Marshal with these nodes and Qi to spend, his abilities ready. */
    private static void marshal(ServerPlayer p, String... nodes) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        LevelManager.initClass(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        for (String k : List.of("jttw_mc_slam_ready_at", "jttw_mc_snout_ready_at", "jttw_mc_rake_next")) {
            p.getPersistentData().remove(k);
        }
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    /** Makes the player the Gold-Winged Peng with these nodes and Qi to spend, his abilities ready. */
    private static void peng(ServerPlayer p, String... nodes) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.GOLD_WINGED_PENG);
        LevelManager.initClass(p, ReincarnationOutcome.GOLD_WINGED_PENG);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        for (String k : List.of("jttw_gp_feathers_ready_at", "jttw_gp_eclipse_ready_at")) p.getPersistentData().remove(k);
        p.getCooldowns().removeCooldown(ModItems.YIN_YANG_VASE.get());
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    /** A block two below the surface at this (x, z) offset, out of sight unless smelt. */
    private static void bury(ServerPlayer p, int x, int z, net.minecraft.world.level.block.Block block) {
        p.serverLevel().setBlock(BlockPos.containing(p.getX() + x, p.getY() - 2, p.getZ() + z), block.defaultBlockState(), 3);
    }

    /** Still husks at these (x, z) offsets from the player. */
    private static void targets(ServerPlayer p, int... xz) {
        for (int i = 0; i + 1 < xz.length; i += 2) {
            Mob z = EntityType.HUSK.create(p.serverLevel()); // husks do not burn in daylight
            z.moveTo(p.getX() + xz[i], p.getY(), p.getZ() + xz[i + 1], 180f, 0f);
            z.setNoAi(true);
            z.setPersistenceRequired();
            p.serverLevel().addFreshEntity(z);
        }
    }

    /** Puts a player back to an ordinary state between scenes. */
    static void reset(ServerPlayer p) {
        if (NineSpiritsAbilityHandler.inTrueForm(p)) NineSpiritsAbilityHandler.endTrueForm(p);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.removeAllEffects();
        p.clearFire();
        p.setGameMode(GameType.CREATIVE);
        NetworkHandler.sendToPlayer(p, new TruffleScentPacket(0, new int[0], new long[0], new int[0]));
    }
}
