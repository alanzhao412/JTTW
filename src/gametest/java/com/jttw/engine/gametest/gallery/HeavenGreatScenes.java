package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.heaven.HeavenBoss;
import com.jttw.engine.registry.HeavenRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.List;

import static com.jttw.engine.gametest.gallery.HeavenScenes.*;

/**
 * Heaven's great names in the gallery: the Four Heavenly Kings (each alone, the four in a line, the four
 * winding up), Li Jing, Nezha on his wheels, Erlang Shen with his hound; each alone from the front, the
 * side, the back and at the face, and winding up his great blow.
 */
final class HeavenGreatScenes {

    private HeavenGreatScenes() {}

    static <T extends Mob> T great(ServerPlayer p, EntityType<T> type, double x, double z, float yaw) {
        return put(p, type, x, ground(p) + 0.05, z, yaw);
    }

    /** One of them put still in his wind-up ({@code ticks} of it left). */
    static <T extends Mob> T winding(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, int ticks) {
        T m = great(p, type, x, z, yaw);
        if (m instanceof HeavenBoss b) b.setWinding(ticks);
        return m;
    }

    private static List<EntityType<? extends Mob>> kings() {
        return List.of(HeavenRegistry.KING_CHIGUO.get(), HeavenRegistry.KING_ZENGZHANG.get(),
                HeavenRegistry.KING_GUANGMU.get(), HeavenRegistry.KING_DUOWEN.get());
    }

    /** A creature winding up, seen from the front a little to one side (its height {@code tall}). */
    private static AbilityScenes.Scene windUp(String name, double tall, EntityType<? extends Mob> type) {
        double d = 1.2 + tall * 1.05, eye = tall * 0.66;
        return scene("heaven_" + name + "_windup", p -> winding(p, type, 0.5, 9.5, 180f, 10),
                new double[]{0.5 - d * 0.25, eye + 0.3, 9.5 - d, 0.5, eye, 9.5});
    }

    static List<AbilityScenes.Scene> all() {
        List<AbilityScenes.Scene> all = new java.util.ArrayList<>();
        String[] names = {"chiguo", "zengzhang", "guangmu", "duowen"};
        List<EntityType<? extends Mob>> kings = kings();
        for (int k = 0; k < 4; k++) {
            EntityType<? extends Mob> type = kings.get(k);
            all.addAll(views("king_" + names[k], 3.0, (p, yaw) -> great(p, type, 0.5, 9.5, yaw)));
            all.add(windUp("king_" + names[k], 3.0, type));
        }
        // the four in a line, East, South, West, North; and the four winding up
        all.add(scene("heaven_king_lineup", p -> {
            for (int k = 0; k < 4; k++) great(p, kings.get(k), -4.0 + 3.0 * k, 10.5, 180f);
        }, new double[]{0.5, 2.3, 2.2, 0.5, 1.7, 10.5}));
        all.add(scene("heaven_king_lineup_windup", p -> {
            for (int k = 0; k < 4; k++) winding(p, kings.get(k), -4.0 + 3.0 * k, 10.5, 180f, 10);
        }, new double[]{0.5, 2.3, 2.2, 0.5, 1.7, 10.5}));
        // the four from a three-quarter view
        all.add(scene("heaven_king_lineup_quarter", p -> {
            for (int k = 0; k < 4; k++) great(p, kings.get(k), -4.0 + 3.0 * k, 10.5, 150f);
        }, new double[]{-4.5, 2.6, 3.5, 0.5, 1.6, 10.5}));

        all.addAll(views("li_jing", 2.6, (p, yaw) -> great(p, HeavenRegistry.LI_JING.get(), 0.5, 9.5, yaw)));
        all.add(windUp("li_jing", 2.6, HeavenRegistry.LI_JING.get()));
        all.addAll(views("nezha", 1.8, (p, yaw) -> great(p, HeavenRegistry.NEZHA.get(), 0.5, 9.5, yaw)));
        all.add(windUp("nezha", 1.8, HeavenRegistry.NEZHA.get()));
        // Nezha's wheels, close and low
        all.add(scene("heaven_nezha_wheels", p -> great(p, HeavenRegistry.NEZHA.get(), 0.5, 9.5, 135f),
                new double[]{-0.9, 0.55, 7.6, 0.5, 0.35, 9.5}));
        all.addAll(views("erlang", 2.2, (p, yaw) -> great(p, HeavenRegistry.ERLANG_SHEN.get(), 0.5, 9.5, yaw)));
        all.add(windUp("erlang", 2.2, HeavenRegistry.ERLANG_SHEN.get()));
        // Erlang with his hound at his heel, from three-quarters
        all.add(scene("heaven_erlang_hound", p -> great(p, HeavenRegistry.ERLANG_SHEN.get(), 0.5, 9.5, 145f),
                new double[]{-2.4, 1.8, 5.2, 0.5, 1.0, 9.5}));
        // the great names together: the Four Kings behind, Li Jing, Nezha and Erlang before them
        all.add(scene("heaven_great_all", p -> {
            for (int k = 0; k < 4; k++) great(p, kings.get(k), -4.0 + 3.0 * k, 13.5, 180f);
            great(p, HeavenRegistry.LI_JING.get(), 0.5, 10.5, 180f);
            great(p, HeavenRegistry.NEZHA.get(), -2.0, 9.5, 190f);
            great(p, HeavenRegistry.ERLANG_SHEN.get(), 3.0, 9.5, 170f);
        }, new double[]{0.5, 2.6, 2.4, 0.5, 1.8, 11.5}));
        return all;
    }
}
