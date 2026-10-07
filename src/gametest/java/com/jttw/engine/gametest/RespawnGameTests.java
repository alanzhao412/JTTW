package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;

/**
 * What survives an ordinary death. Respawning builds a new player object: vanilla restores it
 * from the old one (Forge only carries the "PlayerPersisted" sub-tag of saved data across),
 * then every mod's PlayerEvent.Clone handler copies what it owns. These tests do the same,
 * posting the event on the real bus so all of the mod's handlers take part.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class RespawnGameTests {

    private static ServerPlayer respawn(GameTestHelper h, ServerPlayer dead) {
        ServerPlayer reborn = player(h);
        reborn.restoreFrom(dead, false);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(reborn, dead, true));
        return reborn;
    }

    @GameTest(template = PLATFORM)
    public static void deathKeepsClassLevelSkillsAndOutcome(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 37);
        SkillTreeData.setPoints(p, 2);
        SkillTreeData.grantNode(p, "es_eye_1");
        ServerPlayer after = respawn(h, p);
        eq(h, ProgressionClass.HEAVENLY, PlayerLevel.getProgressionClass(after), "class");
        eq(h, 37, PlayerLevel.getLevel(after), "level");
        eq(h, 2, SkillTreeData.getPoints(after), "points");
        check(h, SkillTreeData.hasNode(after, "es_eye_1"), "node");
        check(h, AlignmentData.hasReincarnated(after), "still reincarnated");
        eq(h, "eye_of_heaven", after.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME), "outcome");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void deathKeepsWukongsImmortality(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PILLS, true);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PEACHES, true);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES, true);
        ServerPlayer after = respawn(h, p);
        check(h, after.getPersistentData().getBoolean(ImmortalItems.KEY_IMMORTAL_PILLS), "pills");
        check(h, after.getPersistentData().getBoolean(ImmortalItems.KEY_IMMORTAL_PEACHES), "peaches");
        check(h, after.getPersistentData().getBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES), "golden eyes");
        h.succeed();
    }

    /**
     * Alignment earned before the first reincarnation is not copied on death today, so any
     * death that doesn't reincarnate starts the four counts again from zero. Whether a death
     * should wipe them is a design decision; until it's made this test reports, not gates.
     */
    @GameTest(template = PLATFORM, required = false)
    public static void deathBeforeReincarnatingKeepsAlignment(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentData.setHeavenly(p, 120);
        AlignmentData.setYaoguai(p, 40);
        ServerPlayer after = respawn(h, p);
        eq(h, 160, AlignmentData.getTotal(after), "alignment after an ordinary death");
        h.succeed();
    }
}
