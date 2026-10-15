package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.ResistanceHandler;
import com.jttw.engine.progression.stats.StatBlock;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;

/** Physical and magic resistance, and magic vulnerability. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ResistanceGameTests {

    // 40-damage hits against 100 resistance: (40 - 10) x 100/200 = 15

    @GameTest(template = PLATFORM)
    public static void physicalResistanceReducesBlowsNotMagic(GameTestHelper h) {
        ServerPlayer p = player(h);
        setResistances(p, 100.0, 0.0);
        var src = p.damageSources();
        near(h, 15.0, afterResistance(p, src.cactus(), 40f), "cactus");
        near(h, 15.0, afterResistance(p, src.sweetBerryBush(), 40f), "berry bush");
        near(h, 40.0, afterResistance(p, src.magic(), 40f), "magic");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void magicResistanceReducesEveryKindOfMagic(GameTestHelper h) {
        ServerPlayer p = player(h);
        setResistances(p, 0.0, 100.0);
        var src = p.damageSources();
        near(h, 15.0, afterResistance(p, src.magic(), 40f), "magic");
        near(h, 15.0, afterResistance(p, src.indirectMagic(p, null), 40f), "indirect magic");
        near(h, 15.0, afterResistance(p, src.wither(), 40f), "wither");
        near(h, 15.0, afterResistance(p, src.dragonBreath(), 40f), "dragon breath");
        near(h, 15.0, afterResistance(p, src.thorns(p), 40f), "thorns");
        near(h, 15.0, afterResistance(p, src.sonicBoom(p), 40f), "sonic boom");
        near(h, 40.0, afterResistance(p, src.cactus(), 40f), "cactus is physical");
        h.succeed();
    }

    /** Small hits shrink to the 5% floor: a zombie's 4 against 100 resistance. */
    @GameTest(template = PLATFORM)
    public static void smallHitsAreShruggedOffButNeverToNothing(GameTestHelper h) {
        ServerPlayer p = player(h);
        setResistances(p, 100.0, 100.0);
        near(h, 0.2, afterResistance(p, p.damageSources().cactus(), 4f), "4 -> floor of 5%");
        near(h, 0.2, afterResistance(p, p.damageSources().magic(), 4f), "magic too");
        h.succeed();
    }

    /** What already skips armor skips resistance: falls, drowning, fire ticks, the void. */
    @GameTest(template = PLATFORM)
    public static void armorBypassingDamageIgnoresResistance(GameTestHelper h) {
        ServerPlayer p = player(h);
        setResistances(p, 1000.0, 1000.0);
        var src = p.damageSources();
        near(h, 10.0, afterResistance(p, src.fall(), 10f), "fall");
        near(h, 10.0, afterResistance(p, src.drown(), 10f), "drown");
        near(h, 10.0, afterResistance(p, src.onFire(), 10f), "on fire");
        near(h, 10.0, afterResistance(p, src.fellOutOfWorld(), 10f), "void");
        near(h, 10.0, afterResistance(p, src.genericKill(), 10f), "/kill");
        h.succeed();
    }

    /** (200 - 90) x 100/1000 = 11. */
    @GameTest(template = PLATFORM)
    public static void resistanceHasNoCap(GameTestHelper h) {
        ServerPlayer p = player(h);
        setResistances(p, 900.0, 0.0);
        near(h, 11.0, afterResistance(p, p.damageSources().cactus(), 200f), "900 resistance");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void magicVulnerabilityAddsAQuarterUntilItRunsOut(GameTestHelper h) {
        IronGolem golem = h.spawn(EntityType.IRON_GOLEM, 1, 2, 1);
        long now = h.getLevel().getGameTime();
        golem.getPersistentData().putLong(ResistanceHandler.KEY_MAGIC_VULN, now + 100);
        var src = h.getLevel().damageSources();
        near(h, 12.5, afterResistance(golem, src.magic(), 10f), "magic while marked");
        near(h, 10.0, afterResistance(golem, src.cactus(), 10f), "physical is unaffected");
        golem.getPersistentData().putLong(ResistanceHandler.KEY_MAGIC_VULN, now);
        near(h, 10.0, afterResistance(golem, src.magic(), 10f), "mark expired");
        h.succeed();
    }

    /** The vulnerable mark applies before resistance: (40 x 1.25 - 10) x 100/200 = 20. */
    @GameTest(template = PLATFORM)
    public static void magicVulnerabilityStacksWithResistance(GameTestHelper h) {
        ServerPlayer p = player(h);
        setResistances(p, 0.0, 100.0);
        p.getPersistentData().putLong(ResistanceHandler.KEY_MAGIC_VULN, h.getLevel().getGameTime() + 100);
        near(h, 20.0, afterResistance(p, p.damageSources().magic(), 40f), "magic");
        h.succeed();
    }

    /** Players who levelled before resistances existed lose the old DEF-as-armor modifier. */
    @GameTest(template = PLATFORM)
    public static void oldDefenceArmorIsRemovedWhenStatsAreReapplied(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        p.getAttribute(Attributes.ARMOR).addPermanentModifier(new AttributeModifier(
                StatBlock.UUID_DEF, 15.0, AttributeModifier.Operation.ADD_VALUE));
        LevelManager.reapplyStats(p);
        near(h, 0.0, p.getAttributeValue(Attributes.ARMOR), "vanilla armor");
        near(h, StatBlock.compute(ProgressionClass.HEAVENLY, 1).physicalResistance(), pres(p), "physical resistance");
        h.succeed();
    }

    /** The resistance attributes survive a save/load like any other attribute. */
    @GameTest(template = PLATFORM)
    public static void resistancesAreSavedWithThePlayer(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        var tag = new net.minecraft.nbt.CompoundTag();
        p.saveWithoutId(tag);
        ServerPlayer loaded = player(h);
        loaded.load(tag);
        near(h, pres(p), pres(loaded), "physical resistance");
        near(h, mres(p), mres(loaded), "magic resistance");
        h.succeed();
    }
}
