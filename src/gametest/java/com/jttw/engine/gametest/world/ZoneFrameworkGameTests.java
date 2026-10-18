package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.ridge.BoneServantEntity;
import com.jttw.engine.entity.ridge.LadyWhiteBoneEntity;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.ZoneAllies;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Predicate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/** The zone framework in the world: a boss's bar, and a zone's allies. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFrameworkGameTests {

    private static ServerPlayer survivor(GameTestHelper h, ReincarnationOutcome outcome) {
        ServerPlayer p = as(h, outcome);
        p.setGameMode(GameType.SURVIVAL); // a creative player is no one's enemy
        return p;
    }

    private static LadyWhiteBoneEntity lady(GameTestHelper h) {
        LadyWhiteBoneEntity lady = h.spawn(ModEntities.LADY_WHITE_BONE.get(), new BlockPos(4, 2, 4));
        lady.setNoAi(true);
        onEnd(h, lady::discard);
        return lady;
    }

    // ── The boss bar ────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aBossBarShowsToThoseNearAndGoesWithHer(GameTestHelper h) {
        ServerPlayer near = survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        ServerPlayer far = survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        far.moveTo(far.getX() + 80, far.getY(), far.getZ());
        LadyWhiteBoneEntity lady = lady(h);
        // a test's players are not in the world's list, so they are handed to the bar
        List<ServerPlayer> players = List.of(near, far);
        lady.updateBossBar(players);
        check(h, lady.bossBarViewers().contains(near), "near her, her bar shows");
        check(h, !lady.bossBarViewers().contains(far), "far off, not");
        near(h, 1.0, lady.bossBar().getProgress(), "at full health");
        check(h, lady.bossBar().getName().getString().startsWith("Lady White Bone"), "named: " + lady.bossBar().getName().getString());

        // whoever strikes her sees it, wherever they are
        lady.hurt(h.getLevel().damageSources().playerAttack(far), 10);
        lady.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5))); // struck, she may have vanished off
        lady.updateBossBar(players);
        check(h, lady.bossBarViewers().contains(far), "a fighter far off sees it");
        check(h, lady.bossBar().getProgress() < 1, "and her health fall");

        // her disguise is not given away by it
        lady.setDisguise(LadyWhiteBoneEntity.SERVANT);
        lady.updateBossBar(players);
        check(h, lady.bossBarViewers().isEmpty(), "none while she passes for one of her dead");
        lady.setDisguise(LadyWhiteBoneEntity.HERSELF);
        lady.updateBossBar(players);
        check(h, lady.bossBarViewers().contains(near), "back once she is found out");

        // she falls and rises: the bar stays; the last time, it goes
        for (int i = 0; i < LadyWhiteBoneEntity.FALSE_DEATHS; i++) {
            lady.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
            lady.die(h.getLevel().damageSources().playerAttack(near));
        }
        lady.updateBossBar(players);
        check(h, lady.bossBarViewers().contains(near), "she rose again: still there");
        lady.die(h.getLevel().damageSources().playerAttack(near));
        check(h, lady.bossBarViewers().isEmpty(), "gone with her death");
        lady.updateBossBar(players);
        check(h, lady.bossBarViewers().isEmpty(), "and stays gone");
        sweepHusks(h);
        h.succeed();
    }

    private static void sweepHusks(GameTestHelper h) {
        onEnd(h, () -> h.getLevel().getEntitiesOfClass(com.jttw.engine.entity.ridge.LadyHuskEntity.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(8)).forEach(Entity::discard));
    }

    @GameTest(template = PLATFORM)
    public static void aBossIsABossOfItsZone(GameTestHelper h) {
        LadyWhiteBoneEntity lady = lady(h);
        eq(h, EnemyTier.BOSS.name(), lady.getPersistentData().getString(com.jttw.engine.entity.zone.ZoneMonster.KEY_TIER), "pays as a Boss");
        eq(h, Zones.WHITE_BONE.id(), lady.getPersistentData().getString(com.jttw.engine.entity.zone.ZoneMonster.KEY_ZONE), "of the ridge");
        check(h, lady.isPersistenceRequired() && !lady.removeWhenFarAway(1e6), "never despawned");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aBossFightComesInPhases(GameTestHelper h) {
        List<Integer> begun = new java.util.ArrayList<>();
        LadyWhiteBoneEntity boss = new LadyWhiteBoneEntity(ModEntities.LADY_WHITE_BONE.get(), h.getLevel()) {
            @Override protected float[] phaseThresholds() { return new float[]{0.66f, 0.33f}; }
            @Override protected void onPhase(int phase) { begun.add(phase); }
            @Override public void tick() { checkPhase(); } // the phases only: nothing else of her
        };
        boss.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
        boss.setNoAi(true);
        h.getLevel().addFreshEntity(boss);
        onEnd(h, boss::discard);
        boss.tick();
        eq(h, 0, boss.phase(), "at full health, the first phase");
        boss.setHealth(boss.getMaxHealth() * 0.5f);
        boss.tick();
        eq(h, 1, boss.phase(), "past two thirds, the second");
        boss.setHealth(boss.getMaxHealth() * 0.1f);
        boss.tick();
        boss.tick();
        eq(h, 2, boss.phase(), "past a third, the third");
        eq(h, List.of(1, 2), begun, "each begun once");
        h.succeed();
    }

    // ── Allies ──────────────────────────────────────────────────────────

    /** A Bone Servant of a zone allied to the Peng. */
    private static BoneServantEntity pengsServant(GameTestHelper h, BlockPos at) {
        openAir(h); // four blocks tall: under the framework's roof it would choke in it
        BoneServantEntity m = new BoneServantEntity(ModEntities.BONE_SERVANT.get(), h.getLevel()) {
            @Override public Predicate<Player> allies() { return ZoneAllies.PENG; }
        };
        m.moveTo(h.absoluteVec(Vec3.atBottomCenterOf(at)));
        m.become(20, false);
        h.getLevel().addFreshEntity(m);
        onEnd(h, m::discard);
        return m;
    }

    @GameTest(template = PLATFORM)
    public static void anAlliedMobNeverTargetsNorFightsBackItsAlly(GameTestHelper h) {
        ServerPlayer peng = survivor(h, ReincarnationOutcome.GOLD_WINGED_PENG);
        ServerPlayer other = survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        BoneServantEntity m = pengsServant(h, new BlockPos(3, 2, 3));
        m.setNoAi(true);
        m.tickCount = 40; // a hurt at its first tick looks to HurtByTargetGoal like none at all

        check(h, m.isAlliedTo(peng), "allied to the Peng (so his area attacks pass it over)");
        check(h, !m.isAlliedTo(other), "not to anyone else");
        // what NearestAttackableTargetGoal asks of whoever is nearest
        TargetingConditions seek = TargetingConditions.forCombat().range(32);
        check(h, !seek.test(m, peng), "it never seeks out the Peng");
        check(h, seek.test(m, other), "it seeks out others");
        // what HurtByTargetGoal asks of whoever hurt it
        m.setLastHurtByMob(peng);
        check(h, !new HurtByTargetGoal(m).canUse(), "struck by the Peng, it does not fight back");
        m.setLastHurtByMob(other);
        check(h, new HurtByTargetGoal(m).canUse(), "struck by another, it does");
        // nor is it set on him by anyone
        m.setTarget(peng);
        check(h, m.getTarget() == null, "never made the Peng's foe");
        m.setTarget(other);
        check(h, m.getTarget() == other, "another's, yes");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void struckItTurnsOnOthersButNotOnItsAlly(GameTestHelper h) {
        ServerPlayer peng = survivor(h, ReincarnationOutcome.GOLD_WINGED_PENG);
        ServerPlayer other = survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        BoneServantEntity spared = pengsServant(h, new BlockPos(1, 2, 3));
        BoneServantEntity angered = pengsServant(h, new BlockPos(4, 2, 3));
        // not at their first tick: a hurt then looks to HurtByTargetGoal like none at all
        boolean[] hurt = new boolean[2];
        h.runAfterDelay(5, () -> {
            hurt[0] = spared.hurt(h.getLevel().damageSources().playerAttack(peng), 1);
            hurt[1] = angered.hurt(h.getLevel().damageSources().playerAttack(other), 1);
        });
        h.runAfterDelay(25, () -> {
            check(h, hurt[0], "the Peng's blow lands");
            check(h, spared.getTarget() == null, "struck by the Peng, it lets it be: " + spared.getTarget());
            check(h, angered.getTarget() == other, "struck by another, it turns on them: " + angered.getTarget()
                    + " (hurt: " + hurt[1] + ")");
            h.succeed();
        });
    }
}
