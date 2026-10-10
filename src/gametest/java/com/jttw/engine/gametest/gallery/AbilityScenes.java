package com.jttw.engine.gametest.gallery;

import com.jttw.engine.character.bulldemon.BullDemonAbilityHandler;
import com.jttw.engine.character.marshal.MarshalAbilityHandler;
import com.jttw.engine.item.BananaLeafFanItem;
import com.jttw.engine.character.ninespirits.NineSpiritsAbilityHandler;
import com.jttw.engine.character.peng.PengAbilityHandler;
import com.jttw.engine.item.YinYangVaseItem;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.TruffleScentPacket;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * Ability scenes for the form gallery: each sets a character up, fires an ability a few ticks
 * before the picture, and is photographed from a three-quarter camera, or in first person with
 * the HUD showing. The player stands at the origin facing south (+z); targets stand in front.
 */
final class AbilityScenes {

    private AbilityScenes() {}

    /**
     * @param setup     on the server, after the stage is reset: character, nodes, items, targets
     * @param trigger   on the server, {@code triggerBefore} ticks before the picture
     * @param firstPerson photograph through the player's eyes with the HUD on
     * @param pitch     where the player looks: 0 ahead, positive down
     */
    record Scene(String name, Consumer<ServerPlayer> setup, Consumer<ServerPlayer> trigger,
                 int triggerBefore, boolean firstPerson, float pitch) {
        Scene(String name, Consumer<ServerPlayer> setup, Consumer<ServerPlayer> trigger,
              int triggerBefore, boolean firstPerson) {
            this(name, setup, trigger, triggerBefore, firstPerson, 0f);
        }
    }

    /** Set by a scene of Wukong at his top speed: the gallery keeps his momentum at its height. */
    static volatile boolean topSpeed;

    /** Keeps a top-speed scene's momentum at its height, each tick. */
    static void holdTopSpeed(ServerPlayer p) {
        if (topSpeed) com.jttw.engine.flight.JTTWFlight.setBoost(p, com.jttw.engine.character.wukong.WukongAbilityHandler.momentumMax(), false);
    }

    /** Set by a scene that photographs a screen (a furnace's, say), so the gallery leaves it open. */
    static volatile boolean keepScreen;

