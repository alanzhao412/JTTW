package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.flowerfruit.FakePilgrimEntity;
import com.jttw.engine.entity.flowerfruit.FlowerFruitMonster;
import com.jttw.engine.entity.flowerfruit.GreatBaboonEntity;
import com.jttw.engine.entity.flowerfruit.MacaqueCloneEntity;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.StatBlock;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.entity.flowerfruit.SixEaredMacaqueEntity;
import com.jttw.engine.entity.flowerfruit.SpearApeEntity;
import com.jttw.engine.entity.flowerfruit.StoneGibbonEntity;
import com.jttw.engine.entity.flowerfruit.ThrownStoneEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.FlowerFruitRegistry;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.ZoneAllies;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.FlowerFruitLifeFeature;
import com.jttw.engine.world.flowerfruit.CavePlan;
import com.jttw.engine.world.flowerfruit.IslandShape;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/**
 * Flower-Fruit Mountain: its monkeys (their ranks and their warned attacks: the gibbon's stones, the
 * spear ape's lunge, the great baboon's pound), the monkey truce (they spare the true Monkey King and
 * kneel to him; the fake pilgrims and the Macaque do not), the fake pilgrims' tricks, the Six-Eared
 * Macaque (level 80, a level-80 Wukong's equal, his whirling staff, cloud somersault, hair clones, Dharma image and
 * heaven-splitting blow, kept to his sanctum), what they leave, the band's XP and karma, and the
 * Water Curtain Cave's plan in blocks. The island's placement and shape: {@code FlowerFruitPlacementTest}.
 *
 * <p>Each test runs in a batch of its own: the monkeys rouse each other, and a boss of his
 * strength is nothing to share a world with.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlowerFruitGameTests {

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 1e-3 + 1e-3, what + ": expected " + expected + " but was " + actual);
    }

    private static void sweepAround(GameTestHelper h, Entity... held) {
        AABB bounds = h.getBounds().inflate(24);
        onEnd(h, () -> {
            h.getLevel().getEntities((Entity) null, bounds, x -> x instanceof FlowerFruitMonster || x instanceof SixEaredMacaqueEntity
                    || x instanceof MacaqueCloneEntity || x instanceof ThrownStoneEntity).forEach(Entity::discard);
            for (Entity e : held) e.discard();
        });
    }

    private static <T extends ZoneMonster> T monkey(GameTestHelper h, EntityType<T> type, int level, boolean captain, BlockPos at) {
        T m = h.spawn(type, at);
        m.become(level, captain);
        sweepAround(h, m);
        return m;
    }

    private static FakePilgrimEntity pilgrim(GameTestHelper h, int variant, BlockPos at) {
        FakePilgrimEntity f = h.spawn(FlowerFruitRegistry.FAKE_PILGRIM.get(), at);
        f.setVariant(variant);
        f.becomeMiniboss(29, CavePlan.PILGRIM_HEALTH);
        f.setNoAi(true);
        sweepAround(h, f);
        return f;
    }

    private static SixEaredMacaqueEntity macaque(GameTestHelper h, BlockPos at) {
        openAir(h);
        SixEaredMacaqueEntity m = h.spawn(FlowerFruitRegistry.SIX_EARED_MACAQUE.get(), at);
        sweepAround(h, m);
        return m;
    }

    /** A still iron golem, its health raised so no monkey's blow ends it (and fine enough that one shows). */
    private static LivingEntity golem(GameTestHelper h, BlockPos at) {
        return golem(h, at, 1e6);
    }

    /** A still iron golem of {@code health}: so much that no blow of the Macaque's ends it. */
    private static LivingEntity golem(GameTestHelper h, BlockPos at, double health) {
        var g = h.spawn(EntityType.IRON_GOLEM, at);
        g.goalSelector.removeAllGoals(x -> true);
        g.targetSelector.removeAllGoals(x -> true);
        g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        g.setHealth(g.getMaxHealth());
        sweepAround(h, g);
        return g;
    }

    private static ServerPlayer survivor(GameTestHelper h, ReincarnationOutcome outcome) {
        ServerPlayer p = as(h, outcome);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    // ── The monkeys ─────────────────────────────────────────────────────

    @GameTest(batch = "ff_ranks", template = PLATFORM)
    public static void theMonkeyArmyStandsInItsRanks(GameTestHelper h) {
        StoneGibbonEntity gibbon = monkey(h, FlowerFruitRegistry.STONE_GIBBON.get(), 20, false, new BlockPos(1, 2, 1));
        SpearApeEntity ape = monkey(h, FlowerFruitRegistry.SPEAR_APE.get(), 20, false, new BlockPos(2, 2, 2));
        GreatBaboonEntity baboon = monkey(h, FlowerFruitRegistry.GREAT_BABOON.get(), 20, false, new BlockPos(3, 2, 3));
        close(h, StoneGibbonEntity.HITS * MobScaling.typicalHit(20), gibbon.getMaxHealth(), "a light gibbon");
        close(h, SpearApeEntity.HITS * MobScaling.typicalHit(20), ape.getMaxHealth(), "a normal ape");
        close(h, GreatBaboonEntity.HITS * MobScaling.typicalHit(20), baboon.getMaxHealth(), "a heavy baboon");
        check(h, StoneGibbonEntity.HITS <= 4 && SpearApeEntity.HITS >= 4 && SpearApeEntity.HITS <= 8 && GreatBaboonEntity.HITS >= 10, "light, normal, heavy");
        check(h, StoneGibbonEntity.CLAW <= 0.07 && SpearApeEntity.BLOW >= 0.08 && GreatBaboonEntity.BLOW >= 0.11, "their blows");
        eq(h, 20, FlowerFruitMonster.LEVEL, "regulars about 20");
        eq(h, 27, gibbon.captainLevel(), "captains about 27");
        GreatBaboonEntity captain = monkey(h, FlowerFruitRegistry.GREAT_BABOON.get(), 27, true, new BlockPos(0, 2, 4));
        eq(h, ZoneMonster.PURPLE, captain.getDisplayName().getStyle().getColor().getValue(), "a captain's name is purple");
        check(h, captain.getDisplayName().getString().contains("Captain") && captain.getDisplayName().getString().contains("Lv 27"), captain.getDisplayName().getString());
        captain.bringEscorts(h.getLevel());
        eq(h, captain.escorts(), h.getLevel().getEntitiesOfClass(GreatBaboonEntity.class, captain.getBoundingBox().inflate(6), e -> e != captain && e != baboon).size(), "escorts");
        h.succeed();
    }

    @GameTest(batch = "ff_gibbon", template = PLATFORM)
    public static void aGibbonRearsBackThenThrowsStones(GameTestHelper h) {
        StoneGibbonEntity g = monkey(h, FlowerFruitRegistry.STONE_GIBBON.get(), 20, false, new BlockPos(0, 2, 0));
        g.setNoAi(true);
        LivingEntity target = golem(h, new BlockPos(4, 2, 4));
        g.rearBack();
        check(h, g.isRearing(), "the warning: a stone up over its head");
        List<ThrownStoneEntity> stones = g.hurl(target);
        eq(h, 1, stones.size(), "a regular throws one");
        close(h, StoneGibbonEntity.STONE * MobScaling.typicalHealth(20), stones.get(0).damage(), "a stone of 7%");
        float before = target.getHealth();
        check(h, stones.get(0).strike(target) && target.getHealth() < before, "it hurts");
        SpearApeEntity friend = monkey(h, FlowerFruitRegistry.SPEAR_APE.get(), 20, false, new BlockPos(2, 2, 2));
        check(h, !stones.get(0).strike(friend), "never its own side");
        StoneGibbonEntity captain = monkey(h, FlowerFruitRegistry.STONE_GIBBON.get(), 27, true, new BlockPos(1, 2, 3));
        captain.setNoAi(true);
        eq(h, 3, captain.hurl(target).size(), "a captain three, fanned");
        h.succeed();
    }

    @GameTest(batch = "ff_ape", template = PLATFORM)
    public static void aSpearApeDrawsBackAndLungesDownItsLine(GameTestHelper h) {
        SpearApeEntity ape = monkey(h, FlowerFruitRegistry.SPEAR_APE.get(), 20, false, new BlockPos(0, 2, 2));
        ape.setNoAi(true);
        LivingEntity inLine = golem(h, new BlockPos(3, 2, 2));
        LivingEntity aside = golem(h, new BlockPos(1, 2, 4));
        ape.drawBack();
        check(h, ape.isDrawing(), "the warning: its spear drawn back");
        float a = inLine.getHealth(), b = aside.getHealth();
        List<LivingEntity> hit = ape.lunge(new Vec3(1, 0, 0));
        check(h, hit.contains(inLine) && inLine.getHealth() < a, "it strikes what is in its line");
        check(h, !hit.contains(aside) && aside.getHealth() == b, "not what is beside it");
        close(h, SpearApeEntity.LUNGE * MobScaling.typicalHealth(20), ape.lungeDamage(), "a lunge of 10%");
        h.succeed();
    }

    @GameTest(batch = "ff_baboon", template = PLATFORM)
    public static void aGreatBaboonBeatsItsChestAndPounds(GameTestHelper h) {
        GreatBaboonEntity b = monkey(h, FlowerFruitRegistry.GREAT_BABOON.get(), 20, false, new BlockPos(2, 2, 2));
        b.setNoAi(true);
        LivingEntity near = golem(h, new BlockPos(4, 2, 2));
        b.rearUp();
        check(h, b.isBeating(), "the warning: up on its legs beating its chest");
        float before = near.getHealth();
        check(h, b.pound().contains(near) && near.getHealth() < before, "the pound strikes all near");
        close(h, GreatBaboonEntity.POUND * MobScaling.typicalHealth(20), b.poundDamage(), "11%");
        h.succeed();
    }

    // ── The monkey truce ────────────────────────────────────────────────

    @GameTest(batch = "ff_truce", template = PLATFORM)
    public static void theMonkeysSpareTheTrueMonkeyKingButNotHisImpostors(GameTestHelper h) {
        openAir(h);
        ServerPlayer king = survivor(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        ServerPlayer other = survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        check(h, ZoneAllies.WUKONG.test(king) && !ZoneAllies.WUKONG.test(other), "Wukong is known");
        TargetingConditions seek = TargetingConditions.forCombat().range(32);
        for (EntityType<? extends ZoneMonster> kind : List.of(FlowerFruitRegistry.STONE_GIBBON.get(), FlowerFruitRegistry.SPEAR_APE.get(),
                FlowerFruitRegistry.GREAT_BABOON.get())) {
            ZoneMonster m = monkey(h, kind, 20, false, new BlockPos(2, 2, 2));
            m.setNoAi(true);
            m.tickCount = 40;
            String who = EntityType.getKey(m.getType()).getPath();
            check(h, m.isAlliedTo(king) && !m.isAlliedTo(other), who + " is the king's");
            check(h, !seek.test(m, king) && seek.test(m, other), who + " never seeks out the king");
            m.setLastHurtByMob(king);
            check(h, !new HurtByTargetGoal((PathfinderMob) m).canUse(), who + ", struck by the king, does not fight back");
            m.setTarget(king);
            check(h, m.getTarget() == null, who + " is never made the king's foe");
            m.setTarget(other);
            check(h, m.getTarget() == other, who + " fights others");
            m.discard();
        }
        // the fake pilgrims and the Macaque know him, and fight him all the same
        FakePilgrimEntity f = pilgrim(h, FakePilgrimEntity.TANG, new BlockPos(1, 2, 1));
        check(h, !f.isAlliedTo(king) && seek.test(f, king), "the false Tang Sanzang fights the king");
        f.setTarget(king);
        check(h, f.getTarget() == king, "made his foe");
        SixEaredMacaqueEntity mac = macaque(h, new BlockPos(3, 2, 3));
        mac.setNoAi(true);
        check(h, !mac.isAlliedTo(king), "the Macaque is no ally of the king's");
        mac.setTarget(king);
        check(h, mac.getTarget() == king, "the Macaque fights him hardest of all");
        h.succeed();
    }

    @GameTest(batch = "ff_kneel", template = PLATFORM)
    public static void aMonkeyKneelsToTheKingAndHailsHim(GameTestHelper h) {
        ServerPlayer king = survivor(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        List<String> heard = new java.util.ArrayList<>();
        listen(king, heard);
        SpearApeEntity ape = monkey(h, FlowerFruitRegistry.SPEAR_APE.get(), 20, false, new BlockPos(1, 2, 1));
        ape.kneel(king);
        check(h, ape.isBowing() && ape.isImmobile(), "it kneels, and stays put");
        check(h, heard.stream().anyMatch(s -> s.contains("Great") || s.contains("King")), "and hails him: " + heard);
        int n = heard.size();
        ape.kneel(king);
        eq(h, n, heard.size(), "not again at once");
        ape.kneel(null);
        check(h, !ape.isBowing(), "and rises");
        FakePilgrimEntity f = pilgrim(h, FakePilgrimEntity.BAJIE, new BlockPos(3, 2, 3));
        check(h, !f.knowsTheKing(), "a fake pilgrim kneels to no one");
        h.succeed();
    }

    // ── The fake pilgrims ───────────────────────────────────────────────

    @GameTest(batch = "ff_pilgrims", template = PLATFORM)
    public static void theFakePilgrimsAreTheZonesMinibosses(GameTestHelper h) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (int v = 0; v < 3; v++) {
            FakePilgrimEntity f = pilgrim(h, v, new BlockPos(v + 1, 2, 2));
            String name = f.getDisplayName().getString();
            check(h, name.contains("False") && name.contains("Lv 29") && !name.contains("Captain"), name);
            eq(h, ZoneMonster.PURPLE, f.getDisplayName().getStyle().getColor().getValue(), "purple");
            check(h, f.isMiniboss(), "a miniboss");
            close(h, ZoneMonster.MINIBOSS_HIT * MobScaling.typicalHealth(29), f.getAttributeValue(Attributes.ATTACK_DAMAGE), "a miniboss's blow");
            close(h, FakePilgrimEntity.HITS * MobScaling.typicalHit(29) * ZoneMonster.CAPTAIN_HEALTH * CavePlan.PILGRIM_HEALTH, f.getMaxHealth(), "a miniboss's health");
            names.add(name);
        }
        eq(h, 3, names.size(), "the three");
        check(h, names.stream().anyMatch(s -> s.contains("Tang Sanzang")) && names.stream().anyMatch(s -> s.contains("Bajie"))
                && names.stream().anyMatch(s -> s.contains("Sha Wujing")), names.toString());
        h.succeed();
    }

    @GameTest(batch = "ff_chant", template = PLATFORM)
    public static void theFalseTangSanzangChantsTheTighteningFilletSpell(GameTestHelper h) {
        FakePilgrimEntity f = pilgrim(h, FakePilgrimEntity.TANG, new BlockPos(0, 2, 0));
        LivingEntity near = golem(h, new BlockPos(4, 2, 4));
        f.warn();
        check(h, f.isWarning() && f.warning() == FakePilgrimEntity.CHANT, "the warning: his chant");
        float before = near.getHealth();
        check(h, f.chant().contains(near) && near.getHealth() < before, "all near struck");
        check(h, near.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && near.hasEffect(MobEffects.CONFUSION), "slowed and dizzied");
        h.succeed();
    }

    @GameTest(batch = "ff_rake", template = PLATFORM)
    public static void theFalseBajieSweepsHisRakeAndTheFalseShaWujingCharges(GameTestHelper h) {
        FakePilgrimEntity pig = pilgrim(h, FakePilgrimEntity.BAJIE, new BlockPos(2, 2, 0));
        pig.setYRot(0); // facing south, +z
        LivingEntity before = golem(h, new BlockPos(2, 2, 3));
        float a = before.getHealth();
        check(h, pig.sweep().contains(before) && before.getHealth() < a, "the rake sweeps what is before him");
        pig.discard();
        FakePilgrimEntity sha = pilgrim(h, FakePilgrimEntity.SHA, new BlockPos(0, 2, 2));
        eq(h, FakePilgrimEntity.LEVEL_SPADE, sha.warning(), "he levels his spade first");
        sha.charge(new Vec3(1, 0, 0));
        check(h, sha.isCharging(), "then charges");
        h.succeed();
    }

    // ── The Six-Eared Macaque ───────────────────────────────────────────

    @GameTest(batch = "ff_macaque", template = PLATFORM)
    public static void theSixEaredMacaqueIsALevel80WukongsEqual(GameTestHelper h) {
        SixEaredMacaqueEntity m = macaque(h, new BlockPos(2, 2, 2));
        m.setNoAi(true);
        eq(h, 80, m.mobLevel(), "level 80");
        check(h, m.getDisplayName().getString().contains("Six-Eared Macaque") && m.getDisplayName().getString().contains("Lv 80"), m.getDisplayName().getString());
        eq(h, BossEvent.BossBarColor.YELLOW, m.bossBar().getColor(), "a yellow bar");
        // measured against the Monkey King at 80, not a typical character of 80
        var wukong = StatBlock.compute(ProgressionClass.PRIMORDIAL, ReincarnationOutcome.CELESTIAL_PRIMATE, 80);
        close(h, SixEaredMacaqueEntity.HITS * MobScaling.ORDINARY_HIT * wukong.attackDamage(), m.getMaxHealth(), "430 hits of a level-80 Wukong");
        close(h, SixEaredMacaqueEntity.STAFF * wukong.maxHealth(), m.getAttributeValue(Attributes.ATTACK_DAMAGE), "his staff a third of that Wukong's health");
        check(h, wukong.maxHealth() > MobScaling.typicalHealth(80) * 1.5, "a level-80 Wukong far over a typical character of 80");
        // beatable: a group of capped players' fight, not an endless one
        double capped = m.getMaxHealth() / MobScaling.typicalHit(100), blow = m.getAttributeValue(Attributes.ATTACK_DAMAGE) / MobScaling.typicalHealth(100);
        check(h, capped >= 100 && capped <= 250, "a group of level-100 players' fight: " + capped + " of their hits");
        check(h, blow >= 0.05 && blow <= 0.2, "his blow no one-shot of a level-100 player: " + blow);
        check(h, Zones.FLOWER_FRUIT.minLevel() == 20 && m.mobLevel() > Zones.FLOWER_FRUIT.maxLevel() + 40, "far past his zone's band");
        h.succeed();
    }

    @GameTest(batch = "ff_spin", template = PLATFORM)
    public static void hisWhirlingStaffSweepsAllRoundHim(GameTestHelper h) {
        SixEaredMacaqueEntity m = macaque(h, new BlockPos(2, 2, 2));
        m.setNoAi(true);
        LivingEntity a = golem(h, new BlockPos(0, 2, 0), 1e9), b = golem(h, new BlockPos(4, 2, 4), 1e9);
        m.startSpin();
        eq(h, SixEaredMacaqueEntity.SPINNING, m.action(), "the warning: the staff whirls over his head");
        float ha = a.getHealth(), hb = b.getHealth();
        List<LivingEntity> hit = m.spin();
        check(h, hit.contains(a) && hit.contains(b) && a.getHealth() < ha && b.getHealth() < hb, "all round him struck");
        check(h, m.spinDamage() <= SixEaredMacaqueEntity.STAFF * SixEaredMacaqueEntity.WUKONG_HEALTH + 1e-3, "no harder than his blow");
        h.succeed();
    }

    @GameTest(batch = "ff_somersault", template = PLATFORM)
    public static void hisCloudSomersaultStrikesAllInHisPath(GameTestHelper h) {
        SixEaredMacaqueEntity m = macaque(h, new BlockPos(0, 2, 2));
        m.setNoAi(true);
        LivingEntity target = golem(h, new BlockPos(4, 2, 2), 1e9);
        double x0 = m.getX();
        float before = target.getHealth();
        List<LivingEntity> hit = m.somersaultNow(target);
        check(h, hit.contains(target) && target.getHealth() < before, "his foe struck in his path");
        check(h, m.getX() > target.getX(), "and he stands past him: " + (m.getX() - x0));
        h.succeed();
    }

    @GameTest(batch = "ff_clones", template = PLATFORM, timeoutTicks = 800)
    public static void hisHairClonesSpringUpAndAreHairAgain(GameTestHelper h) {
        SixEaredMacaqueEntity m = macaque(h, new BlockPos(2, 2, 2));
        m.setNoAi(true);
        m.startClones();
        eq(h, SixEaredMacaqueEntity.PLUCKING, m.action(), "the warning: he plucks and chews his hair");
        List<MacaqueCloneEntity> clones = m.clones();
        eq(h, SixEaredMacaqueEntity.CLONES, clones.size(), "three of himself");
        MacaqueCloneEntity c = clones.get(0);
        check(h, c.getDisplayName().getString().contains("Six-Eared Macaque"), "in his very likeness: " + c.getDisplayName().getString());
        check(h, !c.getPersistentData().contains(ZoneMonster.KEY_TIER), "a hair pays nothing");
        close(h, MacaqueCloneEntity.HITS * MobScaling.typicalHit(SixEaredMacaqueEntity.LEVEL), c.getMaxHealth(), "frail");
        check(h, c.getAttributeValue(Attributes.ATTACK_DAMAGE) <= 0.07 * MobScaling.typicalHealth(SixEaredMacaqueEntity.LEVEL), "a light mob's blow");
        clones.forEach(x -> x.setNoAi(true));
        h.runAfterDelay(MacaqueCloneEntity.LIFE + 5, () -> {
            check(h, clones.stream().noneMatch(Entity::isAlive), "gone in half a minute");
            h.succeed();
        });
    }

    @GameTest(batch = "ff_giant", template = PLATFORM)
    public static void atAThirdHeTakesHisDharmaImageAndSplitsTheGround(GameTestHelper h) {
        SixEaredMacaqueEntity m = macaque(h, new BlockPos(0, 2, 2));
        m.setNoAi(true);
        float h0 = m.getBbHeight();
        m.becomeGiant();
        check(h, m.isGiant() && m.getBbHeight() > h0 * 1.5f, "half again his height: " + m.getBbHeight());
        LivingEntity onLine = golem(h, new BlockPos(4, 2, 2), 1e9), off = golem(h, new BlockPos(1, 2, 4), 1e9);
        off.teleportTo(off.getX(), off.getY(), m.getZ() + 3.5);
        m.startSlam(onLine);
        eq(h, SixEaredMacaqueEntity.RAISING, m.action(), "the warning: the staff raised, its line lit");
        float a = onLine.getHealth(), b = off.getHealth();
        List<LivingEntity> hit = m.slam();
        check(h, hit.contains(onLine) && onLine.getHealth() < a, "on its line, struck");
        check(h, !hit.contains(off) && off.getHealth() == b, "off it, spared");
        h.succeed();
    }

    @GameTest(batch = "ff_phases", template = PLATFORM)
    public static void hisPhasesBringClonesThenTheDharmaImage(GameTestHelper h) {
        SixEaredMacaqueEntity m = macaque(h, new BlockPos(2, 2, 2));
        m.setNoAi(true);
        m.setHealth(m.getMaxHealth() * 0.6f);
        m.tick();
        eq(h, 1, m.phase(), "at two thirds, phase one");
        check(h, !m.isGiant(), "not yet giant");
        m.setHealth(m.getMaxHealth() * 0.3f);
        m.tick();
        eq(h, 2, m.phase(), "at a third");
        check(h, m.isGiant(), "his Dharma image");
        h.succeed();
    }

    @GameTest(batch = "ff_lair", template = PLATFORM, timeoutTicks = 200)
    public static void heKeepsToHisSanctum(GameTestHelper h) {
        SixEaredMacaqueEntity m = macaque(h, new BlockPos(2, 2, 2));
        BlockPos lair = m.blockPosition();
        m.restrictTo(lair, m.lairRadius());
        // just past his leash; the chunk he lands in kept ticking, as a player near him would keep it
        double x = m.getX() + SixEaredMacaqueEntity.LEASH + 3;
        net.minecraft.world.level.ChunkPos far = new net.minecraft.world.level.ChunkPos(BlockPos.containing(x, m.getY(), m.getZ()));
        h.getLevel().setChunkForced(far.x, far.z, true);
        m.teleportTo(x, m.getY(), m.getZ());
        h.succeedWhen(() -> {
            check(h, m.distanceToSqr(Vec3.atBottomCenterOf(lair)) < 9, "brought far off, he is back: " + m.position());
            h.getLevel().setChunkForced(far.x, far.z, false);
        });
    }

    // ── What they leave, XP and karma ───────────────────────────────────

    private static List<ItemStack> loot(GameTestHelper h, Mob mob, ServerPlayer p) {
        var source = h.getLevel().damageSources().playerAttack(p);
        LootParams params = new LootParams.Builder(h.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(LootContextParams.ATTACKING_ENTITY, p)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, p)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, p)
                .create(LootContextParamSets.ENTITY);
        return h.getLevel().getServer().reloadableRegistries().getLootTable(mob.getLootTable()).getRandomItems(params);
    }

    private static int count(List<ItemStack> items, Item item) {
        return items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(batch = "ff_loot", template = PLATFORM)
    public static void whatTheyLeave(GameTestHelper h) {
        ServerPlayer p = player(h);
        SixEaredMacaqueEntity m = macaque(h, new BlockPos(2, 2, 2));
        m.setNoAi(true);
        int staves = 0, rolls = 60;
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> items = loot(h, m, p);
            check(h, count(items, com.jttw.engine.registry.ModItems.IMMORTAL_PEACH.get()) >= 2, "immortal peaches: " + items);
            check(h, count(items, net.minecraft.world.item.Items.DIAMOND) >= 3, "diamonds");
            staves += count(items, FlowerFruitRegistry.MACAQUE_STAFF.get());
        }
        check(h, staves >= 12 && staves <= 48, "his iron staff, every other time: " + staves + " in " + rolls);
        List<ItemStack> fp = loot(h, pilgrim(h, FakePilgrimEntity.SHA, new BlockPos(0, 2, 0)), p);
        check(h, count(fp, net.minecraft.world.item.Items.GOLD_INGOT) >= 1 && count(fp, FlowerFruitRegistry.MONKEY_HAIR.get()) >= 2, "a pilgrim's: " + fp);
        int hair = 0;
        for (int i = 0; i < 10; i++) hair += count(loot(h, monkey(h, FlowerFruitRegistry.SPEAR_APE.get(), 20, false, new BlockPos(4, 2, 4)), p), FlowerFruitRegistry.MONKEY_HAIR.get());
        check(h, hair >= 10, "the monkeys leave their hair: " + hair);
        h.succeed();
    }

    private static void killedBy(ServerPlayer p, LivingEntity victim) {
        LevelManager.onEntityKilled(new LivingDeathEvent(victim, p.level().damageSources().playerAttack(p)));
    }

    @GameTest(batch = "ff_band", template = PLATFORM)
    public static void theMountainPaysItsBandAndSubduingTheMacaqueIsAGoodDeed(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 22);
        PlayerLevel.setXp(p, 0);
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.FLOWER_FRUIT_BIOME.location());
        check(h, band != null && band.minLevel() == 20 && band.maxLevel() == 30, "its band is 20-30");
        killedBy(p, monkey(h, FlowerFruitRegistry.STONE_GIBBON.get(), 20, false, new BlockPos(1, 2, 1)));
        eq(h, band.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a regular");
        killedBy(p, pilgrim(h, FakePilgrimEntity.TANG, new BlockPos(3, 2, 3)));
        eq(h, band.xpForTier(EnemyTier.GRUNT) + band.xpForTier(EnemyTier.GENERAL), PlayerLevel.getXp(p), "plus a fake pilgrim, a general");
        long xp = PlayerLevel.getXp(p);
        SixEaredMacaqueEntity m = macaque(h, new BlockPos(2, 2, 0));
        m.setNoAi(true);
        MacaqueCloneEntity clone = m.clones().get(0);
        killedBy(p, clone);
        eq(h, xp, PlayerLevel.getXp(p), "a hair clone pays nothing");
        eq(h, Morality.SLAY_MACAQUE, Morality.killDeed(p, m), "he beat the Tang Monk and stole his luggage");
        check(h, Morality.SLAY_MACAQUE > 0, "a good deed");
        eq(h, Morality.SLAY_CAPTAIN, Morality.killDeed(p, pilgrim(h, FakePilgrimEntity.BAJIE, new BlockPos(0, 2, 4))), "a fake pilgrim, a captain");
        h.succeed();
    }

    // ── The cave and the island in blocks ───────────────────────────────

    @GameTest(batch = "ff_plan", template = PLATFORM)
    public static void theWaterCurtainCaveInBlocks(GameTestHelper h) {
        int mid = (CavePlan.CHASM_Z0 + CavePlan.CHASM_Z1) / 2;
        check(h, CavePlan.at(0, 0, mid).is(Blocks.IRON_BLOCK), "an iron bridge over the chasm");
        check(h, CavePlan.at(CavePlan.DECK_HALF, 1, mid).is(Blocks.CHAIN), "chains for its rails");
        check(h, CavePlan.at(15, CavePlan.STREAM_TOP, mid).is(Blocks.WATER), "the stream under it");
        check(h, CavePlan.at(0, 1, CavePlan.GATE_Z).isAir() && !CavePlan.at(10, 1, CavePlan.GATE_Z).isAir(), "the stone gate's doorway in its wall");
        check(h, CavePlan.at(0, 13, CavePlan.GATE_Z).is(Blocks.POLISHED_BLACKSTONE) && CavePlan.at(4, 13, CavePlan.GATE_Z).is(Blocks.GOLD_BLOCK), "the tablet's dark face over it, framed in gold");
        check(h, CavePlan.SIGNS.stream().anyMatch(s -> String.join(" ", s.lines()).contains("Lv 80")), "a warning at the sanctum's way");
        check(h, CavePlan.at(0, 1, 0).isAir(), "the hall open");
        check(h, CavePlan.at(0, 3, CavePlan.SANCTUM_THRONE_Z - 1) != null, "the false king's throne");
        check(h, CavePlan.at(0, 0, CavePlan.SANCTUM_Z).isSolid(), "the sanctum's floor");
        check(h, FlowerFruitLifeFeature.campAt(4, 8, 4).is(Blocks.YELLOW_BANNER), "the Great Sage's banner over a camp");
        check(h, FlowerFruitLifeFeature.campAt(0, 1, 0).is(Blocks.CAMPFIRE), "its fire");
        int[] c = Zones.FLOWER_FRUIT.centres(h.getLevel().getSeed())[0];
        check(h, com.jttw.engine.world.feature.WaterCurtainCaveFeature.bossAt(c).getY() == IslandShape.BASIN_Y + 1, "the Macaque on the sanctum's floor");
        check(h, CavePlan.inSanctum(CavePlan.BOSS_X, CavePlan.BOSS_Z), "his place in the sanctum");
        h.succeed();
    }
}
