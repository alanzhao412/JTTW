package com.jttw.engine.gametest.gallery;

import com.jttw.engine.character.wukong.EyeLaser;
import com.jttw.engine.character.wukong.FightingBuddha;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Wukong's Fiery Golden Eyes firing, in the form gallery: at several outputs, from his side, over
 * his shoulder, at his face, through his own eyes (the HUD showing the output by slot N), and as
 * the Fighting Buddha. He stands facing south; ahead of him a stone-brick wall (the beam bores
 * through it), a husk, and an obsidian wall backed with bedrock, which stops the beam (the fifth
 * output burns through the obsidian; the Buddha's beam through all of it).
 */
final class EyeLaserScenes {

    private EyeLaserScenes() {}

    /** Wukong with the eyes, at this level and output, Qi to spare, ready to fire; the range built before him. */
    static void wukong(ServerPlayer p, int level, int output, boolean hud) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(p, level);
        if (level >= 100) p.getPersistentData().putBoolean(FightingBuddha.KEY_ANNOUNCED, true);
        LevelManager.reapplyStats(p);
        LevelManager.syncToClient(p);
        if (level >= 100) FightingBuddha.sync(p);
        ImmortalItems.grantFieryGoldenEyes(p);
        QiData.setMax(p, 5000f);
        QiData.setCurrent(p, 5000f);
        EyeLaser.setOutput(p, output);
        p.getPersistentData().remove(EyeLaser.KEY_READY_AT);
        if (hud) p.setGameMode(GameType.SURVIVAL); // the skill bar shows only out of creative
        range(p);
    }

    /** The range before him: ground made good, a brick wall, a husk, obsidian backed by bedrock. */
    private static void range(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = level.getMinBuildHeight() + 4;
        for (int x = -10; x <= 10; x++) for (int z = -6; z <= 30; z++) {
            set(level, x, g - 3, z, Blocks.DIRT);
            set(level, x, g - 2, z, Blocks.DIRT);
            set(level, x, g - 1, z, Blocks.GRASS_BLOCK);
            for (int y = 0; y <= 12; y++) set(level, x, g + y, z, Blocks.AIR);
        }
        for (int x = -4; x <= 4; x++) for (int y = 0; y <= 5; y++) for (int z = 8; z <= 9; z++) set(level, x, g + y, z, Blocks.STONE_BRICKS);
        for (int x = -6; x <= 6; x++) for (int y = 0; y <= 7; y++) {
            set(level, x, g + y, 18, Blocks.OBSIDIAN);
            set(level, x, g + y, 19, Blocks.BEDROCK);
        }
        level.getEntitiesOfClass(ItemEntity.class, new AABB(-12, g - 4, -8, 12, g + 14, 32)).forEach(ItemEntity::discard);
        var husk = EntityType.HUSK.create(level);
        if (husk != null) {
            husk.moveTo(0.5, g, 13.5, 180f, 0f);
            husk.setNoAi(true);
            husk.setYHeadRot(180f);
            husk.setYBodyRot(180f);
            level.addFreshEntity(husk);
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }

    /** A shot: him at this output, fired {@code before} ticks before the picture, seen from {@code cam} (null: his own eyes, with the HUD). */
    private static AbilityScenes.Scene shot(String name, int level, int output, int before, double[] cam) {
        boolean first = cam == null;
        return new AbilityScenes.Scene(name, p -> {
            wukong(p, level, output, first);
            AbilityScenes.facing = 0f;
            AbilityScenes.sceneCam = cam;
        }, EyeLaser::fire, before, first);
    }

    static List<AbilityScenes.Scene> all() {
        double[] side = {12.5, 3.2, 8.0, 0.5, 1.4, 9.5};
        double[] shoulder = {-2.3, 2.7, -3.2, 0.6, 1.4, 9.0};
        double[] face = {2.9, 2.0, 2.6, 0.5, 1.55, 1.0};
        double[] front = {4.5, 2.4, 6.0, 0.5, 1.5, 1.5};
        return List.of(
                shot("eye_laser_1_side", 40, 1, 3, side),
                shot("eye_laser_3_side", 40, 3, 3, side),
                shot("eye_laser_3_side_fading", 40, 3, 7, side),
                shot("eye_laser_5_side", 40, 5, 3, side),
                shot("eye_laser_3_shoulder", 40, 3, 3, shoulder),
                shot("eye_laser_5_shoulder", 40, 5, 3, shoulder),
                shot("eye_laser_2_face", 40, 2, 3, face),
                shot("eye_laser_4_front", 40, 4, 3, front),
                shot("eye_laser_2_first_person", 40, 2, 3, null),
                shot("eye_laser_5_first_person", 40, 5, 3, null),
                shot("eye_laser_buddha_3_side", 100, 3, 3, side));
    }
}
