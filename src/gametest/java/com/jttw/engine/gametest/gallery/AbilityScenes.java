package com.jttw.engine.gametest.gallery;

import com.jttw.engine.character.ninespirits.NineSpiritsAbilityHandler;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;
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
     */
    record Scene(String name, Consumer<ServerPlayer> setup, Consumer<ServerPlayer> trigger,
                 int triggerBefore, boolean firstPerson) {}

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
                        new ItemStack(ModItems.BELL_OF_TAIYI.get())), p -> {}, 1, true));
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
    }
}
