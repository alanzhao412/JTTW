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

    /** The way the player faces in the scene being shot (0 south, toward the camera's side; 180 away). */
    static volatile float facing;
    /** Set by a scene that wants the camera close on the player rather than on the ground ahead. */
    static volatile boolean closeUp;
    /** The scene holds the use key down from its trigger until it is shot (charging, as a player would). */
    static volatile boolean holdUse;
    /** The scene opens the skill tree at its trigger, to be shot. */
    static volatile boolean openSkillTree;
    /** The scene opens the character screen on its Skills tab (the class abilities) at its trigger, to be shot. */
    static volatile boolean openClassAbilities;
    /** A scene's own camera: where it stands and what it looks at, from the stage's origin at ground level; null for the usual. */
    static volatile double[] sceneCam;
    /** Played, not posed: forward (and sprint, unless {@code liveCruise}) held from the trigger, the player moving freely. */
    static volatile boolean live, liveCruise;
    /** In a live cruising scene, sprint pressed this many ticks before the picture (0: never). */
    static volatile int liveSprintAt;
    /** In a live scene, the player's own F5 camera as it is (behind, or {@code liveFront} in front), not moved alongside. */
    static volatile boolean liveF5, liveFront;

    /** Set by a scene of Wukong at his top speed: the gallery keeps his momentum at its height. */
    static volatile boolean topSpeed;

    /** Keeps a top-speed scene's momentum at its height, each tick. */
    static void holdTopSpeed(ServerPlayer p) {
        if (topSpeed) com.jttw.engine.flight.JTTWFlight.setBoost(p, com.jttw.engine.character.wukong.WukongAbilityHandler.momentumMax(), false);
    }

    /** Scenes that take longer to come right than the rest, and by how many more ticks: a fall, and its smoke clearing. */
    static final java.util.Map<String, Integer> LONG = java.util.Map.of("staff_fallen", 150, "staff_crossed", 150,
            "staff_obsidian", 150, "staff_obsidian_mighty", 150);

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
                // in creative too: Wukong at Flight I stretched out along his flight, and at Flight III on his cloud
                new Scene("flight_creative_wukong", p -> fly(p, ReincarnationOutcome.CELESTIAL_PRIMATE, 1, false, true), p -> {}, 1, false, 0f),
                new Scene("flight_creative_cloud", p -> fly(p, ReincarnationOutcome.CELESTIAL_PRIMATE, 1, true, true), p -> {}, 1, false, 0f),
                // flown as a player flies, in creative: the flight nodes bought as from the tree, forward
                // held for two seconds, nothing posed (Flight I glides; II flies; III his fastest)
                new Scene("flight_live_1", p -> liveWukong(p, 1), p -> {}, 40, false, 0f),
                new Scene("flight_live_2", p -> liveWukong(p, 2), p -> {}, 40, false, 0f),
                new Scene("flight_live_3", p -> liveWukong(p, 3), p -> {}, 40, false, 0f),
                // as F5 shows it, unmoved: from behind and in front, the staff in hand
                // cruising at Flight III, then sprint: he somersaults onto his cloud (caught mid-flip, then on it)
                new Scene("flip_a", p -> { liveWukong(p, 3); liveCruise = true; liveSprintAt = 5; }, p -> {}, 40, false, 0f),
                new Scene("flip_b", p -> { liveWukong(p, 3); liveCruise = true; liveSprintAt = 8; }, p -> {}, 40, false, 0f),
                new Scene("flip_c", p -> { liveWukong(p, 3); liveCruise = true; liveSprintAt = 11; }, p -> {}, 40, false, 0f),
                new Scene("flip_d", p -> { liveWukong(p, 3); liveCruise = true; liveSprintAt = 25; }, p -> {}, 40, false, 0f),
                new Scene("flight_live_cruise_3", p -> { liveWukong(p, 3); liveCruise = true; }, p -> {}, 40, false, 0f),
                new Scene("flight_f5_back_2", p -> { liveWukong(p, 2); liveF5 = true; staffInHand(p); }, p -> {}, 40, false, 0f),
                new Scene("flight_f5_back_3", p -> { liveWukong(p, 3); liveF5 = true; staffInHand(p); }, p -> {}, 40, false, 0f),
                new Scene("flight_f5_front_2", p -> { liveWukong(p, 2); liveF5 = true; liveFront = true; staffInHand(p); }, p -> {}, 40, false, 0f),
                new Scene("flight_f5_front_3", p -> { liveWukong(p, 3); liveF5 = true; liveFront = true; staffInHand(p); }, p -> {}, 40, false, 0f),
                new Scene("flight_live_survival_2", p -> { liveWukong(p, 2); p.setGameMode(GameType.SURVIVAL); p.getAbilities().flying = true; p.onUpdateAbilities(); }, p -> {}, 40, false, 0f),
                new Scene("flight_live_survival_3", p -> { liveWukong(p, 3); p.setGameMode(GameType.SURVIVAL); p.getAbilities().flying = true; p.onUpdateAbilities(); }, p -> {}, 40, false, 0f),
                // a Heavenly (the Marshal) with Heavenly Mobility bought, sprinting through the air: on his slower cloud, no somersault
                new Scene("flight_heavenly_cloud", p -> liveHeavenly(p, false), p -> {}, 40, false, 0f),
                new Scene("flight_heavenly_cloud_f5", p -> { liveHeavenly(p, false); liveF5 = true; }, p -> {}, 40, false, 0f),
                new Scene("flight_heavenly_cloud_survival", p -> liveHeavenly(p, true), p -> {}, 40, false, 0f),
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
                new Scene("heaven_palace_hall", p -> {
                    // inside, at the gate, looking up the hall to the throne
                    com.jttw.engine.world.heaven.HeavenFeatures.JadePalace.build(p.serverLevel(), p.blockPosition().offset(0, 0, -6), p.getRandom());
                    facing = 180f;
                }, p -> {}, 1, true, 8f),
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
                    p.openMenu(f, at);
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
                // ── Divine Beasts growing into their beasts ──
                new Scene("beast_dragon_20", p -> beast(p, ReincarnationOutcome.DRAGON, 20, -25f), p -> {}, 1, false),
                new Scene("beast_dragon_60", p -> beast(p, ReincarnationOutcome.DRAGON, 60, -25f), p -> {}, 1, false),
                new Scene("beast_dragon_80", p -> beast(p, ReincarnationOutcome.DRAGON, 80, 200f), p -> {}, 1, false),
                new Scene("beast_dragon_100", p -> beast(p, ReincarnationOutcome.DRAGON, 100, -35f), p -> {}, 1, false),
                new Scene("beast_turtle_20", p -> beast(p, ReincarnationOutcome.TURTLE, 20, -25f), p -> {}, 1, false),
                new Scene("beast_turtle_60", p -> beast(p, ReincarnationOutcome.TURTLE, 60, -25f), p -> {}, 1, false),
                new Scene("beast_turtle_80", p -> beast(p, ReincarnationOutcome.TURTLE, 80, 200f), p -> {}, 1, false),
                new Scene("beast_turtle_100", p -> beast(p, ReincarnationOutcome.TURTLE, 100, -35f), p -> {}, 1, false),
                new Scene("beast_tiger_20", p -> beast(p, ReincarnationOutcome.WHITE_TIGER, 20, -25f), p -> {}, 1, false),
                new Scene("beast_tiger_60", p -> beast(p, ReincarnationOutcome.WHITE_TIGER, 60, -25f), p -> {}, 1, false),
                new Scene("beast_tiger_80", p -> beast(p, ReincarnationOutcome.WHITE_TIGER, 80, 200f), p -> {}, 1, false),
                new Scene("beast_tiger_100", p -> beast(p, ReincarnationOutcome.WHITE_TIGER, 100, -35f), p -> {}, 1, false),
                new Scene("beast_phoenix_20", p -> beast(p, ReincarnationOutcome.PHOENIX, 20, -25f), p -> {}, 1, false),
                new Scene("beast_phoenix_60", p -> beast(p, ReincarnationOutcome.PHOENIX, 60, -25f), p -> {}, 1, false),
                new Scene("beast_phoenix_80", p -> beast(p, ReincarnationOutcome.PHOENIX, 80, 200f), p -> {}, 1, false),
                new Scene("beast_phoenix_100", p -> beast(p, ReincarnationOutcome.PHOENIX, 100, -35f), p -> {}, 1, false),
                // ── Armaments on the body ──
                new Scene("armaments_nezha_back", p -> armed(p, ReincarnationOutcome.THIRD_LOTUS_PRINCE, 200f), p -> {}, 1, false),
                new Scene("armaments_nezha_front", p -> armed(p, ReincarnationOutcome.THIRD_LOTUS_PRINCE, -20f), p -> {}, 1, false),
                new Scene("armaments_erlang_back", p -> armed(p, ReincarnationOutcome.EYE_OF_HEAVEN, 200f), p -> {}, 1, false),
                new Scene("armaments_erlang_front", p -> armed(p, ReincarnationOutcome.EYE_OF_HEAVEN, -20f), p -> {}, 1, false),
                new Scene("xtq_howl", p -> {}, p -> {
                    // a howl crossing in front of the camera, from left to right
                    var howl = new com.jttw.engine.entity.HowlProjectileEntity(com.jttw.engine.registry.ModEntities.HOWL_PROJECTILE.get(), p.level());
                    howl.setPos(p.getX() - 1, p.getY() + 1.4, p.getZ() + 5);
                    howl.shoot(0.6, 0, -0.8, 0.25f, 0); // across the camera's view
                    howl.setNoGravity(true);
                    p.level().addFreshEntity(howl);
                }, 6, false),
                // the Ruyi Jingu Bang grows smoothly: caught three ticks into growing from 1 to 6, and once grown
                new Scene("staff_growing", AbilityScenes::staff, p -> grow(p), 3, false),
                new Scene("staff_grown", AbilityScenes::staff, p -> grow(p), 40, false),
                // the staff fallen: lying in its crater; two crossing, one on the other; a tall one from below
                new Scene("staff_fallen", p -> plantAt(p, -3, 4, 6f), p -> fell(p, -90f), 195, false),
                new Scene("staff_crossed", p -> { plantAt(p, -4, 7, 5f); plantAt(p, 0, 2, 5f); }, p -> fellEach(p, -90f, 0f), 195, false),
                new Scene("staff_tall_below", p -> plantAt(p, 0, 2, 30f), p -> {}, 1, true, -70f),
                new Scene("staff_tall_far", p -> plantAt(p, 0, 2, 30f), p -> {}, 1, false),
                // toppled into an obsidian wall: the weak cannot pay to break it and it rests leaning on it;
                // the mighty go through
                new Scene("staff_obsidian", p -> obsidianWall(p, 1), p -> fell(p, 90f), 195, false),
                new Scene("staff_obsidian_mighty", p -> obsidianWall(p, 100), p -> fell(p, 90f), 195, false),
                // looking at his own standing staff beneath an obsidian ceiling: how tall it is, how tall it can grow
                new Scene("staff_growth_meter", p -> {
                    for (int dx = -3; dx <= 3; dx++) for (int dz = -1; dz <= 5; dz++)
                        p.level().setBlockAndUpdate(p.blockPosition().offset(dx, 14, dz), Blocks.OBSIDIAN.defaultBlockState());
                    plantAt(p, 0, 2, 4f);
                    facing = -14f; // onto it: it stands half a block east of straight ahead
                }, p -> {}, 1, true, -25f),
                // Byakko smells the wounded: the husk on the left at half health glows (for him alone)
                // through the wall; the healthy one on the right does not
                new Scene("byakko_instinct", p -> {
                    p.getPersistentData().remove("jttw_skill_nodes");
                    AlignmentData.markReincarnated(p, ReincarnationOutcome.WHITE_TIGER);
                    LevelManager.initClass(p, ReincarnationOutcome.WHITE_TIGER);
                    SkillTreeData.grantNode(p, "bk_instinct_1");
                    foe(p, 2, 7);
                    foe(p, -2, 7);
                    p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, p.getBoundingBox().inflate(12))
                            .stream().filter(z -> z.getX() > p.getX()).forEach(z -> z.setHealth(z.getMaxHealth() * 0.4f));
                    for (int dx = -4; dx <= 4; dx++) for (int dy = 0; dy < 4; dy++)
                        p.level().setBlockAndUpdate(p.blockPosition().offset(dx, dy, 5), Blocks.STONE.defaultBlockState());
                }, p -> {}, 1, true),
                // the Phoenix a sun: from its own eyes (the white-gold rim), and from outside (its ring of reach)
                new Scene("solar_flare", p -> { phoenix(p, "ph_radiance_1", "ph_radiance_2", "ph_radiance_3"); foe(p, -3, 6); foe(p, 3, 7); },
                        p -> com.jttw.engine.character.phoenix.SolarFlare.cast(p), 30, true),
                new Scene("solar_flare_outside", p -> { phoenix(p, "ph_radiance_1", "ph_radiance_2", "ph_radiance_3"); foe(p, -3, 6); foe(p, 3, 7); },
                        p -> com.jttw.engine.character.phoenix.SolarFlare.cast(p), 30, false),
                // the Turtle has taken 24 in blows: its Tectonic Slam key shows the +12 it would add
                new Scene("stored_tremor", p -> {
                    p.getPersistentData().remove("jttw_skill_nodes");
                    AlignmentData.markReincarnated(p, ReincarnationOutcome.TURTLE);
                    LevelManager.initClass(p, ReincarnationOutcome.TURTLE);
                    for (String n : List.of("tu_shell_1", "tu_force_1", "tu_tectonic_1", "tu_gravity_1", "tu_tremor_1")) SkillTreeData.grantNode(p, n);
                    LevelManager.reapplyStats(p);
                    p.setGameMode(GameType.SURVIVAL); // the skill bar shows outside creative
                    LevelManager.syncToClient(p); // the skill bar, as a player's would show it
                }, p -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                        com.jttw.engine.gametest.GT.damage(p, p.damageSources().generic(), 24f)), 10, true),
                // the Ruyi Jingu Bang, plain, in hand: as he sees it, and as others do
                new Scene("staff_base_hand", p -> plainStaff(p), p -> {}, 1, true, 10f),
                new Scene("staff_base_held", p -> { plainStaff(p); closeUp = true; facing = -40f; }, p -> {}, 1, false),
                // at ease, the staff across his shoulders (from the side and from behind); and mid-blow, in hand
                new Scene("staff_rest_side", p -> { plainStaff(p); closeUp = true; facing = -70f; }, p -> {}, 1, false),
                new Scene("staff_rest_back", p -> { plainStaff(p); closeUp = true; facing = 150f; }, p -> {}, 1, false),
                new Scene("staff_rest_front", p -> { plainStaff(p); closeUp = true; facing = -20f; }, p -> {}, 1, false),
                new Scene("staff_swing", p -> { plainStaff(p); closeUp = true; facing = -70f; },
                        p -> p.swing(InteractionHand.MAIN_HAND, true), 3, false),
                // the Dragon's skill tree, nine columns: the window keeps its size and the tree scrolls
                new Scene("skill_tree_dragon", p -> {
                    p.getPersistentData().remove("jttw_skill_nodes");
                    AlignmentData.markReincarnated(p, ReincarnationOutcome.DRAGON);
                    LevelManager.initClass(p, ReincarnationOutcome.DRAGON);
                    p.setGameMode(GameType.SURVIVAL);
                    LevelManager.syncToClient(p);
                    openSkillTree = true;
                    keepScreen = true;
                }, p -> {}, 10, true),
                // the Marshal's class abilities at level 40: one learned, two to buy, the rest still locked
                new Scene("class_abilities_heavenly", p -> {
                    p.getPersistentData().remove("jttw_abilities");
                    AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
                    LevelManager.initClass(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
                    p.setGameMode(GameType.SURVIVAL);
                    com.jttw.engine.progression.level.PlayerLevel.setLevel(p, 40);
                    SkillTreeData.setPoints(p, 2);
                    com.jttw.engine.progression.skilltree.SkillPurchase.purchaseClassAbility(p, com.jttw.engine.flight.JTTWFlight.HEAVENLY_MOBILITY);
                    LevelManager.syncToClient(p);
                    openClassAbilities = true;
                    keepScreen = true;
                }, p -> {}, 10, true),
                // a transformation: the puff of cloud as he becomes a horse, seen from outside
                new Scene("transform_puff", p -> {},
                        p -> com.jttw.engine.transformation.TransformationManager.transform(p, net.minecraft.world.entity.EntityType.HORSE), 3, false),
                // Byakko's true form, the White Tiger: from the side, from in front, through its own eyes,
                // and the moment of its taking
                // the Azure Dragon, the Dragon's true form: from the side, from in front, its head
                new Scene("dragon_side", p -> { beast(p, ReincarnationOutcome.DRAGON, "dr_true"); facing = -90f; sceneCam = new double[]{-7.5, 7.0, 27.5, -7.5, 6.0, 0.5}; },
                        p -> trueForm(p, com.jttw.engine.registry.ModEntities.AZURE_DRAGON.get(), "dr_true"), 45, false),
                new Scene("dragon_front", p -> { beast(p, ReincarnationOutcome.DRAGON, "dr_true"); facing = -60f; sceneCam = new double[]{21.5, 8.0, 17.5, -3.5, 4.8, 0.5}; },
                        p -> trueForm(p, com.jttw.engine.registry.ModEntities.AZURE_DRAGON.get(), "dr_true"), 45, false),
                new Scene("dragon_face", p -> { beast(p, ReincarnationOutcome.DRAGON, "dr_true"); facing = -90f; sceneCam = new double[]{14.5, 8.0, 7.5, 3.5, 6.6, 0.5}; },
                        p -> trueForm(p, com.jttw.engine.registry.ModEntities.AZURE_DRAGON.get(), "dr_true"), 45, false),
                // the Black Tortoise, the World Turtle's true form
                new Scene("tortoise_side", p -> { beast(p, ReincarnationOutcome.TURTLE, "tu_true"); facing = -90f; sceneCam = new double[]{0.5, 9.0, 29.5, 0.5, 4.4, 0.5}; },
                        p -> trueForm(p, com.jttw.engine.registry.ModEntities.BLACK_TORTOISE.get(), "tu_true"), 45, false),
                new Scene("tortoise_front", p -> { beast(p, ReincarnationOutcome.TURTLE, "tu_true"); facing = -60f; sceneCam = new double[]{21.5, 10.0, 17.5, 0.5, 4.8, 0.5}; },
                        p -> trueForm(p, com.jttw.engine.registry.ModEntities.BLACK_TORTOISE.get(), "tu_true"), 45, false),
                // the Vermilion Bird, the Phoenix's true form
                new Scene("bird_front", p -> { beast(p, ReincarnationOutcome.PHOENIX, "ph_true"); facing = -60f; sceneCam = new double[]{25.5, 10.0, 21.5, 0.5, 8.0, 0.5}; },
                        p -> trueForm(p, com.jttw.engine.registry.ModEntities.VERMILION_BIRD.get(), "ph_true"), 45, false),
                new Scene("bird_close", p -> { beast(p, ReincarnationOutcome.PHOENIX, "ph_true"); facing = -90f; sceneCam = new double[]{10.0, 8.0, 12.0, 0.5, 7.0, 0.5}; },
                        p -> trueForm(p, com.jttw.engine.registry.ModEntities.VERMILION_BIRD.get(), "ph_true"), 45, false),
                new Scene("dragon_eye", p -> { beast(p, ReincarnationOutcome.DRAGON, "dr_true"); facing = -90f; sceneCam = new double[]{1.5, 6.5, 9.0, 1.5, 6.0, -10.0}; },
                        p -> trueForm(p, com.jttw.engine.registry.ModEntities.AZURE_DRAGON.get(), "dr_true"), 45, false),
                new Scene("bird_eye", p -> { beast(p, ReincarnationOutcome.PHOENIX, "ph_true"); facing = -90f; sceneCam = new double[]{6.0, 10.5, 2.0, 7.5, 9.5, -9.0}; },
                        p -> trueForm(p, com.jttw.engine.registry.ModEntities.VERMILION_BIRD.get(), "ph_true"), 45, false),
                new Scene("bird_side", p -> { beast(p, ReincarnationOutcome.PHOENIX, "ph_true"); facing = -90f; sceneCam = new double[]{0.5, 10.0, 33.5, 0.5, 8.0, 0.5}; },
                        p -> trueForm(p, com.jttw.engine.registry.ModEntities.VERMILION_BIRD.get(), "ph_true"), 45, false),
                new Scene("dragon_f5", p -> { beast(p, ReincarnationOutcome.DRAGON, "dr_true"); p.setGameMode(GameType.SURVIVAL); live = true; liveF5 = true; liveCruise = true; },
                        p -> com.jttw.engine.character.divinebeast.TrueForm.toggle(p, com.jttw.engine.registry.ModEntities.AZURE_DRAGON.get(), 3, "dr_true"), 40, false, 0f),
                new Scene("dragon_first", p -> { beast(p, ReincarnationOutcome.DRAGON, "dr_true"); p.setGameMode(GameType.SURVIVAL); },
                        p -> com.jttw.engine.character.divinebeast.TrueForm.toggle(p, com.jttw.engine.registry.ModEntities.AZURE_DRAGON.get(), 3, "dr_true"), 40, true, 0f),
                new Scene("tortoise_f5", p -> { beast(p, ReincarnationOutcome.TURTLE, "tu_true"); p.setGameMode(GameType.SURVIVAL); live = true; liveF5 = true; liveCruise = true; },
                        p -> com.jttw.engine.character.divinebeast.TrueForm.toggle(p, com.jttw.engine.registry.ModEntities.BLACK_TORTOISE.get(), 3, "tu_true"), 40, false, 0f),
                new Scene("tortoise_first", p -> { beast(p, ReincarnationOutcome.TURTLE, "tu_true"); p.setGameMode(GameType.SURVIVAL); },
                        p -> com.jttw.engine.character.divinebeast.TrueForm.toggle(p, com.jttw.engine.registry.ModEntities.BLACK_TORTOISE.get(), 3, "tu_true"), 40, true, 0f),
                new Scene("bird_f5", p -> { beast(p, ReincarnationOutcome.PHOENIX, "ph_true"); p.setGameMode(GameType.SURVIVAL); live = true; liveF5 = true; liveCruise = true; },
                        p -> com.jttw.engine.character.divinebeast.TrueForm.toggle(p, com.jttw.engine.registry.ModEntities.VERMILION_BIRD.get(), 3, "ph_true"), 40, false, 0f),
                new Scene("bird_first", p -> { beast(p, ReincarnationOutcome.PHOENIX, "ph_true"); p.setGameMode(GameType.SURVIVAL); },
                        p -> com.jttw.engine.character.divinebeast.TrueForm.toggle(p, com.jttw.engine.registry.ModEntities.VERMILION_BIRD.get(), 3, "ph_true"), 40, true, 0f),
                new Scene("tiger_f5", p -> { beast(p, ReincarnationOutcome.WHITE_TIGER, "bk_true"); p.setGameMode(GameType.SURVIVAL); live = true; liveF5 = true; liveCruise = true; },
                        p -> com.jttw.engine.character.divinebeast.TrueForm.toggle(p, com.jttw.engine.registry.ModEntities.WHITE_TIGER.get(), 3, "bk_true"), 40, false, 0f),
                new Scene("tiger_first", p -> { beast(p, ReincarnationOutcome.WHITE_TIGER, "bk_true"); p.setGameMode(GameType.SURVIVAL); },
                        p -> com.jttw.engine.character.divinebeast.TrueForm.toggle(p, com.jttw.engine.registry.ModEntities.WHITE_TIGER.get(), 3, "bk_true"), 40, true, 0f),
                new Scene("tiger_side", p -> { byakko(p); facing = -90f; sceneCam = new double[]{0.5, 2.0, 11.0, 0.5, 1.6, 0.5}; }, p -> tiger(p), 45, false),
                new Scene("tiger_front", p -> { byakko(p); facing = -50f; sceneCam = new double[]{7.5, 2.0, 8.0, 0.5, 1.6, 0.5}; }, p -> tiger(p), 45, false),
                new Scene("tiger_paws", p -> { byakko(p); facing = -90f; sceneCam = new double[]{6.5, 1.2, 3.5, 2.8, 0.3, 0.5}; }, p -> tiger(p), 45, false),
                new Scene("tiger_stare", p -> { byakko(p); facing = -90f; sceneCam = new double[]{8.5, 2.8, 0.5, 3.6, 2.8, 0.5}; }, p -> tiger(p), 45, false),
                new Scene("tiger_face", p -> { byakko(p); facing = -90f; sceneCam = new double[]{7.0, 2.9, 2.2, 3.6, 2.7, 0.5}; }, p -> tiger(p), 45, false),
                new Scene("tiger_first", p -> byakko(p), p -> tiger(p), 20, true),
                new Scene("tiger_taking", p -> { byakko(p); facing = -60f; }, p -> tiger(p), 2, false),
                // a Qi Wave half charged: the meter above the crosshair
                new Scene("charge_meter", p -> {
                    empoweredStaff(p);
                    holdUse = true;
                }, p -> {}, 35, true),
                // the mod's tough stone, column by column
                new Scene("tough_blocks", p -> {
                    var blocks = java.util.List.of(com.jttw.engine.registry.ModBlocks.BLEACHED_STONE, com.jttw.engine.registry.ModBlocks.GRAVE_TUFF,
                            com.jttw.engine.registry.ModBlocks.BONE_ANDESITE, com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE,
                            com.jttw.engine.registry.ModBlocks.BONE_GRAVEL, com.jttw.engine.registry.ModBlocks.CELESTIAL_MARBLE,
                            com.jttw.engine.registry.ModBlocks.UNDERWORLD_STONE, com.jttw.engine.registry.ModBlocks.SOUL_ASH);
                    for (int i = 0; i < blocks.size(); i++) for (int dy = 0; dy < 2; dy++)
                        p.level().setBlockAndUpdate(p.blockPosition().offset(i - 4, dy, 3), blocks.get(i).get().defaultBlockState());
                }, p -> {}, 1, false),
                // the Ruyi Jingu Bang in hand, Qi-empowered: a gold glint, not the enchantment's purple
                new Scene("staff_empowered", p -> {
                    empoweredStaff(p);
                }, p -> {}, 1, true, 20f),
                new Scene("staff_empowered_side", p -> {
                    empoweredStaff(p);
                    closeUp = true;
                    facing = -40f;
                }, p -> {}, 1, false),
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
                // four ticks: on 1.21 the riptide pose reaches the client a tick later than it did
                new Scene("saurian_surge", p -> saurian(p, com.jttw.engine.character.saurian.SaurianSkillTree.SURGE_1),
                        p -> com.jttw.engine.character.saurian.SaurianAbilityHandler.surge(p, 1f), 4, false),
                new Scene("heaven_host", p -> {
                    soldier(p, -2, 2, false);
                    soldier(p, 1, 4, true);
                    soldier(p, -4, 6, false);
                }, p -> {}, 1, false),
                // Lady White Bone hiding among her dead: as seen, and as truth-seeing eyes see her
                new Scene("lady_disguised", p -> disguisedLady(p, false), p -> {}, 1, false),
                new Scene("lady_mirage", p -> disguisedLady(p, true), p -> {}, 1, false),
                new Scene("lady_truth_sight", p -> {
                    // through the Fiery Golden Eyes: her true name and health, though she wears a servant's shape
                    var lady = com.jttw.engine.registry.ModEntities.LADY_WHITE_BONE.get().create(p.serverLevel());
                    if (lady == null) return;
                    lady.moveTo(p.getX(), p.getY(), p.getZ() + 5, 180f, 0f);
                    lady.setDisguise(com.jttw.engine.entity.ridge.LadyWhiteBoneEntity.SERVANT);
                    lady.setNoAi(true);
                    lady.setYHeadRot(180f);
                    lady.setYBodyRot(180f);
                    p.serverLevel().addFreshEntity(lady);
                    com.jttw.engine.client.renderer.GoldenEyesRenderer.setMode(p.getUUID(), 1);
                }, p -> {}, 1, true),
                new Scene("lady_husks", p -> {
                    for (int i = 0; i < 3; i++) {
                        var body = com.jttw.engine.registry.ModEntities.LADY_HUSK.get().create(p.serverLevel());
                        if (body == null) continue;
                        body.moveTo(p.getX() - 2 + i * 2, p.getY(), p.getZ() + 3, 150f + i * 20f, 0f);
                        body.setMortal(i);
                        p.serverLevel().addFreshEntity(body);
                    }
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
                // the Great White Bull close: its shape from the side, its head, its horns
                new Scene("bull_side", p -> { bull(p, "bdk_bull_1", "bdk_bull_2", "bdk_bull_3"); facing = -90f; sceneCam = new double[]{0.5, 2.0, 10.0, 0.5, 1.6, 0.5}; },
                        BullDemonAbilityHandler::toggleGreatBull, 45, false),
                new Scene("bull_front", p -> { bull(p, "bdk_bull_1", "bdk_bull_2", "bdk_bull_3"); facing = -50f; sceneCam = new double[]{7.5, 2.0, 8.0, 0.5, 1.6, 0.5}; },
                        BullDemonAbilityHandler::toggleGreatBull, 45, false),
                new Scene("bull_face", p -> { bull(p, "bdk_bull_1", "bdk_bull_2", "bdk_bull_3"); facing = -90f; sceneCam = new double[]{5.5, 2.0, 1.6, 2.0, 1.5, 0.5}; },
                        BullDemonAbilityHandler::toggleGreatBull, 45, false),
                new Scene("bull_rear", p -> { bull(p, "bdk_bull_1", "bdk_bull_2", "bdk_bull_3"); facing = -90f; sceneCam = new double[]{-6.0, 2.6, 6.5, 0.5, 1.4, 0.5}; },
                        BullDemonAbilityHandler::toggleGreatBull, 45, false),
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
    /** A Divine Beast of {@code who}'s line at {@code level}, seen close, facing {@code yaw}. */
    private static void beast(ServerPlayer p, ReincarnationOutcome who, int level, float yaw) {
        AlignmentData.markReincarnated(p, who);
        LevelManager.initClass(p, who);
        com.jttw.engine.progression.level.PlayerLevel.setLevel(p, level);
        facing = yaw;
        closeUp = true;
    }

    /** {@code who} with every one of their armaments in their armory, none in hand, facing {@code yaw}. */
    private static void armed(ServerPlayer p, ReincarnationOutcome who, float yaw) {
        AlignmentData.markReincarnated(p, who);
        LevelManager.initClass(p, who);
        var armory = com.jttw.engine.item.Armory.of(p);
        var slots = com.jttw.engine.item.Armory.slots(p);
        for (int i = 0; i < slots.size(); i++) armory.setItem(i, slots.get(i).forged().get());
        facing = yaw;
        closeUp = true;
    }

    /** Wukong, strong enough to hold it, with the Ruyi Jingu Bang Qi-empowered in hand. */
    /** Wukong in creative, his flight bought to {@code tier} through the skill tree as a player buys it, flying, to be flown live. */
    private static void liveWukong(ServerPlayer p, int tier) {
        p.getPersistentData().remove("jttw_skill_nodes");
        com.jttw.engine.flight.JTTWFlight.setUniversalFlight(p, false);
        com.jttw.engine.flight.JTTWFlight.setBoost(p, 1, false);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        SkillTreeData.addPoints(p, 50);
        String[] flight = {com.jttw.engine.character.wukong.WukongSkillTree.FLIGHT_1,
                com.jttw.engine.character.wukong.WukongSkillTree.FLIGHT_2, com.jttw.engine.character.wukong.WukongSkillTree.FLIGHT_3};
        for (int i = 0; i < tier; i++) com.jttw.engine.progression.skilltree.SkillPurchase.purchase(p, flight[i]);
        p.setGameMode(GameType.CREATIVE);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(p.getX(), p.getY() + 6, p.getZ());
        live = true;
    }

    /** The Marshal at level 10, Heavenly Mobility bought with his points as from the character screen, flying, to be flown live. */
    private static void liveHeavenly(ServerPlayer p, boolean survival) {
        p.getPersistentData().remove("jttw_skill_nodes");
        p.getPersistentData().remove("jttw_abilities");
        com.jttw.engine.flight.JTTWFlight.setUniversalFlight(p, false);
        com.jttw.engine.flight.JTTWFlight.setBoost(p, 1, false);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        LevelManager.initClass(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        com.jttw.engine.progression.level.PlayerLevel.setLevel(p, 10);
        SkillTreeData.addPoints(p, 2);
        com.jttw.engine.progression.skilltree.SkillPurchase.purchaseClassAbility(p, com.jttw.engine.flight.JTTWFlight.HEAVENLY_MOBILITY);
        p.setGameMode(survival ? GameType.SURVIVAL : GameType.CREATIVE);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(p.getX(), p.getY() + 6, p.getZ());
        live = true;
    }

    private static void staffInHand(ServerPlayer p) {
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.jttw.engine.registry.ModItems.RUYI_JINGU_BANG.get()));
    }

    /** A divine beast, its true form ({@code prefix}_true) learned to its height, rested and with Qi to spare. */
    private static void beast(ServerPlayer p, ReincarnationOutcome who, String prefix) {
        p.getPersistentData().remove("jttw_skill_nodes");
        p.getPersistentData().remove(com.jttw.engine.character.divinebeast.TrueForm.KEY_READY);
        p.removeEffect(com.jttw.engine.registry.ModEffects.EXHAUSTED);
        AlignmentData.markReincarnated(p, who);
        LevelManager.initClass(p, who);
        for (int t = 1; t <= 3; t++) SkillTreeData.grantNode(p, prefix + "_" + t);
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    private static void trueForm(ServerPlayer p, net.minecraft.world.entity.EntityType<?> form, String id) {
        com.jttw.engine.character.divinebeast.TrueForm.toggle(p, form, 3, id);
        // a villager beside it, for its size
        var v = net.minecraft.world.entity.EntityType.VILLAGER.create(p.serverLevel());
        if (v != null) {
            v.moveTo(p.getX() + 5, p.getY(), p.getZ() + 3, 200f, 0f);
            v.setNoAi(true);
            v.setYHeadRot(200f);
            p.serverLevel().addFreshEntity(v);
        }
    }

    /** Byakko, his White Tiger learned to its height, rested and with Qi to spare. */
    private static void byakko(ServerPlayer p) {
        p.getPersistentData().remove("jttw_skill_nodes");
        p.getPersistentData().remove(com.jttw.engine.character.divinebeast.TrueForm.KEY_READY);
        p.removeEffect(com.jttw.engine.registry.ModEffects.EXHAUSTED);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.WHITE_TIGER);
        LevelManager.initClass(p, ReincarnationOutcome.WHITE_TIGER);
        for (String n : List.of("bk_true_1", "bk_true_2", "bk_true_3")) SkillTreeData.grantNode(p, n);
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    private static void tiger(ServerPlayer p) {
        com.jttw.engine.character.divinebeast.TrueForm.toggle(p, com.jttw.engine.registry.ModEntities.WHITE_TIGER.get(), 3, "bk_true");
    }

    private static void plainStaff(ServerPlayer p) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.jttw.engine.registry.ModItems.RUYI_JINGU_BANG.get()));
    }

    private static void empoweredStaff(ServerPlayer p) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        com.jttw.engine.progression.level.PlayerLevel.setLevel(p, 60);
        LevelManager.reapplyStats(p);
        var stack = new ItemStack(com.jttw.engine.registry.ModItems.RUYI_JINGU_BANG.get());
        com.jttw.engine.item.StackData.putBoolean(stack, com.jttw.engine.item.RuyiJinguBangItem.TAG_QI_EMPOWERED, true);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    /** An obsidian wall two blocks west, and a staff planted two blocks east to fall into it, felled by a Wukong of this level. */
    private static void obsidianWall(ServerPlayer p, int level) {
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        com.jttw.engine.progression.level.PlayerLevel.setLevel(p, level);
        var l = p.serverLevel();
        for (int dz = 0; dz <= 8; dz++) for (int dy = 0; dy < 8; dy++)
            l.setBlockAndUpdate(p.blockPosition().offset(-2, dy, dz), Blocks.OBSIDIAN.defaultBlockState());
        plantAt(p, 2, 4, 6f);
    }

    /** A Ruyi Jingu Bang of the player's planted {@code dx}, {@code dz} from them, at {@code scale}. */
    private static void plantAt(ServerPlayer p, int dx, int dz, float scale) {
        var staff = new com.jttw.engine.entity.PlacedStaffEntity(p.level(), p.getX() + dx + 0.5, p.getY(), p.getZ() + dz + 0.5, p.getUUID());
        staff.setStaffScale(scale);
        p.level().addFreshEntity(staff);
    }

    /** Topples the staffs by the camera, facing {@code yaw}. */
    private static void fell(ServerPlayer p, float yaw) {
        p.setYRot(yaw);
        p.serverLevel().getEntitiesOfClass(com.jttw.engine.entity.PlacedStaffEntity.class, p.getBoundingBox().inflate(12))
                .forEach(s -> s.toppleBy(p));
    }

    /** Topples the staffs by the camera in turn, the first facing the first yaw, the next the next. */
    private static void fellEach(ServerPlayer p, float... yaws) {
        var staffs = p.serverLevel().getEntitiesOfClass(com.jttw.engine.entity.PlacedStaffEntity.class, p.getBoundingBox().inflate(12));
        staffs.sort(java.util.Comparator.comparingDouble(s -> s.getZ()));
        for (int i = 0; i < staffs.size() && i < yaws.length; i++) {
            p.setYRot(yaws[yaws.length - 1 - i]);
            staffs.get(i).toppleBy(p);
        }
    }

    /** A Ruyi Jingu Bang standing on the ground before the camera, at its smallest. */
    private static void staff(ServerPlayer p) {
        var staff = new com.jttw.engine.entity.PlacedStaffEntity(p.level(), p.getX() + 1, p.getY(), p.getZ() + 3, p.getUUID());
        staff.setStaffScale(1f);
        p.level().addFreshEntity(staff);
    }

    /** Grows the staff by the camera to six times its size at once. */
    private static void grow(ServerPlayer p) {
        p.serverLevel().getEntitiesOfClass(com.jttw.engine.entity.PlacedStaffEntity.class, p.getBoundingBox().inflate(8))
                .forEach(s -> s.setStaffScale(6f));
    }

    /** The Saurian Demon King with these nodes and Qi to spare. */
    private static void phoenix(ServerPlayer p, String... nodes) {
        p.getPersistentData().remove("jttw_skill_nodes");
        p.getPersistentData().remove("jttw_ph_flare_ready");
        AlignmentData.markReincarnated(p, ReincarnationOutcome.PHOENIX);
        LevelManager.initClass(p, ReincarnationOutcome.PHOENIX);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
    }

    private static void saurian(ServerPlayer p, String... nodes) {
        p.getPersistentData().remove("jttw_skill_nodes");
        AlignmentData.markReincarnated(p, ReincarnationOutcome.SAURIAN_DEMON_KING);
        LevelManager.initClass(p, ReincarnationOutcome.SAURIAN_DEMON_KING);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        for (String k : List.of("jttw_sdk_surge_ready", "jttw_sdk_lash_ready", "jttw_sdk_whirl_ready", "jttw_sdk_wrath_ready")) {
            p.getPersistentData().remove(k);
        }
        p.removeEffect(com.jttw.engine.registry.ModEffects.FLOOD_WRATH);
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

    /**
     * A Bone Servant Captain on the left, and on the right Lady White Bone passing for one; with
     * {@code eyes}, the Fiery Golden Eyes are open, and her true form shows over her disguise.
     */
    private static void disguisedLady(ServerPlayer p, boolean eyes) {
        ridge(p, com.jttw.engine.registry.ModEntities.BONE_SERVANT.get(), -4, 4, true);
        var lady = com.jttw.engine.registry.ModEntities.LADY_WHITE_BONE.get().create(p.serverLevel());
        if (lady == null) return;
        lady.moveTo(p.getX() + 1, p.getY(), p.getZ() + 4, 180f, 0f);
        lady.setDisguise(com.jttw.engine.entity.ridge.LadyWhiteBoneEntity.SERVANT_CAPTAIN);
        lady.setNoAi(true);
        lady.setYHeadRot(180f);
        lady.setYBodyRot(180f);
        p.serverLevel().addFreshEntity(lady);
        com.jttw.engine.client.renderer.GoldenEyesRenderer.setMode(p.getUUID(), eyes ? 1 : 0);
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
        fly(p, who, underWay, who == ReincarnationOutcome.CELESTIAL_PRIMATE, false);
    }

    /** As {@link #fly(ServerPlayer, ReincarnationOutcome, float)}, with or without his cloud, in creative or survival. */
    private static void fly(ServerPlayer p, ReincarnationOutcome who, float underWay, boolean cloud, boolean creative) {
        p.getPersistentData().remove("jttw_skill_nodes"); // an earlier scene's nodes
        AlignmentData.markReincarnated(p, who);
        LevelManager.initClass(p, who);
        if (cloud) { // at his top speed: his somersault momentum at its height
            SkillTreeData.grantNode(p, com.jttw.engine.character.wukong.WukongSkillTree.FLIGHT_3);
            com.jttw.engine.flight.JTTWFlight.setFlightSpeedTier(p, 3); // as buying it sets
            topSpeed = true; // held there: standing still for the picture would lose it
        }
        com.jttw.engine.client.flight.FlightPose.posedUnderWay = underWay;
        if (!cloud) com.jttw.engine.flight.JTTWFlight.setFlightSpeedTier(p, 1);
        p.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        com.jttw.engine.flight.JTTWFlight.setUniversalFlight(p, true);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(p.getX(), p.getY() + 2.5, p.getZ());
    }

    static void reset(ServerPlayer p) {
        keepScreen = false;
        topSpeed = false;
        com.jttw.engine.client.renderer.GoldenEyesRenderer.setMode(p.getUUID(), 0);
        facing = 0;
        closeUp = false;
        holdUse = false;
        openSkillTree = false;
        openClassAbilities = false;
        sceneCam = null;
        if (p.getPersistentData().contains(com.jttw.engine.character.divinebeast.TrueForm.KEY_UNTIL)) {
            com.jttw.engine.character.divinebeast.TrueForm.end(p);
            p.removeEffect(com.jttw.engine.registry.ModEffects.EXHAUSTED);
            p.removeAllEffects();
        }
        live = false;
        liveF5 = false;
        liveCruise = false;
        liveSprintAt = 0;
        liveFront = false;
        com.jttw.engine.client.flight.FlightPose.posedUnderWay = -1;
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
