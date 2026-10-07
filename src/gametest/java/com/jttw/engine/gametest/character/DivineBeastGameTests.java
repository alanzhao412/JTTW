package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.divinebeast.DivineBeastHandler;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Divine Beast passives shared by Dragon, Phoenix, Byakko and Turtle: bare-hand mining and domains. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class DivineBeastGameTests {

    private static boolean canHarvest(ServerPlayer p, BlockState state) {
        PlayerEvent.HarvestCheck ev = new PlayerEvent.HarvestCheck(p, state, false);
        DivineBeastHandler.onHarvestCheck(ev);
        return ev.canHarvest();
    }

    private static float breakSpeed(ServerPlayer p, BlockState state, float original) {
        PlayerEvent.BreakSpeed ev = new PlayerEvent.BreakSpeed(p, state, original, BlockPos.ZERO);
        DivineBeastHandler.onBreakSpeed(ev);
        return ev.getNewSpeed();
    }

    // ── Bare-handed mining ──────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "spiritFormMinesLikeAWoodenPickaxe")
    public static void spiritFormMinesLikeAWoodenPickaxe(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.TURTLE);
        eq(h, 0, PlayerLevel.getFormStage(p), "Spirit Form");
        check(h, canHarvest(p, Blocks.STONE.defaultBlockState()), "stone");
        check(h, canHarvest(p, Blocks.COAL_ORE.defaultBlockState()), "coal ore");
        check(h, !canHarvest(p, Blocks.IRON_ORE.defaultBlockState()), "iron ore needs stone tier");
        near(h, 2.0f, breakSpeed(p, Blocks.STONE.defaultBlockState(), 1f), "wooden-pickaxe speed");
        near(h, 1.0f, breakSpeed(p, Blocks.IRON_ORE.defaultBlockState(), 1f), "no boost on blocks it cannot harvest");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "guardianFormMinesLikeAnIronPickaxe")
    public static void guardianFormMinesLikeAnIronPickaxe(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.DRAGON);
        PlayerLevel.setFormStage(p, 1);
        check(h, canHarvest(p, Blocks.IRON_ORE.defaultBlockState()), "iron ore");
        check(h, canHarvest(p, Blocks.DIAMOND_ORE.defaultBlockState()), "diamond ore");
        check(h, !canHarvest(p, Blocks.OBSIDIAN.defaultBlockState()), "obsidian needs diamond tier");
        near(h, 6.0f, breakSpeed(p, Blocks.IRON_ORE.defaultBlockState(), 1f), "iron-pickaxe speed");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "trueFormAndAboveMineObsidian")
    public static void trueFormAndAboveMineObsidian(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.PHOENIX);
        PlayerLevel.setFormStage(p, 2);
        check(h, canHarvest(p, Blocks.OBSIDIAN.defaultBlockState()), "True Form: obsidian");
        near(h, 8.0f, breakSpeed(p, Blocks.OBSIDIAN.defaultBlockState(), 1f), "diamond-pickaxe speed");
        PlayerLevel.setFormStage(p, 4);
        near(h, 9.0f, breakSpeed(p, Blocks.OBSIDIAN.defaultBlockState(), 1f), "Sovereign: netherite speed");
        near(h, 12.0f, breakSpeed(p, Blocks.OBSIDIAN.defaultBlockState(), 12f), "never slows a faster break");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "bareHandMiningOnlyForDivineBeastsWithEmptyHands")
    public static void bareHandMiningOnlyForDivineBeastsWithEmptyHands(GameTestHelper h) {
        ServerPlayer beast = as(h, ReincarnationOutcome.WHITE_TIGER);
        PlayerLevel.setFormStage(beast, 4);
        beast.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        check(h, !canHarvest(beast, Blocks.IRON_ORE.defaultBlockState()), "holding something: vanilla rules");
        near(h, 1.0f, breakSpeed(beast, Blocks.IRON_ORE.defaultBlockState(), 1f), "holding something: no boost");

        ServerPlayer wukong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setFormStage(wukong, 4);
        check(h, !canHarvest(wukong, Blocks.IRON_ORE.defaultBlockState()), "not a Divine Beast");
        h.succeed();
    }

    // ── Domain buffs ────────────────────────────────────────────────

    private static void domainTick(ServerPlayer p) {
        p.tickCount = 19;
        tick(p, DivineBeastHandler::onPlayerTick, 1);
    }

    @GameTest(template = PLATFORM, batch = "earthLineIsEmpoweredBelowYZero")
    public static void earthLineIsEmpoweredBelowYZero(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.TURTLE);
        p.setPos(p.getX(), -10, p.getZ());
        domainTick(p);
        eq(h, 0.20, modifier(p, Attributes.MAX_HEALTH, "jttw:domain_hp"), "+20% health");
        eq(h, 0.20, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:domain_str"), "+20% attack");
        eq(h, 0.20, modifier(p, PHYSICAL_RESISTANCE(), "jttw:domain_def"), "+20% physical resistance");
        eq(h, 0.20, modifier(p, MAGIC_RESISTANCE(), "jttw:domain_mres"), "+20% magic resistance");
        eq(h, null, modifier(p, Attributes.ARMOR, "jttw:domain_def"), "no vanilla armor");
        p.setHealth(p.getMaxHealth());
        p.setPos(p.getX(), 10, p.getZ());
        domainTick(p);
        eq(h, null, modifier(p, Attributes.MAX_HEALTH, "jttw:domain_hp"), "removed above ground");
        near(h, p.getMaxHealth(), p.getHealth(), "health clamped to the lower maximum");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "domainBuffsAreOnlyCheckedOncePerSecond")
    public static void domainBuffsAreOnlyCheckedOncePerSecond(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.TURTLE);
        p.setPos(p.getX(), -10, p.getZ());
        p.tickCount = 4;
        tick(p, DivineBeastHandler::onPlayerTick, 1);
        eq(h, null, modifier(p, Attributes.MAX_HEALTH, "jttw:domain_hp"), "tick 5: not checked");
        p.tickCount = 39;
        tick(p, DivineBeastHandler::onPlayerTick, 1);
        eq(h, 0.20, modifier(p, Attributes.MAX_HEALTH, "jttw:domain_hp"), "tick 40: applied");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "otherClassesNeverGetDomainBuffs")
    public static void otherClassesNeverGetDomainBuffs(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        p.setPos(p.getX(), -10, p.getZ());
        domainTick(p);
        eq(h, null, modifier(p, Attributes.MAX_HEALTH, "jttw:domain_hp"), "Heavenly class underground");
        h.succeed();
    }
}
