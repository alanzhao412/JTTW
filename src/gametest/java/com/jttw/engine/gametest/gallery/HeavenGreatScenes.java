package com.jttw.engine.gametest.gallery;

import com.jttw.engine.registry.HeavenRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.List;

import static com.jttw.engine.gametest.gallery.HeavenScenes.*;

/** Heaven's great names in the gallery: the Four Heavenly Kings, Li Jing, Nezha, Erlang Shen. */
final class HeavenGreatScenes {

    private HeavenGreatScenes() {}

    static <T extends Mob> T great(ServerPlayer p, EntityType<T> type, double x, double z, float yaw) {
        return put(p, type, x, ground(p) + 0.05, z, yaw);
    }

    static List<AbilityScenes.Scene> all() {
        List<AbilityScenes.Scene> all = new java.util.ArrayList<>();
        all.addAll(views("king_chiguo", 3.0, (p, yaw) -> great(p, HeavenRegistry.KING_CHIGUO.get(), 0.5, 9.5, yaw)));
        all.addAll(views("king_zengzhang", 3.0, (p, yaw) -> great(p, HeavenRegistry.KING_ZENGZHANG.get(), 0.5, 9.5, yaw)));
        all.addAll(views("king_guangmu", 3.0, (p, yaw) -> great(p, HeavenRegistry.KING_GUANGMU.get(), 0.5, 9.5, yaw)));
        all.addAll(views("king_duowen", 3.0, (p, yaw) -> great(p, HeavenRegistry.KING_DUOWEN.get(), 0.5, 9.5, yaw)));
        all.addAll(views("li_jing", 2.6, (p, yaw) -> great(p, HeavenRegistry.LI_JING.get(), 0.5, 9.5, yaw)));
        all.addAll(views("nezha", 1.8, (p, yaw) -> great(p, HeavenRegistry.NEZHA.get(), 0.5, 9.5, yaw)));
        all.addAll(views("erlang", 2.2, (p, yaw) -> great(p, HeavenRegistry.ERLANG_SHEN.get(), 0.5, 9.5, yaw)));
        return all;
    }
}
