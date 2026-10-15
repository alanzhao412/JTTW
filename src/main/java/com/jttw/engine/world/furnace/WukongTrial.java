package com.jttw.engine.world.furnace;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.character.wukong.WukongCheckpoints;
import com.jttw.engine.character.wukong.WukongCheckpoints.Checkpoint;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.List;

/**
 * Sun Wukong in the Eight Trigrams Furnace. Anyone else sealed in its fire burns to death; so does
 * he, unless he has made himself immortal first: eaten the Peaches of Immortality and Laozi's
 * elixir pills, drunk the Heavenly Wine, and struck his name from the Book of Life and Death. Then
 * the fire cannot kill him; after {@value #TRIAL_TICKS} ticks inside he bursts the furnace apart
 * and comes out with the Fiery Golden Eyes, the Yao Wang arts opened to him (the first of them
 * his outright), and his power ever after half again what it would be.
 *
 * <p>That last is a measure on his power curve ({@link #BOON}), not a lump on his stats as they
 * stand, so it is worth the same whenever he goes in.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class WukongTrial {

    private WukongTrial() {}

    /** How long he must last in the fire. */
    public static final int TRIAL_TICKS = 200;
    /** His power, ever after, over what it would be. */
    public static final double BOON = 1.5;

    public static final String KEY_SURVIVED = "jttw_furnace_survived", KEY_TICKS = "jttw_furnace_trial_ticks";

    /** What must be done before the fire cannot kill him. */
    public static final List<Checkpoint> PREREQUISITES =
            List.of(Checkpoint.HELLS_BOOK, Checkpoint.IMMORTAL_PEACHES, Checkpoint.HEAVENLY_WINE, Checkpoint.IMMORTALITY_PILLS);

    public static boolean survived(Player p) {
        return p.getPersistentData().getBoolean(KEY_SURVIVED);
    }

    public static boolean ready(Player p) {
        return WukongCharacter.is(p) && PREREQUISITES.stream().allMatch(cp -> WukongCheckpoints.isComplete(p, cp));
    }

    /** His power's measure from the trial: {@link #BOON} once he has survived it. */
    public static double boon(Player p) {
        return survived(p) ? BOON : 1;
    }

    /**
     * One tick of this one in the furnace's fire: true if the fire does not harm them (Wukong,
     * immortal), and at the end of his trial, the furnace bursts.
     */
    public static boolean endures(TrigramsFurnaceBlockEntity furnace, LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || !WukongCharacter.is(p)) return false;
        if (survived(p)) return true; // it has nothing left to do to him
        if (!ready(p)) return false;
        int ticks = p.getPersistentData().getInt(KEY_TICKS) + 1;
        p.getPersistentData().putInt(KEY_TICKS, ticks);
        if (ticks >= TRIAL_TICKS) burstOut(furnace, p);
        return true;
    }

    /** The furnace bursts apart, and Wukong comes out of it changed. */
    static void burstOut(TrigramsFurnaceBlockEntity furnace, ServerPlayer p) {
        p.getPersistentData().remove(KEY_TICKS);
        p.getPersistentData().putBoolean(KEY_SURVIVED, true);
        WukongCheckpoints.complete(p, Checkpoint.TRUE_SAMADHI_FIRE);
        ImmortalItems.grantFieryGoldenEyes(p);
        SkillTreeData.grantNode(p, YAO_WANG_FIRST);
        LevelManager.reapplyStats(p);
        LevelManager.syncToClient(p);

        Level level = furnace.getLevel();
        BlockPos core = furnace.getBlockPos();
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, core.getX() + 0.5, core.getY() + 2.5, core.getZ() + 0.5, 3, 1, 1, 1, 0);
            server.sendParticles(ParticleTypes.FLAME, core.getX() + 0.5, core.getY() + 2.5, core.getZ() + 0.5, 200, 1.5, 2, 1.5, 0.2);
            server.playSound(null, core, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4f, 0.6f);
            level.destroyBlock(core, false); // kicked over: the furnace is no more
        }
        p.teleportTo(core.getX() + 0.5, core.getY() + 5.5, core.getZ() + 0.5);
        p.setDeltaMovement(0, 1.2, 0);
        p.hurtMarked = true;
        p.clearFire();
        p.sendSystemMessage(Component.literal("§6§lYou burst out of the Eight Trigrams Furnace!"));
        p.sendSystemMessage(Component.literal("§eThe Samadhi Fire has given you the Fiery Golden Eyes, and the Yao Wang arts are open to you."));
    }

    // ── Yao Wang waits on the furnace ────────────────────────────────────

    public static final String YAO_WANG_FIRST = "wu_combat_1";

    /** Whether this node is one of the Yao Wang arts and he has not yet survived the furnace. */
    public static boolean lockedUntilFurnace(Player p, String nodeId) {
        return nodeId.startsWith("wu_combat_") && !survived(p);
    }

    /** The trial's outcome outlasts death. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (survived(event.getOriginal())) event.getEntity().getPersistentData().putBoolean(KEY_SURVIVED, true);
    }
}
