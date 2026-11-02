package com.jttw.engine.entity.heaven;

import com.jttw.engine.world.Zones;
import com.jttw.engine.world.heaven.HeavenHostility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * One of Heaven's two champions, Nezha and Erlang Shen, whom players may also be: lesser great names of
 * the Jade Palace (its band's top and two, 102), each seventy ordinary hits of a level-100 character.
 *
 * <p><b>When a player is the same character</b>: the champion will not fight himself. He never takes that
 * player for his foe nor joins a fight against them, his blows pass over them, and their blows pass through
 * him (he says so, now and then: {@link #refusal()}). Every other player he fights as all Heaven's host
 * does. (So an outlawed Erlang Shen is hunted by all Heaven but its Erlang Shen.)
 */
public abstract class HeavenChampion extends HeavenBoss {

    /** How often he says his refusal to the same player, at most. */
    public static final int REFUSAL_EVERY = 100;

    private final Map<UUID, Long> refused = new HashMap<>();

    protected HeavenChampion(EntityType<? extends HeavenChampion> type, Level level) {
        super(type, level);
    }

    /** Whether {@code p} plays the same character as he is. */
    public abstract boolean isHimself(Player p);

    /** What he says to his own self. */
    protected abstract Component refusal();

    @Override
    public Zones.Zone zone() {
        return Zones.HEAVEN_JADE_PALACE;
    }

    @Override
    public boolean lesser() {
        return true;
    }

    @Override
    public int crime() {
        return HeavenHostility.CHAMPION;
    }

    /** He spares his own self: the player who plays him. */
    @Override
    public Predicate<Player> allies() {
        return this::isHimself;
    }

    /** His own self's blows pass through him. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && isHimself(p)) {
            if (!level().isClientSide && p instanceof ServerPlayer sp) {
                long now = level().getGameTime();
                Long last = refused.get(p.getUUID());
                if (last == null || now - last >= REFUSAL_EVERY) {
                    refused.put(p.getUUID(), now);
                    sp.displayClientMessage(refusal(), true);
                }
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Whoever plays Erlang Shen (the Eye of Heaven). */
    public static final Predicate<Player> ERLANG = p -> com.jttw.engine.character.Characters.of(p)
            == com.jttw.engine.character.erlang.ErlangShenCharacter.INSTANCE;
    /** Whoever plays Nezha (the Third Lotus Prince). */
    public static final Predicate<Player> NEZHA = p -> com.jttw.engine.character.nezha.NezhaCharacter.is(p);
}
