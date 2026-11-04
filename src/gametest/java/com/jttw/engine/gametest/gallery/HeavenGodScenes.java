package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.heaven.HeavenMonster;
import com.jttw.engine.entity.heaven.StarLordEntity;
import com.jttw.engine.registry.HeavenRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import java.util.List;

import static com.jttw.engine.gametest.gallery.HeavenScenes.*;

/**
 * Heaven's gods and star lords in the gallery: the Nine Luminaries (each alone, all nine in a line, winding
 * up their starfall), the Great Spirit Giant, the Thunder Lord, the Lightning Mother, the Eye and the Ear
 * (each alone, winding up its blow; the Eye and the Ear side by side).
 */
final class HeavenGodScenes {

    private HeavenGodScenes() {}

    static <T extends HeavenMonster> T god(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, int variant) {
        T m = put(p, type, x, ground(p) + 0.05, z, yaw);
        if (m != null) {
            m.becomeMiniboss(45, 1);
            m.setVariant(variant);
        }
        return m;
    }

    /** One winding up its blow (held mid-pose: with no AI it never lets it go). */
    static <T extends HeavenMonster> T winding(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, int variant) {
        T m = god(p, type, x, z, yaw, variant);
        if (m != null) m.setWinding(20);
        return m;
    }

    /** A front shot of one at (0.5, 9.5), {@code tall} high, named heaven_{name}. */
    static AbilityScenes.Scene front(String name, double tall, java.util.function.Consumer<ServerPlayer> put) {
        double d = 1.6 + tall * 1.25, eye = tall * 0.62;
        return scene("heaven_" + name, put, new double[]{0.5, eye + 0.3, 9.5 - d, 0.5, eye, 9.5});
    }

    /** A three-quarter shot of one at (0.5, 9.5), {@code tall} high, named heaven_{name}. */
    static AbilityScenes.Scene quarter(String name, double tall, java.util.function.Consumer<ServerPlayer> put) {
        double d = 1.4 + tall * 1.2, eye = tall * 0.6;
        return scene("heaven_" + name, put, new double[]{0.5 - d * 0.6, eye + 0.5, 9.5 - d * 0.8, 0.5, eye, 9.5});
    }

