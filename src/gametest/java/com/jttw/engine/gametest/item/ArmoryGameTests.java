package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.nezha.NezhaAbilityHandler;
import com.jttw.engine.item.Armaments;
import com.jttw.engine.item.Armory;
import com.jttw.engine.item.YinYangSwordItem;
import com.jttw.engine.registry.ModAttributes;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/** A character's armory: a slot for each of their armaments, all of it equipped at once, kept through death. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ArmoryGameTests {

    private static void put(ServerPlayer p, ItemStack armament) {
        var armory = Armory.of(p);
        var slots = Armory.slots(p);
        for (int i = 0; i < slots.size(); i++) if (slots.get(i).is().test(armament)) armory.setItem(i, armament);
    }

    @GameTest(template = PLATFORM)
    public static void eachCharacterHasASlotForEachOfTheirArmaments(GameTestHelper h) {
        ServerPlayer nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        ServerPlayer wukong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        check(h, Armory.slots(nezha).size() > Armory.slots(erlang).size(), "Nezha has the most");
        eq(h, 0, Armory.slots(wukong).size(), "Wukong has none");
        var armory = Armory.of(nezha);
        check(h, armory.canPlaceItem(0, new ItemStack(ModItems.FIRE_TIP_SPEAR.get())), "the spear's slot takes the spear");
        check(h, !armory.canPlaceItem(0, new ItemStack(ModItems.QIANKUN_RING.get())), "and nothing else");
        check(h, !armory.canPlaceItem(0, new ItemStack(ModItems.THREE_POINTED_SPEAR.get())), "not even Erlang's spear");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void itKeepsWhatIsPutInIt(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        ItemStack axe = new ItemStack(ModItems.ERLANG_AXE.get());
        Armaments.setRefine(axe, 3);
        put(p, axe);
        ItemStack kept = Armory.find(p, "erlang_axe");
        check(h, kept.is(ModItems.ERLANG_AXE.get()) && Armaments.refine(kept) == 3, "read back, refinement and all");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void cyclingPutsWhatIsInHandAwayWithoutLosingIt(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        put(p, new ItemStack(ModItems.THREE_POINTED_SPEAR.get()));
        p.getInventory().items.set(p.getInventory().selected, new ItemStack(Items.DIRT, 7));
        Armory.cycle(p);
        check(h, p.getMainHandItem().is(ModItems.THREE_POINTED_SPEAR.get()), "the spear in hand");
        eq(h, 7, p.getInventory().countItem(Items.DIRT), "the dirt in his inventory, all of it");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wornArmamentsWorkFromTheArmory(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        double mres = p.getAttributeValue(ModAttributes.MAGIC_RESISTANCE);
        put(p, new ItemStack(ModItems.HUNTIAN_CLOTH.get()));
        put(p, new ItemStack(ModItems.WIND_FIRE_WHEELS.get()));
        NezhaAbilityHandler.onPlayerTick(new PlayerTickEvent.Post(p));
        check(h, p.getAttributeValue(ModAttributes.MAGIC_RESISTANCE) > mres, "the sash's magic resistance, unworn");
        check(h, p.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().stream().anyMatch(m -> m.id().equals(com.jttw.engine.JTTWEngine.id("wheels_speed"))), "the wheels' speed");
        check(h, p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).isEmpty(), "his armour slots stay free");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theMoreArmamentsTheStronger(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        p.tickCount = 20;
        Armory.onPlayerTick(new PlayerTickEvent.Post(p));
        double bare = p.getAttributeValue(Attributes.MAX_HEALTH);
        put(p, new ItemStack(ModItems.FIRE_TIP_SPEAR.get()));
        ItemStack ring = new ItemStack(ModItems.QIANKUN_RING.get());
        Armaments.setRefine(ring, 4);
        put(p, ring);
        Armory.onPlayerTick(new PlayerTickEvent.Post(p));
        near(h, bare * (1 + 2 * 0.06 + 4 * 0.005), p.getAttributeValue(Attributes.MAX_HEALTH), "6% an armament, 0.5% a refinement level");
        near(h, 0.06 * 6 + 0.005 * 6 * 10, Armory.bonus(List.of(
                full(ModItems.FIRE_TIP_SPEAR.get()), full(ModItems.QIANKUN_RING.get()), fullSword(true), fullSword(false),
                full(ModItems.WIND_FIRE_WHEELS.get()), full(ModItems.HUNTIAN_CLOTH.get()))), "Nezha's full armory, fully refined: +66%");
        h.succeed();
    }

    private static ItemStack full(net.minecraft.world.item.Item item) {
        ItemStack s = new ItemStack(item);
        Armaments.setRefine(s, Armaments.MAX_REFINE);
        return s;
    }

    private static ItemStack fullSword(boolean yin) {
        ItemStack s = yin ? YinYangSwordItem.createYin() : YinYangSwordItem.createYang();
        Armaments.setRefine(s, Armaments.MAX_REFINE);
        return s;
    }

    @GameTest(template = PLATFORM)
    public static void hisArmamentsGoBackToHisArmoryWhenHeDies(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        List<ItemEntity> drops = new ArrayList<>();
        drops.add(new ItemEntity(h.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(ModItems.ERLANG_AXE.get())));
        drops.add(new ItemEntity(h.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(Items.DIRT)));
        LivingDropsEvent event = new LivingDropsEvent(p, h.getLevel().damageSources().generic(), drops, true);
        Armory.onDrops(event);
        eq(h, 1, event.getDrops().size(), "only the dirt drops");
        check(h, Armory.find(p, "erlang_axe").is(ModItems.ERLANG_AXE.get()), "the axe is in his armory");
        ServerPlayer reborn = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        Armory.onClone(new PlayerEvent.Clone(reborn, p, true));
        check(h, Armory.find(reborn, "erlang_axe").is(ModItems.ERLANG_AXE.get()), "and still there after he respawns");
        h.succeed();
    }
}
