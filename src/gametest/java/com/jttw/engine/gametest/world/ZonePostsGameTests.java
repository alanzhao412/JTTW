package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.zone.MobLevel;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.BlackWindRegistry;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.world.ZonePosts;
import com.jttw.engine.world.cave.CaveHousehold;
import com.jttw.engine.world.ridge.CaveGarrison;
import com.jttw.engine.world.ridge.WhiteBoneCave;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.jttw.engine.gametest.GT.*;

/**
 * Every zone's posts come back ({@link ZonePosts}): a post whose holders are all dead stays empty
 * {@value ZonePosts#RESPAWN_MINUTES} minutes, is not held again while someone is in it, and then is
 * held again by the same as held it: each its kind, rank, level, health and name, held to the same
 * post. One test to each zone, its post put as the land puts it (where that is a call a test can make,
 * through it: the White Bone Cave's gate wardens, the Bottomless Cave's gate guard), its ground's
 * chunks held while it is counted. Flower-Fruit's garrison has its own tests (FlowerFruitGarrisonGameTests);
 * Mount Wuzhuang's household is finite, and never comes back (TempleRecord counts it down to Zhenyuan).
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZonePostsGameTests {

    private static final TicketType<ChunkPos> HELD = TicketType.create("jttw_gametest_zone_posts", java.util.Comparator.comparingLong(ChunkPos::toLong), 20 * 60 * 10);

    private static void hold(ServerLevel level, BlockPos post, int reach) {
        for (int cx = (post.getX() - reach) >> 4; cx <= (post.getX() + reach) >> 4; cx++) {
            for (int cz = (post.getZ() - reach) >> 4; cz <= (post.getZ() + reach) >> 4; cz++) {
                ChunkPos c = new ChunkPos(cx, cz);
                level.getChunkSource().addRegionTicket(HELD, c, 2, c, true);
            }
        }
    }

    private static void whenLoaded(GameTestHelper h, BlockPos post, int reach, Runnable body) {
        if (!ZonePosts.loaded(h.getLevel(), post, reach)) {
            h.runAfterDelay(5, () -> whenLoaded(h, post, reach, body));
            return;
        }
        body.run();
    }

    /** One of a zone's kind, as the land puts it: made where it stands, a structure's. */
    @SuppressWarnings("unchecked")
    private static <T extends Mob> T made(ServerLevel level, EntityType<? extends T> type, BlockPos at) {
        T m = (T) type.create(level);
        m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        m.setPersistenceRequired();
        return m;
    }

    private record Was(EntityType<?> type, int level, boolean captain, boolean miniboss, float health, String name, String zone) {
        static Was of(Mob m) {
            return new Was(m.getType(), MobLevel.of(m), m instanceof ZoneMonster z && z.isCaptain(), m instanceof ZoneMonster z && z.isMiniboss(),
                    m.getMaxHealth(), m.getName().getString(), m.getPersistentData().getString(ZoneMonster.KEY_ZONE));
        }
    }

    /**
     * The post put by {@code put} at the platform's middle (its holders kept, in the world or not yet): held, it is left alone; all killed, it is marked cleared and stays empty; its time come
     * round, not held again while someone is in it; then, no one near, held again by the same.
     */
    private static void returns(GameTestHelper h, Function<BlockPos, List<? extends Mob>> put) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = player(h);
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        List<? extends Mob> held = put.apply(at);
        check(h, !held.isEmpty(), "a post put");
        BlockPos post = ZonePosts.postOf(held.get(0));
        int reach = ZonePosts.reachOf(held.get(0));
        check(h, post != null && reach > 0, "kept at a post: " + post + ", " + reach);
        for (Mob m : held) {
            check(h, ZonePosts.kept(m), m.getName().getString() + " carries its likeness");
            eq(h, post, ZonePosts.postOf(m), m.getName().getString() + "'s post, the band's");
        }
        List<Was> was = held.stream().map(Was::of).toList();
        var state = ZonePosts.State.of(level);
        state.forget(level.dimension(), post);
        hold(level, post, reach);
        List<Mob> again = new ArrayList<>();
        onEnd(h, () -> {
            held.forEach(Entity::discard);
            again.forEach(Entity::discard);
            state.forget(level.dimension(), post);
        });
        held.forEach(m -> {
            if (!m.isAddedToLevel()) level.addFreshEntityWithPassengers(m); // the land's own putting adds some itself
            m.setNoAi(true);
        });
        whenLoaded(h, post, reach, () -> {
            // held: nothing to do
            p.moveTo(post.getX() + reach + ZonePosts.NEAR + 20, post.getY(), post.getZ() + 0.5);
            check(h, ZonePosts.look(level, post, List.of(p)).isEmpty(), "held: nothing put back");
            // all killed: written down, marked cleared, left empty
            held.forEach(Mob::kill);
            eq(h, held.size(), state.fallen(level.dimension(), post), "every one of them written down as it fell");
            check(h, ZonePosts.look(level, post, List.of(p)).isEmpty() && state.clearedAt(level.dimension(), post) > 0, "marked cleared");
            check(h, ZonePosts.look(level, post, List.of(p)).isEmpty() && ZonePosts.holders(level, post, reach).isEmpty(), "a cleared post stays empty");
            // its time come round, but someone in it: still empty
            state.cleared(level.dimension(), post, level.getGameTime() - ZonePosts.RESPAWN - 1);
            p.moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5);
            check(h, ZonePosts.look(level, post, List.of(p)).isEmpty(), "not held again under someone's nose");
            p.moveTo(post.getX() + reach + ZonePosts.NEAR - 2, post.getY(), post.getZ() + 0.5);
            check(h, ZonePosts.look(level, post, List.of(p)).isEmpty(), "nor with someone near it");
            // no one near: held again, by the same
            p.moveTo(post.getX() + reach + ZonePosts.NEAR + 20, post.getY(), post.getZ() + 0.5);
            again.addAll(ZonePosts.look(level, post, List.of(p)));
            eq(h, held.size(), again.size(), "the whole of it back");
            List<Was> now = new ArrayList<>(again.stream().map(Was::of).toList());
            for (Was w : was) check(h, now.remove(w), "back as it was: " + w + " among " + now);
            for (Mob m : again) {
                check(h, m.isAlive() && m.hasRestriction() && post.equals(m.getRestrictCenter()), m.getName().getString() + " held to its post");
                check(h, ZonePosts.kept(m), m.getName().getString() + " kept again, to come back again");
                check(h, m.getHealth() == m.getMaxHealth(), m.getName().getString() + " whole");
            }
            eq(h, 0, state.fallen(level.dimension(), post), "its dead forgotten, put back");
            check(h, ZonePosts.holders(level, post, reach).size() == held.size(), "counted at its post");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "zone_posts_white_bone", timeoutTicks = 600)
    public static void whiteBonePostsReturn(GameTestHelper h) {
        // the Keepers of the Bone Gate: outside, miniboss wardens, through the cave's own garrison
        returns(h, at -> {
            List<Mob> out = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                WhiteBoneCave.Post post = new WhiteBoneCave.Post(0, 0, 0, WhiteBoneCave.OUTSIDE, i == 0 ? WhiteBoneCave.SERVANT : WhiteBoneCave.WRAITH,
                        WhiteBoneCave.WARDEN, 28);
                var m = CaveGarrison.spawn(h.getLevel(), post, i, at, h.getLevel().random, MobSpawnType.STRUCTURE);
                if (m != null) out.add(m);
            }
            return out;
        });
    }

    @GameTest(template = PLATFORM, batch = "zone_posts_black_wind", timeoutTicks = 600)
    public static void blackWindPostsReturn(GameTestHelper h) {
        // a camp's band round its fire: a boar captain and two imps
        returns(h, at -> {
            List<Mob> out = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                com.jttw.engine.entity.blackwind.BlackWindMonster m = made(h.getLevel(), i == 0 ? BlackWindRegistry.BOAR_DEMON.get() : BlackWindRegistry.WIND_IMP.get(), at);
                m.become(i == 0 ? m.rollCaptainLevel(h.getLevel().random) : m.rollRegularLevel(h.getLevel().random), i == 0);
                m.restrictTo(at, 12);
                ZonePosts.keep(m);
                out.add(m);
            }
            return out;
        });
    }

    @GameTest(template = PLATFORM, batch = "zone_posts_bottomless", timeoutTicks = 600)
    public static void bottomlessPostsReturn(GameTestHelper h) {
        // the gate's guard before the great door, through the household's own putting (it adds them itself)
        returns(h, at -> {
            return CaveHousehold.gateGuard(h.getLevel(), at, h.getLevel().random);
        });
    }

    @GameTest(template = PLATFORM, batch = "zone_posts_waters", timeoutTicks = 600)
    public static void watersPostsReturn(GameTestHelper h) {
        // a palace's hall guard: held to no reach of his own, he is kept where he stands
        returns(h, at -> {
            var m = made(h.getLevel(), ModEntities.SHRIMP_SOLDIER.get(), at);
            m.become(m.rollRegularLevel(h.getLevel().random), false);
            ZonePosts.keep(m);
            return List.of(m);
        });
    }

    @GameTest(template = PLATFORM, batch = "zone_posts_hell", timeoutTicks = 600)
    public static void hellPostsReturn(GameTestHelper h) {
        // King Yama's door guard: an Ox-Head captain, put as a court's are (Site.mob keeps him)
        returns(h, at -> {
            var m = made(h.getLevel(), ModEntities.OX_HEAD.get(), at);
            m.become(m.regularLevel(), true);
            ZonePosts.keep(m);
            return List.of(m);
        });
    }

    @GameTest(template = PLATFORM, batch = "zone_posts_fire", timeoutTicks = 600)
    public static void flamingMountainsPostsReturn(GameTestHelper h) {
        // an outpost's band: an imp captain and a brute
        returns(h, at -> {
            List<Mob> out = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                com.jttw.engine.entity.fire.FireMonster m = made(h.getLevel(), i == 0 ? ModEntities.EMBER_IMP.get() : ModEntities.FURNACE_BRUTE.get(), at);
                m.become(i == 0 ? m.rollCaptainLevel(h.getLevel().random) : m.rollRegularLevel(h.getLevel().random), i == 0);
                m.restrictTo(at, 14);
                ZonePosts.keep(m);
                out.add(m);
            }
            return out;
        });
    }

    @GameTest(template = PLATFORM, batch = "zone_posts_spider", timeoutTicks = 600)
    public static void spiderCavePostsReturn(GameTestHelper h) {
        // a hall's keepers: a Mantis Son captain and a Cockroach Son
        returns(h, at -> {
            List<Mob> out = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                com.jttw.engine.entity.spider.SilkMonster m = made(h.getLevel(), i == 0 ? SpiderCaveRegistry.MANTIS_SON.get() : SpiderCaveRegistry.COCKROACH_SON.get(), at);
                m.become(i == 0 ? 85 : 80, i == 0);
                m.keepTo(at, 12);
                ZonePosts.keep(m);
                out.add(m);
            }
            return out;
        });
    }

    @GameTest(template = PLATFORM, batch = "zone_posts_lion", timeoutTicks = 600)
    public static void lionCamelPostsReturn(GameTestHelper h) {
        // a floor's warden: a lion-demon miniboss, kept to his floor
        returns(h, at -> {
            var m = made(h.getLevel(), ModEntities.LION_DEMON.get(), at);
            m.becomeMiniboss(98, 1.5);
            m.keepTo(at, 10, 40, at.getY() - 4, at.getY() + 8);
            ZonePosts.keep(m);
            return List.of(m);
        });
    }
}