    static List<Scene> all() {
        return List.of(
                // ── Blocks ──
                new Scene("block_furnace", p -> {
                    // the great furnace, lit, its hatch open
                    var f = com.jttw.engine.world.furnace.TrigramsFurnaceBlock.build(p.serverLevel(), p.blockPosition().offset(1, 0, 6));
                    f.ignite(com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity.SAMADHI_BURN);
                    f.setHatch(true);
                }, p -> {}, 1, false),
                // ── Flight: stretched along it (Dragon Block C), or Wukong upright on his cloud ──
                new Scene("flight_hover", p -> fly(p, ReincarnationOutcome.THIRD_LOTUS_PRINCE, 0), p -> {}, 1, false, 0f),
                new Scene("flight_under_way", p -> fly(p, ReincarnationOutcome.THIRD_LOTUS_PRINCE, 1), p -> {}, 1, false, 0f),
                new Scene("flight_climbing", p -> fly(p, ReincarnationOutcome.THIRD_LOTUS_PRINCE, 1), p -> {}, 1, false, -45f),
                new Scene("flight_cloud_hover", p -> fly(p, ReincarnationOutcome.CELESTIAL_PRIMATE, 0), p -> {}, 1, false, 0f),
                new Scene("flight_cloud_under_way", p -> fly(p, ReincarnationOutcome.CELESTIAL_PRIMATE, 1), p -> {}, 1, false, 0f),
                new Scene("flight_squall_hover", p -> fly(p, ReincarnationOutcome.EYE_OF_HEAVEN, 0), p -> {}, 1, false, 0f),
                new Scene("flight_squall_under_way", p -> fly(p, ReincarnationOutcome.EYE_OF_HEAVEN, 1), p -> {}, 1, false, 0f),
                new Scene("heaven_pavilion", p -> com.jttw.engine.world.heaven.HeavenFeatures.IslandBuilding.pavilion(
                        p.serverLevel(), p.blockPosition().offset(0, 0, 8), p.getRandom()), p -> {}, 1, true, 5f),
                new Scene("heaven_garden", p -> com.jttw.engine.world.heaven.HeavenFeatures.IslandBuilding.garden(
                        p.serverLevel(), p.blockPosition().offset(0, 0, 9), p.getRandom()), p -> {}, 1, true, 8f),
                new Scene("heaven_palace", p -> {
                    // the earlier scenes' pavilion and garden stand in the way: clear the view first
                    var level = p.serverLevel();
                    for (int dx = -12; dx <= 12; dx++) for (int dz = 2; dz <= 14; dz++) for (int dy = 0; dy <= 10; dy++) {
                        level.setBlockAndUpdate(p.blockPosition().offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                    }
                    com.jttw.engine.world.heaven.HeavenFeatures.JadePalace.build(level, p.blockPosition().offset(0, 0, 24), p.getRandom());
                }, p -> {}, 1, true, -4f),
                // ── Screens ──
                new Scene("ui_furnace", p -> {
                    var level = p.serverLevel();
                    BlockPos at = p.blockPosition().offset(0, 0, 2);
                    level.setBlockAndUpdate(at, com.jttw.engine.registry.ModBlocks.EIGHT_TRIGRAMS_FURNACE.get().defaultBlockState());
                    var f = (com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity) level.getBlockEntity(at);
                    var spear = new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.FIRE_TIP_SPEAR.get());
                    com.jttw.engine.item.Armaments.setRefine(spear, 4);
                    f.setItem(com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity.CENTRE, spear);
                    f.setItem(0, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.SERVANT_BONE.get(), 12));
                    f.setItem(1, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.WRAITH_WISP.get(), 5));
                    f.setItem(2, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.CAPTAINS_SIGIL.get(), 2));
                    f.setItem(com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity.FUEL, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 9));
                    keepScreen = true;
                    net.minecraftforge.network.NetworkHooks.openScreen(p, f, at);
                }, p -> {}, 1, true),
                new Scene("ui_armory", p -> {
                    AlignmentData.markReincarnated(p, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
                    LevelManager.initClass(p, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
                    var armory = com.jttw.engine.item.Armory.of(p);
                    var spear = new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.FIRE_TIP_SPEAR.get());
                    com.jttw.engine.item.Armaments.setRefine(spear, 6);
                    armory.setItem(0, spear);
                    armory.setItem(1, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.QIANKUN_RING.get()));
                    armory.setItem(5, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.HUNTIAN_CLOTH.get()));
                    keepScreen = true;
                    com.jttw.engine.network.c2s.ArmoryPacket.open(p);
                }, p -> {}, 1, true),
                // ── HUD: the Heavenly's minimap, Zhu Bajie's Keen Ears ──
                new Scene("hud_minimap", p -> {
                    AlignmentData.markReincarnated(p, ReincarnationOutcome.EYE_OF_HEAVEN);
                    LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
                    // something on the land to map: a pond, a sand path, a stone yard
                    var level = p.serverLevel();
                    BlockPos ground = p.blockPosition().below();
                    for (int dx = -14; dx <= -6; dx++) for (int dz = 4; dz <= 12; dz++) {
                        level.setBlockAndUpdate(ground.offset(dx, 0, dz), Blocks.WATER.defaultBlockState());
                    }
                    for (int dz = -20; dz <= 20; dz++) {
                        level.setBlockAndUpdate(ground.offset(3, 0, dz), Blocks.SAND.defaultBlockState());
                        level.setBlockAndUpdate(ground.offset(4, 0, dz), Blocks.SAND.defaultBlockState());
                    }
                    for (int dx = 8; dx <= 16; dx++) for (int dz = -16; dz <= -8; dz++) {
                        level.setBlockAndUpdate(ground.offset(dx, 1, dz), Blocks.STONE.defaultBlockState());
                    }
                    targets(p, 6, 14, -10, 18); // husks: hostile, red on the map
                    Mob pig = EntityType.PIG.create(level);
                    pig.moveTo(p.getX() - 4, p.getY(), p.getZ() + 20, 0f, 0f);
                    pig.setNoAi(true);
                    level.addFreshEntity(pig);
                }, p -> {}, 1, true),
                new Scene("hud_keen_ears", p -> {
                    AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
                    LevelManager.initClass(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
                }, p -> {
                    // sounds round him, near and far: a zombie close on his left, a creeper far off on his right, steps behind
                    var level = p.serverLevel();
                    level.playSound(null, p.getX() + 3, p.getY(), p.getZ() + 4, net.minecraft.sounds.SoundEvents.ZOMBIE_AMBIENT, net.minecraft.sounds.SoundSource.HOSTILE, 1f, 1f);
                    level.playSound(null, p.getX() - 9, p.getY(), p.getZ() + 18, net.minecraft.sounds.SoundEvents.CREEPER_PRIMED, net.minecraft.sounds.SoundSource.HOSTILE, 1f, 1f);
                    level.playSound(null, p.getX(), p.getY(), p.getZ() - 5, net.minecraft.sounds.SoundEvents.IRON_GOLEM_STEP, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1f);
                    level.playSound(null, p.getX() + 1, p.getY(), p.getZ() + 10, net.minecraft.sounds.SoundEvents.CHEST_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
                }, 6, true),
                // ── White Bone Ridge's dead ──
                new Scene("xtq_howl", p -> {}, p -> {
                    // a howl crossing in front of the camera, from left to right
                    var howl = new com.jttw.engine.entity.HowlProjectileEntity(com.jttw.engine.registry.ModEntities.HOWL_PROJECTILE.get(), p.level());
                    howl.setPos(p.getX() - 1, p.getY() + 1.4, p.getZ() + 5);
                    howl.shoot(0.6, 0, -0.8, 0.25f, 0); // across the camera's view
                    howl.setNoGravity(true);
                    p.level().addFreshEntity(howl);
                }, 6, false),
                // ── The Saurian Demon King: seen from his own eyes ──
                new Scene("saurian_lash", p -> {
                    saurian(p, com.jttw.engine.character.saurian.SaurianSkillTree.LASH_1);
                    foe(p, 0, 9);
                }, p -> com.jttw.engine.character.saurian.SaurianAbilityHandler.lash(p), 2, true),
                new Scene("saurian_whirlpool", p -> {
                    saurian(p, com.jttw.engine.character.saurian.SaurianSkillTree.WHIRL_1);
                    foe(p, -2, 7);
                    foe(p, 2, 8);
                }, p -> com.jttw.engine.character.saurian.SaurianAbilityHandler.whirlpool(p), 25, true, 35f),
                new Scene("saurian_wrath", p -> {
                    saurian(p, com.jttw.engine.character.saurian.SaurianSkillTree.WRATH_1);
                    foe(p, 0, 3);
                    foe(p, 1, 4);
                }, p -> {
                    com.jttw.engine.character.saurian.SaurianAbilityHandler.wrath(p);
                    p.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, p.getBoundingBox().inflate(6))
                            .stream().min(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(p))).ifPresent(p::attack);
                }, 3, true),
                new Scene("saurian_surge", p -> saurian(p, com.jttw.engine.character.saurian.SaurianSkillTree.SURGE_1),
                        p -> com.jttw.engine.character.saurian.SaurianAbilityHandler.surge(p, 1f), 2, false),
                new Scene("heaven_host", p -> {
                    soldier(p, -2, 2, false);
                    soldier(p, 1, 4, true);
                    soldier(p, -4, 6, false);
                }, p -> {}, 1, false),
                new Scene("ridge_servants", p -> {
                    ridge(p, com.jttw.engine.registry.ModEntities.BONE_SERVANT.get(), -3, 1, false);
                    ridge(p, com.jttw.engine.registry.ModEntities.BONE_SERVANT.get(), -6, 5, true);
                }, p -> {}, 1, false),
                new Scene("ridge_wraiths", p -> {
                    ridge(p, com.jttw.engine.registry.ModEntities.WRAITH.get(), -2, 2, false);
                    ridge(p, com.jttw.engine.registry.ModEntities.WRAITH.get(), -4, 5, true);
                }, p -> {}, 1, false),
                new Scene("ridge_names", p -> {
                    ridge(p, com.jttw.engine.registry.ModEntities.BONE_SERVANT.get(), -3, 9, false);
                    ridge(p, com.jttw.engine.registry.ModEntities.WRAITH.get(), 0, 6, false);
                    ridge(p, com.jttw.engine.registry.ModEntities.WRAITH.get(), 3, 8, true);
                }, p -> {}, 1, true, -8f),
                new Scene("ridge_lady", p -> {
                    var lady = com.jttw.engine.registry.ModEntities.LADY_WHITE_BONE.get().create(p.serverLevel());
                    lady.moveTo(p.getX() + 1, p.getY(), p.getZ() + 5, -63f, 0f);
                    lady.setYHeadRot(-63f);
                    lady.setYBodyRot(-63f);
                    lady.setNoAi(true);
                    p.serverLevel().addFreshEntity(lady);
                }, p -> {}, 1, false),
                // ── Grand Saint of Nine Spirits ──
                new Scene("ns_fire_head", p -> saint(p, NineSpiritsAbilityHandler.FIRE, "ns_heads_1"),
                        NineSpiritsAbilityHandler::fireHead, 4, false),
                new Scene("ns_frost_head", p -> saint(p, NineSpiritsAbilityHandler.FROST, "ns_heads_1", "ns_heads_2", "ns_heads_3"),
                        NineSpiritsAbilityHandler::fireHead, 4, false),
                new Scene("ns_thunder_head", p -> {
                    saint(p, NineSpiritsAbilityHandler.THUNDER, "ns_heads_1", "ns_heads_2", "ns_heads_3");
                    targets(p, 0, 5, 2, 6, -2, 6);
                }, NineSpiritsAbilityHandler::fireHead, 6, false),
                new Scene("ns_roar", p -> {
                    saint(p, NineSpiritsAbilityHandler.FIRE, "ns_roar_1", "ns_roar_2", "ns_roar_3");
                    targets(p, 0, 4, 1, 5, -1, 5);
                }, NineSpiritsAbilityHandler::roar, 6, false),
                new Scene("ns_mane_flare", p -> saint(p, NineSpiritsAbilityHandler.FIRE, "ns_flare_1", "ns_flare_2"),
                        NineSpiritsAbilityHandler::flare, 6, false),
                new Scene("ns_true_form", p -> {
                    saint(p, NineSpiritsAbilityHandler.FIRE, "ns_form_1", "ns_form_2", "ns_form_3");
                    NineSpiritsAbilityHandler.toggleTrueForm(p);
                }, p -> {}, 1, false),
                new Scene("ns_head_tint_thunder", p -> {
                    saint(p, NineSpiritsAbilityHandler.THUNDER, "ns_heads_1", "ns_heads_2");
                    NineSpiritsAbilityHandler.syncHead(p);
                }, p -> {}, 1, true),
                new Scene("bell_of_taiyi_in_hand", p -> p.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.BELL_OF_TAIYI.get())), p -> {}, 1, true),

                // ── Marshal of the Heavenly Canopy ──
                new Scene("mc_belly_slam", p -> {
                    marshal(p, "mc_slam_1", "mc_slam_2", "mc_slam_3", "mc_stomach_1");
                    p.getFoodData().setFoodLevel(20);
                    MarshalAbilityHandler.setReserve(p, 20);
                    targets(p, 0, 3, 3, 2, -3, 2, 2, 5, -2, 5);
                }, p -> MarshalAbilityHandler.slamImpact(p), 6, false),
                new Scene("mc_truffle_snout", p -> {
                    marshal(p, "mc_snout_1", "mc_snout_2", "mc_snout_3");
                    bury(p, 1, 4, Blocks.DIAMOND_ORE);
                    bury(p, -2, 6, Blocks.GOLD_ORE);
                    bury(p, 3, 7, Blocks.IRON_ORE);
                    bury(p, 0, 9, Blocks.CHEST);
                    targets(p, 4, 10, -5, 12);
                }, p -> MarshalAbilityHandler.sniff(p), 4, true, 35f),
                new Scene("mc_second_stomach_hud", p -> {
                    marshal(p, "mc_stomach_1");
                    p.setGameMode(GameType.SURVIVAL); // the food bar only shows in survival
                    p.getFoodData().setFoodLevel(20);
                    MarshalAbilityHandler.setReserve(p, 13);
                }, p -> {}, 4, true),
                new Scene("mc_rake_burst", p -> {
                    marshal(p);
                    p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NINE_TOOTH_RAKE.get()));
                    targets(p, 0, 2, 1, 3, -1, 3);
                }, p -> p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(3)).stream()
                        .min(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(p))).ifPresent(p::attack), 6, false),
                new Scene("nine_tooth_rake_in_hand", p -> p.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.NINE_TOOTH_RAKE.get())), p -> {}, 1, true),

                // ── Gold-Winged Peng ──
                new Scene("gp_feather_storm", p -> {
                    peng(p, "gp_feathers_1", "gp_feathers_2", "gp_feathers_3");
                    targets(p, 0, 9, 3, 9, -3, 9);
                }, p -> PengAbilityHandler.featherStorm(p), 3, false),
                new Scene("gp_eclipse_wings", p -> {
                    peng(p, "gp_eclipse_1", "gp_eclipse_2");
                    targets(p, 0, 4, 3, 2, -3, 2, 2, -2);
                }, p -> PengAbilityHandler.eclipse(p), 8, false),
                new Scene("gp_vase_burst", p -> {
                    peng(p);
                    targets(p, 1, 4, -1, 5, 2, 6);
                    ItemStack vase = new ItemStack(ModItems.YIN_YANG_VASE.get());
                    p.setItemInHand(InteractionHand.MAIN_HAND, vase);
                    Mob husk = EntityType.HUSK.create(p.serverLevel());
                    husk.moveTo(p.getX(), p.getY(), p.getZ() + 4);
                    p.serverLevel().addFreshEntity(husk);
                    vase.interactLivingEntity(p, husk, InteractionHand.MAIN_HAND);
                }, p -> YinYangVaseItem.release(p.getMainHandItem(), p.serverLevel(),
                        p.position().add(0, 0, 4), p), 6, false),
                new Scene("yin_yang_vase_in_hand", p -> p.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.YIN_YANG_VASE.get())), p -> {}, 1, true),

                // ── Bull Demon King ──
                new Scene("bdk_wall_ram", p -> {
                    bull(p, "bdk_rush_1", "bdk_rush_2", "bdk_rush_3");
                    for (int x = -2; x <= 2; x++) for (int y = 0; y <= 1; y++) {
                        // north of him, so the camera in the south-east sees him hit it
                        p.serverLevel().setBlock(BlockPos.containing(p.getX() + x, p.getY() + y, p.getZ() - 2),
                                Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }
                    targets(p, 2, -1, -2, -1);
                }, p -> BullDemonAbilityHandler.wallImpact(p, new net.minecraft.world.phys.Vec3(0, 0, -1), 3), 6, false),
                new Scene("bdk_great_white_bull", p -> {
                    bull(p, "bdk_bull_1", "bdk_bull_2", "bdk_bull_3");
                    BullDemonAbilityHandler.toggleGreatBull(p);
                }, p -> {}, 1, false),
                new Scene("bdk_cudgel_shockwave", p -> {
                    bull(p);
                    p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MIXED_IRON_CUDGEL.get()));
                    targets(p, 0, 3, 0, 6, 1, 9);
                }, p -> BullDemonAbilityHandler.startShockwave(p, 3), 6, false),
                new Scene("bdk_fan_gust", p -> {
                    bull(p);
                    p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BANANA_LEAF_FAN.get()));
                    targets(p, 0, 3, 2, 4, -2, 4);
                }, p -> BananaLeafFanItem.fan(p), 4, false),
                new Scene("banana_leaf_fan_in_hand", p -> p.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.BANANA_LEAF_FAN.get())), p -> {}, 1, true),
                new Scene("mixed_iron_cudgel_in_hand", p -> p.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.MIXED_IRON_CUDGEL.get())), p -> {}, 1, true));
    }

    /** Makes the player the Grand Saint with these nodes, this head leading, and Qi to spend. */
    private static void saint(ServerPlayer p, int head, String... nodes) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS);
        LevelManager.initClass(p, ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        p.getPersistentData().putInt(NineSpiritsAbilityHandler.KEY_HEAD, head);
        for (String k : List.of("jttw_ns_heads_ready_at", "jttw_ns_roar_ready_at", "jttw_ns_flare_ready_at")) {
            p.getPersistentData().remove(k);
        }
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    /** Makes the player the Marshal with these nodes and Qi to spend, his abilities ready. */
    private static void marshal(ServerPlayer p, String... nodes) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        LevelManager.initClass(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        for (String k : List.of("jttw_mc_slam_ready_at", "jttw_mc_snout_ready_at", "jttw_mc_rake_next")) {
            p.getPersistentData().remove(k);
        }
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    /** Makes the player the Gold-Winged Peng with these nodes and Qi to spend, his abilities ready. */
    private static void peng(ServerPlayer p, String... nodes) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.GOLD_WINGED_PENG);
        LevelManager.initClass(p, ReincarnationOutcome.GOLD_WINGED_PENG);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        for (String k : List.of("jttw_gp_feathers_ready_at", "jttw_gp_eclipse_ready_at")) p.getPersistentData().remove(k);
        p.getCooldowns().removeCooldown(ModItems.YIN_YANG_VASE.get());
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    /** Makes the player the Bull Demon King with these nodes and Qi to spend, his abilities ready. */
    private static void bull(ServerPlayer p, String... nodes) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.BULL_DEMON_KING);
        LevelManager.initClass(p, ReincarnationOutcome.BULL_DEMON_KING);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        p.getPersistentData().remove("jttw_bdk_rush_ready_at");
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    /** A block two below the surface at this (x, z) offset, out of sight unless smelt. */
    private static void bury(ServerPlayer p, int x, int z, net.minecraft.world.level.block.Block block) {
        p.serverLevel().setBlock(BlockPos.containing(p.getX() + x, p.getY() - 2, p.getZ() + z), block.defaultBlockState(), 3);
    }

    /** Still husks at these (x, z) offsets from the player. */
    private static void targets(ServerPlayer p, int... xz) {
        for (int i = 0; i + 1 < xz.length; i += 2) {
            Mob z = EntityType.HUSK.create(p.serverLevel()); // husks do not burn in daylight
            z.moveTo(p.getX() + xz[i], p.getY(), p.getZ() + xz[i + 1], 180f, 0f);
            z.setNoAi(true);
            z.setPersistenceRequired();
            p.serverLevel().addFreshEntity(z);
        }
    }

    /** One of the ridge's dead, standing still at (dx, dz) from the player, facing them (a captain if asked). */
    /** The Saurian Demon King with these nodes and Qi to spare. */
    private static void saurian(ServerPlayer p, String... nodes) {
        p.getPersistentData().remove("jttw_skill_nodes");
        AlignmentData.markReincarnated(p, ReincarnationOutcome.SAURIAN_DEMON_KING);
        LevelManager.initClass(p, ReincarnationOutcome.SAURIAN_DEMON_KING);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        for (String k : List.of("jttw_sdk_surge_ready", "jttw_sdk_lash_ready", "jttw_sdk_whirl_ready", "jttw_sdk_wrath_ready")) {
            p.getPersistentData().remove(k);
        }
        p.removeEffect(com.jttw.engine.registry.ModEffects.FLOOD_WRATH.get());
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    /** A husk standing {@code dz} blocks ahead (south) and {@code dx} across, to be struck. */
    private static void foe(ServerPlayer p, int dx, int dz) {
        var z = net.minecraft.world.entity.EntityType.HUSK.create(p.serverLevel()); // a husk: daylight does not burn it
        if (z == null) return;
        z.moveTo(p.getX() + dx, p.getY(), p.getZ() + dz, 180f, 0f);
        z.setYHeadRot(180f);
        z.setYBodyRot(180f);
        z.setBaby(false);
        z.setPersistenceRequired();
        z.setTarget(null);
        p.serverLevel().addFreshEntity(z);
    }

    /** A soldier of Heaven's host, still, on its cloud a little above the ground, facing the camera. */
    private static void soldier(ServerPlayer p, int dx, int dz, boolean general) {
        var s = com.jttw.engine.registry.ModEntities.HEAVENLY_SOLDIER.get().create(p.serverLevel());
        if (s == null) return;
        s.moveTo(p.getX() + dx, p.getY() + 0.6, p.getZ() + dz, -40f, 0f);
        s.become(30, general, null);
        s.setNoAi(true);
        s.setYHeadRot(-40f);
        s.setYBodyRot(-40f);
        s.setPersistenceRequired();
        p.serverLevel().addFreshEntity(s);
    }

    private static void ridge(ServerPlayer p, net.minecraft.world.entity.EntityType<? extends com.jttw.engine.entity.ridge.RidgeMonster> type,
                              int dx, int dz, boolean captain) {
        var m = com.jttw.engine.entity.ridge.RidgeMonster.summon(p.serverLevel(), type,
                net.minecraft.core.BlockPos.containing(p.getX() + dx, p.getY() + (type == com.jttw.engine.registry.ModEntities.WRAITH.get() ? 1 : 0), p.getZ() + dz), captain);
        if (m == null) return;
        m.setNoAi(true);
        m.moveTo(m.getX(), m.getY(), m.getZ(), 180f, 0f);
        m.setYHeadRot(180f);
        m.setYBodyRot(180f);
        m.setPersistenceRequired();
    }

    /** Puts a player back to an ordinary state between scenes. */
    /**
     * Up in the air, flying (in survival: creative's flying is drawn as vanilla's) as {@code who}:
     * Wukong at his top speed, on his cloud; Erlang in his squall; anyone else on their own. Drawn
     * hovering (0) or under way (1).
     */
    private static void fly(ServerPlayer p, ReincarnationOutcome who, float underWay) {
        p.getPersistentData().remove("jttw_skill_nodes"); // an earlier scene's nodes
        boolean cloud = who == ReincarnationOutcome.CELESTIAL_PRIMATE;
        AlignmentData.markReincarnated(p, who);
        LevelManager.initClass(p, who);
        if (cloud) { // at his top speed: his somersault momentum at its height
            SkillTreeData.grantNode(p, com.jttw.engine.character.wukong.WukongSkillTree.FLIGHT_3);
            com.jttw.engine.flight.JTTWFlight.setFlightSpeedTier(p, 3); // as buying it sets
            topSpeed = true; // held there: standing still for the picture would lose it
        }
        com.jttw.engine.client.flight.FlightPose.posedUnderWay = underWay;
        p.setGameMode(GameType.SURVIVAL);
        com.jttw.engine.flight.JTTWFlight.setUniversalFlight(p, true);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(p.getX(), p.getY() + 2.5, p.getZ());
    }

    static void reset(ServerPlayer p) {
        keepScreen = false;
        topSpeed = false;
        if (NineSpiritsAbilityHandler.inTrueForm(p)) NineSpiritsAbilityHandler.endTrueForm(p);
        if (BullDemonAbilityHandler.inGreatBull(p)) BullDemonAbilityHandler.endGreatBull(p);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.removeAllEffects();
        p.clearFire();
        p.setGameMode(GameType.CREATIVE);
        com.jttw.engine.flight.JTTWFlight.setUniversalFlight(p, false);
        com.jttw.engine.client.flight.FlightPose.posedUnderWay = -1;
        NetworkHandler.sendToPlayer(p, new TruffleScentPacket(0, new int[0], new long[0], new int[0]));
    }
}
