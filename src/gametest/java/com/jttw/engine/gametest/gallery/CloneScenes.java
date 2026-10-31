package com.jttw.engine.gametest.gallery;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.wukong.WuCloneManager;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.entity.WuCloneEntity;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Wukong's shadow clones at work in the gallery: three set to work at a rock face seamed with
 * ore, each at its own vein and from a player's reach (none on top of what it mines); and three
 * at his side splitting between three zombies.
 */
final class CloneScenes {

    private CloneScenes() {}

    /** The rock face: across the stage, a few blocks in front of him. */
    private static final int X0 = -5, X1 = 5, Z0 = 6, Z1 = 8, H = 4;
    private static boolean built;
    private static final String STAGED = "jttw_gallery_clone_scene";

    /** Wukong with Shadow Clone III, Qi to spare, and an iron pickaxe and axe on him. */
    private static void wukong(ServerPlayer p) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        SkillTreeData.grantNode(p, "wu_clone_1");
        SkillTreeData.grantNode(p, "wu_clone_2");
        SkillTreeData.grantNode(p, "wu_clone_3");
        QiData.setMax(p, 500f);
        QiData.setCurrent(p, 500f);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        p.getInventory().add(new ItemStack(Items.IRON_SWORD));
    }

    private static void face(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos o = p.blockPosition();
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = X0; x <= X1; x++) for (int y = 0; y < H; y++) for (int z = Z0; z <= Z1; z++) {
            level.setBlock(o.offset(x, y, z), stone, Block.UPDATE_CLIENTS);
        }
        // three veins in its face: iron, coal, gold; and one buried behind
        for (BlockPos v : List.of(new BlockPos(-4, 0, 6), new BlockPos(-4, 1, 6), new BlockPos(-3, 1, 6), new BlockPos(-3, 1, 7)))
            level.setBlock(o.offset(v), Blocks.IRON_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (BlockPos v : List.of(new BlockPos(0, 1, 6), new BlockPos(1, 1, 6), new BlockPos(0, 2, 6), new BlockPos(1, 2, 7)))
            level.setBlock(o.offset(v), Blocks.COAL_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (BlockPos v : List.of(new BlockPos(4, 0, 6), new BlockPos(4, 1, 6), new BlockPos(3, 0, 6)))
            level.setBlock(o.offset(v), Blocks.GOLD_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
        built = true;
    }

    private static void summon(ServerPlayer p, int n) {
        for (int i = 0; i < n; i++) WukongCharacter.INSTANCE.use(p, "wu_clone", AbilityAction.USE);
    }

    static void clear(ServerPlayer p) {
        WuCloneManager.dismissAll(p);
        ServerLevel level = p.serverLevel();
        BlockPos o = p.blockPosition();
        level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(o).inflate(16),
                e -> e.getPersistentData().getBoolean(STAGED)).forEach(net.minecraft.world.entity.Entity::discard);
        if (!built) return;
        for (int x = X0 - 2; x <= X1 + 2; x++) for (int y = 0; y < H + 2; y++) for (int z = 0; z <= Z1 + 2; z++) {
            level.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(o).inflate(16))
                .forEach(net.minecraft.world.entity.Entity::discard);
        built = false;
    }

    static List<AbilityScenes.Scene> all() {
        return List.of(
                // set to work at the face: each at its own vein, from a step or two off
                new AbilityScenes.Scene("clones_mining", p -> {
                    wukong(p);
                    face(p);
                    AbilityScenes.facing = 0f;
                    AbilityScenes.sceneCam = new double[]{8.5, 4.5, 0.5, 0.0, 1.2, 5.0};
                }, p -> {
                    summon(p, 3);
                    for (WuCloneEntity c : WuCloneManager.clonesOf(p)) {
                        c.teleportTo(c.getX(), c.getY(), c.getZ() + 2);
                        c.setPriority(WuCloneEntity.Priority.WORK);
                    }
                }, 70, false),
                // later: the face opened, the buried ore reached
                new AbilityScenes.Scene("clones_mining_later", p -> {
                    wukong(p);
                    face(p);
                    AbilityScenes.facing = 0f;
                    AbilityScenes.sceneCam = new double[]{0.5, 6.5, 0.0, 0.0, 0.8, 6.0};
                }, p -> {
                    summon(p, 3);
                    for (WuCloneEntity c : WuCloneManager.clonesOf(p)) {
                        c.teleportTo(c.getX(), c.getY(), c.getZ() + 2);
                        c.setPriority(WuCloneEntity.Priority.WORK);
                    }
                }, 200, false),
                // at his side: three clones, three zombies, one each
                new AbilityScenes.Scene("clones_fighting", p -> {
                    wukong(p);
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                    AbilityScenes.facing = 0f;
                    AbilityScenes.sceneCam = new double[]{7.5, 4.0, 1.0, 0.0, 1.2, 5.0};
                    ServerLevel level = p.serverLevel();
                    for (int x : new int[]{-4, 0, 4}) {
                        var z = EntityType.ZOMBIE.create(level);
                        if (z == null) continue;
                        z.moveTo(p.getX() + x, p.getY(), p.getZ() + 7, 180f, 0f);
                        z.setNoAi(true);
                        z.setPersistenceRequired();
                        z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)); // no burning in the sun
                        z.getPersistentData().putBoolean(STAGED, true);
                        z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
                        z.setHealth(400);
                        level.addFreshEntity(z);
                    }
                }, p -> summon(p, 3), 40, false));
    }
}
