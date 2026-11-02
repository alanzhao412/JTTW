package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.heaven.HeavenMonster;
import com.jttw.engine.registry.HeavenRegistry;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import java.util.List;

import static com.jttw.engine.gametest.gallery.HeavenScenes.*;

/** Heaven's rank and file in the gallery: the halberdier, the archer, the cloud cavalry, the thunder drummer, the Lunar Mansions. */
final class HeavenHostScenes {

    private HeavenHostScenes() {}

    static <T extends HeavenMonster> T one(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, boolean captain, int variant) {
        T m = put(p, type, x, ground(p) + 0.05, z, yaw);
        if (m != null) {
            m.become(captain ? 25 : 20, captain);
            m.setVariant(variant);
        }
        return m;
    }

    static List<AbilityScenes.Scene> all() {
        List<AbilityScenes.Scene> all = new java.util.ArrayList<>();
        all.addAll(views("soldier", 2.0, (p, yaw) -> one(p, ModEntities.HEAVENLY_SOLDIER.get(), 0.5, 9.5, yaw, false, 0)));
        all.addAll(views("soldier_captain", 2.0, (p, yaw) -> one(p, ModEntities.HEAVENLY_SOLDIER.get(), 0.5, 9.5, yaw, true, 0)));
        all.addAll(views("archer", 2.0, (p, yaw) -> one(p, HeavenRegistry.HEAVENLY_ARCHER.get(), 0.5, 9.5, yaw, false, 0)));
        all.addAll(views("archer_captain", 2.0, (p, yaw) -> one(p, HeavenRegistry.HEAVENLY_ARCHER.get(), 0.5, 9.5, yaw, true, 0)));
        // drawing his bow, as he does while he fights
        all.addAll(views("archer_draw", 2.0, (p, yaw) -> {
            var m = one(p, HeavenRegistry.HEAVENLY_ARCHER.get(), 0.5, 9.5, yaw, false, 0);
            if (m != null) m.setAggressive(true);
        }));
        all.addAll(views("cavalry", 2.6, (p, yaw) -> one(p, HeavenRegistry.CLOUD_CAVALRY.get(), 0.5, 9.5, yaw, false, 0)));
        all.addAll(views("cavalry_captain", 2.6, (p, yaw) -> one(p, HeavenRegistry.CLOUD_CAVALRY.get(), 0.5, 9.5, yaw, true, 0)));
        all.addAll(views("cavalry_windup", 2.6, (p, yaw) -> {
            var m = one(p, HeavenRegistry.CLOUD_CAVALRY.get(), 0.5, 9.5, yaw, false, 0);
            if (m != null) m.setWinding(1000);
        }));
        all.addAll(views("drummer", 2.0, (p, yaw) -> one(p, HeavenRegistry.THUNDER_DRUMMER.get(), 0.5, 9.5, yaw, false, 0)));
        all.addAll(views("drummer_captain", 2.0, (p, yaw) -> one(p, HeavenRegistry.THUNDER_DRUMMER.get(), 0.5, 9.5, yaw, true, 0)));
        all.addAll(views("drummer_windup", 2.0, (p, yaw) -> {
            var m = one(p, HeavenRegistry.THUNDER_DRUMMER.get(), 0.5, 9.5, yaw, false, 0);
            if (m != null) m.setWinding(1000);
        }));
        // a Lunar Mansion of each quarter alone (the Tail Tiger, the Ox, the Wolf, the Horse)
        int[] shown = {5, 8, 14, 24};
        for (int q = 0; q < 4; q++) {
            int v = shown[q];
            all.addAll(views("mansion_q" + q, 2.1, (p, yaw) -> one(p, HeavenRegistry.LUNAR_MANSION.get(), 0.5, 9.5, yaw, true, v)));
        }
        // the twenty-eight: a quarter a row, and each quarter's seven close
        all.add(scene("heaven_mansions_lineup", p -> {
            for (int v = 0; v < 28; v++) {
                one(p, HeavenRegistry.LUNAR_MANSION.get(), -6.5 + (v % 7) * 2.2, 7.0 + (v / 7) * 3.0, 180f, true, v);
            }
        }, new double[]{0.5, 9.0, -1.5, 0.5, 0.5, 11.5}));
        for (int q = 0; q < 4; q++) {
            int quarter = q;
            all.add(scene("heaven_mansions_row" + q, p -> {
                for (int i = 0; i < 7; i++) {
                    one(p, HeavenRegistry.LUNAR_MANSION.get(), -5.5 + i * 2.0, 9.5, 180f, true, quarter * 7 + i);
                }
            }, new double[]{0.5, 2.0, 2.6, 0.5, 1.4, 9.5}));
        }
        all.add(scene("heaven_host_line", p -> {
            one(p, ModEntities.HEAVENLY_SOLDIER.get(), -4.5, 10.5, 180f, false, 0);
            one(p, HeavenRegistry.HEAVENLY_ARCHER.get(), -1.5, 10.5, 180f, false, 0);
            one(p, HeavenRegistry.THUNDER_DRUMMER.get(), 1.5, 10.5, 180f, false, 0);
            one(p, HeavenRegistry.CLOUD_CAVALRY.get(), 5.0, 11.0, 200f, false, 0);
        }, new double[]{0.5, 2.2, 3.0, 0.5, 1.4, 10.5}));
        return all;
    }
}
