package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.heaven.HeavenMonster;
import com.jttw.engine.registry.HeavenRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import java.util.List;

import static com.jttw.engine.gametest.gallery.HeavenScenes.*;

/** Heaven's gods and star lords in the gallery: the Nine Luminaries, the Great Spirit Giant, the Thunder Lord, the Lightning Mother, the Eye and the Ear. */
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

    static List<AbilityScenes.Scene> all() {
        List<AbilityScenes.Scene> all = new java.util.ArrayList<>();
        all.addAll(views("star_lord", 2.2, (p, yaw) -> god(p, HeavenRegistry.STAR_LORD.get(), 0.5, 9.5, yaw, 0)));
        all.addAll(views("giant", 4.2, (p, yaw) -> god(p, HeavenRegistry.GREAT_SPIRIT_GIANT.get(), 0.5, 9.5, yaw, 0)));
        all.addAll(views("thunder_lord", 2.3, (p, yaw) -> god(p, HeavenRegistry.THUNDER_LORD.get(), 0.5, 9.5, yaw, 0)));
        all.addAll(views("lightning_mother", 2.1, (p, yaw) -> god(p, HeavenRegistry.LIGHTNING_MOTHER.get(), 0.5, 9.5, yaw, 0)));
        all.addAll(views("eye", 2.1, (p, yaw) -> god(p, HeavenRegistry.THOUSAND_MILE_EYE.get(), 0.5, 9.5, yaw, 0)));
        all.addAll(views("ear", 2.1, (p, yaw) -> god(p, HeavenRegistry.WIND_FOLLOWING_EAR.get(), 0.5, 9.5, yaw, 0)));
        return all;
    }
}
