package com.jttw.engine.reincarnation;

import com.jttw.engine.JTTWEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * Awards alignment points from gameplay actions (before first reincarnation).
 *
 * <p><b>Balance target:</b> ~300 points each for Heavenly, Spirit Beast, and Yaoguai
 * in a typical pre-Nether playthrough. Primordial is intentionally harder (end-game).
 *
 * <p>Typical playthrough estimates:
 * <ul>
 *   <li>~200 logs mined, ~30 iron ore, ~10 diamond ore → Spirit Beast ~310</li>
 *   <li>~150 undead killed → Heavenly ~300</li>
 *   <li>~50 animals killed, ~250 food eaten → Yaoguai ~350</li>
 *   <li>Primordial requires ancient debris, boss kills, exploration</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class AlignmentEarningHandler {

    private AlignmentEarningHandler() {}

    // =====================================================================
    // HEAVENLY — killing undead
    // =====================================================================

    // Undead mobs: Zombie, Skeleton, Husk, Drowned, Stray, Phantom,
    //              Wither Skeleton, Zombie Villager, etc.
    // All extend Monster and have MobType.UNDEAD

    // =====================================================================
    // SPIRIT BEAST — mining wood, iron, diamond
    // =====================================================================

    // No stone — too common, would completely dominate alignment.

    // =====================================================================
    // PRIMORDIAL — exploration, ancient debris, boss kills
    // =====================================================================

    // Intentionally the hardest to accumulate. Requires end-game content.

    // =====================================================================
    // YAOGUAI — killing passive mobs, villagers, iron golems, eating food
    // =====================================================================

    // -----------------------------------------------------------------------
    // Block breaking: Spirit Beast + Primordial
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        if (AlignmentData.hasReincarnated(sp)) return;

        BlockState state = event.getState();

        // -- Spirit Beast --

        // Logs: ~200 mined × 1 = ~200
        if (state.is(BlockTags.LOGS)) {
            AlignmentData.addSpiritBeast(sp, 1);
            return;
        }

        // Iron ore: ~30 mined × 3 = ~90
        if (state.is(BlockTags.IRON_ORES)) {
            AlignmentData.addSpiritBeast(sp, 3);
            return;
        }

        // Diamond ore: ~10 mined × 5 = ~50
        if (state.is(BlockTags.DIAMOND_ORES)) {
            AlignmentData.addSpiritBeast(sp, 5);
            return;
        }

        // -- Primordial --

        // Ancient debris: rare, ~4 found × 15 = ~60
        if (state.is(Blocks.ANCIENT_DEBRIS)) {
            AlignmentData.addPrimordial(sp, 15);
        }
    }

    // -----------------------------------------------------------------------
    // Advancement discovery: Primordial
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (AlignmentData.hasReincarnated(sp)) return;

        String id = event.getAdvancement().id().toString();

        // Structure/exploration advancements
        if (id.contains("adventure/") || id.contains("nether/") || id.contains("end/")) {
            AlignmentData.addPrimordial(sp, 5);
        }

        // Major milestones worth extra
        if (id.contains("adventuring_time") || id.contains("find_bastion")
                || id.contains("find_fortress") || id.contains("enter_the_end")) {
            AlignmentData.addPrimordial(sp, 10);
        }
    }

    // -----------------------------------------------------------------------
    // Entity kills: Heavenly, Yaoguai, Primordial
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (AlignmentData.hasReincarnated(sp)) return;

        // -- Primordial: boss kills --

        // Warden: +200
        if (victim.getType() == EntityType.WARDEN) {
            AlignmentData.addPrimordial(sp, 200);
        }

        // Ender Dragon: +200
        if (victim.getType() == EntityType.ENDER_DRAGON) {
            AlignmentData.addPrimordial(sp, 200);
        }

        // A Dragon King, a boss of the Four Seas: +50 (all four: 200, as one Warden)
        if (victim instanceof com.jttw.engine.entity.waters.DragonKingEntity) {
            AlignmentData.addPrimordial(sp, 50);
        }

        // -- Heavenly: killing undead --
        // ~150 undead × 2 = ~300
        // (undead is the entity-type tag now; the mod's own undead answer isInvertedHealAndHarm)
        if (victim instanceof Monster monster && (monster.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)
                || monster.isInvertedHealAndHarm())) {
            AlignmentData.addHeavenly(sp, 2);
            return;
        }

        // -- Yaoguai: killing innocents --

        // Passive animals: ~50 × 2 = ~100
        if (victim instanceof Animal) {
            AlignmentData.addYaoguai(sp, 2);
        }

        // Villagers: rare, ~3 × 15 = ~45
        if (victim instanceof AbstractVillager) {
            AlignmentData.addYaoguai(sp, 15);
        }

        // Iron golems: rare, ~2 × 10 = ~20
        if (victim instanceof IronGolem) {
            AlignmentData.addYaoguai(sp, 10);
        }
    }

    // -----------------------------------------------------------------------
    // Eating food: Yaoguai
    // -----------------------------------------------------------------------

    // ~250 food eaten × 1 = ~250
    @SubscribeEvent
    public static void onEatFood(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (AlignmentData.hasReincarnated(sp)) return;

        ItemStack stack = event.getItem();
        if (stack.getFoodProperties(sp) == null) return;

        AlignmentData.addYaoguai(sp, 1);
    }
}
