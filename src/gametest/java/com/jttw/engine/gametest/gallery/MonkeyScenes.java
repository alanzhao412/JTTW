package com.jttw.engine.gametest.gallery;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.wukong.FightingBuddha;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Sun Wukong as he is drawn from the start (MonkeyKingModel): the Monkey King, before, beside and behind
 * him, his face close, in real armour (iron, and diamond from behind), in first person (his own arms),
 * and with a shadow clone (drawn as he is).
 */
final class MonkeyScenes {

    private MonkeyScenes() {}

    /** Wukong at level 40, the staff in his hand; armour if asked for. */
    static void monkey(ServerPlayer p, String armour) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(p, 40);
        LevelManager.reapplyStats(p);
        LevelManager.syncToClient(p);
        FightingBuddha.sync(p);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.jttw.engine.registry.ModItems.RUYI_JINGU_BANG.get()));
        boolean iron = "iron".equals(armour), diamond = "diamond".equals(armour);
        p.setItemSlot(EquipmentSlot.HEAD, iron ? new ItemStack(Items.IRON_HELMET) : diamond ? new ItemStack(Items.DIAMOND_HELMET) : ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.CHEST, iron ? new ItemStack(Items.IRON_CHESTPLATE) : diamond ? new ItemStack(Items.DIAMOND_CHESTPLATE) : ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.LEGS, iron ? new ItemStack(Items.IRON_LEGGINGS) : diamond ? new ItemStack(Items.DIAMOND_LEGGINGS) : ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.FEET, iron ? new ItemStack(Items.IRON_BOOTS) : diamond ? new ItemStack(Items.DIAMOND_BOOTS) : ItemStack.EMPTY);
    }

    private static AbilityScenes.Scene shot(String name, String armour, float facing, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            monkey(p, armour);
            AbilityScenes.facing = facing;
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    static List<AbilityScenes.Scene> all() {
        double[] front = {0.5, 1.5, 3.6, 0.5, 1.05, 0.5};
        return List.of(
                shot("monkey_front", "", 0f, front),
                shot("monkey_quarter", "", -35f, new double[]{2.5, 1.6, 3.2, 0.5, 1.05, 0.5}),
                shot("monkey_side", "", 90f, front),
                // his right side: the staff at ease across the shoulder, gripped
                shot("monkey_grip", "", -90f, new double[]{0.5, 1.7, 2.6, 0.5, 1.4, 0.5}),
                shot("monkey_back", "", 180f, front),
                shot("monkey_face", "", -15f, new double[]{0.85, 1.7, 1.65, 0.5, 1.6, 0.5}),
                shot("monkey_profile", "", 90f, new double[]{0.5, 1.65, 2.1, 0.5, 1.55, 0.5}),
                shot("monkey_armour_iron", "iron", -30f, new double[]{2.3, 1.6, 3.0, 0.5, 1.05, 0.5}),
                shot("monkey_armour_diamond_back", "diamond", 150f, new double[]{1.6, 1.6, 3.3, 0.5, 1.05, 0.5}),
                // first person: his own arm and hand, the staff in it
                new AbilityScenes.Scene("monkey_first_person", p -> {
                    monkey(p, "");
                    p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                }, p -> {}, 1, true, 10f),
                // a shadow clone at his side, drawn as he is
                new AbilityScenes.Scene("monkey_clone", p -> {
                    monkey(p, "");
                    SkillTreeData.grantNode(p, "wu_clone_1");
                    QiData.setMax(p, 500f);
                    QiData.setCurrent(p, 500f);
                    AbilityScenes.facing = -20f;
                    AbilityScenes.sceneCam = new double[]{3.5, 2.0, 5.0, 0.5, 1.0, 1.5};
                }, p -> WukongCharacter.INSTANCE.use(p, "wu_clone", AbilityAction.USE), 30, false));
    }

    static void clear(ServerPlayer p) {
        com.jttw.engine.character.wukong.WuCloneManager.dismissAll(p);
    }
}
