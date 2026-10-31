package com.jttw.engine.gametest.gallery;

import com.jttw.engine.character.wukong.FightingBuddha;
import com.jttw.engine.character.wukong.SomersaultTravel;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The Victorious Fighting Buddha (Wukong at level 100) in the form gallery: before, behind, beside
 * him, his face close, and from afar (the Monkey King's head, the crown, the robe and kasaya, the
 * halo and the mandorla), in netherite armour (his robes drawn instead), and his screens: the Skills tab with his four powers, the skill tree with
 * its milestone, and Somersault Anywhere's menu.
 */
final class BuddhaScenes {

    private BuddhaScenes() {}

    /** Wukong at level 100, told he has ascended, the staff in his hand. */
    static void buddha(ServerPlayer p, boolean armour) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(p, 100);
        p.getPersistentData().putBoolean(FightingBuddha.KEY_ANNOUNCED, true);
        LevelManager.reapplyStats(p);
        LevelManager.syncToClient(p);
        FightingBuddha.sync(p);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.jttw.engine.registry.ModItems.RUYI_JINGU_BANG.get()));
        p.setItemSlot(EquipmentSlot.HEAD, armour ? new ItemStack(Items.NETHERITE_HELMET) : ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.CHEST, armour ? new ItemStack(Items.NETHERITE_CHESTPLATE) : ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.LEGS, armour ? new ItemStack(Items.NETHERITE_LEGGINGS) : ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.FEET, armour ? new ItemStack(Items.NETHERITE_BOOTS) : ItemStack.EMPTY);
    }

    /** Him, from {@code cam} (x, y, z it stands at and x, y, z it looks at, heights over the ground), facing {@code facing}. */
    private static AbilityScenes.Scene shot(String name, boolean armour, float facing, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            buddha(p, armour);
            AbilityScenes.facing = facing;
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    private static AbilityScenes.Scene screen(String name, java.util.function.Consumer<ServerPlayer> open) {
        return new AbilityScenes.Scene(name, p -> {
            buddha(p, false);
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            open.accept(p);
            AbilityScenes.keepScreen = true;
        }, p -> {}, 10, true);
    }

    static List<AbilityScenes.Scene> all() {
        double[] front = {0.5, 1.6, 4.2, 0.5, 1.25, 0.5};
        return List.of(
                shot("buddha_front", false, 0f, front),
                shot("buddha_quarter", false, -35f, new double[]{2.8, 1.8, 3.6, 0.5, 1.25, 0.5}),
                shot("buddha_side", false, 90f, front),
                shot("buddha_back", false, 180f, new double[]{0.5, 1.7, 4.4, 0.5, 1.3, 0.5}),
                shot("buddha_afar", false, -25f, new double[]{3.5, 2.6, 6.5, 0.5, 1.2, 0.5}),
                shot("buddha_face", false, -15f, new double[]{0.9, 1.85, 1.75, 0.5, 1.72, 0.5}),
                shot("buddha_face_side", false, 70f, new double[]{0.9, 1.85, 1.75, 0.5, 1.72, 0.5}),
                // in netherite: his robes are drawn, not his armour
                shot("buddha_armour", true, -25f, new double[]{2.0, 1.6, 3.0, 0.5, 1.1, 0.5}),
                screen("buddha_skills", p -> AbilityScenes.openClassAbilities = true),
                screen("buddha_skill_tree", p -> AbilityScenes.openSkillTree = true),
                new AbilityScenes.Scene("buddha_somersault_menu", p -> {
                    buddha(p, false);
                    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                    AbilityScenes.keepScreen = true;
                }, SomersaultTravel::openMenu, 8, true));
    }
}