    static List<AbilityScenes.Scene> all() {
        List<AbilityScenes.Scene> all = new java.util.ArrayList<>();
        EntityType<StarLordEntity> lord = HeavenRegistry.STAR_LORD.get();
        // the Nine Luminaries: the sun alone from every side, then each of the nine to the front and close at the face
        all.addAll(views("star_lord", 2.3, (p, yaw) -> god(p, lord, 0.5, 9.5, yaw, 0)));
        for (int i = 0; i < 9; i++) {
            int v = i;
            String n = StarLordEntity.LUMINARIES[i];
            all.add(front("star_lord_" + n + "_front", 2.3, p -> god(p, lord, 0.5, 9.5, 180f, v)));
            all.add(quarter("star_lord_" + n + "_quarter", 2.3, p -> god(p, lord, 0.5, 9.5, 180f, v)));
            all.addAll(faces("star_lord_" + n, 2.3, p -> god(p, lord, 0.5, 9.5, 180f, v)));
        }
        // Ketu's comet from the side, its tail streaming back over his shoulder
        all.add(front("star_lord_ketu_side", 2.3, p -> god(p, lord, 0.5, 9.5, 90f, StarLordEntity.KETU)));
        all.add(scene("heaven_luminaries_lineup", p -> {
            for (int i = 0; i < 9; i++) god(p, lord, -7.5 + i * 2.0, 11.5 - 0.6 * Math.abs(i - 4) * 0.5, 180f, i);
        }, new double[]{0.5, 2.3, 1.8, 0.5, 1.4, 11}));
        all.add(scene("heaven_luminaries_close_a", p -> {
            for (int i = 0; i < 5; i++) god(p, lord, -3.5 + i * 2.0, 11, 180f, i);
        }, new double[]{0.5, 2.0, 3.6, 0.5, 1.4, 11}));
        all.add(scene("heaven_luminaries_close_b", p -> {
            for (int i = 5; i < 9; i++) god(p, lord, -2.5 + (i - 5) * 2.0, 11, 180f, i);
        }, new double[]{0.5, 2.0, 4.4, 0.5, 1.4, 11}));
        all.add(scene("heaven_star_lord_windup", p -> {
            winding(p, lord, -1.5, 10.5, 180f, StarLordEntity.SUN);
            winding(p, lord, 2.5, 10.5, 200f, StarLordEntity.KETU);
        }, new double[]{0.5, 2.0, 4.0, 0.5, 1.7, 10.5}));

        // the Great Spirit Giant
        all.addAll(views("giant", 4.4, (p, yaw) -> god(p, HeavenRegistry.GREAT_SPIRIT_GIANT.get(), 0.5, 9.5, yaw, 0)));
        all.add(quarter("giant_quarter", 4.4, p -> god(p, HeavenRegistry.GREAT_SPIRIT_GIANT.get(), 0.5, 9.5, 180f, 0)));
        all.add(quarter("giant_windup", 4.4, p -> winding(p, HeavenRegistry.GREAT_SPIRIT_GIANT.get(), 0.5, 9.5, 180f, 0)));

        // the Thunder Lord and the Lightning Mother
        all.addAll(views("thunder_lord", 2.4, (p, yaw) -> god(p, HeavenRegistry.THUNDER_LORD.get(), 0.5, 9.5, yaw, 0)));
        all.add(quarter("thunder_lord_quarter", 2.4, p -> god(p, HeavenRegistry.THUNDER_LORD.get(), 0.5, 9.5, 180f, 0)));
        all.add(quarter("thunder_lord_windup", 2.4, p -> winding(p, HeavenRegistry.THUNDER_LORD.get(), 0.5, 9.5, 180f, 0)));
        all.addAll(views("lightning_mother", 2.2, (p, yaw) -> god(p, HeavenRegistry.LIGHTNING_MOTHER.get(), 0.5, 9.5, yaw, 0)));
        all.add(quarter("lightning_mother_quarter", 2.2, p -> god(p, HeavenRegistry.LIGHTNING_MOTHER.get(), 0.5, 9.5, 180f, 0)));
        all.add(quarter("lightning_mother_windup", 2.2, p -> winding(p, HeavenRegistry.LIGHTNING_MOTHER.get(), 0.5, 9.5, 180f, 0)));
        all.add(scene("heaven_thunder_court", p -> {
            god(p, HeavenRegistry.THUNDER_LORD.get(), -1.0, 10.5, 165f, 0);
            god(p, HeavenRegistry.LIGHTNING_MOTHER.get(), 2.0, 10.5, 195f, 0);
        }, new double[]{0.5, 1.9, 4.6, 0.5, 1.4, 10.5}));

        // Thousand-Mile Eye and Wind-Following Ear: each alone, winding up, then side by side
        all.addAll(views("eye", 2.2, (p, yaw) -> god(p, HeavenRegistry.THOUSAND_MILE_EYE.get(), 0.5, 9.5, yaw, 0)));
        all.add(quarter("eye_quarter", 2.2, p -> god(p, HeavenRegistry.THOUSAND_MILE_EYE.get(), 0.5, 9.5, 180f, 0)));
        all.add(quarter("eye_windup", 2.2, p -> winding(p, HeavenRegistry.THOUSAND_MILE_EYE.get(), 0.5, 9.5, 180f, 0)));
        all.addAll(views("ear", 2.2, (p, yaw) -> god(p, HeavenRegistry.WIND_FOLLOWING_EAR.get(), 0.5, 9.5, yaw, 0)));
        all.add(quarter("ear_quarter", 2.2, p -> god(p, HeavenRegistry.WIND_FOLLOWING_EAR.get(), 0.5, 9.5, 180f, 0)));
        all.add(quarter("ear_windup", 2.2, p -> winding(p, HeavenRegistry.WIND_FOLLOWING_EAR.get(), 0.5, 9.5, 180f, 0)));
        all.add(scene("heaven_eye_and_ear", p -> {
            god(p, HeavenRegistry.THOUSAND_MILE_EYE.get(), -1.0, 10.5, 165f, 0);
            god(p, HeavenRegistry.WIND_FOLLOWING_EAR.get(), 2.0, 10.5, 195f, 0);
        }, new double[]{0.5, 1.9, 4.6, 0.5, 1.4, 10.5}));
        return all;
    }
}
