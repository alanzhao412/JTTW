package com.jttw.engine.entity.heaven;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * One of the Twenty-Eight Lunar Mansions (二十八宿), the star generals of the four quarters of the sky,
 * each with the beast of his mansion for his emblem: Heaven's captains. One body for the twenty-eight,
 * armoured in his quarter's colour (the Azure Dragon's east in green, the Black Tortoise's north in black,
 * the White Tiger's west in white, the Vermilion Bird's south in red), his beast on his helm and shield
 * ({@link #variant()}, {@link #MANSIONS}). The world brings one as a captain leading a band of the host,
 * three to five levels over it: fourteen ordinary hits to kill, his blow a captain's (13.2%).
 */
public class LunarMansionEntity extends HeavenMonster {

    /** Seven ordinary hits as a regular (a captain twice it); a blow of 11%, a captain's 13.2%. */
    public static final double HITS = 7, BLOW = 0.11;

    /**
     * The twenty-eight, east to south, each its id (its star, its element-day, its beast): the Azure
     * Dragon's seven (east), the Black Tortoise's (north), the White Tiger's (west), the Vermilion Bird's (south).
     */
    public static final String[] MANSIONS = {
            "jiao_flood_dragon", "kang_dragon", "di_badger", "fang_hare", "xin_fox", "wei_tiger", "ji_leopard",
            "dou_xiezhi", "niu_ox", "nu_bat", "xu_rat", "wei_swallow", "shi_boar", "bi_porcupine",
            "kui_wolf", "lou_hound", "wei_pheasant", "mao_rooster", "bi_crow", "zi_monkey", "shen_ape",
            "jing_wild_dog", "gui_goat", "liu_river_deer", "xing_horse", "zhang_deer", "yi_serpent", "zhen_worm"};

    /** The quarter of mansion {@code v}: 0 east (Azure Dragon), 1 north (Black Tortoise), 2 west (White Tiger), 3 south (Vermilion Bird). */
    public static int quarter(int v) {
        return Math.floorMod(v, MANSIONS.length) / 7;
    }

    public LunarMansionEntity(EntityType<? extends LunarMansionEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(HITS, BLOW, 0.32, 0.62);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BLOW;
    }

    @Override
    public int variants() {
        return MANSIONS.length;
    }

    /** A star general goes by his mansion's name, captain or not. */
    @Override
    protected boolean namedRank() {
        return true;
    }

    @Override
    protected Component kindName() {
        return Component.translatable("entity.jttw_engine.lunar_mansion." + MANSIONS[variant()]);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.ARMOR_EQUIP_IRON.value();
    }
}
